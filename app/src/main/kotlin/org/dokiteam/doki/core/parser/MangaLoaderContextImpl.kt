package org.dokiteam.doki.core.parser

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import org.dokiteam.doki.core.exceptions.InteractiveActionRequiredException
import org.dokiteam.doki.core.image.BitmapDecoderCompat
import org.dokiteam.doki.core.network.MangaHttpClient
import org.dokiteam.doki.core.network.cookies.MutableCookieJar
import org.dokiteam.doki.core.network.webview.ContinuationResumeWebViewClient
import org.dokiteam.doki.core.prefs.SourceSettings
import org.dokiteam.doki.core.util.ext.configureForParser
import org.dokiteam.doki.core.util.ext.printStackTraceDebug
import org.dokiteam.doki.core.util.ext.sanitizeHeaderValue
import org.dokiteam.doki.core.util.ext.toList
import org.dokiteam.doki.core.util.ext.toMimeType
import org.dokiteam.doki.core.util.ext.use
import org.dokiteam.doki.parsers.MangaLoaderContext
import org.dokiteam.doki.parsers.MangaParser
import org.dokiteam.doki.parsers.bitmap.Bitmap
import org.dokiteam.doki.parsers.config.MangaSourceConfig
import org.dokiteam.doki.parsers.model.MangaSource
import org.dokiteam.doki.parsers.network.UserAgents
import org.dokiteam.doki.parsers.util.map
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class MangaLoaderContextImpl @Inject constructor(
	@MangaHttpClient override val httpClient: OkHttpClient,
	override val cookieJar: MutableCookieJar,
	@ApplicationContext private val androidContext: Context,
) : MangaLoaderContext() {

	private var webViewCached: WeakReference<WebView>? = null
	private val webViewUserAgent by lazy { obtainWebViewUserAgent() }
	private val jsMutex = Mutex()
	private val jsTimeout = TimeUnit.SECONDS.toMillis(4)

	@Deprecated("Provide a base url")
	@SuppressLint("SetJavaScriptEnabled")
	override suspend fun evaluateJs(script: String): String? = evaluateJs("", script)

	override suspend fun evaluateJs(baseUrl: String, script: String): String? = withTimeout(jsTimeout) {
		jsMutex.withLock {
			withContext(Dispatchers.Main.immediate) {
				val webView = obtainWebView()
				if (baseUrl.isNotEmpty()) {
					suspendCoroutine { cont ->
						webView.webViewClient = ContinuationResumeWebViewClient(cont)
						webView.loadDataWithBaseURL(baseUrl, " ", "text/html", null, null)
					}
				}
				suspendCoroutine { cont ->
					webView.evaluateJavascript(script) { result ->
						cont.resume(result?.takeUnless { it == "null" })
					}
				}
			}
		}
	}

	override fun getDefaultUserAgent(): String = webViewUserAgent

	override fun getConfig(source: MangaSource): MangaSourceConfig {
		return SourceSettings(androidContext, source)
	}

	override fun encodeBase64(data: ByteArray): String {
		return Base64.encodeToString(data, Base64.NO_WRAP)
	}

	override fun decodeBase64(data: String): ByteArray {
		return Base64.decode(data, Base64.DEFAULT)
	}

	override fun getPreferredLocales(): List<Locale> {
		return LocaleListCompat.getAdjustedDefault().toList()
	}

	override fun requestBrowserAction(
		parser: MangaParser,
		url: String,
	): Nothing = throw InteractiveActionRequiredException(parser.source, url)

	override fun redrawImageResponse(response: Response, redraw: (image: Bitmap) -> Bitmap): Response {
		return response.map { body ->
			BitmapDecoderCompat.decode(body.byteStream(), body.contentType()?.toMimeType(), isMutable = true)
				.use { bitmap ->
					(redraw(BitmapWrapper.create(bitmap)) as BitmapWrapper).use { result ->
						Buffer().also {
							result.compressTo(it.outputStream())
						}.asResponseBody("image/jpeg".toMediaType())
					}
				}
		}
	}

	override fun createBitmap(width: Int, height: Int): Bitmap = BitmapWrapper.create(width, height)

	@MainThread
	private fun obtainWebView(): WebView = webViewCached?.get() ?: WebView(androidContext).also {
		it.configureForParser(null)
		webViewCached = WeakReference(it)
	}

	private fun obtainWebViewUserAgent(): String {
		val mainDispatcher = Dispatchers.Main.immediate
		return if (!mainDispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
			obtainWebViewUserAgentImpl()
		} else {
			runBlocking(mainDispatcher) {
				obtainWebViewUserAgentImpl()
			}
		}
	}

	@MainThread
	private fun obtainWebViewUserAgentImpl() = runCatching {
		obtainWebView().settings.userAgentString.sanitizeHeaderValue()
	}.onFailure { e ->
		e.printStackTraceDebug()
	}.getOrDefault(UserAgents.FIREFOX_MOBILE)
}
