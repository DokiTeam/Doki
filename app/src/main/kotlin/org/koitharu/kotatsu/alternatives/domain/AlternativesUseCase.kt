package org.dokiteam.doki.alternatives.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.dokiteam.doki.core.parser.MangaRepository
import org.dokiteam.doki.core.util.ext.toLocale
import org.dokiteam.doki.explore.data.MangaSourcesRepository
import org.dokiteam.doki.parsers.model.Manga
import org.dokiteam.doki.parsers.model.MangaParserSource
import org.dokiteam.doki.parsers.model.MangaSource
import org.dokiteam.doki.parsers.util.runCatchingCancellable
import org.dokiteam.doki.search.domain.SearchKind
import org.dokiteam.doki.search.domain.SearchV2Helper
import java.util.Locale
import javax.inject.Inject

private const val MAX_PARALLELISM = 4

class AlternativesUseCase @Inject constructor(
	private val sourcesRepository: MangaSourcesRepository,
	private val searchHelperFactory: SearchV2Helper.Factory,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend operator fun invoke(manga: Manga, throughDisabledSources: Boolean): Flow<Manga> {
		val sources = getSources(manga.source, throughDisabledSources)
		if (sources.isEmpty()) {
			return emptyFlow()
		}
		val semaphore = Semaphore(MAX_PARALLELISM)
		return channelFlow {
			for (source in sources) {
				launch {
					val searchHelper = searchHelperFactory.create(source)
					val list = runCatchingCancellable {
						semaphore.withPermit {
							searchHelper(manga.title, SearchKind.TITLE)?.manga
						}
					}.getOrNull()
					list?.forEach { m ->
						if (m.id != manga.id) {
							launch {
								val details = runCatchingCancellable {
									mangaRepositoryFactory.create(m.source).getDetails(m)
								}.getOrDefault(m)
								send(details)
							}
						}
					}
				}
			}
		}
	}

	private suspend fun getSources(ref: MangaSource, disabled: Boolean): List<MangaSource> = if (disabled) {
		sourcesRepository.getDisabledSources()
	} else {
		sourcesRepository.getEnabledSources()
	}.sortedByDescending { it.priority(ref) }

	private fun MangaSource.priority(ref: MangaSource): Int {
		var res = 0
		if (this is MangaParserSource && ref is MangaParserSource) {
			if (locale == ref.locale) {
				res += 4
			} else if (locale.toLocale() == Locale.getDefault()) {
				res += 2
			}
			if (contentType == ref.contentType) {
				res++
			}
		}
		return res
	}
}
