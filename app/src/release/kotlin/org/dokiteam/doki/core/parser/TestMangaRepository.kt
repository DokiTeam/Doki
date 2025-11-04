package org.dokiteam.doki.core.parser

import org.dokiteam.doki.core.cache.MemoryContentCache
import org.dokiteam.doki.core.model.TestMangaSource
import org.dokiteam.doki.parsers.MangaLoaderContext

@Suppress("unused")
class TestMangaRepository(
	private val loaderContext: MangaLoaderContext,
	cache: MemoryContentCache
) : EmptyMangaRepository(TestMangaSource)
