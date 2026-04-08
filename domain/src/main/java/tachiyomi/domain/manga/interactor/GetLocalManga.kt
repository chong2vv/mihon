package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

class GetLocalManga(
    private val mangaRepository: MangaRepository,
) {

    fun subscribe(query: String = ""): Flow<List<Manga>> {
        return mangaRepository.getLocalMangaAsFlow(query)
    }

    suspend fun await(): List<Manga> {
        return mangaRepository.getLocalManga()
    }
}
