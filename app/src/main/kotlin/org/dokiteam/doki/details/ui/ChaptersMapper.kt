package org.dokiteam.doki.details.ui

import android.content.Context
import org.dokiteam.doki.R
import org.dokiteam.doki.bookmarks.domain.Bookmark
import org.dokiteam.doki.details.data.MangaDetails
import org.dokiteam.doki.details.ui.model.ChapterListItem
import org.dokiteam.doki.details.ui.model.toListItem
import org.dokiteam.doki.list.ui.model.ListHeader
import org.dokiteam.doki.list.ui.model.ListModel
import org.dokiteam.doki.parsers.util.mapToSet

fun MangaDetails.mapChapters(
	currentChapterId: Long,
	newCount: Int,
	branch: String?,
	bookmarks: List<Bookmark>,
	isGrid: Boolean,
	isDownloadedOnly: Boolean,
): List<ChapterListItem> {
	val remoteChapters = chapters[branch].orEmpty()
	val localChapters = local?.manga?.getChapters(branch).orEmpty()
	if (remoteChapters.isEmpty() && localChapters.isEmpty()) {
		return emptyList()
	}
	val bookmarked = bookmarks.mapToSet { it.chapterId }
	val newFrom = if (newCount == 0 || remoteChapters.isEmpty()) Int.MAX_VALUE else remoteChapters.size - newCount
	val ids = buildSet(maxOf(remoteChapters.size, localChapters.size)) {
		remoteChapters.mapTo(this) { it.id }
		localChapters.mapTo(this) { it.id }
	}
	val result = ArrayList<ChapterListItem>(ids.size)
	val localMap = if (localChapters.isNotEmpty()) {
		localChapters.associateByTo(LinkedHashMap(localChapters.size)) { it.id }
	} else {
		null
	}
	var isUnread = currentChapterId !in ids
	if (!isDownloadedOnly || local?.manga?.chapters == null) {
		for (chapter in remoteChapters) {
			val local = localMap?.remove(chapter.id)
			if (chapter.id == currentChapterId) {
				isUnread = true
			}
			result += (local ?: chapter).toListItem(
				isCurrent = chapter.id == currentChapterId,
				isUnread = isUnread,
				isNew = isUnread && result.size >= newFrom,
				isDownloaded = local != null,
				isBookmarked = chapter.id in bookmarked,
				isGrid = isGrid,
			)
		}
	}
	if (!localMap.isNullOrEmpty()) {
		for (chapter in localMap.values) {
			if (chapter.id == currentChapterId) {
				isUnread = true
			}
			result += chapter.toListItem(
				isCurrent = chapter.id == currentChapterId,
				isUnread = isUnread,
				isNew = false,
				isDownloaded = !isLocal,
				isBookmarked = chapter.id in bookmarked,
				isGrid = isGrid,
			)
		}
	}
	return result
}

fun List<ChapterListItem>.withVolumeHeaders(context: Context): MutableList<ListModel> {
	var prevVolume = 0
	val result = ArrayList<ListModel>((size * 1.4).toInt())
	for (item in this) {
		val chapter = item.chapter
		if (chapter.volume != prevVolume) {
			val text = if (chapter.volume == 0) {
				context.getString(R.string.volume_unknown)
			} else {
				context.getString(R.string.volume_, chapter.volume)
			}
			result.add(ListHeader(text))
			prevVolume = chapter.volume
		}
		result.add(item)
	}
	return result
}
