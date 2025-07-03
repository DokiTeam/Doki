package org.dokiteam.doki.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.dokiteam.doki.core.util.ext.readEnumSet
import org.dokiteam.doki.core.util.ext.readParcelableCompat
import org.dokiteam.doki.core.util.ext.readSerializableCompat
import org.dokiteam.doki.core.util.ext.writeEnumSet
import org.dokiteam.doki.parsers.model.ContentRating
import org.dokiteam.doki.parsers.model.ContentType
import org.dokiteam.doki.parsers.model.Demographic
import org.dokiteam.doki.parsers.model.MangaListFilter
import org.dokiteam.doki.parsers.model.MangaState

object MangaListFilterParceler : Parceler<MangaListFilter> {

	override fun MangaListFilter.write(parcel: Parcel, flags: Int) {
		parcel.writeString(query)
		parcel.writeParcelable(ParcelableMangaTags(tags), 0)
		parcel.writeParcelable(ParcelableMangaTags(tagsExclude), 0)
		parcel.writeSerializable(locale)
		parcel.writeSerializable(originalLocale)
		parcel.writeEnumSet(states)
		parcel.writeEnumSet(contentRating)
		parcel.writeEnumSet(types)
		parcel.writeEnumSet(demographics)
		parcel.writeInt(year)
		parcel.writeInt(yearFrom)
		parcel.writeInt(yearTo)
		parcel.writeString(author)
	}

	override fun create(parcel: Parcel) = MangaListFilter(
		query = parcel.readString(),
		tags = parcel.readParcelableCompat<ParcelableMangaTags>()?.tags.orEmpty(),
		tagsExclude = parcel.readParcelableCompat<ParcelableMangaTags>()?.tags.orEmpty(),
		locale = parcel.readSerializableCompat(),
		originalLocale = parcel.readSerializableCompat(),
		states = parcel.readEnumSet<MangaState>().orEmpty(),
		contentRating = parcel.readEnumSet<ContentRating>().orEmpty(),
		types = parcel.readEnumSet<ContentType>().orEmpty(),
		demographics = parcel.readEnumSet<Demographic>().orEmpty(),
		year = parcel.readInt(),
		yearFrom = parcel.readInt(),
		yearTo = parcel.readInt(),
		author = parcel.readString(),
	)
}

@Parcelize
@TypeParceler<MangaListFilter, MangaListFilterParceler>
data class ParcelableMangaListFilter(val filter: MangaListFilter) : Parcelable
