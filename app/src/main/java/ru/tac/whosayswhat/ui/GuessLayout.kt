package ru.tac.whosayswhat.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Раскладка экрана «Угадай» под текущий размер окна.
 *
 * Пока всё помещается, экран не прокручивается: сначала пробуем две колонки,
 * затем — больше колонок в один ряд. Это спасает ландшафт: на телефоне
 * 640×360 dp три-четыре карточки в две колонки не влезают по высоте, а в один
 * ряд влезают. Если не помещается ничего, включается прокрутка: лучше
 * прокрутить, чем обрезать карточку.
 */
internal data class GuessLayout(
    val columns: Int,
    val rows: Int,
    val promptSize: Dp,
    val cardHeight: Dp,
    val scrollable: Boolean,
)

/** Промежуток между карточками. */
internal val GUESS_CARD_GAP = 16.dp

/** Промежуток между кнопкой звука и карточками. */
internal val GUESS_PROMPT_GAP = 24.dp

/** Ниже этого порога карточку неудобно нажимать. */
internal val GUESS_MIN_CARD_HEIGHT = 96.dp

/** Уже этого карточка в ряду из трёх-четырёх колонок становится бесполезной. */
internal val GUESS_MIN_CARD_WIDTH = 96.dp

/** Кнопка звука не должна становиться слишком мелкой. */
internal val GUESS_MIN_PROMPT_SIZE = 88.dp

/** На планшете крупнее кнопку звука делать незачем. */
internal val GUESS_MAX_PROMPT_SIZE = 148.dp

/** Отношение ширины карточки к высоте: не даём карточкам вытягиваться. */
private const val CARD_ASPECT = 0.82f

private const val MAX_COLUMNS = 4

/** Запас по высоте: сумма долей dp не должна вылезать за границу. */
private val HEIGHT_SLACK = 1.dp

internal fun guessLayout(maxWidth: Dp, maxHeight: Dp, options: Int): GuessLayout {
    val promptSize = promptSize(maxWidth, maxHeight)
    if (options <= 0) {
        return GuessLayout(
            columns = 1,
            rows = 0,
            promptSize = promptSize,
            cardHeight = 0.dp,
            scrollable = false,
        )
    }

    val budget = maxHeight - HEIGHT_SLACK
    for (columns in 2..minOf(options, MAX_COLUMNS)) {
        val rows = (options + columns - 1) / columns
        val cellWidth = (maxWidth - GUESS_CARD_GAP * (columns - 1)) / columns
        val byHeight = (budget - promptSize - GUESS_PROMPT_GAP - GUESS_CARD_GAP * (rows - 1)) / rows
        val cardHeight = minOf(cellWidth / CARD_ASPECT, byHeight)
        if (cellWidth >= GUESS_MIN_CARD_WIDTH && cardHeight >= GUESS_MIN_CARD_HEIGHT) {
            return GuessLayout(
                columns = columns,
                rows = rows,
                promptSize = promptSize,
                cardHeight = cardHeight,
                scrollable = false,
            )
        }
    }

    // Ни один вариант не влез: карточки остаются нажимаемыми, а экран
    // прокручивается — обрезать нижний ряд нельзя.
    val columns = if (options == 1) 1 else 2
    return GuessLayout(
        columns = columns,
        rows = (options + columns - 1) / columns,
        promptSize = promptSize,
        cardHeight = GUESS_MIN_CARD_HEIGHT,
        scrollable = true,
    )
}

private fun promptSize(maxWidth: Dp, maxHeight: Dp): Dp =
    minOf(GUESS_MAX_PROMPT_SIZE, maxWidth * 0.38f, maxHeight * 0.34f)
        .coerceAtLeast(GUESS_MIN_PROMPT_SIZE)
