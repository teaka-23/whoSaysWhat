package ru.tac.whosayswhat.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверяет решение «оставить запись или попросить повторить». Система
 * останавливает запись на `setMaxDuration` сама, и повторный `stop()` в этом
 * состоянии падает — такие записи выбрасывать нельзя.
 */
class EchoRecordingPolicyTest {

    @Test
    fun `successful stop keeps the recording`() {
        assertTrue(keep(stopSucceeded = true, stoppedByLimit = false, elapsedMs = 1_000))
    }

    @Test
    fun `recording stopped by the system limit survives a failed stop`() {
        assertTrue(
            keep(
                stopSucceeded = false,
                stoppedByLimit = true,
                elapsedMs = ECHO_MAX_DURATION_MS,
            ),
        )
    }

    @Test
    fun `limit is recognised even without the info callback`() {
        // Тикер интерфейса может успеть раньше колбэка: сравниваем и время.
        assertTrue(
            keep(
                stopSucceeded = false,
                stoppedByLimit = false,
                elapsedMs = ECHO_MAX_DURATION_MS - 100,
            ),
        )
    }

    @Test
    fun `failed stop before the limit asks to repeat`() {
        assertFalse(keep(stopSucceeded = false, stoppedByLimit = false, elapsedMs = 300))
    }

    @Test
    fun `empty file is always discarded`() {
        assertFalse(
            keep(
                stopSucceeded = true,
                stoppedByLimit = true,
                elapsedMs = ECHO_MAX_DURATION_MS,
                fileBytes = 0,
            ),
        )
    }

    private fun keep(
        stopSucceeded: Boolean,
        stoppedByLimit: Boolean,
        elapsedMs: Long,
        fileBytes: Long = 12_000,
    ) = shouldKeepEchoRecording(
        stopSucceeded = stopSucceeded,
        stoppedByLimit = stoppedByLimit,
        elapsedMs = elapsedMs,
        fileBytes = fileBytes,
    )
}
