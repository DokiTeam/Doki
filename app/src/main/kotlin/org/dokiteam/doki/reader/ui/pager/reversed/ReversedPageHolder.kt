package org.dokiteam.doki.reader.ui.pager.reversed

import android.graphics.PointF
import android.view.Gravity
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.dokiteam.doki.core.exceptions.resolve.ExceptionResolver
import org.dokiteam.doki.core.model.ZoomMode
import org.dokiteam.doki.core.os.NetworkState
import org.dokiteam.doki.databinding.ItemPageBinding
import org.dokiteam.doki.reader.domain.PageLoader
import org.dokiteam.doki.reader.ui.config.ReaderSettings
import org.dokiteam.doki.reader.ui.pager.standard.PageHolder

class ReversedPageHolder(
	owner: LifecycleOwner,
	binding: ItemPageBinding,
	loader: PageLoader,
	readerSettingsProducer: ReaderSettings.Producer,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
) : PageHolder(
	owner = owner,
	binding = binding,
	loader = loader,
	readerSettingsProducer = readerSettingsProducer,
	networkState = networkState,
	exceptionResolver = exceptionResolver,
) {

	init {
		(binding.textViewNumber.layoutParams as FrameLayout.LayoutParams)
			.gravity = Gravity.START or Gravity.BOTTOM
	}

	override fun onReady() {
		with(binding.ssiv) {
			maxScale = 2f * maxOf(
				width / sWidth.toFloat(),
				height / sHeight.toFloat(),
			)
			binding.ssiv.colorFilter = settings.colorFilter?.toColorFilter()
			when (settings.zoomMode) {
				ZoomMode.FIT_CENTER -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
					resetScaleAndCenter()
				}

				ZoomMode.FIT_HEIGHT -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
					minScale = height / sHeight.toFloat()
					setScaleAndCenter(
						minScale,
						PointF(sWidth.toFloat(), sHeight / 2f),
					)
				}

				ZoomMode.FIT_WIDTH -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
					minScale = width / sWidth.toFloat()
					setScaleAndCenter(
						minScale,
						PointF(sWidth / 2f, 0f),
					)
				}

				ZoomMode.KEEP_START -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
					setScaleAndCenter(
						maxScale,
						PointF(sWidth.toFloat(), 0f),
					)
				}
			}
		}
	}
}
