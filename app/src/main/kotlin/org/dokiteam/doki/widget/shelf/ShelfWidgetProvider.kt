package org.dokiteam.doki.widget.shelf

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import org.dokiteam.doki.R
import org.dokiteam.doki.core.nav.ReaderIntent
import org.dokiteam.doki.core.prefs.AppWidgetConfig
import org.dokiteam.doki.core.ui.BaseAppWidgetProvider
import org.dokiteam.doki.reader.ui.ReaderActivity

class ShelfWidgetProvider : BaseAppWidgetProvider() {

	override fun onUpdate(
		context: Context,
		appWidgetManager: AppWidgetManager,
		appWidgetIds: IntArray
	) {
		super.onUpdate(context, appWidgetManager, appWidgetIds)
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.gridView)
	}

	override fun onUpdateWidget(context: Context, config: AppWidgetConfig): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.widget_shelf)
		if (!config.hasBackground) {
			views.setInt(R.id.widget_root, "setBackgroundColor", Color.TRANSPARENT)
		} else {
			views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_appwidget_root)
		}
		val adapter = Intent(context, ShelfWidgetService::class.java)
		adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
		adapter.data = adapter.toUri(Intent.URI_INTENT_SCHEME).toUri()
		views.setRemoteAdapter(R.id.gridView, adapter)
		val intent = Intent(context, ReaderActivity::class.java)
		intent.action = ReaderIntent.ACTION_MANGA_READ
		views.setPendingIntentTemplate(
			R.id.gridView,
			PendingIntentCompat.getActivity(
				context,
				0,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT,
				true,
			),
		)
		views.setEmptyView(R.id.gridView, R.id.textView_holder)
		return views
	}
}
