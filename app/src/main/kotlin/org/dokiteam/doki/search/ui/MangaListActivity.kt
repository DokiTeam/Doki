package org.dokiteam.doki.search.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.dokiteam.doki.R
import org.dokiteam.doki.core.model.LocalMangaSource
import org.dokiteam.doki.core.model.MangaSource
import org.dokiteam.doki.core.model.getSummary
import org.dokiteam.doki.core.model.getTitle
import org.dokiteam.doki.core.model.isNsfw
import org.dokiteam.doki.core.model.parcelable.ParcelableManga
import org.dokiteam.doki.core.model.parcelable.ParcelableMangaListFilter
import org.dokiteam.doki.core.nav.AppRouter
import org.dokiteam.doki.core.nav.router
import org.dokiteam.doki.core.ui.BaseActivity
import org.dokiteam.doki.core.ui.model.titleRes
import org.dokiteam.doki.core.ui.util.FadingAppbarMediator
import org.dokiteam.doki.core.util.ViewBadge
import org.dokiteam.doki.core.util.ext.consumeSystemBarsInsets
import org.dokiteam.doki.core.util.ext.end
import org.dokiteam.doki.core.util.ext.getParcelableExtraCompat
import org.dokiteam.doki.core.util.ext.getSerializableExtraCompat
import org.dokiteam.doki.core.util.ext.getThemeColor
import org.dokiteam.doki.core.util.ext.observe
import org.dokiteam.doki.core.util.ext.setTextAndVisible
import org.dokiteam.doki.core.util.ext.start
import org.dokiteam.doki.databinding.ActivityMangaListBinding
import org.dokiteam.doki.filter.ui.FilterCoordinator
import org.dokiteam.doki.filter.ui.FilterHeaderFragment
import org.dokiteam.doki.filter.ui.sheet.FilterSheetFragment
import org.dokiteam.doki.list.ui.preview.PreviewFragment
import org.dokiteam.doki.local.ui.LocalListFragment
import org.dokiteam.doki.main.ui.owners.AppBarOwner
import org.dokiteam.doki.parsers.model.Manga
import org.dokiteam.doki.parsers.model.MangaListFilter
import org.dokiteam.doki.parsers.model.MangaSource
import org.dokiteam.doki.parsers.model.SortOrder
import org.dokiteam.doki.remotelist.ui.RemoteListFragment
import kotlin.math.absoluteValue
import com.google.android.material.R as materialR

@AndroidEntryPoint
class MangaListActivity :
	BaseActivity<ActivityMangaListBinding>(),
	AppBarOwner, View.OnClickListener,
	FilterCoordinator.Owner,
	AppBarLayout.OnOffsetChangedListener {

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override val filterCoordinator: FilterCoordinator
		get() = checkNotNull(findFilterOwner()) {
			"Cannot find FilterCoordinator.Owner fragment in ${supportFragmentManager.fragments}"
		}.filterCoordinator

	private lateinit var source: MangaSource

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMangaListBinding.inflate(layoutInflater))
		viewBinding.collapsingToolbarLayout?.let { collapsingToolbarLayout ->
			FadingAppbarMediator(viewBinding.appbar, collapsingToolbarLayout).bind()
		}
		val filter = intent.getParcelableExtraCompat<ParcelableMangaListFilter>(AppRouter.KEY_FILTER)?.filter
		val sortOrder = intent.getSerializableExtraCompat<SortOrder>(AppRouter.KEY_SORT_ORDER)
		source = MangaSource(intent.getStringExtra(AppRouter.KEY_SOURCE))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)
		if (viewBinding.containerFilterHeader != null) {
			viewBinding.appbar.addOnOffsetChangedListener(this)
		}
		viewBinding.buttonOrder?.setOnClickListener(this)
		title = source.getTitle(this)
		initList(source, filter, sortOrder)
	}

	override fun isNsfwContent(): Flow<Boolean> = flowOf(source.isNsfw())

	override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
		val container = viewBinding.containerFilterHeader ?: return
		container.background = if (verticalOffset.absoluteValue < appBarLayout.totalScrollRange) {
			container.context.getThemeColor(materialR.attr.backgroundColor).toDrawable()
		} else {
			viewBinding.collapsingToolbarLayout?.contentScrim
		}
	}

	/**
	 * Only for landscape
	 */
	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		viewBinding.cardSide?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			marginEnd = barsInsets.end(v) + resources.getDimensionPixelOffset(R.dimen.side_card_offset)
			topMargin = barsInsets.top + resources.getDimensionPixelOffset(R.dimen.grid_spacing_outer_double)
			bottomMargin = barsInsets.bottom + resources.getDimensionPixelOffset(R.dimen.side_card_offset)
		}
		viewBinding.appbar.updatePaddingRelative(
			top = barsInsets.top,
			end = if (viewBinding.cardSide == null) barsInsets.end(v) else 0,
			start = barsInsets.start(v),
		)
		return insets.consumeSystemBarsInsets(v, top = true, end = true)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_order -> router.showFilterSheet()
		}
	}

	fun showPreview(manga: Manga): Boolean = setSideFragment(
		PreviewFragment::class.java,
		bundleOf(AppRouter.KEY_MANGA to ParcelableManga(manga)),
	)

	fun hidePreview() = setSideFragment(FilterSheetFragment::class.java, null)

	private fun initList(source: MangaSource, filter: MangaListFilter?, sortOrder: SortOrder?) {
		val fm = supportFragmentManager
		val existingFragment = fm.findFragmentById(R.id.container)
		if (existingFragment is FilterCoordinator.Owner) {
			initFilter(existingFragment)
		} else {
			fm.commit {
				setReorderingAllowed(true)
				val fragment = if (source == LocalMangaSource) {
					LocalListFragment()
				} else {
					RemoteListFragment.newInstance(source)
				}
				replace(R.id.container, fragment)
				runOnCommit { initFilter(fragment) }
				if (filter != null || sortOrder != null) {
					runOnCommit(ApplyFilterRunnable(fragment, filter, sortOrder))
				}
			}
		}
	}

	private fun initFilter(filterOwner: FilterCoordinator.Owner) {
		if (viewBinding.containerSide != null) {
			if (supportFragmentManager.findFragmentById(R.id.container_side) == null) {
				setSideFragment(FilterSheetFragment::class.java, null)
			}
		} else if (viewBinding.containerFilterHeader != null) {
			if (supportFragmentManager.findFragmentById(R.id.container_filter_header) == null) {
				supportFragmentManager.commit {
					setReorderingAllowed(true)
					replace(R.id.container_filter_header, FilterHeaderFragment::class.java, null)
				}
			}
		}
		val filter = filterOwner.filterCoordinator
		val chipSort = viewBinding.buttonOrder
		if (chipSort != null) {
			val filterBadge = ViewBadge(chipSort, this)
			filterBadge.setMaxCharacterCount(0)
			filter.observe().observe(this) { snapshot ->
				chipSort.setTextAndVisible(snapshot.sortOrder.titleRes)
				filterBadge.counter = if (snapshot.listFilter.hasNonSearchOptions()) 1 else 0
			}
		} else {
			filter.observe().map {
				it.listFilter.getSummary()
			}.flowOn(Dispatchers.Default)
				.observe(this) {
					supportActionBar?.subtitle = it
				}
		}
	}

	private fun findFilterOwner(): FilterCoordinator.Owner? {
		return supportFragmentManager.findFragmentById(R.id.container) as? FilterCoordinator.Owner
	}

	private fun setSideFragment(cls: Class<out Fragment>, args: Bundle?) = if (viewBinding.containerSide != null) {
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container_side, cls, args)
		}
		true
	} else {
		false
	}

	private class ApplyFilterRunnable(
		private val filterOwner: FilterCoordinator.Owner,
		private val filter: MangaListFilter?,
		private val sortOrder: SortOrder?,
	) : Runnable {

		override fun run() {
			if (sortOrder != null) {
				filterOwner.filterCoordinator.setSortOrder(sortOrder)
			}
			if (filter != null) {
				filterOwner.filterCoordinator.setAdjusted(filter)
			}
		}
	}
}
