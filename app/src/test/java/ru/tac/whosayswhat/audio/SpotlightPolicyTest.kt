package ru.tac.whosayswhat.audio

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверяет, сколько крупный план животного держится на экране.
 *
 * Крупный план закрывается сам, когда звук и озвучка названия доиграли, но
 * у него есть нижняя граница: короткая запись иначе мигала бы на экране, и
 * ребёнок не успел бы рассмотреть животное. Верхняя граница нужна на случай,
 * когда синтезатор речи не сообщил о конце фразы: без неё картинка осталась
 * бы поверх песочницы навсегда.
 */
class SpotlightPolicyTest {

    @Test
    fun `short sound keeps the close-up long enough to look at it`() {
        // Звук кошки короче двух секунд, но увеличение не должно мигать.
        assertEquals(2_500L, spotlightFloorMs(1_200L))
    }

    @Test
    fun `long sound holds the close-up until the record ends`() {
        assertEquals(4_300L, spotlightFloorMs(4_000L))
    }

    @Test
    fun `sound just below the minimum still gets the tail`() {
        assertEquals(2_700L, spotlightFloorMs(2_400L))
    }

    @Test
    fun `unknown duration falls back to the minimum`() {
        // WAV-заголовок может не читаться: SoundPool не сообщает конец звука.
        assertEquals(2_500L, spotlightFloorMs(0L))
        assertEquals(2_500L, spotlightFloorMs(-1L))
    }

    @Test
    fun `narration wait is bounded`() {
        assertEquals(6_500L, spotlightMaxWaitMs(2_500L))
    }
}
