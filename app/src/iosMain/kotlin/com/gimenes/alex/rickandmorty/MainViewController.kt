package com.gimenes.alex.rickandmorty

import androidx.compose.ui.window.ComposeUIViewController
import com.gimenes.alex.rickandmorty.di.initKoin
import platform.UIKit.UIViewController

/**
 * iOS entry point. Called from Swift (see iosApp/) to obtain the root UIViewController hosting
 * the shared Compose Multiplatform UI. Not built/run in this pass - see the follow-up note
 * about completing Xcode project setup on a Mac.
 */
fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
