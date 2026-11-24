package ru.tac.whosayswhat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EchoTargetTest {

    private val cow = Animal(id = "cow", name = "Корова", soundFile = "sounds/cow.wav")
    private val cat = Animal(id = "cat", name = "Кошка", soundFile = "sounds/cat.wav")
    private val dog = Animal(id = "dog", name = "Собака", soundFile = "sounds/dog.wav")
    private val animals = listOf(cow, cat, dog)

    @Test
    fun `explicit parent choice wins over last tapped`() {
        val result = EchoTarget.resolve(
            animals = animals,
            preferredId = cat.id,
            lastTapped = dog,
        )

        assertEquals(cat, result)
    }

    @Test
    fun `auto mode uses last tapped animal`() {
        val result = EchoTarget.resolve(
            animals = animals,
            preferredId = null,
            lastTapped = dog,
        )

        assertEquals(dog, result)
    }

    @Test
    fun `unknown saved animal falls back to last tapped`() {
        val result = EchoTarget.resolve(
            animals = animals,
            preferredId = "unicorn",
            lastTapped = cat,
        )

        assertEquals(cat, result)
    }

    @Test
    fun `first animal is used before any tap`() {
        val result = EchoTarget.resolve(
            animals = animals,
            preferredId = null,
            lastTapped = null,
        )

        assertEquals(cow, result)
    }

    @Test
    fun `empty catalog returns null`() {
        val result = EchoTarget.resolve(
            animals = emptyList(),
            preferredId = cow.id,
            lastTapped = cow,
        )

        assertNull(result)
    }
}
