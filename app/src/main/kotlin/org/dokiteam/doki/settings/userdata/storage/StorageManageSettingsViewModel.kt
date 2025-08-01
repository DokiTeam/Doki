package org.dokiteam.doki.settings.userdata.storage

import android.annotation.SuppressLint
import android.webkit.WebStorage
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewFeature
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import okhttp3.Cache
import org.dokiteam.doki.R
import org.dokiteam.doki.core.network.cookies.MutableCookieJar
import org.dokiteam.doki.core.parser.MangaDataRepository
import org.dokiteam.doki.core.prefs.AppSettings
import org.dokiteam.doki.core.ui.BaseViewModel
import org.dokiteam.doki.core.ui.util.ReversibleAction
import org.dokiteam.doki.core.util.ext.MutableEventFlow
import org.dokiteam.doki.core.util.ext.call
import org.dokiteam.doki.core.util.ext.firstNotNull
import org.dokiteam.doki.local.data.CacheDir
import org.dokiteam.doki.local.data.LocalStorageManager
import org.dokiteam.doki.local.domain.DeleteReadChaptersUseCase
import org.dokiteam.doki.search.domain.MangaSearchRepository
import org.dokiteam.doki.tracker.domain.TrackingRepository
import java.util.EnumMap
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class StorageManageSettingsViewModel @Inject constructor(
	private val storageManager: LocalStorageManager,
	private val httpCache: Cache,
	private val searchRepository: MangaSearchRepository,
	private val trackingRepository: TrackingRepository,
	private val cookieJar: MutableCookieJar,
	private val deleteReadChaptersUseCase: DeleteReadChaptersUseCase,
	private val mangaDataRepositoryProvider: Provider<MangaDataRepository>,
	private val coil: ImageLoader,
) : BaseViewModel() {

	val onActionDone = MutableEventFlow<ReversibleAction>()
	val loadingKeys = MutableStateFlow(emptySet<String>())

	val searchHistoryCount = MutableStateFlow(-1)
	val feedItemsCount = MutableStateFlow(-1)
	val httpCacheSize = MutableStateFlow(-1L)
	val cacheSizes = EnumMap<CacheDir, MutableStateFlow<Long>>(CacheDir::class.java)
	val storageUsage = MutableStateFlow<StorageUsage?>(null)

	val onChaptersCleanedUp = MutableEventFlow<Pair<Int, Long>>()

	val isBrowserDataCleanupEnabled: Boolean
		get() = WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)

	private var storageUsageJob: Job? = null

	init {
		CacheDir.entries.forEach {
			cacheSizes[it] = MutableStateFlow(-1L)
		}
		launchJob(Dispatchers.Default) {
			searchHistoryCount.value = searchRepository.getSearchHistoryCount()
		}
		launchJob(Dispatchers.Default) {
			feedItemsCount.value = trackingRepository.getLogsCount()
		}
		CacheDir.entries.forEach { cache ->
			launchJob(Dispatchers.Default) {
				checkNotNull(cacheSizes[cache]).value = storageManager.computeCacheSize(cache)
			}
		}
		launchJob(Dispatchers.Default) {
			httpCacheSize.value = runInterruptible { httpCache.size() }
		}
		loadStorageUsage()
	}

	fun clearCache(key: String, vararg caches: CacheDir) {
		launchJob(Dispatchers.Default) {
			try {
				loadingKeys.update { it + key }
				for (cache in caches) {
					storageManager.clearCache(cache)
					checkNotNull(cacheSizes[cache]).value = storageManager.computeCacheSize(cache)
					if (cache == CacheDir.THUMBS) {
						coil.memoryCache?.clear()
					}
				}
				loadStorageUsage()
			} finally {
				loadingKeys.update { it - key }
			}
		}
	}

	fun clearHttpCache() {
		launchJob(Dispatchers.Default) {
			try {
				loadingKeys.update { it + AppSettings.KEY_HTTP_CACHE_CLEAR }
				val size = runInterruptible(Dispatchers.IO) {
					httpCache.evictAll()
					httpCache.size()
				}
				httpCacheSize.value = size
				loadStorageUsage()
			} finally {
				loadingKeys.update { it - AppSettings.KEY_HTTP_CACHE_CLEAR }
			}
		}
	}

	fun clearSearchHistory() {
		launchJob(Dispatchers.Default) {
			searchRepository.clearSearchHistory()
			searchHistoryCount.value = searchRepository.getSearchHistoryCount()
			onActionDone.call(ReversibleAction(R.string.search_history_cleared, null))
		}
	}

	fun clearCookies() {
		launchJob {
			cookieJar.clear()
			onActionDone.call(ReversibleAction(R.string.cookies_cleared, null))
		}
	}

	@SuppressLint("RequiresFeature")
	fun clearBrowserData() {
		launchJob {
			try {
				loadingKeys.update { it + AppSettings.KEY_WEBVIEW_CLEAR }
				val storage = WebStorage.getInstance()
				suspendCoroutine { cont ->
					WebStorageCompat.deleteBrowsingData(storage) {
						cont.resume(Unit)
					}
				}
				onActionDone.call(ReversibleAction(R.string.updates_feed_cleared, null))
			} finally {
				loadingKeys.update { it - AppSettings.KEY_WEBVIEW_CLEAR }
			}
		}
	}

	fun clearUpdatesFeed() {
		launchJob(Dispatchers.Default) {
			try {
				loadingKeys.update { it + AppSettings.KEY_UPDATES_FEED_CLEAR }
				trackingRepository.clearLogs()
				feedItemsCount.value = trackingRepository.getLogsCount()
				onActionDone.call(ReversibleAction(R.string.updates_feed_cleared, null))
			} finally {
				loadingKeys.update { it - AppSettings.KEY_UPDATES_FEED_CLEAR }
			}
		}
	}

	fun clearMangaData() {
		launchJob(Dispatchers.Default) {
			try {
				loadingKeys.update { it + AppSettings.KEY_CLEAR_MANGA_DATA }
				trackingRepository.gc()
				val repository = mangaDataRepositoryProvider.get()
				repository.cleanupLocalManga()
				repository.cleanupDatabase()
				onActionDone.call(ReversibleAction(R.string.updates_feed_cleared, null))
			} finally {
				loadingKeys.update { it - AppSettings.KEY_CLEAR_MANGA_DATA }
			}
		}
	}

	fun cleanupChapters() {
		launchJob(Dispatchers.Default) {
			try {
				loadingKeys.update { it + AppSettings.KEY_CHAPTERS_CLEAR }
				val oldSize = storageUsage.firstNotNull().savedManga.bytes
				val chaptersCount = deleteReadChaptersUseCase.invoke()
				loadStorageUsage().join()
				val newSize = storageUsage.firstNotNull().savedManga.bytes
				onChaptersCleanedUp.call(chaptersCount to oldSize - newSize)
			} finally {
				loadingKeys.update { it - AppSettings.KEY_CHAPTERS_CLEAR }
			}
		}
	}

	private fun loadStorageUsage(): Job {
		val prevJob = storageUsageJob
		return launchJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			val pagesCacheSize = storageManager.computeCacheSize(CacheDir.PAGES)
			val otherCacheSize = storageManager.computeCacheSize() - pagesCacheSize
			val storageSize = storageManager.computeStorageSize()
			val availableSpace = storageManager.computeAvailableSize()
			val totalBytes = pagesCacheSize + otherCacheSize + storageSize + availableSpace
			storageUsage.value = StorageUsage(
				savedManga = StorageUsage.Item(
					bytes = storageSize,
					percent = (storageSize.toDouble() / totalBytes).toFloat(),
				),
				pagesCache = StorageUsage.Item(
					bytes = pagesCacheSize,
					percent = (pagesCacheSize.toDouble() / totalBytes).toFloat(),
				),
				otherCache = StorageUsage.Item(
					bytes = otherCacheSize,
					percent = (otherCacheSize.toDouble() / totalBytes).toFloat(),
				),
				available = StorageUsage.Item(
					bytes = availableSpace,
					percent = (availableSpace.toDouble() / totalBytes).toFloat(),
				),
			)
		}.also {
			storageUsageJob = it
		}
	}
}
