package org.dokiteam.doki.list.domain

import org.dokiteam.doki.core.prefs.ProgressIndicatorMode
import org.dokiteam.doki.core.prefs.ProgressIndicatorMode.CHAPTERS_LEFT
import org.dokiteam.doki.core.prefs.ProgressIndicatorMode.CHAPTERS_READ
import org.dokiteam.doki.core.prefs.ProgressIndicatorMode.NONE
import org.dokiteam.doki.core.prefs.ProgressIndicatorMode.PERCENT_LEFT
import org.dokiteam.doki.core.prefs.ProgressIndicatorMode.PERCENT_READ

data class ReadingProgress(
	val percent: Float,
	val totalChapters: Int,
	val mode: ProgressIndicatorMode,
) {

	val percentLeft: Float
		get() = 1f - percent

	val chapters: Int
		get() = (totalChapters * percent).toInt()

	val chaptersLeft: Int
		get() = (totalChapters * percentLeft).toInt()

	fun isValid() = when (mode) {
		NONE -> false
		PERCENT_READ,
		PERCENT_LEFT -> percent in 0f..1f

		CHAPTERS_READ,
		CHAPTERS_LEFT -> totalChapters > 0 && percent in 0f..1f
	}

	fun isCompleted() = isCompleted(percent)

	fun isReversed() = mode == PERCENT_LEFT || mode == CHAPTERS_LEFT

	companion object {

		const val PROGRESS_NONE = -1f
		const val PROGRESS_COMPLETED = 1f
		private const val PROGRESS_COMPLETED_THRESHOLD = 0.99999f

		fun isValid(percent: Float) = percent in 0f..1f

		fun isCompleted(percent: Float) = percent >= PROGRESS_COMPLETED_THRESHOLD

		fun percentToString(percent: Float): String = if (isValid(percent)) {
			if (isCompleted(percent)) "100" else (percent * 100f).toInt().toString()
		} else {
			"0"
		}
	}
}
