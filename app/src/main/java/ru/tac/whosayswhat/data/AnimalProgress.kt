package ru.tac.whosayswhat.data

import kotlinx.serialization.Serializable

/**
 * Накопленная статистика по одному животному. Хранится локально и служит
 * источником звёзд и отметки «освоено» в родительской панели.
 *
 * [attempts] — сколько ответов ребёнок дал, когда это животное было целью
 * раунда (ошибочные нажатия тоже считаются),
 * [correct] — сколько из этих ответов оказались верными.
 */
@Serializable
data class AnimalProgress(
    val attempts: Int = 0,
    val correct: Int = 0,
) {
    /** 0…3 звезды: 1 верный ответ, 3 и 5 — следующие ступени. */
    val stars: Int
        get() = when {
            correct >= STARS_3 -> 3
            correct >= STARS_2 -> 2
            correct >= STARS_1 -> 1
            else -> 0
        }

    val isMastered: Boolean get() = correct >= MASTERED_CORRECT

    val accuracy: Float
        get() = if (attempts == 0) 0f else correct.toFloat() / attempts

    fun withAnswer(correctAnswer: Boolean): AnimalProgress = copy(
        attempts = attempts + 1,
        correct = correct + if (correctAnswer) 1 else 0,
    )

    companion object {
        /** Сколько всего верных ответов нужно, чтобы животное считалось освоенным. */
        const val MASTERED_CORRECT = 3
        const val STARS_1 = 1
        const val STARS_2 = 3
        const val STARS_3 = 5
        const val MAX_STARS = 3

        val EMPTY = AnimalProgress()
    }
}
