package org.dokiteam.doki.details.domain

import org.dokiteam.doki.core.db.MangaDatabase
import org.dokiteam.doki.core.model.isLocal
import org.dokiteam.doki.core.os.NetworkState
import org.dokiteam.doki.core.parser.MangaRepository
import org.dokiteam.doki.list.domain.ReadingProgress.Companion.PROGRESS_NONE
import org.dokiteam.doki.local.data.LocalMangaRepository
import org.dokiteam.doki.parsers.model.Manga
import javax.inject.Inject

class ProgressUpdateUseCase @Inject constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val database: MangaDatabase,
	private val localMangaRepository: LocalMangaRepository,
	private val networkState: NetworkState,
) {

	suspend operator fun invoke(manga: Manga): Float {
		val history = database.getHistoryDao().find(manga.id) ?: return PROGRESS_NONE
		val seed = if (manga.isLocal) {
			localMangaRepository.getRemoteManga(manga) ?: manga
		} else {
			manga
		}
		if (!seed.isLocal && !networkState.value) {
			return PROGRESS_NONE
		}
		val repo = mangaRepositoryFactory.create(seed.source)
		val details = if (manga.source != seed.source || seed.chapters.isNullOrEmpty()) {
			repo.getDetails(seed)
		} else {
			seed
		}
		val chapter = details.findChapterById(history.chapterId) ?: return PROGRESS_NONE
		val chapters = details.getChapters(chapter.branch)
		val chaptersCount = chapters.size
		if (chaptersCount == 0) {
			return PROGRESS_NONE
		}
		val chapterIndex = chapters.indexOfFirst { x -> x.id == history.chapterId }
		val pagesCount = repo.getPages(chapter).size
		if (pagesCount == 0) {
			return PROGRESS_NONE
		}
		val pagePercent = (history.page + 1) / pagesCount.toFloat()
		val ppc = 1f / chaptersCount
		val result = ppc * chapterIndex + ppc * pagePercent
		if (result != history.percent) {
			database.getHistoryDao().update(
				history.copy(
					chapterId = chapter.id,
					percent = result,
				),
			)
		}
		return result
	}
}
