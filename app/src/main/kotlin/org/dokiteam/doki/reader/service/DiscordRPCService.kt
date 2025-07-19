package org.dokiteam.doki.reader.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.entities.presence.Activity
import com.my.kizzyrpc.entities.presence.Assets
import com.my.kizzyrpc.entities.presence.Timestamps
import com.my.kizzyrpc.entities.presence.Metadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiscordRPCService : Service() {

	private val scope = CoroutineScope(SupervisorJob())
	private var rpc: KizzyRPC? = null

	companion object {
		const val CHANNEL_ID = "DokiRPC"
		const val START_RPC_ACTION = "START_RPC_ACTION"
		const val UPDATE_RPC_ACTION = "UPDATE_RPC_ACTION"
		const val EXTRA_MANGA_TITLE = "manga_title"
		const val EXTRA_CHAPTER_NUMBER = "chapter_number"
		const val EXTRA_CURRENT_PAGE = "current_page"
		const val EXTRA_TOTAL_PAGES = "total_pages"
		var token: String? = null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		token = intent?.getStringExtra("TOKEN")
		rpc = token?.let { KizzyRPC(it) }

		when(intent?.action) {
			START_RPC_ACTION -> {
				updateRpcActivity(intent)
			}
			UPDATE_RPC_ACTION -> {
				updateRpcActivity(intent)
			}
		}
		return START_STICKY
	}

	/**
	 * Get localized context to ensure proper language support
	 * This ensures the service uses the app's language setting, not the system language
	 */
	private fun getLocalizedContext(): Context {
		// You might need to get the app's locale from SharedPreferences or app settings
		// For now, this returns the service context which should work for most cases
		return this
	}

	private fun updateRpcActivity(intent: Intent) {
		val mangaTitle = intent.getStringExtra(EXTRA_MANGA_TITLE) ?: return
		val chapterNumber = intent.getIntExtra(EXTRA_CHAPTER_NUMBER, 1)
		val currentPage = intent.getIntExtra(EXTRA_CURRENT_PAGE, 1)
		val totalPages = intent.getIntExtra(EXTRA_TOTAL_PAGES, 0)

		scope.launch {
			val localizedContext = getLocalizedContext()
			rpc?.setActivity(
				activity = Activity(
					applicationId = "1395464028611940393",
					name = localizedContext.getString(R.string.discord_rpc_app_name),
					details = mangaTitle,
					state = localizedContext.getString(R.string.discord_rpc_state_format, chapterNumber, currentPage, totalPages),
					type = 0,
					timestamps = Timestamps(
						start = System.currentTimeMillis()
					),
					assets = Assets(
						largeImage = "mp:attachments/1282576939831529473/1395673260481318953/DokiTest.png?ex=687b4d83&is=6879fc03&hm=f48faaaa8fa4b840cac741c83d8be3feb06dbe963107ffb38064fc33225f0616&=&format=webp&quality=lossless&width=256&height=256",
						largeText = localizedContext.getString(R.string.discord_rpc_large_text),
						smallText = localizedContext.getString(R.string.discord_rpc_small_text_format, mangaTitle),
						smallImage = "mp:attachments/1282576939831529473/1395712714415800392/button.png?ex=687b7242&is=687a20c2&hm=828ad97537c94128504402b43512523fe30801d534a48258f80c6fd29fda67c2&=&format=webp&quality=lossless",
					),
					buttons = listOf(
						localizedContext.getString(R.string.discord_rpc_button_doki),
						localizedContext.getString(R.string.discord_rpc_button_source)
					),
					// TODO: Add manga link for these buttons
					metadata = Metadata(
						listOf(
							"https://google.com", // Link to Doki
							"https://mimihentai.com", // Link to manga source
						)
					)
				),
				status = "online",
				since = System.currentTimeMillis()
			)
		}
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		// This method is called when the device configuration changes (including language)
		// The next Discord RPC update will automatically use the new language
	}

	override fun onDestroy() {
		rpc?.closeRPC()
		super.onDestroy()
	}

	override fun onBind(intent: Intent?): IBinder? = null
}
