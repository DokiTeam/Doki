package org.dokiteam.doki.core.network.imageproxy

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.dokiteam.doki.core.prefs.AppSettings
import org.dokiteam.doki.core.prefs.observeAsStateFlow
import org.dokiteam.doki.core.util.ext.processLifecycleScope
import org.dokiteam.doki.parsers.util.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealImageProxyInterceptor @Inject constructor(
	private val settings: AppSettings,
) : ImageProxyInterceptor {

	private val delegate = settings.observeAsStateFlow(
		scope = processLifecycleScope + Dispatchers.Default,
		key = AppSettings.KEY_IMAGES_PROXY,
		valueProducer = { createDelegate() },
	)

	override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
		return delegate.value?.intercept(chain) ?: chain.proceed()
	}

	override suspend fun interceptPageRequest(request: Request, okHttp: OkHttpClient): Response {
		return delegate.value?.interceptPageRequest(request, okHttp) ?: okHttp.newCall(request).await()
	}

	private fun createDelegate(): ImageProxyInterceptor? = when (val proxy = settings.imagesProxy) {
		-1 -> null
		0 -> WsrvNlProxyInterceptor()
		1 -> ZeroMsProxyInterceptor()
		else -> error("Unsupported images proxy $proxy")
	}
}
