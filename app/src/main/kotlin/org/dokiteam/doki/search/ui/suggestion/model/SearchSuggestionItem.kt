package org.dokiteam.doki.search.ui.suggestion.model

import androidx.annotation.StringRes
import org.dokiteam.doki.core.model.isNsfw
import org.dokiteam.doki.core.ui.widgets.ChipsView
import org.dokiteam.doki.list.ui.ListModelDiffCallback
import org.dokiteam.doki.list.ui.model.ListModel
import org.dokiteam.doki.parsers.model.Manga
import org.dokiteam.doki.parsers.model.MangaSource

sealed interface SearchSuggestionItem : ListModel {

	data class MangaList(
		val items: List<Manga>,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is MangaList
		}
	}

	data class RecentQuery(
		val query: String,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is RecentQuery && query == other.query
		}
	}

	data class Hint(
		val query: String,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Hint && query == other.query
		}
	}

	data class Author(
		val name: String,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Author && name == other.name
		}
	}

	data class Source(
		val source: MangaSource,
		val isEnabled: Boolean,
	) : SearchSuggestionItem {

		val isNsfw: Boolean
			get() = source.isNsfw()

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Source && other.source.name == source.name
		}

		override fun getChangePayload(previousState: ListModel): Any? {
			if (previousState !is Source) {
				return super.getChangePayload(previousState)
			}
			return if (isEnabled != previousState.isEnabled) {
				ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
			} else {
				null
			}
		}
	}

	data class SourceTip(
		val source: MangaSource,
	) : SearchSuggestionItem {

		val isNsfw: Boolean
			get() = source.isNsfw()

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is SourceTip && other.source.name == source.name
		}
	}

	data class Tags(
		val tags: List<ChipsView.ChipModel>,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Tags
		}

		override fun getChangePayload(previousState: ListModel): Any {
			return ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
		}
	}

	data class Text(
		@StringRes val textResId: Int,
		val error: Throwable?,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean = other is Text
			&& textResId == other.textResId
			&& error?.javaClass == other.error?.javaClass
			&& error?.message == other.error?.message
	}
}
