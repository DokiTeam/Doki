package org.dokiteam.doki.core.util.ext

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import okio.Path
import org.dokiteam.doki.core.util.MimeTypes
import java.io.File

const val URI_SCHEME_ZIP = "file+zip"
private const val URI_SCHEME_FILE = "file"
private const val URI_SCHEME_HTTP = "http"
private const val URI_SCHEME_HTTPS = "https"
private const val URI_SCHEME_LEGACY_CBZ = "cbz"
private const val URI_SCHEME_LEGACY_ZIP = "zip"

fun Uri.isZipUri() = scheme.let {
	it == URI_SCHEME_ZIP || it == URI_SCHEME_LEGACY_CBZ || it == URI_SCHEME_LEGACY_ZIP
}

fun Uri.isFileUri() = scheme == URI_SCHEME_FILE

fun Uri.isNetworkUri() = scheme.let {
	it == URI_SCHEME_HTTP || it == URI_SCHEME_HTTPS
}

fun File.toZipUri(entryPath: String): Uri = "$URI_SCHEME_ZIP://$absolutePath#$entryPath".toUri()

fun File.toZipUri(entryPath: Path?): Uri =
	toZipUri(entryPath?.toString()?.removePrefix(Path.DIRECTORY_SEPARATOR).orEmpty())

fun String.toUriOrNull() = if (isEmpty()) null else this.toUri()

fun File.toUri(fragment: String?): Uri = toUri().run {
	if (fragment != null) {
		buildUpon().fragment(fragment).build()
	} else {
		this
	}
}

fun Uri.isAnimatedImage(context: Context): Boolean {
    val mimeType = when {
        isFileUri() -> {
            try {
                toFile().let { MimeTypes.probeMimeType(it) }
            } catch (e: Exception) {
                null
            }
        }
        else -> {
            context.contentResolver.getType(this)?.toMimeTypeOrNull()
        }
    }
    if (mimeType?.type != "image") return false
    return when (mimeType.subtype) {
        "gif" -> true
        "webp" -> false
        else -> false
    }
}
