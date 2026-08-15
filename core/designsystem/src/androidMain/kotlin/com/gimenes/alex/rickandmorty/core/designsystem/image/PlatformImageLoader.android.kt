package com.gimenes.alex.rickandmorty.core.designsystem.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

/**
 * Android's app-private cache directory (`/data/data/<package>/cache`, or the equivalent on
 * external storage) - cleared by the OS under storage pressure, never backed up, and not
 * readable by other apps. This is the same directory class Android's own docs recommend for any
 * "large amounts of data that shouldn't be persisted indefinitely", which is exactly the shared
 * character-image cache's use case.
 *
 * Confirmed on-device (see `PlatformImageLoaderInstrumentedTest`) that on Android's ART runtime
 * `System.getProperty("java.io.tmpdir")` - what Coil 3's own default disk cache location is
 * rooted at - already resolves to this exact same directory, `Context.cacheDir`. That's a
 * runtime implementation detail rather than a documented guarantee though (see
 * [imageDiskCacheDirectory]'s doc comment), so this is still pinned explicitly - which also gets
 * a dedicated `image_cache` subdirectory, keeping Coil's cache files from mixing with any other
 * cache use `context.cacheDir` may see elsewhere in the app.
 */
actual fun imageDiskCacheDirectory(context: PlatformContext): Path =
    context.cacheDir.resolve("image_cache").toOkioPath()
