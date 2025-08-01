package org.dokiteam.doki.picker.ui.page

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.dokiteam.doki.core.nav.MangaIntent
import org.dokiteam.doki.core.prefs.AppSettings
import org.dokiteam.doki.core.prefs.observeAsStateFlow
import org.dokiteam.doki.core.ui.BaseViewModel
import org.dokiteam.doki.core.util.ext.firstNotNull
import org.dokiteam.doki.details.data.MangaDetails
import org.dokiteam.doki.details.domain.DetailsLoadUseCase
import org.dokiteam.doki.details.ui.pager.pages.PageThumbnail
import org.dokiteam.doki.list.ui.model.ListHeader
import org.dokiteam.doki.list.ui.model.ListModel
import org.dokiteam.doki.reader.domain.ChaptersLoader
import javax.inject.Inject

@HiltViewModel
class PagePickerViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val chaptersLoader: ChaptersLoader,
	private val detailsLoadUseCase: DetailsLoadUseCase,
	settings: AppSettings,
) : BaseViewModel() {

	private val intent = MangaIntent(savedStateHandle)

	private var loadingJob: Job? = null
	private var loadingNextJob: Job? = null

	val thumbnails = MutableStateFlow<List<ListModel>>(emptyList())
	val isLoadingDown = MutableStateFlow(false)
	val manga = MutableStateFlow(intent.manga?.let { MangaDetails(it) })

	val isNoChapters = manga.map {
		it != null && it.isLoaded && it.allChapters.isEmpty()
	}

	val gridScale = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_GRID_SIZE_PAGES,
		valueProducer = { gridSizePages / 100f },
	)

	init {
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			doInit()
		}
	}

	private suspend fun doInit() {
		val details = detailsLoadUseCase.invoke(intent, force = false)
			.onEach { manga.value = it }
			.first { x -> x.isLoaded }
		chaptersLoader.init(details)
		val initialChapterId = details.allChapters.firstOrNull()?.id ?: return
		if (!chaptersLoader.hasPages(initialChapterId)) {
			chaptersLoader.loadSingleChapter(initialChapterId)
		}
		updateList()
	}

	fun loadNextChapter() {
		if (loadingJob?.isActive == true || loadingNextJob?.isActive == true) {
			return
		}
		loadingNextJob = launchJob(Dispatchers.Default) {
			isLoadingDown.value = true
			try {
				val currentId = chaptersLoader.last().chapterId
				chaptersLoader.loadPrevNextChapter(manga.firstNotNull(), currentId, isNext = true)
				updateList()
			} finally {
				isLoadingDown.value = false
			}
		}
	}

	private fun updateList() {
		val snapshot = chaptersLoader.snapshot()
		val pages = buildList(snapshot.size + chaptersLoader.size + 2) {
			var previousChapterId = 0L
			for (page in snapshot) {
				if (page.chapterId != previousChapterId) {
					chaptersLoader.peekChapter(page.chapterId)?.let {
						add(ListHeader(it))
					}
					previousChapterId = page.chapterId
				}
				this += PageThumbnail(
					isCurrent = false,
					page = page,
				)
			}
		}
		thumbnails.value = pages
	}
}
