package com.gimenes.alex.rickandmorty.core.designsystem.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import okio.Path

/**
 * Max on-disk size for the shared character-image cache. Character avatars served by the Rick
 * and Morty API are small square JPEGs (a few KB each) and the full roster is a few hundred
 * characters, so this comfortably holds the entire cast (used across both Trivia Quiz and
 * Guess the Character) with headroom.
 */
internal const val IMAGE_DISK_CACHE_MAX_SIZE_BYTES = 25L * 1024 * 1024 // 25 MB

/**
 * Resolves the directory Coil should use for its on-disk image cache: Android's app-private
 * cache directory, iOS's `NSCachesDirectory` (see the `.android.kt`/`.ios.kt` actuals).
 *
 * This is pinned explicitly rather than left to Coil 3's own built-in default, based on reading
 * coil3 3.3.0's sources directly (`coil-core`'s `nonJsCommonMain/coil3/disk/utils.kt::
 * singletonDiskCache()`, the function silently used by [ImageLoader.Builder] when no
 * [ImageLoader.Builder.diskCache] is configured at all): that default resolves to
 * `okio.FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "coil3_disk_cache"`, i.e. wherever the platform's
 * general *temporary* directory is, not a platform-idiomatic, app-scoped cache location.
 *
 * Verified on-device (instrumented test) what that distinction actually means per platform:
 * - **Android**: `SYSTEM_TEMPORARY_DIRECTORY` (`java.io.tmpdir`) happens to already equal
 *   `Context.cacheDir` on Android's ART runtime, so Coil's default would land in a real,
 *   app-private, OS-cache-managed directory here too. That's a JVM/ART implementation detail
 *   this project shouldn't quietly depend on, though - it isn't part of any documented contract,
 *   and pinning explicitly also gives a dedicated `image_cache` subdirectory rather than sharing
 *   Coil's internal cache root with anything else that might land in `cacheDir`.
 * - **iOS**: the platform's true temporary directory (`NSTemporaryDirectory`) is a materially
 *   different, more aggressively-purged location than `NSCachesDirectory` - files there can be
 *   removed by the OS even mid-session, not just under storage pressure between launches. This is
 *   the platform where relying on Coil's default would actually risk undermining the "available
 *   offline after being viewed once" guarantee this component exists for.
 *
 * Pinning it here follows the same expect/actual split already used for this project's
 * platform-specific builders (see `core:database`'s `getRickAndMortyDatabaseBuilder`).
 */
expect fun imageDiskCacheDirectory(context: PlatformContext): Path

/**
 * Builds the shared [ImageLoader] used by every
 * [com.gimenes.alex.rickandmorty.core.designsystem.component.CachedCharacterImage] across the
 * app. Network fetches go through Ktor - [KtorNetworkFetcherFactory] is registered explicitly
 * (rather than relying on coil-network-ktor3's `ServiceLoader`-based auto-discovery, which is a
 * JVM/reflection mechanism whose behavior on Kotlin/Native/iOS isn't something this project
 * depends on implicitly) - and successful loads are persisted to [imageDiskCacheDirectory] so a
 * character image viewed once is available again on a later cold start with no network
 * connectivity.
 */
fun buildImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory()) }
        .diskCache {
            DiskCache.Builder()
                .directory(imageDiskCacheDirectory(context))
                .maxSizeBytes(IMAGE_DISK_CACHE_MAX_SIZE_BYTES)
                .build()
        }
        .build()
