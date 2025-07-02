package org.dokiteam.doki.core.util

import android.content.Context
import android.text.Editable
import android.widget.EditText
import androidx.annotation.CallSuper
import org.dokiteam.doki.core.ui.util.DefaultTextWatcher
import org.dokiteam.doki.core.util.ext.getDisplayMessage
import java.lang.ref.WeakReference

abstract class EditTextValidator : DefaultTextWatcher {

	private var editTextRef: WeakReference<EditText>? = null

	protected val context: Context
		get() = checkNotNull(editTextRef?.get()?.context) {
			"EditTextValidator is not attached to EditText"
		}

	@CallSuper
	override fun afterTextChanged(s: Editable?) {
		val editText = editTextRef?.get() ?: return
		val newText = s?.toString().orEmpty()
		val result = runCatching {
			validate(newText)
		}.getOrElse { e ->
			ValidationResult.Failed(e.getDisplayMessage(editText.resources))
		}
		editText.error = when (result) {
			is ValidationResult.Failed -> result.message
			ValidationResult.Success -> null
		}
	}

	fun attachToEditText(editText: EditText) {
		editTextRef = WeakReference(editText)
		editText.removeTextChangedListener(this)
		editText.addTextChangedListener(this)
		afterTextChanged(editText.text)
	}

	abstract fun validate(text: String): ValidationResult

	sealed class ValidationResult {

		object Success : ValidationResult()

		class Failed(val message: CharSequence) : ValidationResult()
	}
}
