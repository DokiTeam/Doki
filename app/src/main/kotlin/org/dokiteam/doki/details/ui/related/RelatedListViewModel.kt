package org.dokiteam.doki.details.ui.related

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.dokiteam.doki.R
import org.dokiteam.doki.core.model.parcelable.ParcelableManga
import org.dokiteam.doki.core.nav.AppRouter
import org.dokiteam.doki.core.parser.MangaDataRepository
import org.dokiteam.doki.core.parser.MangaRepository
import org.dokiteam.doki.core.prefs.AppSettings
import org.dokiteam.doki.core.util.ext.call
import org.dokiteam.doki.core.util.ext.printStackTraceDebug
import org.dokiteam.doki.core.util.ext.require
import org.dokiteam.doki.list.domain.MangaListMapper
import org.dokiteam.doki.list.ui.MangaListViewModel
import org.dokiteam.doki.list.ui.model.EmptyState
import org.dokiteam.doki.list.ui.model.LoadingState
import org.dokiteam.doki.list.ui.model.toErrorState
import org.dokiteam.doki.local.data.LocalStorageChanges
import org.dokiteam.doki.local.domain.model.LocalManga
import org.dokiteam.doki.parsers.model.Manga
import javax.inject.Inject

@HiltViewModel
class RelatedListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	settings: AppSettings,
	private val mangaListMapper: MangaListMapper,
	mangaDataRepository: MangaDataRepository,
	@LocalStorageChanges localStorageChanges: SharedFlow<LocalManga?>,
) : MangaListViewModel(settings, mangaDataRepository, localStorageChanges) {

	private val seed = savedStateHandle.require<ParcelableManga>(AppRouter.KEY_MANGA).manga
	private val repository = mangaRepositoryFactory.create(seed.source)
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null

	override val content = combine(
		mangaList,
		observeListModeWithTriggers(),
		listError,
	) { list, mode, error ->
		when {
			list.isNullOrEmpty() && error != null -> listOf(error.toErrorState(canRetry = true))
			list == null -> listOf(LoadingState)
			list.isEmpty() -> listOf(createEmptyState())
			else -> mangaListMapper.toListModelList(list, mode)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	init {
		loadList()
	}

	override fun onRefresh() {
		loadList()
	}

	override fun onRetry() {
		loadList()
	}

	private fun loadList(): Job {
		loadingJob?.let {
			if (it.isActive) return it
		}
		return launchLoadingJob(Dispatchers.Default) {
			try {
				listError.value = null
				mangaList.value = repository.getRelated(seed)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				e.printStackTraceDebug()
				listError.value = e
				if (!mangaList.value.isNullOrEmpty()) {
					errorEvent.call(e)
				}
			}
		}.also { loadingJob = it }
	}

	private fun createEmptyState() = EmptyState(
		icon = R.drawable.ic_empty_common,
		textPrimary = R.string.nothing_found,
		textSecondary = 0,
		actionStringRes = 0,
	)
}

