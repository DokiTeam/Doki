package org.dokiteam.doki.reader.ui.pager

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import coil3.request.ImageRequest
import com.davemorrissey.labs.subscaleview.DefaultOnImageEventListener
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dokiteam.doki.BuildConfig
import org.dokiteam.doki.R
import org.dokiteam.doki.core.exceptions.resolve.ExceptionResolver
import org.dokiteam.doki.core.image.CoilImageView
import org.dokiteam.doki.core.os.NetworkState
import org.dokiteam.doki.core.ui.list.lifecycle.LifecycleAwareViewHolder
import org.dokiteam.doki.core.util.ext.getDisplayMessage
import org.dokiteam.doki.core.util.ext.isAnimatedImage
import org.dokiteam.doki.core.util.ext.isLowRamDevice
import org.dokiteam.doki.core.util.ext.isSerializable
import org.dokiteam.doki.core.util.ext.observe
import org.dokiteam.doki.databinding.LayoutPageInfoBinding
import org.dokiteam.doki.parsers.util.ifZero
import org.dokiteam.doki.reader.domain.PageLoader
import org.dokiteam.doki.reader.ui.config.ReaderSettings
import org.dokiteam.doki.reader.ui.pager.vm.PageState
import org.dokiteam.doki.reader.ui.pager.vm.PageViewModel
import org.dokiteam.doki.reader.ui.pager.webtoon.WebtoonHolder

abstract class BasePageHolder<B : ViewBinding>(
	protected val binding: B,
	loader: PageLoader,
	readerSettingsProducer: ReaderSettings.Producer,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
	lifecycleOwner: LifecycleOwner,
) : LifecycleAwareViewHolder(binding.root, lifecycleOwner), DefaultOnImageEventListener, ComponentCallbacks2 {

	protected val viewModel = PageViewModel(
		loader = loader,
		settingsProducer = readerSettingsProducer,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
		isWebtoon = this is WebtoonHolder,
	)
	protected val bindingInfo = LayoutPageInfoBinding.bind(binding.root)
	protected abstract val ssiv: SubsamplingScaleImageView
    protected abstract val animatedImageView: CoilImageView?

	protected val settings: ReaderSettings
		get() = viewModel.settingsProducer.value

	val context: Context
		get() = itemView.context

	var boundData: ReaderPage? = null
		private set

	init {
		lifecycleScope.launch(Dispatchers.Main) {
			ssiv.bindToLifecycle(this@BasePageHolder)
			ssiv.isEagerLoadingEnabled = !context.isLowRamDevice()
			ssiv.addOnImageEventListener(viewModel)
			ssiv.addOnImageEventListener(this@BasePageHolder)
		}
		val clickListener = View.OnClickListener { v ->
			when (v.id) {
				R.id.button_retry -> viewModel.retry(
					page = boundData?.toMangaPage() ?: return@OnClickListener,
					isFromUser = true,
				)

				R.id.button_error_details -> viewModel.showErrorDetails(boundData?.url)
			}
		}
		bindingInfo.buttonRetry.setOnClickListener(clickListener)
		bindingInfo.buttonErrorDetails.setOnClickListener(clickListener)
	}

	@CallSuper
	protected open fun onConfigChanged(settings: ReaderSettings) {
		settings.applyBackground(itemView)
		if (settings.applyBitmapConfig(ssiv)) {
			reloadImage()
		} else if (viewModel.state.value is PageState.Shown) {
			onReady()
		}
		ssiv.applyDownSampling(isResumed())
	}

    fun reloadImage() {
        val state = viewModel.state.value as? PageState.Shown ?: return
        val uri = state.uri
        if (uri.isAnimatedImage(context) && animatedImageView != null) {
            showAnimatedImage(uri)
        } else {
            ssiv.setImage(state.source)
        }
    }

	fun bind(data: ReaderPage) {
		boundData = data
		viewModel.onBind(data.toMangaPage())
		onBind(data)
	}

	@CallSuper
	protected open fun onBind(data: ReaderPage) = Unit

	override fun onCreate() {
		super.onCreate()
		context.registerComponentCallbacks(this)
		viewModel.state.observe(this, ::onStateChanged)
		viewModel.settingsProducer.observe(this, ::onConfigChanged)
	}

	override fun onResume() {
		super.onResume()
		ssiv.applyDownSampling(isForeground = true)
		if (viewModel.state.value is PageState.Error && !viewModel.isLoading()) {
			boundData?.let { viewModel.retry(it.toMangaPage(), isFromUser = false) }
		}
	}

	override fun onPause() {
		super.onPause()
		ssiv.applyDownSampling(isForeground = false)
	}

	override fun onDestroy() {
		context.unregisterComponentCallbacks(this)
		super.onDestroy()
	}

	open fun onAttachedToWindow() = Unit

	open fun onDetachedFromWindow() = Unit

	@CallSuper
	open fun onRecycled() {
		viewModel.onRecycle()
		ssiv.recycle()
        animatedImageView?.disposeImage()
	}

	override fun onTrimMemory(level: Int) {
		// TODO
	}

	override fun onConfigurationChanged(newConfig: Configuration) = Unit

	@Deprecated("Deprecated in Java")
	final override fun onLowMemory() = onTrimMemory(TRIM_MEMORY_COMPLETE)

	protected open fun onStateChanged(state: PageState) {
		bindingInfo.layoutError.isVisible = state is PageState.Error
		bindingInfo.layoutProgress.isGone = state.isFinalState()
		val progress = (state as? PageState.Loading)?.progress ?: -1
		if (progress in 0..100) {
			bindingInfo.progressBar.isIndeterminate = false
			bindingInfo.progressBar.setProgressCompat(progress, true)
			bindingInfo.textViewStatus.text = context.getString(R.string.percent_string_pattern, progress.toString())
		} else {
			bindingInfo.progressBar.isIndeterminate = true
			bindingInfo.textViewStatus.setText(R.string.loading_)
		}
		when (state) {
			is PageState.Converting -> {
				bindingInfo.textViewStatus.setText(R.string.processing_)
			}

			is PageState.Empty -> Unit

			is PageState.Error -> {
				val e = state.error
				bindingInfo.textViewError.text = e.getDisplayMessage(context.resources)
				bindingInfo.buttonRetry.setText(
					ExceptionResolver.getResolveStringId(e).ifZero { R.string.try_again },
				)
				bindingInfo.buttonErrorDetails.isVisible = e.isSerializable()
				bindingInfo.layoutError.isVisible = true
				bindingInfo.progressBar.hide()
			}

			is PageState.Loaded -> {
				bindingInfo.textViewStatus.setText(R.string.preparing_)
                val uri = state.uri
                if (uri.isAnimatedImage(context) && animatedImageView != null) {
                    showAnimatedImage(uri)
                } else {
                    showStaticImage(state.source)
                }
			}

			is PageState.Loading -> {
				if (state.preview != null && ssiv.getState() == null) {
					ssiv.setImage(state.preview)
				}
			}

			is PageState.Shown -> Unit
		}
	}

    private fun showAnimatedImage(uri: android.net.Uri) {
        animatedImageView?.let { imageView ->
            ssiv.isVisible = false
            imageView.isVisible = true
            imageView.colorFilter = settings.colorFilter?.toColorFilter()
            imageView.addImageRequestListener(object : ImageRequest.Listener {
                override fun onSuccess(request: ImageRequest, result: coil3.request.SuccessResult) {
                    imageView.removeImageRequestListener(this)
                    viewModel.onImageLoaded()
                }

                override fun onError(request: ImageRequest, result: coil3.request.ErrorResult) {
                    imageView.removeImageRequestListener(this)
                    viewModel.onImageLoadError(result.throwable)
                }
            })
            imageView.setImageAsync(uri.toString())
        }
    }

    private fun showStaticImage(source: com.davemorrissey.labs.subscaleview.ImageSource) {
        animatedImageView?.let { imageView ->
            imageView.isVisible = false
            imageView.disposeImage()
        }
        ssiv.isVisible = true
        ssiv.setImage(source)
    }

	protected fun SubsamplingScaleImageView.applyDownSampling(isForeground: Boolean) {
		downSampling = when {
			isForeground || !settings.isReaderOptimizationEnabled -> 1
			BuildConfig.DEBUG -> 32
			context.isLowRamDevice() -> 8
			else -> 4
		}
	}
}
