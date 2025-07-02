package org.dokiteam.doki.suggestions.domain

import org.dokiteam.doki.core.prefs.AppSettings
import org.dokiteam.doki.list.domain.ListFilterOption
import org.dokiteam.doki.list.domain.MangaListQuickFilter
import javax.inject.Inject

class SuggestionsListQuickFilter @Inject constructor(
	private val settings: AppSettings,
	private val suggestionRepository: SuggestionRepository,
) : MangaListQuickFilter(settings) {

	override suspend fun getAvailableFilterOptions(): List<ListFilterOption> = buildList(6) {
		suggestionRepository.getTopTags(5).mapTo(this) {
			ListFilterOption.Tag(it)
		}
		if (!settings.isNsfwContentDisabled && !settings.isSuggestionsExcludeNsfw) {
			add(ListFilterOption.Macro.NSFW)
			add(ListFilterOption.SFW)
		}
		suggestionRepository.getTopSources(3).mapTo(this) {
			ListFilterOption.Source(it)
		}
	}
}
