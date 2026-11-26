package ru.tac.whosayswhat.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ожидание движка синтеза речи.
 *
 * Фраза «Это слоник!» ставится в очередь сразу после звука животного, и если
 * движок ещё поднимается, её надо придержать. Без счётчика попыток
 * обработчик перепланировал бы сам себя бесконечно: батарея тратится, а
 * фраза так и не звучит и не отменяется.
 */
class SpeechRetryPolicyTest {

    @Test
    fun `engine gets about three seconds to start`() {
        assertEquals(3_000L, TTS_READY_MAX_ATTEMPTS * TTS_READY_RETRY_MS)
    }

    @Test
    fun `waiting continues before the limit`() {
        assertTrue(shouldKeepWaitingForTts(attempts = 0))
        assertTrue(shouldKeepWaitingForTts(attempts = TTS_READY_MAX_ATTEMPTS - 1))
    }

    @Test
    fun `waiting stops at the limit`() {
        assertFalse(shouldKeepWaitingForTts(attempts = TTS_READY_MAX_ATTEMPTS))
        assertFalse(shouldKeepWaitingForTts(attempts = TTS_READY_MAX_ATTEMPTS + 5))
    }
}
