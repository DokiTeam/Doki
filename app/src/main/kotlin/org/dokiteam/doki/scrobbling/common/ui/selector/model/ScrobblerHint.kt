package org.dokiteam.doki.scrobbling.common.ui.selector.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.dokiteam.doki.list.ui.model.ListModel

data class ScrobblerHint(
	@DrawableRes val icon: Int,
	@StringRes val textPrimary: Int,
	@StringRes val textSecondary: Int,
	val error: Throwable?,
	@StringRes val actionStringRes: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ScrobblerHint && other.textPrimary == textPrimary
	}
}
