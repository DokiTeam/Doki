package org.dokiteam.doki.widget.shelf.model

import org.dokiteam.doki.list.ui.ListModelDiffCallback
import org.dokiteam.doki.list.ui.model.ListModel

data class CategoryItem(
	val id: Long,
	val name: String?,
	val isSelected: Boolean
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is CategoryItem && other.id == id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is CategoryItem && previousState.isSelected != isSelected) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			null
		}
	}
}
