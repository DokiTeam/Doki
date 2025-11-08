package org.dokiteam.doki.backups.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dokiteam.doki.core.model.MangaSourceSerializer
import org.dokiteam.doki.filter.data.MangaListFilterSerializer
import org.dokiteam.doki.filter.data.PersistableFilter
import org.dokiteam.doki.parsers.model.MangaListFilter
import org.dokiteam.doki.parsers.model.MangaSource

@Serializable
data class SavedFilterBackup(
    @SerialName("name")
    val name: String,
    @Serializable(with = MangaSourceSerializer::class)
    @SerialName("source")
    val source: MangaSource,
    @Serializable(with = MangaListFilterSerializer::class)
    @SerialName("filter")
    val filter: MangaListFilter,
) {

    constructor(persistableFilter: PersistableFilter) : this(
        name = persistableFilter.name,
        source = persistableFilter.source,
        filter = persistableFilter.filter,
    )

    fun toPersistableFilter() = PersistableFilter(
        name = name,
        source = source,
        filter = filter,
    )
}
