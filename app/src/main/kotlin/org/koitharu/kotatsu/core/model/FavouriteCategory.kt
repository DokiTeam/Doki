package org.dokiteam.doki.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.dokiteam.doki.list.domain.ListSortOrder
import org.dokiteam.doki.list.ui.ListModelDiffCallback
import org.dokiteam.doki.list.ui.model.ListModel
import java.time.Instant

@Parcelize
data class FavouriteCategory(
	val id: Long,
	val title: String,
	val sortKey: Int,
	val order: ListSortOrder,
	val createdAt: Instant,
	val isTrackingEnabled: Boolean,
	val isVisibleInLibrary: Boolean,
) : Parcelable, ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is FavouriteCategory && id == other.id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		if (previousState !is FavouriteCategory) {
			return null
		}
		return if (isTrackingEnabled != previousState.isTrackingEnabled || isVisibleInLibrary != previousState.isVisibleInLibrary) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			null
		}
	}
}
