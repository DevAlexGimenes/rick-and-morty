package com.gimenes.alex.rickandmorty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() per the SplashScreen API's required usage
        // pattern - it needs to install itself before the Activity's first draw. The manifest
        // theme (Theme.RickAndMorty.Starting) declares `postSplashScreenTheme`, so the library
        // switches the Activity back to the normal Theme.RickAndMorty automatically once the
        // splash is dismissed - no manual setTheme() call needed.
        //
        // No setKeepOnScreenCondition(): initKoin() runs synchronously in
        // RickAndMortyApplication.onCreate(), which always completes before this Activity's
        // onCreate() runs, and startKoin {} registers modules synchronously (no async/IO work
        // gating readiness). Home has nothing left to wait on by the time Compose lays out its
        // first frame, so the API's default behavior - dismiss on first frame drawn - is
        // sufficient. An artificial hold would violate the "simple, no extra delay" requirement.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
