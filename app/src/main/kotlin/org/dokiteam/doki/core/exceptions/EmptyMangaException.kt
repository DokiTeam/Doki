package org.dokiteam.doki.core.exceptions

import org.dokiteam.doki.details.ui.pager.EmptyMangaReason
import org.dokiteam.doki.parsers.model.Manga

class EmptyMangaException(
    val reason: EmptyMangaReason?,
    val manga: Manga,
    cause: Throwable?
) : IllegalStateException(cause)
