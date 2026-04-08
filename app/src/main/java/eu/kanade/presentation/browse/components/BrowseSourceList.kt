package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.library.components.CommonMangaItemDefaults
import eu.kanade.presentation.library.components.MangaListItem
import kotlinx.coroutines.flow.StateFlow
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceList(
    mangaList: LazyPagingItems<StateFlow<Manga>>,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
    selection: Set<Long> = emptySet(),
    isLocalSource: Boolean = false,
    onDeleteSwipe: ((Manga) -> Unit)? = null,
) {
    LazyColumn(
        contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
    ) {
        item {
            if (mangaList.loadState.prepend is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }

        items(count = mangaList.itemCount) { index ->
            val manga by mangaList[index]?.collectAsState() ?: return@items
            if (isLocalSource && onDeleteSwipe != null && selection.isEmpty()) {
                val deleteAction = SwipeAction(
                    icon = {
                        Icon(
                            modifier = Modifier.padding(16.dp),
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    },
                    background = MaterialTheme.colorScheme.errorContainer,
                    onSwipe = { onDeleteSwipe(manga) },
                )
                SwipeableActionsBox(
                    modifier = Modifier.clipToBounds(),
                    endActions = listOf(deleteAction),
                    swipeThreshold = 56.dp,
                    backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    BrowseSourceListItem(
                        manga = manga,
                        onClick = { onMangaClick(manga) },
                        onLongClick = { onMangaLongClick(manga) },
                        isSelected = manga.id in selection,
                    )
                }
            } else {
                BrowseSourceListItem(
                    manga = manga,
                    onClick = { onMangaClick(manga) },
                    onLongClick = { onMangaLongClick(manga) },
                    isSelected = manga.id in selection,
                )
            }
        }

        item {
            if (mangaList.loadState.refresh is LoadState.Loading || mangaList.loadState.append is LoadState.Loading) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseSourceListItem(
    manga: Manga,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
    isSelected: Boolean = false,
) {
    MangaListItem(
        isSelected = isSelected,
        title = manga.title,
        coverData = MangaCover(
            mangaId = manga.id,
            sourceId = manga.source,
            isMangaFavorite = manga.favorite,
            url = manga.thumbnailUrl,
            lastModified = manga.coverLastModified,
        ),
        coverAlpha = if (manga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        badge = {
            InLibraryBadge(enabled = manga.favorite)
        },
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
