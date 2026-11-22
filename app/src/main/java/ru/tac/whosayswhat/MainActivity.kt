package ru.tac.whosayswhat

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import ru.tac.whosayswhat.audio.SoundPlayer
import ru.tac.whosayswhat.data.CatalogRepository
import ru.tac.whosayswhat.data.ProgressStore
import ru.tac.whosayswhat.ui.App
import ru.tac.whosayswhat.ui.theme.WhoSaysWhatTheme

class MainActivity : ComponentActivity() {

    private lateinit var soundPlayer: SoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        soundPlayer = SoundPlayer(this)

        // Киоск-режим для ребёнка: экран не гаснет, системные панели скрыты.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            WhoSaysWhatTheme {
                App(
                    soundPlayer = soundPlayer,
                    repository = remember { CatalogRepository(applicationContext) },
                    progressStore = remember { ProgressStore(applicationContext) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onDestroy() {
        soundPlayer.release()
        super.onDestroy()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
