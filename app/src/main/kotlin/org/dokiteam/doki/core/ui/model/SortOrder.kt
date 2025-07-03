package org.dokiteam.doki.core.ui.model

import androidx.annotation.StringRes
import org.dokiteam.doki.R
import org.dokiteam.doki.core.model.SortDirection
import org.dokiteam.doki.parsers.model.SortOrder
import org.dokiteam.doki.parsers.model.SortOrder.ADDED
import org.dokiteam.doki.parsers.model.SortOrder.ADDED_ASC
import org.dokiteam.doki.parsers.model.SortOrder.ALPHABETICAL
import org.dokiteam.doki.parsers.model.SortOrder.ALPHABETICAL_DESC
import org.dokiteam.doki.parsers.model.SortOrder.NEWEST
import org.dokiteam.doki.parsers.model.SortOrder.NEWEST_ASC
import org.dokiteam.doki.parsers.model.SortOrder.POPULARITY
import org.dokiteam.doki.parsers.model.SortOrder.POPULARITY_ASC
import org.dokiteam.doki.parsers.model.SortOrder.POPULARITY_HOUR
import org.dokiteam.doki.parsers.model.SortOrder.POPULARITY_MONTH
import org.dokiteam.doki.parsers.model.SortOrder.POPULARITY_TODAY
import org.dokiteam.doki.parsers.model.SortOrder.POPULARITY_WEEK
import org.dokiteam.doki.parsers.model.SortOrder.POPULARITY_YEAR
import org.dokiteam.doki.parsers.model.SortOrder.RATING
import org.dokiteam.doki.parsers.model.SortOrder.RATING_ASC
import org.dokiteam.doki.parsers.model.SortOrder.RELEVANCE
import org.dokiteam.doki.parsers.model.SortOrder.UPDATED
import org.dokiteam.doki.parsers.model.SortOrder.UPDATED_ASC

@get:StringRes
val SortOrder.titleRes: Int
	get() = when (this) {
		UPDATED -> R.string.updated
		POPULARITY -> R.string.popular
		RATING -> R.string.by_rating
		NEWEST -> R.string.newest
		ALPHABETICAL -> R.string.by_name
		ALPHABETICAL_DESC -> R.string.by_name_reverse
		UPDATED_ASC -> R.string.updated_long_ago
		POPULARITY_ASC -> R.string.unpopular
		RATING_ASC -> R.string.low_rating
		NEWEST_ASC -> R.string.order_oldest
		ADDED -> R.string.recently_added
		ADDED_ASC -> R.string.added_long_ago
		RELEVANCE -> R.string.by_relevance
		POPULARITY_HOUR -> R.string.popular_in_hour
		POPULARITY_TODAY -> R.string.popular_today
		POPULARITY_WEEK -> R.string.popular_in_week
		POPULARITY_MONTH -> R.string.popular_in_month
		POPULARITY_YEAR -> R.string.popular_in_year
	}

val SortOrder.direction: SortDirection
	get() = when (this) {
		UPDATED_ASC,
		POPULARITY_ASC,
		RATING_ASC,
		NEWEST_ASC,
		ADDED_ASC,
		ALPHABETICAL -> SortDirection.ASC

		UPDATED,
		POPULARITY,
		POPULARITY_HOUR,
		POPULARITY_TODAY,
		POPULARITY_WEEK,
		POPULARITY_MONTH,
		POPULARITY_YEAR,
		RATING,
		NEWEST,
		ADDED,
		RELEVANCE,
		ALPHABETICAL_DESC -> SortDirection.DESC
	}
