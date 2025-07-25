package org.dokiteam.doki.reader.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import org.dokiteam.doki.core.util.ext.getEnumValue
import org.dokiteam.doki.core.util.ext.observeChanges
import org.dokiteam.doki.core.util.ext.putAll
import org.dokiteam.doki.core.util.ext.putEnumValue
import org.dokiteam.doki.reader.domain.TapGridArea
import org.dokiteam.doki.reader.ui.tapgrid.TapAction
import javax.inject.Inject

@Reusable
class TapGridSettings @Inject constructor(@ApplicationContext context: Context) {

	private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

	init {
		if (!prefs.getBoolean(KEY_INIT, false)) {
			initPrefs(withDefaultValues = true)
		}
	}

	fun getTapAction(area: TapGridArea, isLongTap: Boolean): TapAction? {
		val key = getPrefKey(area, isLongTap)
		return prefs.getEnumValue(key, TapAction::class.java)
	}

	fun setTapAction(area: TapGridArea, isLongTap: Boolean, action: TapAction?) {
		val key = getPrefKey(area, isLongTap)
		prefs.edit { putEnumValue(key, action) }
	}

	fun reset() {
		initPrefs(withDefaultValues = true)
	}

	fun disableAll() {
		initPrefs(withDefaultValues = false)
	}

	fun observeChanges() = prefs.observeChanges().flowOn(Dispatchers.IO)

	fun getAllValues(): Map<String, *> = prefs.all

	fun upsertAll(m: Map<String, *>) = prefs.edit {
		clear()
		putAll(m)
	}

	private fun initPrefs(withDefaultValues: Boolean) {
		prefs.edit {
			clear()
			if (withDefaultValues) {
				initDefaultActions(this)
			}
			putBoolean(KEY_INIT, true)
		}
	}

	private fun getPrefKey(area: TapGridArea, isLongTap: Boolean): String = if (isLongTap) {
		area.name + SUFFIX_LONG
	} else {
		area.name
	}

	private fun initDefaultActions(editor: SharedPreferences.Editor) {
		editor.putEnumValue(getPrefKey(TapGridArea.TOP_LEFT, false), TapAction.PAGE_PREV)
		editor.putEnumValue(getPrefKey(TapGridArea.TOP_CENTER, false), TapAction.PAGE_PREV)
		editor.putEnumValue(getPrefKey(TapGridArea.CENTER_LEFT, false), TapAction.PAGE_PREV)
		editor.putEnumValue(getPrefKey(TapGridArea.BOTTOM_LEFT, false), TapAction.PAGE_PREV)

		editor.putEnumValue(getPrefKey(TapGridArea.CENTER, false), TapAction.TOGGLE_UI)
		editor.putEnumValue(getPrefKey(TapGridArea.CENTER, true), TapAction.SHOW_MENU)

		editor.putEnumValue(getPrefKey(TapGridArea.TOP_RIGHT, false), TapAction.PAGE_NEXT)
		editor.putEnumValue(getPrefKey(TapGridArea.CENTER_RIGHT, false), TapAction.PAGE_NEXT)
		editor.putEnumValue(getPrefKey(TapGridArea.BOTTOM_CENTER, false), TapAction.PAGE_NEXT)
		editor.putEnumValue(getPrefKey(TapGridArea.BOTTOM_RIGHT, false), TapAction.PAGE_NEXT)
	}

	private companion object {

		private const val PREFS_NAME = "tap_grid"
		private const val KEY_INIT = "_init"
		private const val SUFFIX_LONG = "_long"
	}
}
