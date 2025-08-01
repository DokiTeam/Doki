package org.dokiteam.doki.local.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.dokiteam.doki.R
import org.dokiteam.doki.core.ErrorReporterReceiver
import org.dokiteam.doki.core.model.isNsfw
import org.dokiteam.doki.core.nav.AppRouter
import org.dokiteam.doki.core.ui.CoroutineIntentService
import org.dokiteam.doki.core.util.ext.checkNotificationPermission
import org.dokiteam.doki.core.util.ext.getDisplayMessage
import org.dokiteam.doki.core.util.ext.mangaSourceExtra
import org.dokiteam.doki.core.util.ext.powerManager
import org.dokiteam.doki.core.util.ext.printStackTraceDebug
import org.dokiteam.doki.core.util.ext.toBitmapOrNull
import org.dokiteam.doki.core.util.ext.toUriOrNull
import org.dokiteam.doki.core.util.ext.withPartialWakeLock
import org.dokiteam.doki.local.data.importer.SingleMangaImporter
import org.dokiteam.doki.parsers.model.Manga
import org.dokiteam.doki.parsers.util.runCatchingCancellable
import javax.inject.Inject

@AndroidEntryPoint
class ImportService : CoroutineIntentService() {

	@Inject
	lateinit var importer: SingleMangaImporter

	@Inject
	lateinit var coil: ImageLoader

	private lateinit var notificationManager: NotificationManagerCompat

	override fun onCreate() {
		super.onCreate()
		notificationManager = NotificationManagerCompat.from(applicationContext)
	}

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		val uri = requireNotNull(intent.getStringExtra(DATA_URI)?.toUriOrNull()) { "No input uri" }
		startForeground(this)
		powerManager.withPartialWakeLock(TAG) {
			val result = runCatchingCancellable {
				importer.import(uri).manga
			}
			if (applicationContext.checkNotificationPermission(CHANNEL_ID)) {
				val notification = buildNotification(startId, result)
				notificationManager.notify(TAG, startId, notification)
			}
		}
	}

	override fun IntentJobContext.onError(error: Throwable) {
		if (applicationContext.checkNotificationPermission(CHANNEL_ID)) {
			val notification = runBlocking { buildNotification(startId, Result.failure(error)) }
			notificationManager.notify(TAG, startId, notification)
		}
	}

	@SuppressLint("InlinedApi")
	private fun startForeground(jobContext: IntentJobContext) {
		val title = applicationContext.getString(R.string.importing_manga)
		val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
			.setName(title)
			.setShowBadge(false)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(false)
			.build()
		notificationManager.createNotificationChannel(channel)

		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setContentTitle(title)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setDefaults(0)
			.setSilent(true)
			.setOngoing(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_sys_download)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			.build()

		jobContext.setForeground(
			FOREGROUND_NOTIFICATION_ID,
			notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
	}

	private suspend fun buildNotification(startId: Int, result: Result<Manga>): Notification {
		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setDefaults(0)
			.setSilent(true)
			.setAutoCancel(true)
		result.onSuccess { manga ->
			notification.setLargeIcon(
				coil.execute(
					ImageRequest.Builder(applicationContext)
						.data(manga.coverUrl)
						.mangaSourceExtra(manga.source)
						.build(),
				).toBitmapOrNull(),
			)
			notification.setSubText(manga.title)
			val intent = AppRouter.detailsIntent(applicationContext, manga)
			notification.setContentIntent(
				PendingIntentCompat.getActivity(
					applicationContext,
					manga.id.toInt(),
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT,
					false,
				),
			).setVisibility(
				if (manga.isNsfw()) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PUBLIC,
			)
			notification.setContentTitle(applicationContext.getString(R.string.import_completed))
				.setContentText(applicationContext.getString(R.string.import_completed_hint))
				.setSmallIcon(R.drawable.ic_stat_done)
			NotificationCompat.BigTextStyle(notification)
				.bigText(applicationContext.getString(R.string.import_completed_hint))
		}.onFailure { error ->
			notification.setContentTitle(applicationContext.getString(R.string.error_occurred))
				.setContentText(error.getDisplayMessage(applicationContext.resources))
				.setSmallIcon(android.R.drawable.stat_notify_error)
			ErrorReporterReceiver.getNotificationAction(
				context = applicationContext,
				e = error,
				notificationId = startId,
				notificationTag = TAG,
			)?.let { action ->
				notification.addAction(action)
			}
		}
		return notification.build()
	}

	companion object {

		private const val DATA_URI = "uri"
		private const val TAG = "import"
		private const val CHANNEL_ID = "importing"
		private const val FOREGROUND_NOTIFICATION_ID = 37

		fun start(context: Context, uris: Collection<Uri>): Boolean = try {
			require(uris.isNotEmpty())
			for (uri in uris) {
				val intent = Intent(context, ImportService::class.java)
				intent.putExtra(DATA_URI, uri.toString())
				ContextCompat.startForegroundService(context, intent)
			}
			true
		} catch (e: Exception) {
			e.printStackTraceDebug()
			false
		}
	}
}
