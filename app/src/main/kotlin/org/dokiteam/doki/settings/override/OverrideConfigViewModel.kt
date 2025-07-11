package org.dokiteam.doki.settings.override

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.dokiteam.doki.core.model.parcelable.ParcelableManga
import org.dokiteam.doki.core.nav.AppRouter
import org.dokiteam.doki.core.parser.MangaDataRepository
import org.dokiteam.doki.core.ui.BaseViewModel
import org.dokiteam.doki.core.ui.model.MangaOverride
import org.dokiteam.doki.core.util.ext.MutableEventFlow
import org.dokiteam.doki.core.util.ext.call
import org.dokiteam.doki.core.util.ext.require
import org.dokiteam.doki.parsers.model.Manga
import javax.inject.Inject

@HiltViewModel
class OverrideConfigViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val dataRepository: MangaDataRepository,
) : BaseViewModel() {

	private val manga = savedStateHandle.require<ParcelableManga>(AppRouter.KEY_MANGA).manga

	val data = MutableStateFlow<Pair<Manga, MangaOverride>?>(null)
	val onSaved = MutableEventFlow<Unit>()

	init {
		launchLoadingJob(Dispatchers.Default) {
			data.value = manga to (dataRepository.getOverride(manga.id) ?: emptyOverride())
		}
	}

	fun save(title: String?) {
		launchLoadingJob(Dispatchers.Default) {
			val override = checkNotNull(data.value).second.copy(
				title = title,
			)
			dataRepository.setOverride(manga.id, override)
			onSaved.call(Unit)
		}
	}

	fun updateCover(coverUri: String?) {
		val snapshot = data.value ?: return
		data.value = snapshot.first to snapshot.second.copy(
			coverUrl = coverUri,
		)
	}

	private fun emptyOverride() = MangaOverride(null, null, null)
}
