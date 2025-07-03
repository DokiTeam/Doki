package org.dokiteam.doki.favourites.ui.categories.select

import androidx.collection.MutableLongObjectMap
import androidx.collection.MutableLongSet
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.material.checkbox.MaterialCheckBox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.dokiteam.doki.R
import org.dokiteam.doki.core.model.FavouriteCategory
import org.dokiteam.doki.core.model.ids
import org.dokiteam.doki.core.model.parcelable.ParcelableManga
import org.dokiteam.doki.core.nav.AppRouter
import org.dokiteam.doki.core.prefs.AppSettings
import org.dokiteam.doki.core.prefs.observeAsFlow
import org.dokiteam.doki.core.ui.BaseViewModel
import org.dokiteam.doki.core.util.ext.require
import org.dokiteam.doki.favourites.domain.FavouritesRepository
import org.dokiteam.doki.favourites.ui.categories.select.model.MangaCategoryItem
import org.dokiteam.doki.list.ui.model.EmptyState
import org.dokiteam.doki.list.ui.model.ListModel
import org.dokiteam.doki.list.ui.model.LoadingState
import javax.inject.Inject

@HiltViewModel
class FavoriteDialogViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val favouritesRepository: FavouritesRepository,
	settings: AppSettings,
) : BaseViewModel() {

	val manga = savedStateHandle.require<List<ParcelableManga>>(AppRouter.KEY_MANGA_LIST).map {
		it.manga
	}

	private val refreshTrigger = MutableStateFlow(Any())
	val content = combine(
		favouritesRepository.observeCategories(),
		refreshTrigger,
		settings.observeAsFlow(AppSettings.KEY_TRACKER_ENABLED) { isTrackerEnabled },
	) { categories, _, tracker ->
		mapList(categories, tracker)
	}.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	fun setChecked(categoryId: Long, isChecked: Boolean) {
		launchJob(Dispatchers.Default) {
			if (isChecked) {
				favouritesRepository.addToCategory(categoryId, manga)
			} else {
				favouritesRepository.removeFromCategory(categoryId, manga.ids())
			}
			refreshTrigger.value = Any()
		}
	}

	private suspend fun mapList(categories: List<FavouriteCategory>, tracker: Boolean): List<ListModel> {
		if (categories.isEmpty()) {
			return listOf(
				EmptyState(
					icon = 0,
					textPrimary = R.string.empty_favourite_categories,
					textSecondary = 0,
					actionStringRes = 0,
				),
			)
		}
		val cats = MutableLongObjectMap<MutableLongSet>(categories.size)
		categories.forEach { cats[it.id] = MutableLongSet(manga.size) }
		for (m in manga) {
			val ids = favouritesRepository.getCategoriesIds(m.id)
			ids.forEach { id -> cats[id]?.add(m.id) }
		}
		return categories.map { cat ->
			MangaCategoryItem(
				category = cat,
				checkedState = when (cats[cat.id]?.size ?: 0) {
					0 -> MaterialCheckBox.STATE_UNCHECKED
					manga.size -> MaterialCheckBox.STATE_CHECKED
					else -> MaterialCheckBox.STATE_INDETERMINATE
				},
				isTrackerEnabled = tracker,
			)
		}
	}
}
