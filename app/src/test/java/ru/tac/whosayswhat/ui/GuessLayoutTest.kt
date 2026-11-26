package ru.tac.whosayswhat.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверяет раскладку экрана «Угадай»: кнопка звука и карточки должны
 * помещаться целиком в портрете, в ландшафте и на планшете, а на низком окне
 * — прокручиваться, а не обрезаться. Размеры экранов — рабочая область после
 * отступов экрана (`padding` 16 dp по бокам, 64 dp сверху, 16 dp снизу).
 */
class GuessLayoutTest {

    private val phonePortrait = 328.dp to 700.dp // 360×780 dp
    private val phoneLandscape = 608.dp to 280.dp // 640×360 dp
    private val smallPhone = 288.dp to 400.dp // 320×480 dp
    private val tablet = 1248.dp to 720.dp // 1280×800 dp
    private val shortWindow = 288.dp to 220.dp // split-screen, 320×300 dp

    @Test
    fun `portrait phone keeps two columns without scrolling`() {
        val layout = layout(phonePortrait, options = 4)
        assertEquals(2, layout.columns)
        assertEquals(2, layout.rows)
        assertFalse(layout.scrollable)
        assertFits(phonePortrait, layout)
    }

    @Test
    fun `landscape phone puts three cards in one row`() {
        val layout = layout(phoneLandscape, options = 3)
        assertEquals(3, layout.columns)
        assertEquals(1, layout.rows)
        assertFalse(layout.scrollable)
        assertFits(phoneLandscape, layout)
    }

    @Test
    fun `landscape phone puts four cards in one row`() {
        val layout = layout(phoneLandscape, options = 4)
        assertEquals(4, layout.columns)
        assertEquals(1, layout.rows)
        assertFalse(layout.scrollable)
        assertFits(phoneLandscape, layout)
    }

    @Test
    fun `two options always make one row`() {
        val layout = layout(phonePortrait, options = 2)
        assertEquals(2, layout.columns)
        assertEquals(1, layout.rows)
        assertFits(phonePortrait, layout)
    }

    @Test
    fun `small phone fits four cards whole`() {
        val layout = layout(smallPhone, options = 4)
        assertFalse(layout.scrollable)
        assertTrue("Карточка меньше 96 dp: ${layout.cardHeight}", layout.cardHeight >= 96.dp)
        assertFits(smallPhone, layout)
    }

    @Test
    fun `tablet keeps two columns and a large sound button`() {
        val layout = layout(tablet, options = 4)
        assertEquals(2, layout.columns)
        assertFalse(layout.scrollable)
        assertEquals(148.dp, layout.promptSize)
        assertFits(tablet, layout)
    }

    @Test
    fun `short window scrolls instead of clipping`() {
        val layout = layout(shortWindow, options = 4)
        assertTrue("Карточки должны остаться нажимаемыми", layout.cardHeight >= 96.dp)
        assertTrue("Нужна прокрутка, иначе нижний ряд обрежется", layout.scrollable)
    }

    @Test
    fun `sound button never shrinks below the touch minimum`() {
        (120..700 step 40).forEach { height ->
            val layout = layout(288.dp to height.dp, options = 4)
            assertTrue(
                "Кнопка звука $height dp: ${layout.promptSize}",
                layout.promptSize >= 88.dp,
            )
        }
    }

    @Test
    fun `every placement fits the window when scrolling is off`() {
        val screens = listOf(phonePortrait, phoneLandscape, smallPhone, tablet)
        screens.forEach { screen ->
            (2..4).forEach { options ->
                val layout = layout(screen, options)
                assertTrue(
                    "$screen, $options карточек: колонок больше, чем карточек",
                    layout.columns <= options,
                )
                if (!layout.scrollable) assertFits(screen, layout)
            }
        }
    }

    private fun layout(screen: Pair<Dp, Dp>, options: Int): GuessLayout =
        guessLayout(maxWidth = screen.first, maxHeight = screen.second, options = options)

    /** Сумма высот ровно так, как их складывает Column на экране. */
    private fun assertFits(screen: Pair<Dp, Dp>, layout: GuessLayout) {
        val rows = layout.rows
        val total = layout.promptSize + GUESS_PROMPT_GAP +
            layout.cardHeight * rows + GUESS_CARD_GAP * (rows - 1)
        assertTrue(
            "Не помещается: $total больше доступных ${screen.second}",
            total.value <= screen.second.value + 0.01f,
        )
    }
}
