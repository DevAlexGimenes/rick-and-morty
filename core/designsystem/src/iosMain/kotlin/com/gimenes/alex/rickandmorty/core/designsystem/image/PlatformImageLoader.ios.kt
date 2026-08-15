package com.gimenes.alex.rickandmorty.core.designsystem.image

import coil3.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS's `Caches` directory (`NSCachesDirectory`) - the platform-idiomatic equivalent of
 * Android's `Context.cacheDir`: not backed up to iCloud/iTunes, and eligible for the OS to purge
 * under disk pressure, but otherwise persisted across app launches, unlike `NSTemporaryDirectory`
 * (which the app - and the OS - can clear far more aggressively, and which is what Coil 3's own
 * built-in default disk cache location resolves to; see [imageDiskCacheDirectory]'s doc comment
 * on the common `expect` declaration for what was verified about that default).
 */
@OptIn(ExperimentalForeignApi::class)
actual fun imageDiskCacheDirectory(context: PlatformContext): Path {
    val cachesDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSCachesDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    val cachesPath = requireNotNull(cachesDirectory?.path) {
        "Could not resolve the iOS Caches directory"
    }
    return "$cachesPath/image_cache".toPath()
}
