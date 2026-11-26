package ru.tac.whosayswhat.ui

/**
 * Что TalkBack читает на карточке животного.
 *
 * Имя идёт отдельной подписью, а действие остаётся коротким («Выбрать» или
 * «Послушать»): склейка «Выбрать Корова» читается с ошибкой в падеже.
 * [outcome] заполняется только после нажатия — тогда [announceOutcome]
 * просит TalkBack объявить исход сам, не дожидаясь новой остановки пальцем.
 */
internal data class GuessCardSemantics(
    val name: String,
    val outcome: String?,
    val announceOutcome: Boolean,
)

/**
 * Тексты для слабовидящего ребёнка. Цвет и покачивание ему ничего не
 * говорят, поэтому исход должен существовать и словами, и как объявление.
 */
internal fun guessCardSemantics(
    name: String,
    feedback: GuessFeedback,
): GuessCardSemantics = GuessCardSemantics(
    name = name,
    outcome = when (feedback) {
        GuessFeedback.Correct -> "Верно"
        GuessFeedback.Wrong -> "Не то, попробуй ещё"
        GuessFeedback.None -> null
    },
    // В покое объявлять нечего: иначе карточки говорили бы хором при каждой
    // смене раунда, когда подсветка сбрасывается.
    announceOutcome = feedback != GuessFeedback.None,
)
