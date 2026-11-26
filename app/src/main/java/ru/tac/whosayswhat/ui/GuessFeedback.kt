package ru.tac.whosayswhat.ui

/**
 * Что показать на карточке сразу после нажатия. Цветом подтверждается только
 * верный ответ: для ребёнка 4 лет это единственный сигнал, что нажатие
 * сработало, — иначе карточка до перехода к раунду выглядит как до нажатия.
 * Ошибка показывает себя мягким покачиванием, без красного.
 */
enum class GuessFeedback {
    None,
    Wrong,
    Correct,
}

/**
 * Подсветка одной карточки. Верный ответ важнее неверного: подсвечена всегда
 * не больше одной карточки, даже если оба идентификатора совпали.
 */
fun guessFeedback(
    animalId: String,
    correctId: String?,
    wrongId: String?,
): GuessFeedback = when {
    animalId == correctId -> GuessFeedback.Correct
    animalId == wrongId -> GuessFeedback.Wrong
    else -> GuessFeedback.None
}
