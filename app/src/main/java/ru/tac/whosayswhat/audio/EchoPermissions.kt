package ru.tac.whosayswhat.audio

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Планирует отзыв RECORD_AUDIO после закрытия приложения на Android 13+.
 *
 * Обычное приложение не может мгновенно отозвать runtime-разрешение.
 * [Context.revokeSelfPermissionsOnKill] отзывает его асинхронно и завершает
 * процесс, когда приложение уходит в фон. На Android 8–12 такого API нет —
 * там остаётся только системный экран настроек. До завершения процесса
 * разрешение может оставаться в системе, но приложение уже не использует
 * микрофон.
 *
 * @return true, если система приняла запрос на отзыв.
 */
fun Context.scheduleRecordAudioPermissionRevocation(): Boolean {
    if (!hasRecordAudioPermission()) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

    return runCatching {
        revokeSelfPermissionsOnKill(listOf(Manifest.permission.RECORD_AUDIO))
    }.onFailure {
        Log.w(TAG, "Не удалось запросить отзыв RECORD_AUDIO", it)
    }.isSuccess
}

/** Доступен ли автоматический отзыв разрешения после закрытия приложения. */
fun supportsRecordAudioPermissionRevocation(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

private const val TAG = "EchoPermissions"
