package org.dokiteam.doki.scrobbling.mal.data

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.dokiteam.doki.core.network.CommonHeaders
import org.dokiteam.doki.core.util.ext.printStackTraceDebug
import org.dokiteam.doki.scrobbling.common.data.ScrobblerStorage
import org.dokiteam.doki.scrobbling.common.domain.model.ScrobblerService
import org.dokiteam.doki.scrobbling.common.domain.model.ScrobblerType
import javax.inject.Inject
import javax.inject.Provider

class MALAuthenticator @Inject constructor(
	@ScrobblerType(ScrobblerService.MAL) private val storage: ScrobblerStorage,
	private val repositoryProvider: Provider<MALRepository>,
) : Authenticator {

	override fun authenticate(route: Route?, response: Response): Request? {
		val accessToken = storage.accessToken ?: return null
		if (!isRequestWithAccessToken(response)) {
			return null
		}
		synchronized(this) {
			val newAccessToken = storage.accessToken ?: return null
			if (accessToken != newAccessToken) {
				return newRequestWithAccessToken(response.request, newAccessToken)
			}
			val updatedAccessToken = refreshAccessToken() ?: return null
			return newRequestWithAccessToken(response.request, updatedAccessToken)
		}
	}

	private fun isRequestWithAccessToken(response: Response): Boolean {
		val header = response.request.header(CommonHeaders.AUTHORIZATION)
		return header?.startsWith("Bearer") == true
	}

	private fun newRequestWithAccessToken(request: Request, accessToken: String): Request {
		return request.newBuilder()
			.header(CommonHeaders.AUTHORIZATION, "Bearer $accessToken")
			.build()
	}

	private fun refreshAccessToken(): String? = runCatching {
		val repository = repositoryProvider.get()
		runBlocking { repository.authorize(null) }
		return storage.accessToken
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrNull()

}
