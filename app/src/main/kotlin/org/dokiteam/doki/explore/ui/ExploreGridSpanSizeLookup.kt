package org.dokiteam.doki.explore.ui

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import org.dokiteam.doki.explore.ui.adapter.ExploreAdapter
import org.dokiteam.doki.list.ui.adapter.ListItemType

class ExploreGridSpanSizeLookup(
	private val adapter: ExploreAdapter,
	private val layoutManager: GridLayoutManager,
) : SpanSizeLookup() {

	override fun getSpanSize(position: Int): Int {
		val itemType = adapter.getItemViewType(position)
		return if (itemType == ListItemType.EXPLORE_SOURCE_GRID.ordinal) 1 else layoutManager.spanCount
	}
}
