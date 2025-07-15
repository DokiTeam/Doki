package org.dokiteam.doki.list.ui.model

import android.content.Context
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import org.dokiteam.doki.core.model.getTitle
import org.dokiteam.doki.core.model.withOverride
import org.dokiteam.doki.core.ui.model.MangaOverride
import org.dokiteam.doki.list.ui.ListModelDiffCallback.Companion.PAYLOAD_ANYTHING_CHANGED
import org.dokiteam.doki.parsers.model.Manga
import org.dokiteam.doki.parsers.model.MangaSource
import org.dokiteam.doki.parsers.util.ifNullOrEmpty

sealed class MangaListModel : ListModel {

	abstract val override: MangaOverride?
	abstract val manga: Manga
	abstract val counter: Int

	val id: Long
		get() = manga.id

	val title: String
		get() = override?.title.ifNullOrEmpty { manga.title }

	val coverUrl: String?
		get() = override?.coverUrl.ifNullOrEmpty { manga.coverUrl }

	val source: MangaSource
		get() = manga.source

	fun toMangaWithOverride() = manga.withOverride(override)

	open fun getSummary(context: Context): CharSequence = buildSpannedString {
		bold {
			append(manga.title)
		}
		appendLine()
		if (manga.tags.isNotEmpty()) {
			manga.tags.joinTo(this) { it.title }
			appendLine()
		}
		append(manga.source.getTitle(context))
	}

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaListModel && other.javaClass == javaClass && id == other.id
	}

	override fun getChangePayload(previousState: ListModel): Any? = when {
		previousState !is MangaListModel || previousState.manga != manga -> null
		previousState.counter != counter -> PAYLOAD_ANYTHING_CHANGED
		else -> null
	}
}
