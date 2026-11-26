package ru.tac.whosayswhat.audio

/** Как часто переспрашивать синтезатор речи, пока он поднимается. */
internal const val TTS_READY_RETRY_MS = 150L

/** Сколько попыток ждать: 20 × 150 мс = 3 с — движку хватает с запасом. */
internal const val TTS_READY_MAX_ATTEMPTS = 20

/**
 * Стоит ли ждать движок ещё раз после [attempts] проверок.
 *
 * Проверка нужна, чтобы ожидание было ограниченным: при зависшем или
 * выгруженном движке фраза просто пропускается, а не перепланируется
 * бесконечно.
 */
internal fun shouldKeepWaitingForTts(attempts: Int): Boolean =
    attempts < TTS_READY_MAX_ATTEMPTS
