package org.dokiteam.doki.search.ui.multi

import androidx.collection.ArraySet
import androidx.collection.LongSet
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.dokiteam.doki.R
import org.dokiteam.doki.core.model.LocalMangaSource
import org.dokiteam.doki.core.model.UnknownMangaSource
import org.dokiteam.doki.core.nav.AppRouter
import org.dokiteam.doki.core.prefs.ListMode
import org.dokiteam.doki.core.ui.BaseViewModel
import org.dokiteam.doki.core.util.ext.append
import org.dokiteam.doki.core.util.ext.printStackTraceDebug
import org.dokiteam.doki.core.util.ext.toLocale
import org.dokiteam.doki.explore.data.MangaSourcesRepository
import org.dokiteam.doki.favourites.domain.FavouritesRepository
import org.dokiteam.doki.history.data.HistoryRepository
import org.dokiteam.doki.list.domain.MangaListMapper
import org.dokiteam.doki.list.ui.model.ButtonFooter
import org.dokiteam.doki.list.ui.model.EmptyState
import org.dokiteam.doki.list.ui.model.ListModel
import org.dokiteam.doki.list.ui.model.LoadingFooter
import org.dokiteam.doki.list.ui.model.LoadingState
import org.dokiteam.doki.parsers.model.Manga
import org.dokiteam.doki.parsers.model.MangaParserSource
import org.dokiteam.doki.parsers.model.MangaSource
import org.dokiteam.doki.parsers.util.runCatchingCancellable
import org.dokiteam.doki.search.domain.SearchKind
import org.dokiteam.doki.search.domain.SearchV2Helper
import java.util.Locale
import javax.inject.Inject

private const val MAX_PARALLELISM = 4

@HiltViewModel
class SearchViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val mangaListMapper: MangaListMapper,
	private val searchHelperFactory: SearchV2Helper.Factory,
	private val sourcesRepository: MangaSourcesRepository,
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
) : BaseViewModel() {

	val query = savedStateHandle.get<String>(AppRouter.KEY_QUERY).orEmpty()
	val kind = savedStateHandle.get<SearchKind>(AppRouter.KEY_KIND) ?: SearchKind.SIMPLE

	private var includeDisabledSources = MutableStateFlow(false)
	private val results = MutableStateFlow<List<SearchResultsListModel>>(emptyList())

	private var searchJob: Job? = null

	val list: StateFlow<List<ListModel>> = combine(
		results,
		isLoading.dropWhile { !it },
		includeDisabledSources,
	) { list, loading, includeDisabled ->
		when {
			list.isEmpty() -> listOf(
				when {
					loading -> LoadingState
					else -> EmptyState(
						icon = R.drawable.ic_empty_common,
						textPrimary = R.string.nothing_found,
						textSecondary = R.string.text_search_holder_secondary,
						actionStringRes = 0,
					)
				},
			)

			loading -> list + LoadingFooter()
			includeDisabled -> list
			else -> list + ButtonFooter(R.string.search_disabled_sources)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	init {
		doSearch()
	}

	fun getItems(ids: LongSet): Set<Manga> {
		val snapshot = results.value
		val result = ArraySet<Manga>(ids.size)
		snapshot.forEach { x ->
			for (item in x.list) {
				if (item.id in ids) {
					result.add(item.manga)
				}
			}
		}
		return result
	}

	fun retry() {
		searchJob?.cancel()
		results.value = emptyList()
		includeDisabledSources.value = false
		doSearch()
	}

	fun continueSearch() {
		if (includeDisabledSources.value) {
			return
		}
		val prevJob = searchJob
		searchJob = launchLoadingJob(Dispatchers.Default) {
			includeDisabledSources.value = true
			prevJob?.join()
			val sources = sourcesRepository.getDisabledSources()
				.sortedByDescending { it.priority() }
			val semaphore = Semaphore(MAX_PARALLELISM)
			sources.map { source ->
				launch {
					semaphore.withPermit {
						appendResult(searchSource(source))
					}
				}
			}.joinAll()
		}
	}

	private fun doSearch() {
		val prevJob = searchJob
		searchJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			appendResult(searchHistory())
			appendResult(searchFavorites())
			appendResult(searchLocal())
			val sources = sourcesRepository.getEnabledSources()
			val semaphore = Semaphore(MAX_PARALLELISM)
			sources.map { source ->
				launch {
					semaphore.withPermit {
						appendResult(searchSource(source))
					}
				}
			}.joinAll()
		}
	}

	// impl

	private suspend fun searchSource(source: MangaSource): SearchResultsListModel? = runCatchingCancellable {
		val searchHelper = searchHelperFactory.create(source)
		searchHelper(query, kind)
	}.fold(
		onSuccess = { result ->
			if (result == null || result.manga.isEmpty()) {
				null
			} else {
				val list = mangaListMapper.toListModelList(
					manga = result.manga,
					mode = ListMode.GRID,
				)
				SearchResultsListModel(
					titleResId = 0,
					source = source,
					list = list,
					error = null,
					listFilter = result.listFilter,
					sortOrder = result.sortOrder,
				)
			}
		},
		onFailure = { error ->
			error.printStackTraceDebug()
			if (source is MangaParserSource && source.isBroken) {
				null
			} else {
				SearchResultsListModel(0, source, null, null, emptyList(), error)
			}
		},
	)

	private suspend fun searchHistory(): SearchResultsListModel? = runCatchingCancellable {
		historyRepository.search(query, kind, Int.MAX_VALUE)
	}.fold(
		onSuccess = { result ->
			if (result.isNotEmpty()) {
				SearchResultsListModel(
					titleResId = R.string.history,
					source = UnknownMangaSource,
					list = mangaListMapper.toListModelList(manga = result, mode = ListMode.GRID),
					error = null,
					listFilter = null,
					sortOrder = null,
				)
			} else {
				null
			}
		},
		onFailure = { error ->
			SearchResultsListModel(
				titleResId = R.string.history,
				source = UnknownMangaSource,
				list = emptyList(),
				error = error,
				listFilter = null,
				sortOrder = null,
			)
		},
	)

	private suspend fun searchFavorites(): SearchResultsListModel? = runCatchingCancellable {
		favouritesRepository.search(query, kind, Int.MAX_VALUE)
	}.fold(
		onSuccess = { result ->
			if (result.isNotEmpty()) {
				SearchResultsListModel(
					titleResId = R.string.favourites,
					source = UnknownMangaSource,
					list = mangaListMapper.toListModelList(
						manga = result,
						mode = ListMode.GRID,
						flags = MangaListMapper.NO_FAVORITE,
					),
					error = null,
					listFilter = null,
					sortOrder = null,
				)
			} else {
				null
			}
		},
		onFailure = { error ->
			SearchResultsListModel(
				titleResId = R.string.favourites,
				source = UnknownMangaSource,
				list = emptyList(),
				error = error,
				listFilter = null,
				sortOrder = null,
			)
		},
	)

	private suspend fun searchLocal(): SearchResultsListModel? = runCatchingCancellable {
		searchHelperFactory.create(LocalMangaSource).invoke(query, kind)
	}.fold(
		onSuccess = { result ->
			if (!result?.manga.isNullOrEmpty()) {
				SearchResultsListModel(
					titleResId = 0,
					source = LocalMangaSource,
					list = mangaListMapper.toListModelList(
						manga = result.manga,
						mode = ListMode.GRID,
						flags = MangaListMapper.NO_SAVED,
					),
					error = null,
					listFilter = result.listFilter,
					sortOrder = result.sortOrder,
				)
			} else {
				null
			}
		},
		onFailure = { error ->
			SearchResultsListModel(
				titleResId = 0,
				source = LocalMangaSource,
				list = emptyList(),
				error = error,
				listFilter = null,
				sortOrder = null,
			)
		},
	)

	private fun appendResult(item: SearchResultsListModel?) {
		if (item != null) {
			results.append(item)
		}
	}

	private fun MangaSource.priority(): Int {
		var res = 0
		if (this is MangaParserSource) {
			if (locale.toLocale() == Locale.getDefault()) res += 2
		}
		return res
	}
}
