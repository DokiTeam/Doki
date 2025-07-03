package org.dokiteam.doki.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.dokiteam.doki.core.util.ext.setTextAndVisible
import org.dokiteam.doki.databinding.ItemEmptyCardBinding
import org.dokiteam.doki.list.ui.model.EmptyHint
import org.dokiteam.doki.list.ui.model.ListModel

fun emptyHintAD(
	listener: ListStateHolderListener,
) = adapterDelegateViewBinding<EmptyHint, ListModel, ItemEmptyCardBinding>(
	{ inflater, parent -> ItemEmptyCardBinding.inflate(inflater, parent, false) },
) {

	binding.buttonRetry.setOnClickListener { listener.onEmptyActionClick() }

	bind {
		binding.icon.setImageAsync(item.icon)
		binding.textPrimary.setText(item.textPrimary)
		binding.textSecondary.setTextAndVisible(item.textSecondary)
		binding.buttonRetry.setTextAndVisible(item.actionStringRes)
	}
}
