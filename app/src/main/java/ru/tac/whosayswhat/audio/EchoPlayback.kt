package ru.tac.whosayswhat.audio

import android.media.AudioManager

/** Что делать с прослушиванием после смены аудиофокуса. */
internal enum class EchoFocusResult {
    /** Фокус остался у нас (или его вернули) — запись продолжается. */
    ContinuePlaying,

    /** Звук забрали: запись остановлена, UI уже знает, что играть нечего. */
    StopAndNotify,
}

/**
 * Проигрыватель записи. Интерфейс намеренно минимальный: благодаря ему
 * [EchoPlayback] проверяется обычными unit-тестами, без MediaPlayer.
 */
internal interface EchoOutput {
    /** Готовит и запускает проигрыватель; false — запись не открылась. */
    fun start(): Boolean

    /** Останавливает и освобождает проигрыватель, если он есть. */
    fun stop()
}

/**
 * Кто останавливает прослушивание «Эха» и кто сообщает UI, что играть больше
 * нечего.
 *
 * Класс не знает про Android, кроме констант [AudioManager]: они объявлены
 * `const` и подставляются компилятором, поэтому политику можно проверить
 * тестом. Android-часть (сам `MediaPlayer`, запрос и возврат фокуса) остаётся
 * в [EchoRecorder].
 *
 * Приглашение к уведомлению ровно одно на прослушивание. Без этого правила
 * было два плохих исхода: звонок останавливал запись, UI продолжал показывать
 * «Играет…», и ребёнок не мог записать заново; либо UI гасил «Играет…» уже от
 * следующего прослушивания.
 */
internal class EchoPlayback(
    private val output: EchoOutput,
    private val onFinished: () -> Unit,
) {

    /** Идёт ли прослушивание: по этому флагу UI показывает «Играет…». */
    var isPlaying: Boolean = false
        private set

    /** UI уже получил «запись кончилась» — второй раз не уведомляем. */
    private var notified = true

    /** Начинает прослушивание. false — файл не открылся, слушать нечего. */
    fun play(): Boolean {
        if (isPlaying) stop(notify = false)
        // Неудачный старт: гасить в UI нечего, поэтому колбэк остаётся
        // недоступным — ошибку показывает сам вызывающий код.
        if (!output.start()) {
            isPlaying = false
            notified = true
            return false
        }
        isPlaying = true
        notified = false
        return true
    }

    /**
     * Останавливает прослушивание. [notify] — сообщить вызывающему коду, что
     * играть больше нечего (потеря фокуса, ошибка декодера). При обычной
     * остановке по кнопке UI и так знает, что запись кончилась, и второй
     * вызов только мигал бы состоянием.
     */
    fun stop(notify: Boolean = false) {
        if (isPlaying) {
            isPlaying = false
            output.stop()
        }
        if (!notify || notified) return
        notified = true
        onFinished()
    }

    /** Запись доиграла или сломалась — плеер освобождается, UI гасит «Играет…». */
    fun finish() = stop(notify = true)

    /**
     * Реакция на смену аудиофокуса. Любая потеря (в том числе с приглушением)
     * означает: запись замолкает и UI возвращается в состояние «можно
     * записать». Возврат фокуса ничего не запускает заново: переслушивать
     * запись без просьбы ребёнка не нужно.
     */
    fun onFocusChange(change: Int): EchoFocusResult =
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> {
                stop(notify = true)
                EchoFocusResult.StopAndNotify
            }

            else -> EchoFocusResult.ContinuePlaying
        }
}
