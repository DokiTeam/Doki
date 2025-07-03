package org.dokiteam.doki.scrobbling.common.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import org.dokiteam.doki.scrobbling.common.domain.ScrobblerRepositoryMap
import org.dokiteam.doki.scrobbling.common.domain.model.ScrobblerService
import org.dokiteam.doki.scrobbling.common.domain.model.ScrobblerUser
import org.dokiteam.doki.scrobbling.kitsu.ui.KitsuAuthActivity
import javax.inject.Inject

class ScrobblerAuthHelper @Inject constructor(
	private val repositoriesMap: ScrobblerRepositoryMap,
) {

	fun isAuthorized(scrobbler: ScrobblerService) = repositoriesMap[scrobbler].isAuthorized

	fun getCachedUser(scrobbler: ScrobblerService): ScrobblerUser? {
		return repositoriesMap[scrobbler].cachedUser
	}

	suspend fun getUser(scrobbler: ScrobblerService): ScrobblerUser {
		return repositoriesMap[scrobbler].loadUser()
	}

	@SuppressLint("UnsafeImplicitIntentLaunch")
	fun startAuth(context: Context, scrobbler: ScrobblerService) = runCatching {
		if (scrobbler == ScrobblerService.KITSU) {
			launchKitsuAuth(context)
		} else {
			val repository = repositoriesMap[scrobbler]
			val intent = Intent(Intent.ACTION_VIEW)
			intent.data = repository.oauthUrl.toUri()
			context.startActivity(intent)
		}
	}

	private fun launchKitsuAuth(context: Context) {
		context.startActivity(Intent(context, KitsuAuthActivity::class.java))
	}
}
