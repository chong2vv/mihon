package tachiyomi.source.local.image

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.io.LocalSourceFileSystem
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_COVER_NAME = "cover.jpg"

actual class LocalCoverManager(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
) {

    private val coverCache = ConcurrentHashMap<String, CoverCacheEntry>()
    private val negativeCacheDuration: Long = 60_000L // 60 seconds TTL for negative cache

    actual fun find(mangaUrl: String): UniFile? {
        coverCache[mangaUrl]?.let { entry ->
            if (entry.file != null && entry.file.exists()) return entry.file
            if (entry.file != null) {
                coverCache.remove(mangaUrl)
            } else {
                // Negative cache: check TTL before returning null
                if (System.currentTimeMillis() - entry.timestamp < negativeCacheDuration) {
                    return null
                }
                coverCache.remove(mangaUrl)
            }
        }
        return findAndCache(mangaUrl)
    }

    private fun findAndCache(mangaUrl: String): UniFile? {
        val result = fileSystem.getFilesInMangaDirectory(mangaUrl)
            .filter { it.isFile && it.nameWithoutExtension.equals("cover", ignoreCase = true) }
            .firstOrNull { ImageUtil.isImage(it.name) { it.openInputStream() } }
        coverCache[mangaUrl] = CoverCacheEntry(result, System.currentTimeMillis())
        return result
    }

    private data class CoverCacheEntry(val file: UniFile?, val timestamp: Long)

    actual fun update(
        manga: SManga,
        inputStream: InputStream,
    ): UniFile? {
        val directory = fileSystem.getMangaDirectory(manga.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        val targetFile = find(manga.url) ?: directory.createFile(DEFAULT_COVER_NAME)!!

        inputStream.use { input ->
            targetFile.openOutputStream().use { output ->
                input.copyTo(output)
            }
        }

        DiskUtil.createNoMediaFile(directory, context)

        manga.thumbnail_url = targetFile.uri.toString()
        coverCache[manga.url] = CoverCacheEntry(targetFile, System.currentTimeMillis())
        return targetFile
    }

    fun clearCache() {
        coverCache.clear()
    }

    fun invalidate(mangaUrl: String) {
        coverCache.remove(mangaUrl)
    }
}
