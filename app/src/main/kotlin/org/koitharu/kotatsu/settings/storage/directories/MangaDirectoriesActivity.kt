package org.dokiteam.doki.settings.storage.directories

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.dokiteam.doki.R
import org.dokiteam.doki.core.exceptions.resolve.SnackbarErrorObserver
import org.dokiteam.doki.core.os.OpenDocumentTreeHelper
import org.dokiteam.doki.core.ui.BaseActivity
import org.dokiteam.doki.core.ui.list.OnListItemClickListener
import org.dokiteam.doki.core.util.ext.consumeAllSystemBarsInsets
import org.dokiteam.doki.core.util.ext.observe
import org.dokiteam.doki.core.util.ext.observeEvent
import org.dokiteam.doki.core.util.ext.tryLaunch
import org.dokiteam.doki.databinding.ActivityMangaDirectoriesBinding
import org.dokiteam.doki.settings.storage.DirectoryDiffCallback
import org.dokiteam.doki.settings.storage.DirectoryModel
import org.dokiteam.doki.settings.storage.RequestStorageManagerPermissionContract

@AndroidEntryPoint
class MangaDirectoriesActivity : BaseActivity<ActivityMangaDirectoriesBinding>(),
	OnListItemClickListener<DirectoryModel>, View.OnClickListener {

	private val viewModel: MangaDirectoriesViewModel by viewModels()
	private val pickFileTreeLauncher = OpenDocumentTreeHelper(
		activityResultCaller = this,
		flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
			or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
	) {
		if (it != null) viewModel.onCustomDirectoryPicked(it)
	}
	private val permissionRequestLauncher = registerForActivityResult(
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			RequestStorageManagerPermissionContract()
		} else {
			ActivityResultContracts.RequestPermission()
		},
	) {
		if (it) {
			viewModel.updateList()
			if (!pickFileTreeLauncher.tryLaunch(null)) {
				Snackbar.make(
					viewBinding.recyclerView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
				).show()
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMangaDirectoriesBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)
		val adapter = AsyncListDifferDelegationAdapter(DirectoryDiffCallback(), directoryConfigAD(this))
		viewBinding.recyclerView.adapter = adapter
		viewBinding.fabAdd.setOnClickListener(this)
		viewModel.items.observe(this) { adapter.items = it }
		viewModel.isLoading.observe(this) { viewBinding.progressBar.isVisible = it }
		viewModel.onError.observeEvent(
			this,
			SnackbarErrorObserver(viewBinding.root, null, exceptionResolver) {
				if (it) viewModel.updateList()
			},
		)
	}

	override fun onItemClick(item: DirectoryModel, view: View) {
		viewModel.onRemoveClick(item.file ?: return)
	}

	override fun onClick(v: View?) {
		if (!permissionRequestLauncher.tryLaunch(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			Snackbar.make(
				viewBinding.recyclerView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
			).show()
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		viewBinding.fabAdd.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			rightMargin = topMargin + barsInsets.right
			leftMargin = topMargin + barsInsets.left
			bottomMargin = topMargin + barsInsets.bottom
		}
		viewBinding.appbar.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
			top = barsInsets.top,
		)
		viewBinding.recyclerView.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
			bottom = barsInsets.bottom,
		)
		return insets.consumeAllSystemBarsInsets()
	}
}
