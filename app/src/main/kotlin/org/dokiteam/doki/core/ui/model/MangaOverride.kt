package org.dokiteam.doki.core.ui.model

import org.koitharu.kotatsu.parsers.model.ContentRating

data class MangaOverride(
	val coverUrl: String?,
	val title: String?,
	val contentRating: ContentRating?,
)
