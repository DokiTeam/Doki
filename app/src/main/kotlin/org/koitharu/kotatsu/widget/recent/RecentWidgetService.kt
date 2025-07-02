package org.dokiteam.doki.widget.recent

import android.content.Intent
import android.widget.RemoteViewsService
import coil3.ImageLoader
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import org.dokiteam.doki.core.prefs.AppSettings
import org.dokiteam.doki.history.data.HistoryRepository
import javax.inject.Inject

@AndroidEntryPoint
class RecentWidgetService : RemoteViewsService() {

	@Inject
	lateinit var historyRepository: HistoryRepository

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var coilLazy: Lazy<ImageLoader>

	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		return RecentListFactory(applicationContext, historyRepository, coilLazy, settings)
	}
}
