package org.dokiteam.doki.core.io

import java.io.OutputStream
import java.util.Objects

class NullOutputStream : OutputStream() {

	override fun write(b: Int) = Unit

	override fun write(b: ByteArray, off: Int, len: Int) {
		Objects.checkFromIndexSize(off, len, b.size)
	}
}
