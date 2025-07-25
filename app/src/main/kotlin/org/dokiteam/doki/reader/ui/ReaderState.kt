package org.dokiteam.doki.reader.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.dokiteam.doki.core.model.MangaHistory
import org.dokiteam.doki.parsers.model.Manga

@Parcelize
data class ReaderState(
	val chapterId: Long,
	val page: Int,
	val scroll: Int,
) : Parcelable {

	constructor(history: MangaHistory) : this(
		chapterId = history.chapterId,
		page = history.page,
		scroll = history.scroll,
	)

	constructor(manga: Manga, branch: String?) : this(
		chapterId = manga.chapters?.let {
			it.firstOrNull { x -> x.branch == branch } ?: it.firstOrNull()
		}?.id ?: error("Cannot find first chapter"),
		page = 0,
		scroll = 0,
	)
}
