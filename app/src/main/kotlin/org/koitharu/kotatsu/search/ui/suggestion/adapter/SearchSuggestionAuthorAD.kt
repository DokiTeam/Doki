package org.dokiteam.doki.search.ui.suggestion.adapter

import android.view.View
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.dokiteam.doki.R
import org.dokiteam.doki.databinding.ItemSearchSuggestionQueryHintBinding
import org.dokiteam.doki.search.domain.SearchKind
import org.dokiteam.doki.search.ui.suggestion.SearchSuggestionListener
import org.dokiteam.doki.search.ui.suggestion.model.SearchSuggestionItem

fun searchSuggestionAuthorAD(
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<SearchSuggestionItem.Author, SearchSuggestionItem, ItemSearchSuggestionQueryHintBinding>(
	{ inflater, parent -> ItemSearchSuggestionQueryHintBinding.inflate(inflater, parent, false) },
) {

	val viewClickListener = View.OnClickListener { _ ->
		listener.onQueryClick(item.name, SearchKind.AUTHOR, true)
	}

	binding.root.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_user, 0, 0, 0)
	binding.root.setOnClickListener(viewClickListener)

	bind {
		binding.root.text = item.name
	}
}
