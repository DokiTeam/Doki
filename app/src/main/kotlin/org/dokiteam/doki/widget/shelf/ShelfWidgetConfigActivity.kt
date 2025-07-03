package org.dokiteam.doki.widget.shelf

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import org.dokiteam.doki.R
import org.dokiteam.doki.core.exceptions.resolve.SnackbarErrorObserver
import org.dokiteam.doki.core.prefs.AppWidgetConfig
import org.dokiteam.doki.core.ui.BaseActivity
import org.dokiteam.doki.core.ui.list.OnListItemClickListener
import org.dokiteam.doki.core.util.ext.consumeAllSystemBarsInsets
import org.dokiteam.doki.core.util.ext.observe
import org.dokiteam.doki.core.util.ext.observeEvent
import org.dokiteam.doki.core.util.ext.systemBarsInsets
import org.dokiteam.doki.databinding.ActivityAppwidgetShelfBinding
import org.dokiteam.doki.widget.shelf.adapter.CategorySelectAdapter
import org.dokiteam.doki.widget.shelf.model.CategoryItem

@AndroidEntryPoint
class ShelfWidgetConfigActivity :
	BaseActivity<ActivityAppwidgetShelfBinding>(),
	OnListItemClickListener<CategoryItem>,
	View.OnClickListener {

	private val viewModel by viewModels<ShelfConfigViewModel>()

	private lateinit var adapter: CategorySelectAdapter
	private lateinit var config: AppWidgetConfig

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityAppwidgetShelfBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = true)
		adapter = CategorySelectAdapter(this)
		viewBinding.recyclerView.adapter = adapter
		viewBinding.buttonDone.setOnClickListener(this)
		val appWidgetId = intent?.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID,
		) ?: AppWidgetManager.INVALID_APPWIDGET_ID
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finishAfterTransition()
			return
		}
		config = AppWidgetConfig(this, ShelfWidgetProvider::class.java, appWidgetId)
		viewModel.checkedId = config.categoryId
		viewBinding.switchBackground.isChecked = config.hasBackground

		viewModel.content.observe(this, adapter)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.recyclerView, null))
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		viewBinding.recyclerView.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
			bottom = barsInsets.bottom,
		)
		viewBinding.appbar.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
			top = barsInsets.top,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_done -> {
				config.categoryId = viewModel.checkedId
				config.hasBackground = viewBinding.switchBackground.isChecked
				updateWidget()
				setResult(
					RESULT_OK,
					Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId),
				)
				finish()
			}
		}
	}

	override fun onItemClick(item: CategoryItem, view: View) {
		viewModel.checkedId = item.id
	}

	private fun updateWidget() {
		val intent = Intent(this, ShelfWidgetProvider::class.java)
		intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
		val ids = intArrayOf(config.widgetId)
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
		sendBroadcast(intent)
	}
}
