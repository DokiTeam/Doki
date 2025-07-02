package org.dokiteam.doki.list.domain

import androidx.collection.ArraySet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dokiteam.doki.core.model.toChipModel
import org.dokiteam.doki.core.prefs.AppSettings
import org.dokiteam.doki.list.ui.model.QuickFilter
import org.dokiteam.doki.parsers.util.suspendlazy.getOrNull
import org.dokiteam.doki.parsers.util.suspendlazy.suspendLazy

abstract class MangaListQuickFilter(
	private val settings: AppSettings,
) : QuickFilterListener {

	private val appliedFilter = MutableStateFlow<Set<ListFilterOption>>(emptySet())
	private val availableFilterOptions = suspendLazy {
		getAvailableFilterOptions()
	}

	val appliedOptions
		get() = appliedFilter.asStateFlow()

	override fun setFilterOption(option: ListFilterOption, isApplied: Boolean) {
		appliedFilter.value = ArraySet(appliedFilter.value).also {
			if (isApplied) {
				it.addNoConflicts(option)
			} else {
				it.remove(option)
			}
		}
	}

	override fun toggleFilterOption(option: ListFilterOption) {
		appliedFilter.value = ArraySet(appliedFilter.value).also {
			if (option in it) {
				it.remove(option)
			} else {
				it.addNoConflicts(option)
			}
		}
	}

	override fun clearFilter() {
		appliedFilter.value = emptySet()
	}

	suspend fun filterItem(
		selectedOptions: Set<ListFilterOption>,
	): QuickFilter? {
		if (!settings.isQuickFilterEnabled) {
			return null
		}
		val availableOptions = availableFilterOptions.getOrNull()?.map { option ->
			option.toChipModel(isChecked = option in selectedOptions)
		}.orEmpty()
		return if (availableOptions.isNotEmpty()) {
			QuickFilter(availableOptions)
		} else {
			null
		}
	}

	protected abstract suspend fun getAvailableFilterOptions(): List<ListFilterOption>

	private fun ArraySet<ListFilterOption>.addNoConflicts(option: ListFilterOption) {
		add(option)
		if (option is ListFilterOption.Inverted) {
			remove(option.option)
		} else {
			removeIf { it is ListFilterOption.Inverted && it.option == option }
		}
	}
}
