package org.dokiteam.doki.bookmarks.ui

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.dokiteam.doki.bookmarks.domain.Bookmark
import org.dokiteam.doki.core.util.ext.getItem
import org.dokiteam.doki.list.ui.MangaSelectionDecoration

class BookmarksSelectionDecoration(context: Context) : MangaSelectionDecoration(context) {

	override fun getItemId(parent: RecyclerView, child: View): Long {
		val holder = parent.getChildViewHolder(child) ?: return RecyclerView.NO_ID
		val item = holder.getItem(Bookmark::class.java) ?: return RecyclerView.NO_ID
		return item.pageId
	}
}
