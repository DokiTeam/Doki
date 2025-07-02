package org.dokiteam.doki.scrobbling.common.domain.model

import org.dokiteam.doki.list.ui.model.ListModel

data class ScrobblerManga(
	val id: Long,
	val name: String,
	val altName: String?,
	val cover: String?,
	val url: String,
	val isBestMatch: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ScrobblerManga && other.id == id
	}

	override fun toString(): String {
		return "ScrobblerManga #$id \"$name\" $url"
	}
}
