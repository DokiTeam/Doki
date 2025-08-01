package org.dokiteam.doki.settings.about

import android.Manifest
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.dokiteam.doki.R
import org.dokiteam.doki.core.github.AppVersion
import org.dokiteam.doki.core.nav.router
import org.dokiteam.doki.core.ui.BaseActivity
import org.dokiteam.doki.core.util.FileSize
import org.dokiteam.doki.core.util.ext.consumeAllSystemBarsInsets
import org.dokiteam.doki.core.util.ext.getDisplayMessage
import org.dokiteam.doki.core.util.ext.observe
import org.dokiteam.doki.core.util.ext.observeEvent
import org.dokiteam.doki.core.util.ext.printStackTraceDebug
import org.dokiteam.doki.core.util.ext.setTextAndVisible
import org.dokiteam.doki.core.util.ext.showOrHide
import org.dokiteam.doki.core.util.ext.systemBarsInsets
import org.dokiteam.doki.core.util.ext.textAndVisible
import org.dokiteam.doki.databinding.ActivityAppUpdateBinding

@AndroidEntryPoint
class AppUpdateActivity : BaseActivity<ActivityAppUpdateBinding>(), View.OnClickListener {

	private val viewModel: AppUpdateViewModel by viewModels()
	private lateinit var downloadReceiver: UpdateDownloadReceiver

	private val permissionRequest = registerForActivityResult(
		ActivityResultContracts.RequestPermission(),
	) {
		if (it) {
			viewModel.startDownload()
		} else {
			openInBrowser()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityAppUpdateBinding.inflate(layoutInflater))
		downloadReceiver = UpdateDownloadReceiver(viewModel)
		viewModel.nextVersion.observe(this, ::onNextVersionChanged)
		viewBinding.buttonCancel.setOnClickListener(this)
		viewBinding.buttonUpdate.setOnClickListener(this)

		ContextCompat.registerReceiver(
			this,
			downloadReceiver,
			IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
			ContextCompat.RECEIVER_EXPORTED,
		)
		combine(viewModel.isLoading, viewModel.downloadProgress, ::Pair)
			.observe(this, ::onProgressChanged)
		viewModel.downloadState.observe(this, ::onDownloadStateChanged)
		viewModel.onError.observeEvent(this, ::onError)
		viewModel.onDownloadDone.observeEvent(this) { intent ->
			try {
				startActivity(intent)
			} catch (e: ActivityNotFoundException) {
				e.printStackTraceDebug()
			}
		}
	}

	override fun onDestroy() {
		unregisterReceiver(downloadReceiver)
		super.onDestroy()
	}

	override fun onApplyWindowInsets(
		v: View,
		insets: WindowInsetsCompat
	): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		viewBinding.root.updatePadding(top = barsInsets.top)
		viewBinding.dockedToolbarChild.updateLayoutParams<MarginLayoutParams> {
			leftMargin = barsInsets.left
			rightMargin = barsInsets.right
			bottomMargin = barsInsets.bottom
		}
		viewBinding.scrollView.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_cancel -> finishAfterTransition()
			R.id.button_update -> doUpdate()
		}
	}

	private suspend fun onNextVersionChanged(version: AppVersion?) {
		viewBinding.buttonUpdate.isEnabled = version != null && !viewModel.isLoading.value
		if (version == null) {
			viewBinding.textViewContent.setText(R.string.loading_)
			return
		}
		val markwon = Markwon.create(this)
		val message = withContext(Dispatchers.Default) {
			buildSpannedString {
				append(getString(R.string.new_version_s, version.name))
				appendLine()
				append(getString(R.string.size_s, FileSize.BYTES.format(this@AppUpdateActivity, version.apkSize)))
				appendLine()
				appendLine()
				append(markwon.toMarkdown(version.description))
			}
		}
		markwon.setParsedMarkdown(viewBinding.textViewContent, message)
	}

	private fun doUpdate() {
		viewModel.installIntent.value?.let { intent ->
			try {
				startActivity(intent)
			} catch (e: ActivityNotFoundException) {
				onError(e)
			}
			return
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			permissionRequest.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
		} else {
			viewModel.startDownload()
		}
	}

	private fun openInBrowser() {
		val latestVersion = viewModel.nextVersion.value ?: return
		if (!router.openExternalBrowser(latestVersion.url, getString(R.string.open_in_browser))) {
			Snackbar.make(viewBinding.scrollView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
		}
	}

	private fun onProgressChanged(value: Pair<Boolean, Float>) {
		val (isLoading, downloadProgress) = value
		val indicator = viewBinding.progressBar
		indicator.showOrHide(isLoading)
		indicator.isIndeterminate = downloadProgress <= 0f
		if (downloadProgress > 0f) {
			indicator.setProgressCompat((indicator.max * downloadProgress).toInt(), true)
		}
		viewBinding.buttonUpdate.isEnabled = !isLoading && viewModel.nextVersion.value != null
	}

	private fun onDownloadStateChanged(state: Int) {
		val message = when (state) {
			DownloadManager.STATUS_FAILED -> R.string.error_occurred
			DownloadManager.STATUS_PAUSED -> R.string.downloads_paused
			else -> 0
		}
		viewBinding.textViewError.setTextAndVisible(message)
	}

	private fun onError(e: Throwable) {
		viewBinding.textViewError.textAndVisible = e.getDisplayMessage(resources)
	}

	private class UpdateDownloadReceiver(
		private val viewModel: AppUpdateViewModel,
	) : BroadcastReceiver() {

		override fun onReceive(context: Context, intent: Intent) {
			when (intent.action) {
				DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
					viewModel.onDownloadComplete(intent)
				}
			}
		}
	}
}
