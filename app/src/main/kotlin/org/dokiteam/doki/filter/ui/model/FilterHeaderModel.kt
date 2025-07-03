package org.dokiteam.doki.filter.ui.model

import org.dokiteam.doki.core.ui.widgets.ChipsView
import org.dokiteam.doki.parsers.model.SortOrder

data class FilterHeaderModel(
	val chips: Collection<ChipsView.ChipModel>,
	val sortOrder: SortOrder?,
	val isFilterApplied: Boolean,
) {

	val textSummary: String
		get() = chips.mapNotNull { if (it.isChecked) it.title else null }.joinToString()
}
