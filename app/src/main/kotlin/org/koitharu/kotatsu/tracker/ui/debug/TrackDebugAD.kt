package org.dokiteam.doki.tracker.ui.debug

import android.graphics.Color
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.dokiteam.doki.R
import org.dokiteam.doki.core.ui.list.OnListItemClickListener
import org.dokiteam.doki.core.util.ext.drawableStart
import org.dokiteam.doki.core.util.ext.getThemeColor
import org.dokiteam.doki.databinding.ItemTrackDebugBinding
import org.dokiteam.doki.tracker.data.TrackEntity
import androidx.appcompat.R as appcompatR

fun trackDebugAD(
	clickListener: OnListItemClickListener<TrackDebugItem>,
) = adapterDelegateViewBinding<TrackDebugItem, TrackDebugItem, ItemTrackDebugBinding>(
	{ layoutInflater, parent -> ItemTrackDebugBinding.inflate(layoutInflater, parent, false) },
) {
	val indicatorNew = ContextCompat.getDrawable(context, R.drawable.ic_new)

	itemView.setOnClickListener { v ->
		clickListener.onItemClick(item, v)
	}

	bind {
		binding.imageViewCover.setImageAsync(item.manga.coverUrl, item.manga)
		binding.textViewTitle.text = item.manga.title
		binding.textViewSummary.text = buildSpannedString {
			append(
				item.lastCheckTime?.let {
					DateUtils.getRelativeDateTimeString(
						context,
						it.toEpochMilli(),
						DateUtils.MINUTE_IN_MILLIS,
						DateUtils.WEEK_IN_MILLIS,
						0,
					)
				} ?: getString(R.string.never),
			)
			if (item.lastResult == TrackEntity.RESULT_FAILED) {
				append(" - ")
				bold {
					color(context.getThemeColor(appcompatR.attr.colorError, Color.RED)) {
						append(item.lastError ?: getString(R.string.error))
					}
				}
			}
		}
		binding.textViewTitle.drawableStart = if (item.newChapters > 0) {
			indicatorNew
		} else {
			null
		}
	}
}
