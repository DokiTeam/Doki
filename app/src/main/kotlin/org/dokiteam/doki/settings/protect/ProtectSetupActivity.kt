package org.dokiteam.doki.settings.protect

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import org.dokiteam.doki.R
import org.dokiteam.doki.core.ui.BaseActivity
import org.dokiteam.doki.core.ui.util.DefaultTextWatcher
import org.dokiteam.doki.core.util.ext.consumeAllSystemBarsInsets
import org.dokiteam.doki.core.util.ext.observe
import org.dokiteam.doki.core.util.ext.observeEvent
import org.dokiteam.doki.core.util.ext.systemBarsInsets
import org.dokiteam.doki.databinding.ActivitySetupProtectBinding

private const val MIN_PASSWORD_LENGTH = 4

@AndroidEntryPoint
class ProtectSetupActivity :
	BaseActivity<ActivitySetupProtectBinding>(),
	DefaultTextWatcher,
	View.OnClickListener,
	TextView.OnEditorActionListener,
	CompoundButton.OnCheckedChangeListener {

	private val viewModel by viewModels<ProtectSetupViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
		setContentView(ActivitySetupProtectBinding.inflate(layoutInflater))
		viewBinding.editPassword.addTextChangedListener(this)
		viewBinding.editPassword.setOnEditorActionListener(this)
		viewBinding.buttonNext.setOnClickListener(this)
		viewBinding.buttonCancel.setOnClickListener(this)

		viewBinding.switchBiometric.isChecked = viewModel.isBiometricEnabled
		viewBinding.switchBiometric.setOnCheckedChangeListener(this)

		viewModel.isSecondStep.observe(this, this::onStepChanged)
		viewModel.onPasswordSet.observeEvent(this) {
			finishAfterTransition()
		}
		viewModel.onPasswordMismatch.observeEvent(this) {
			viewBinding.editPassword.error = getString(R.string.passwords_mismatch)
		}
		viewModel.onClearText.observeEvent(this) {
			viewBinding.editPassword.text?.clear()
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		val basePadding = resources.getDimensionPixelOffset(R.dimen.screen_padding)
		viewBinding.root.setPadding(
			barsInsets.left + basePadding,
			barsInsets.top + basePadding,
			barsInsets.right + basePadding,
			barsInsets.bottom + basePadding,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_cancel -> finish()
			R.id.button_next -> viewModel.onNextClick(
				password = viewBinding.editPassword.text?.toString() ?: return,
			)
		}
	}

	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		viewModel.setBiometricEnabled(isChecked)
	}

	override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
		return if (actionId == EditorInfo.IME_ACTION_DONE && viewBinding.buttonNext.isEnabled) {
			viewBinding.buttonNext.performClick()
			true
		} else {
			false
		}
	}

	override fun afterTextChanged(s: Editable?) {
		viewBinding.editPassword.error = null
		val isEnoughLength = (s?.length ?: 0) >= MIN_PASSWORD_LENGTH
		viewBinding.buttonNext.isEnabled = isEnoughLength
		viewBinding.layoutPassword.isHelperTextEnabled =
			!isEnoughLength || viewModel.isSecondStep.value == true
	}

	private fun onStepChanged(isSecondStep: Boolean) {
		viewBinding.buttonCancel.isGone = isSecondStep
		viewBinding.switchBiometric.isVisible = isSecondStep && isBiometricAvailable()
		if (isSecondStep) {
			viewBinding.layoutPassword.helperText = getString(R.string.repeat_password)
			viewBinding.buttonNext.setText(R.string.confirm)
		} else {
			viewBinding.layoutPassword.helperText = getString(R.string.password_length_hint)
			viewBinding.buttonNext.setText(R.string.next)
		}
	}

	private fun isBiometricAvailable(): Boolean {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
			packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
	}
}
