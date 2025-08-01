package org.dokiteam.doki

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.StrictMode
import androidx.core.content.edit
import androidx.fragment.app.strictmode.FragmentStrictMode
import leakcanary.LeakCanary
import org.dokiteam.doki.core.BaseApp
import org.dokiteam.doki.local.data.LocalMangaRepository
import org.dokiteam.doki.local.data.PageCache
import org.dokiteam.doki.parsers.MangaLoaderContext
import org.dokiteam.doki.reader.domain.PageLoader

class DokiApp : BaseApp() {

	var isLeakCanaryEnabled: Boolean
		get() = getDebugPreferences(this).getBoolean(KEY_LEAK_CANARY, true)
		set(value) {
			getDebugPreferences(this).edit { putBoolean(KEY_LEAK_CANARY, value) }
			configureLeakCanary()
		}

	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(base)
		enableStrictMode()
		configureLeakCanary()
	}

	private fun configureLeakCanary() {
		LeakCanary.config = LeakCanary.config.copy(
			dumpHeap = isLeakCanaryEnabled,
		)
	}

	private fun enableStrictMode() {
		val notifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			StrictModeNotifier(this)
		} else {
			null
		}
		StrictMode.setThreadPolicy(
			StrictMode.ThreadPolicy.Builder().apply {
				detectNetwork()
				detectDiskWrites()
				detectCustomSlowCalls()
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) detectUnbufferedIo()
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) detectResourceMismatches()
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) detectExplicitGc()
				penaltyLog()
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && notifier != null) {
					penaltyListener(notifier.executor, notifier)
				}
			}.build(),
		)
		StrictMode.setVmPolicy(
			StrictMode.VmPolicy.Builder().apply {
				detectActivityLeaks()
				detectLeakedSqlLiteObjects()
				detectLeakedClosableObjects()
				detectLeakedRegistrationObjects()
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					detectContentUriWithoutPermission()
				}
				detectFileUriExposure()
				setClassInstanceLimit(LocalMangaRepository::class.java, 1)
				setClassInstanceLimit(PageCache::class.java, 1)
				setClassInstanceLimit(MangaLoaderContext::class.java, 1)
				setClassInstanceLimit(PageLoader::class.java, 1)
				penaltyLog()
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && notifier != null) {
					penaltyListener(notifier.executor, notifier)
				}
			}.build(),
		)
		FragmentStrictMode.defaultPolicy = FragmentStrictMode.Policy.Builder().apply {
			detectWrongFragmentContainer()
			detectFragmentTagUsage()
			detectRetainInstanceUsage()
			detectSetUserVisibleHint()
			detectWrongNestedHierarchy()
			detectFragmentReuse()
			penaltyLog()
			if (notifier != null) {
				penaltyListener(notifier)
			}
		}.build()
	}

	private companion object {

		const val PREFS_DEBUG = "_debug"
		const val KEY_LEAK_CANARY = "leak_canary"

		fun getDebugPreferences(context: Context): SharedPreferences =
			context.getSharedPreferences(PREFS_DEBUG, MODE_PRIVATE)
	}
}
