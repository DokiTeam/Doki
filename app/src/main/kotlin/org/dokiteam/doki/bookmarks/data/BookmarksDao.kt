package org.dokiteam.doki.bookmarks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.dokiteam.doki.core.db.entity.MangaWithTags

@Dao
abstract class BookmarksDao {

	@Query("SELECT * FROM bookmarks WHERE page_id = :pageId")
	abstract suspend fun find(pageId: Long): BookmarkEntity?

	@Transaction
	@Query(
		"SELECT * FROM manga JOIN bookmarks ON bookmarks.manga_id = manga.manga_id ORDER BY percent LIMIT :limit OFFSET :offset",
	)
	abstract suspend fun findAll(offset: Int, limit: Int): Map<MangaWithTags, List<BookmarkEntity>>

	@Query("SELECT * FROM bookmarks WHERE manga_id = :mangaId AND chapter_id = :chapterId AND page = :page ORDER BY percent")
	abstract fun observe(mangaId: Long, chapterId: Long, page: Int): Flow<BookmarkEntity?>

	@Query("SELECT * FROM bookmarks WHERE manga_id = :mangaId ORDER BY percent")
	abstract fun observe(mangaId: Long): Flow<List<BookmarkEntity>>

	@Transaction
	@Query(
		"SELECT * FROM manga JOIN bookmarks ON bookmarks.manga_id = manga.manga_id ORDER BY percent",
	)
	abstract fun observe(): Flow<Map<MangaWithTags, List<BookmarkEntity>>>

	@Insert
	abstract suspend fun insert(entity: BookmarkEntity)

	@Delete
	abstract suspend fun delete(entity: BookmarkEntity)

	@Query("DELETE FROM bookmarks WHERE page_id = :pageId")
	abstract suspend fun delete(pageId: Long): Int

	@Query("DELETE FROM bookmarks WHERE manga_id = :mangaId AND chapter_id = :chapterId AND page = :page")
	abstract suspend fun delete(mangaId: Long, chapterId: Long, page: Int): Int

	@Upsert
	abstract suspend fun upsert(bookmarks: Collection<BookmarkEntity>)

	fun dump(): Flow<Pair<MangaWithTags, List<BookmarkEntity>>> = flow {
		val window = 4
		var offset = 0
		while (currentCoroutineContext().isActive) {
			val list = findAll(offset, window)
			if (list.isEmpty()) {
				break
			}
			offset += window
			list.forEach { emit(it.key to it.value) }
		}
	}
}
