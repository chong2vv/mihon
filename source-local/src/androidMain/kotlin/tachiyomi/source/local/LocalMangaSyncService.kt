package tachiyomi.source.local

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import logcat.LogPriority
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem

class LocalMangaSyncService(
    private val localSource: LocalSource,
    private val fileSystem: LocalSourceFileSystem,
    private val coverManager: LocalCoverManager,
    private val mangaRepository: MangaRepository,
) {

    /**
     * Syncs file system state with database. Returns number of changes made.
     * - New directories → insert into DB with cover
     * - Removed directories → delete non-favorite records from DB
     * - Existing → update cover URL if missing
     */
    suspend fun sync(): Int = withIOContext {
        val fsDirs = fileSystem.getFilesInBaseDirectory()
            .filter { it.isDirectory && !it.name.orEmpty().startsWith('.') }
            .distinctBy { it.name }

        val fsNames = fsDirs.mapTo(mutableSetOf()) { it.name.orEmpty() }

        val dbManga = mangaRepository.getLocalManga()
        val dbByUrl = dbManga.associateBy { it.url }

        var changes = 0

        // New entries: in file system but not in DB
        val newDirNames = fsNames - dbByUrl.keys
        if (newDirNames.isNotEmpty()) {
            val newMangas = newDirNames.map { name ->
                async {
                    val smanga = SManga.create().apply {
                        title = name
                        url = name
                    }
                    val cover = coverManager.find(name)
                        ?: try {
                            localSource.generateCoverFromFirstChapter(name, smanga)
                        } catch (e: Exception) {
                            logcat(LogPriority.WARN, e) { "Failed to generate cover for $name" }
                            null
                        }
                    cover?.let { smanga.thumbnail_url = it.uri.toString() }
                    smanga.toDomainManga(LocalSource.ID)
                }
            }.awaitAll()

            mangaRepository.insertNetworkManga(newMangas)
            changes += newMangas.size
        }

        // Removed entries: in DB but not in file system
        val removedManga = dbManga.filter { it.url !in fsNames }
        if (removedManga.isNotEmpty()) {
            for (manga in removedManga) {
                mangaRepository.deleteLocalManga(manga.id)
            }
            changes += removedManga.size
        }

        // Existing entries: update cover if missing or changed
        val existingManga = dbManga.filter { it.url in fsNames }
        for (manga in existingManga) {
            if (manga.thumbnailUrl.isNullOrBlank()) {
                val smanga = SManga.create().apply {
                    title = manga.title
                    url = manga.url
                }
                val cover = coverManager.find(manga.url)
                    ?: try {
                        localSource.generateCoverFromFirstChapter(manga.url, smanga)
                    } catch (e: Exception) {
                        logcat(LogPriority.WARN, e) { "Failed to generate cover for ${manga.url}" }
                        null
                    }
                if (cover != null) {
                    mangaRepository.update(
                        MangaUpdate(id = manga.id, thumbnailUrl = cover.uri.toString()),
                    )
                    changes++
                }
            }
        }

        localSource.invalidateCache()
        changes
    }
}
