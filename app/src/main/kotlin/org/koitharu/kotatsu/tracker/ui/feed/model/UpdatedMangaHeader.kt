package org.dokiteam.doki.tracker.ui.feed.model

import org.dokiteam.doki.list.ui.ListModelDiffCallback
import org.dokiteam.doki.list.ui.model.ListModel
import org.dokiteam.doki.list.ui.model.MangaListModel

data class UpdatedMangaHeader(
	val list: List<MangaListModel>,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is UpdatedMangaHeader
	}

	override fun getChangePayload(previousState: ListModel): Any {
		return ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
	}
}
