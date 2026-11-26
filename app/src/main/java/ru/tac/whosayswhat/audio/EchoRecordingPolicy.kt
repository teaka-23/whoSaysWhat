package ru.tac.whosayswhat.audio

/**
 * Допуск между системным лимитом записи и тикером интерфейса: они срабатывают
 * не в одном кадре, поэтому «дошло до лимита» проверяется с запасом.
 */
internal const val ECHO_LIMIT_TOLERANCE_MS = 250L

/**
 * Стоит ли оставлять запись для прослушивания.
 *
 * MediaRecorder с `setMaxDuration` останавливает запись сам, и повторный
 * `stop()` в этом состоянии бросает `IllegalStateException`. Это не значит,
 * что файл плохой: если запись дошла до лимита, её надо оставить. Слишком
 * короткая запись (ребёнок сразу нажал «Стоп») заголовка MP4 не содержит и не
 * проигрывается — её лучше удалить и попросить повторить.
 */
internal fun shouldKeepEchoRecording(
    stopSucceeded: Boolean,
    stoppedByLimit: Boolean,
    elapsedMs: Long,
    fileBytes: Long,
): Boolean {
    if (fileBytes <= 0L) return false
    if (stopSucceeded) return true
    return stoppedByLimit || elapsedMs >= ECHO_MAX_DURATION_MS - ECHO_LIMIT_TOLERANCE_MS
}
