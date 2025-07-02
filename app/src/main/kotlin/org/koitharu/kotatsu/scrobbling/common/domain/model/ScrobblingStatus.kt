package org.dokiteam.doki.scrobbling.common.domain.model

import org.dokiteam.doki.list.ui.model.ListModel

enum class ScrobblingStatus : ListModel {

	PLANNED, READING, RE_READING, COMPLETED, ON_HOLD, DROPPED;

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ScrobblingStatus && other.ordinal == ordinal
	}
}
