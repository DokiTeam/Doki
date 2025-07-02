package org.dokiteam.doki.scrobbling.common.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.dokiteam.doki.R

enum class ScrobblerService(
	val id: Int,
	@StringRes val titleResId: Int,
	@DrawableRes val iconResId: Int,
) {

	SHIKIMORI(1, R.string.shikimori, R.drawable.ic_shikimori),
	ANILIST(2, R.string.anilist, R.drawable.ic_anilist),
	MAL(3, R.string.mal, R.drawable.ic_mal),
	KITSU(4, R.string.kitsu, R.drawable.ic_kitsu)
}
