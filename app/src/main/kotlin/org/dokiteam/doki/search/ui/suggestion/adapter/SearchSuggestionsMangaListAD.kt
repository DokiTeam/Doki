package org.dokiteam.doki.search.ui.suggestion.adapter

import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.dokiteam.doki.R
import org.dokiteam.doki.core.ui.list.decor.SpacingItemDecoration
import org.dokiteam.doki.core.util.RecyclerViewScrollCallback
import org.dokiteam.doki.core.util.ext.setTooltipCompat
import org.dokiteam.doki.databinding.ItemSearchSuggestionMangaGridBinding
import org.dokiteam.doki.parsers.model.Manga
import org.dokiteam.doki.search.ui.suggestion.SearchSuggestionListener
import org.dokiteam.doki.search.ui.suggestion.model.SearchSuggestionItem

fun searchSuggestionMangaListAD(
	listener: SearchSuggestionListener,
) = adapterDelegate<SearchSuggestionItem.MangaList, SearchSuggestionItem>(R.layout.item_search_suggestion_manga_list) {
	val adapter = AsyncListDifferDelegationAdapter(
		SuggestionMangaDiffCallback(),
		searchSuggestionMangaGridAD(listener),
	)
	val recyclerView = itemView as RecyclerView
	recyclerView.adapter = adapter
	val spacing = context.resources.getDimensionPixelOffset(R.dimen.search_suggestions_manga_spacing)
	recyclerView.updatePadding(
		left = recyclerView.paddingLeft - spacing,
		right = recyclerView.paddingRight - spacing,
	)
	recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
	val scrollResetCallback = RecyclerViewScrollCallback(recyclerView, 0, 0)

	bind {
		adapter.setItems(item.items, scrollResetCallback)
	}
}

private fun searchSuggestionMangaGridAD(
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<Manga, Manga, ItemSearchSuggestionMangaGridBinding>(
	{ layoutInflater, parent -> ItemSearchSuggestionMangaGridBinding.inflate(layoutInflater, parent, false) },
) {
	itemView.setOnClickListener {
		listener.onMangaClick(item)
	}

	bind {
		itemView.setTooltipCompat(item.title)
		binding.imageViewCover.setImageAsync(item.coverUrl, item.source)
		binding.textViewTitle.text = item.title
	}
}

private class SuggestionMangaDiffCallback : DiffUtil.ItemCallback<Manga>() {

	override fun areItemsTheSame(oldItem: Manga, newItem: Manga): Boolean {
		return oldItem.id == newItem.id
	}

	override fun areContentsTheSame(oldItem: Manga, newItem: Manga): Boolean {
		return oldItem.title == newItem.title && oldItem.coverUrl == newItem.coverUrl
	}
}
