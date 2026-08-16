package com.gimenes.alex.rickandmorty.core.designsystem.motion

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

/**
 * Reads Android's system-wide "Remove animations" setting -
 * `Settings.Global.ANIMATOR_DURATION_SCALE` - as this project's reduced-motion signal. That
 * setting is exposed to users both as Developer Options' "Animator duration scale" and (on
 * devices/OS versions that offer it) an accessibility "Remove animations" toggle, and is also
 * what `adb shell settings put global animator_duration_scale 0` drives - a scale of `0` means
 * the platform itself is instructed to skip animations; anything above `0` (the default is `1`)
 * means motion is expected.
 *
 * A [ContentObserver] is registered on the setting's `Uri` so a change is picked up immediately
 * rather than only on the next unrelated recomposition - this is what lets manual verification
 * toggle the developer setting and see the very next navigation transition honor it without
 * restarting the app.
 */
@Composable
actual fun rememberReducedMotionEnabled(): State<Boolean> {
    val context = LocalContext.current
    return produceState(initialValue = context.isAnimatorDurationScaleZero(), context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                value = context.isAnimatorDurationScaleZero()
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            /* notifyForDescendants = */ false,
            observer,
        )
        // The setting may have changed between the initial read above and the observer actually
        // being registered; re-read once more to close that window.
        value = context.isAnimatorDurationScaleZero()
        awaitDispose { resolver.unregisterContentObserver(observer) }
    }
}

private fun Context.isAnimatorDurationScaleZero(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
