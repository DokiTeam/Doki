package org.dokiteam.doki.local.ui.info

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import org.dokiteam.doki.R
import org.dokiteam.doki.core.ui.AlertDialogFragment
import org.dokiteam.doki.core.ui.widgets.SegmentedBarView
import org.dokiteam.doki.core.util.FileSize
import org.dokiteam.doki.core.util.KotatsuColors
import org.dokiteam.doki.core.util.ext.getQuantityStringSafe
import org.dokiteam.doki.core.util.ext.observe
import org.dokiteam.doki.core.util.ext.observeEvent
import org.dokiteam.doki.core.util.ext.setProgressIcon
import org.dokiteam.doki.databinding.DialogLocalInfoBinding
import androidx.appcompat.R as appcompatR

@AndroidEntryPoint
class LocalInfoDialog : AlertDialogFragment<DialogLocalInfoBinding>(), View.OnClickListener {

	private val viewModel: LocalInfoViewModel by viewModels()

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder).setTitle(R.string.saved_manga).setNegativeButton(R.string.close, null)
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): DialogLocalInfoBinding {
		return DialogLocalInfoBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: DialogLocalInfoBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		viewModel.path.observe(this) {
			binding.textViewPath.text = it
		}
		binding.chipCleanup.setOnClickListener(this)
		combine(viewModel.size, viewModel.availableSize, ::Pair).observe(viewLifecycleOwner) {
			if (it.first >= 0 && it.second >= 0) {
				setSegments(it.first, it.second)
			} else {
				binding.barView.animateSegments(emptyList())
			}
		}
		viewModel.onCleanedUp.observeEvent(viewLifecycleOwner, ::onCleanedUp)
		viewModel.isCleaningUp.observe(viewLifecycleOwner) { loading ->
			binding.chipCleanup.isClickable = !loading
			dialog?.setCancelable(!loading)
			if (loading) {
				binding.chipCleanup.setProgressIcon()
			} else {
				binding.chipCleanup.setChipIconResource(R.drawable.ic_delete)
			}
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.chip_cleanup -> viewModel.cleanup()
		}
	}

	private fun onCleanedUp(result: Pair<Int, Long>) {
		val c = context ?: return
		val text = if (result.first == 0 && result.second == 0L) {
			c.getString(R.string.no_chapters_deleted)
		} else {
			c.getString(
				R.string.chapters_deleted_pattern,
				c.resources.getQuantityStringSafe(R.plurals.chapters, result.first, result.first),
				FileSize.BYTES.format(c, result.second),
			)
		}
		Toast.makeText(c, text, Toast.LENGTH_SHORT).show()
	}

	private fun setSegments(size: Long, available: Long) {
		val view = viewBinding?.barView ?: return
		val total = size + available
		val segment = SegmentedBarView.Segment(
			percent = (size.toDouble() / total.toDouble()).toFloat(),
			color = KotatsuColors.segmentColor(view.context, appcompatR.attr.colorPrimary),
		)
		requireViewBinding().labelUsed.text = view.context.getString(
			R.string.memory_usage_pattern,
			getString(R.string.this_manga),
			FileSize.BYTES.format(view.context, size),
		)
		requireViewBinding().labelAvailable.text = view.context.getString(
			R.string.memory_usage_pattern,
			getString(R.string.available),
			FileSize.BYTES.format(view.context, available),
		)
		TextViewCompat.setCompoundDrawableTintList(
			requireViewBinding().labelUsed,
			ColorStateList.valueOf(segment.color),
		)
		view.animateSegments(listOf(segment))
	}
}
