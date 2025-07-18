package org.dokiteam.doki.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException
import org.dokiteam.doki.core.exceptions.CloudFlareBlockedException
import org.dokiteam.doki.core.exceptions.CloudFlareProtectedException
import org.dokiteam.doki.parsers.model.MangaSource
import org.dokiteam.doki.parsers.network.CloudFlareHelper

class CloudFlareInterceptor : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val response = chain.proceed(request)
		return when (CloudFlareHelper.checkResponseForProtection(response)) {
			CloudFlareHelper.PROTECTION_BLOCKED -> response.closeThrowing(
				CloudFlareBlockedException(
					url = request.url.toString(),
					source = request.tag(MangaSource::class.java),
				),
			)

			CloudFlareHelper.PROTECTION_CAPTCHA -> response.closeThrowing(
				CloudFlareProtectedException(
					url = request.url.toString(),
					source = request.tag(MangaSource::class.java),
					headers = request.headers,
				),
			)

			else -> response
		}
	}

	private fun Response.closeThrowing(error: IOException): Nothing {
		try {
			close()
		} catch (e: Exception) {
			error.addSuppressed(e)
		}
		throw error
	}
}
