package ru.tac.whosayswhat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Что читает TalkBack, когда открыт крупный план.
 *
 * Картинка, название и звукоподражание — один объект, а не три: иначе
 * ребёнок с TalkBack слышит «Корова», «Му-у-у!» и «Кнопка» по отдельности и
 * теряет связь между ними.
 */
class AnimalCloseUpSemanticsTest {

    @Test
    fun `announces the name and the onomatopoeia together`() {
        assertEquals("Корова. Му-у-у!", closeUpAnnouncement("Корова", "Му-у-у!"))
    }

    @Test
    fun `without onomatopoeia announces just the name`() {
        assertEquals("Корова", closeUpAnnouncement("Корова", null))
    }

    @Test
    fun `blank parts are skipped`() {
        assertEquals("Корова", closeUpAnnouncement("Корова", "   "))
        assertEquals("Му-у-у!", closeUpAnnouncement("  ", "Му-у-у!"))
    }
}
