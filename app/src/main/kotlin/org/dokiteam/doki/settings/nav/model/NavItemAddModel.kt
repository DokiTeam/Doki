package org.dokiteam.doki.settings.nav.model

import org.dokiteam.doki.list.ui.model.ListModel

data class NavItemAddModel(
	val canAdd: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean = other is NavItemAddModel
}
