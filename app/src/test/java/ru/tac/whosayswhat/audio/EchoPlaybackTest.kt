package ru.tac.whosayswhat.audio

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Прослушивание записи «Эхо»: кто останавливает плеер и кто сообщает UI.
 *
 * Главный риск — не сам звонок, а состояние после него: если запись
 * остановилась, а UI об этом не узнал, кнопка навсегда остаётся «Играет…» и
 * ребёнок больше ничего не запишет. Обратный риск — двойное уведомление:
 * сначала по завершении, потом по потере фокуса, и UI гаснет посреди нового
 * прослушивания.
 */
class EchoPlaybackTest {

    /** Плеер-пустышка: считает вызовы, как настоящий MediaPlayer не сможет. */
    private class FakeOutput(var startResult: Boolean = true) : EchoOutput {
        var starts = 0
        var stops = 0

        override fun start(): Boolean {
            starts++
            return startResult
        }

        override fun stop() {
            stops++
        }
    }

    private val output = FakeOutput()
    private var finishedCount = 0
    private val playback = EchoPlayback(output) { finishedCount++ }

    @Test
    fun `play starts the output`() {
        assertTrue(playback.play())
        assertTrue(playback.isPlaying)
        assertEquals(1, output.starts)
    }

    @Test
    fun `failed start is not playing and stays silent`() {
        output.startResult = false

        assertFalse(playback.play())
        assertFalse(playback.isPlaying)

        playback.finish()
        assertEquals(0, finishedCount)
    }

    @Test
    fun `completion notifies the ui once`() {
        playback.play()

        playback.finish()
        playback.finish()

        assertEquals(1, finishedCount)
        assertFalse(playback.isPlaying)
        assertEquals(1, output.stops)
    }

    @Test
    fun `manual stop keeps the ui silent`() {
        playback.play()

        playback.stop()

        assertEquals(0, finishedCount)
        assertEquals(1, output.stops)
    }

    @Test
    fun `focus loss stops the recording and notifies`() {
        playback.play()

        val result = playback.onFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        assertEquals(EchoFocusResult.StopAndNotify, result)
        assertFalse(playback.isPlaying)
        assertEquals(1, output.stops)
        assertEquals(1, finishedCount)
    }

    @Test
    fun `transient loss and ducking stop the recording too`() {
        listOf(
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
        ).forEach { change ->
            val freshOutput = FakeOutput()
            var notified = 0
            val fresh = EchoPlayback(freshOutput) { notified++ }
            fresh.play()

            assertEquals(EchoFocusResult.StopAndNotify, fresh.onFocusChange(change))
            assertEquals(1, notified)
            assertEquals(1, freshOutput.stops)
        }
    }

    @Test
    fun `focus gain keeps the recording playing`() {
        playback.play()

        val result = playback.onFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals(EchoFocusResult.ContinuePlaying, result)
        assertTrue(playback.isPlaying)
        assertEquals(0, output.stops)
        assertEquals(0, finishedCount)
    }

    @Test
    fun `completion after focus loss does not notify twice`() {
        playback.play()

        playback.onFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        playback.finish()

        assertEquals(1, finishedCount)
        assertEquals(1, output.stops)
    }

    @Test
    fun `new playback after a stop notifies again`() {
        playback.play()
        playback.stop()
        playback.play()

        playback.finish()

        assertEquals(1, finishedCount)
    }
}
