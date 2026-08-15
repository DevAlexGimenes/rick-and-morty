package com.gimenes.alex.rickandmorty.core.designsystem.image

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Proves the disk cache is actually pinned to the app's private cache directory, in a dedicated
 * subdirectory, rather than sharing Coil 3's own built-in default location (see
 * [imageDiskCacheDirectory]'s doc comment for what was verified - directly against coil3's own
 * sources and confirmed on-device here - about what that default actually resolves to on
 * Android). End-to-end network-load-then-offline-reload behavior isn't practical to assert in an
 * automated test (it needs real connectivity control), so this focuses on what's mechanically
 * checkable: the configured directory is non-null, correctly scoped under the app's real
 * `cacheDir` in its own subdirectory, and actually used by the [ImageLoader][coil3.ImageLoader]
 * this module builds.
 */
@RunWith(AndroidJUnit4::class)
class PlatformImageLoaderInstrumentedTest {

    @Test
    fun imageDiskCacheDirectory_isScopedUnderTheAppsPrivateCacheDir() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val directory = imageDiskCacheDirectory(context)

        val expectedParent = context.cacheDir.absolutePath.replace('\\', '/')
        assertTrue(
            directory.toString().replace('\\', '/').startsWith(expectedParent),
            "Expected $directory to be nested under the app cache dir $expectedParent"
        )
    }

    @Test
    fun imageDiskCacheDirectory_isADedicatedSubdirectoryNotTheBareCacheRoot() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val directory = imageDiskCacheDirectory(context)

        val cacheRoot = context.cacheDir.absolutePath.replace('\\', '/')
        assertTrue(
            directory.toString().replace('\\', '/') != cacheRoot,
            "Expected a dedicated subdirectory, not the bare cache root $cacheRoot itself"
        )
    }

    @Test
    fun buildImageLoader_wiresItsDiskCacheToTheSameDirectory() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val imageLoader = buildImageLoader(context)
        val diskCache = imageLoader.diskCache

        assertNotNull(diskCache, "Expected the built ImageLoader to have a non-null disk cache")
        assertEquals(imageDiskCacheDirectory(context), diskCache.directory)

        imageLoader.diskCache?.shutdown()
    }
}
