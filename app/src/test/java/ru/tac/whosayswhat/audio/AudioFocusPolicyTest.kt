package ru.tac.whosayswhat.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверяет удержание аудиофокуса. Фокус «на время» нельзя брать один раз на
 * всё приложение: пока он у нас, чужая музыка приглушена. Поэтому фокус
 * возвращается через короткий хвост после расчётного конца звука, а новый звук
 * продлевает удержание.
 */
class AudioFocusPolicyTest {

    @Test
    fun `hold follows the sound duration plus a tail`() {
        assertEquals(1_000L + AUDIO_FOCUS_TAIL_MS, audioFocusHoldMs(1_000L))
    }

    @Test
    fun `unknown duration uses the fallback hold`() {
        // SoundPool не сообщает конец звука, а WAV-заголовок может не читаться.
        assertEquals(AUDIO_FOCUS_UNKNOWN_MS + AUDIO_FOCUS_TAIL_MS, audioFocusHoldMs(0L))
    }

    @Test
    fun `focus is released only after the hold`() {
        val hold = AudioFocusHold()
        hold.onFocusGranted()
        hold.onPlaybackStarted(nowMs = 5_000L, durationMs = 1_000L)

        val releaseAt = 5_000L + 1_000L + AUDIO_FOCUS_TAIL_MS
        assertFalse(hold.shouldRelease(releaseAt - 1))
        assertTrue(hold.shouldRelease(releaseAt))
    }

    @Test
    fun `a new sound postpones the release`() {
        val hold = AudioFocusHold()
        hold.onFocusGranted()
        hold.onPlaybackStarted(nowMs = 0L, durationMs = 1_000L)
        hold.onPlaybackStarted(nowMs = 800L, durationMs = 1_000L)

        assertFalse(hold.shouldRelease(1_200L))
        assertTrue(hold.shouldRelease(800L + 1_000L + AUDIO_FOCUS_TAIL_MS))
    }

    @Test
    fun `lost focus must be requested again`() {
        val hold = AudioFocusHold()
        assertTrue(hold.needsRequest())

        hold.onFocusGranted()
        assertFalse(hold.needsRequest())

        // Звонок или другое приложение забрало фокус.
        hold.onFocusLost()
        assertTrue(hold.needsRequest())
    }

    @Test
    fun `released focus forgets the pending deadline`() {
        val hold = AudioFocusHold()
        hold.onFocusGranted()
        hold.onPlaybackStarted(nowMs = 0L, durationMs = 1_000L)
        hold.onReleased()

        assertTrue(hold.needsRequest())
        assertFalse(hold.shouldRelease(nowMs = 60_000L))
    }

    @Test
    fun `remaining time counts down to the deadline`() {
        val hold = AudioFocusHold()
        hold.onFocusGranted()
        hold.onPlaybackStarted(nowMs = 1_000L, durationMs = 500L)

        val holdMs = 500L + AUDIO_FOCUS_TAIL_MS
        assertEquals(holdMs, hold.msUntilRelease(nowMs = 1_000L))
        assertEquals(holdMs - 200L, hold.msUntilRelease(nowMs = 1_200L))
        assertEquals(0L, hold.msUntilRelease(nowMs = 1_000L + holdMs))
    }

    @Test
    fun `no playback means nothing to wait for`() {
        assertEquals(0L, AudioFocusHold().msUntilRelease(nowMs = 5L))
    }
}
