package ru.tac.whosayswhat.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Открывает системный экран разрешений приложения. */
internal fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
