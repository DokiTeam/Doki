package org.dokiteam.doki.core.fs

import android.os.Build
import androidx.annotation.RequiresApi
import org.dokiteam.doki.core.util.CloseableSequence
import org.dokiteam.doki.core.util.iterator.MappingIterator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

sealed interface FileSequence : CloseableSequence<File> {

	@RequiresApi(Build.VERSION_CODES.O)
	class StreamImpl(dir: File) : FileSequence {

		private val stream = Files.newDirectoryStream(dir.toPath())

		override fun iterator(): Iterator<File> = MappingIterator(stream.iterator(), Path::toFile)

		override fun close() = stream.close()
	}

	class ListImpl(dir: File) : FileSequence {

		private val list = dir.listFiles().orEmpty()

		override fun iterator(): Iterator<File> = list.iterator()

		override fun close() = Unit
	}
}
