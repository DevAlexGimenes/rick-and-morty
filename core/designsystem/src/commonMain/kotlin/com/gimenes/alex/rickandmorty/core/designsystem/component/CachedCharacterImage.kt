package com.gimenes.alex.rickandmorty.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.setSingletonImageLoaderFactory
import com.gimenes.alex.rickandmorty.core.designsystem.image.buildImageLoader
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyExtendedTheme
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyShapes

/** Character avatars served by the Rick and Morty API are square (300x300) JPEGs. */
private const val CHARACTER_IMAGE_ASPECT_RATIO = 1f

/**
 * Shared, disk-cached character image tile. Loads [imageUrl] over the network the first time
 * it's viewed and, from then on, is available from Coil's on-disk cache (see
 * [com.gimenes.alex.rickandmorty.core.designsystem.image.imageDiskCacheDirectory]) even with no
 * network connectivity - including on a true cold app start with no prior successful session for
 * that particular character.
 *
 * The container has a fixed 1:1 aspect ratio so layout doesn't jump between game rounds while
 * different images load. [contentDescription] is intentionally not defaulted or derived
 * internally: callers decide the accessibility text, since the right wording differs by context
 * (e.g. hidden/generic before a guess is made vs. the character's name once revealed).
 *
 * @param imageUrl the character avatar URL to load.
 * @param contentDescription accessibility text for the image; pass `null` only if the image is
 *  purely decorative in that context (e.g. it's paired with adjacent text that already conveys
 *  the same information).
 * @param modifier applied to the outer, fixed-aspect-ratio container.
 */
@Composable
fun CachedCharacterImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    // Idempotent: setSafe() is a no-op once the singleton ImageLoader has already been created,
    // so it's safe to call from every call site of this composable. This is what lets the
    // component configure Coil's disk cache correctly (see buildImageLoader) without requiring
    // every app entry point to remember to wire it up themselves.
    setSingletonImageLoaderFactory { context -> buildImageLoader(context) }

    Box(
        modifier = modifier
            .aspectRatio(CHARACTER_IMAGE_ASPECT_RATIO)
            .clip(RickAndMortyShapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = { CharacterImageLoadingState() },
            error = { CharacterImageErrorState() }
        )
    }
}

@Composable
private fun CharacterImageLoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RickAndMortyExtendedTheme.spacing.sm)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Scanning dimensions...",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CharacterImageErrorState() {
    Column(
        modifier = Modifier.padding(RickAndMortyExtendedTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RickAndMortyExtendedTheme.spacing.sm)
    ) {
        PortalSignalLostIcon()
        Text(
            text = "Signal lost across dimensions",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A small "dim, broken portal ring" glyph used as the error/placeholder icon instead of a
 * generic/broken-image icon, drawn with [Canvas] rather than pulled from a Material icon pack
 * (not currently a dependency of this module, and not warranted for a single on-brand glyph).
 */
@Composable
private fun PortalSignalLostIcon() {
    val color = MaterialTheme.colorScheme.error
    Canvas(modifier = Modifier.size(32.dp)) {
        val strokeWidth = size.minDimension * 0.12f
        drawCircle(
            color = color,
            radius = size.minDimension / 2f - strokeWidth / 2f,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.22f, size.height * 0.22f),
            end = Offset(size.width * 0.78f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
