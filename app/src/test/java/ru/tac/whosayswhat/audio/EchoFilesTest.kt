package ru.tac.whosayswhat.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class EchoFilesTest {

    @Test
    fun `deleteAll removes every recording in directory`() {
        val dir = Files.createTempDirectory("echo-files").toFile()
        try {
            val first = dir.resolve("echo_1.m4a").apply { writeText("one") }
            val second = dir.resolve("echo_2.m4a").apply { writeText("two") }

            assertEquals(2, EchoFiles.deleteAll(dir))
            assertFalse(first.exists())
            assertFalse(second.exists())
            assertTrue(dir.isDirectory)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `deleteAll handles missing directory`() {
        val dir = Files.createTempDirectory("echo-files").toFile()
        dir.deleteRecursively()

        assertEquals(0, EchoFiles.deleteAll(dir))
    }

    @Test
    fun `deleteAll keeps subdirectories`() {
        val dir = Files.createTempDirectory("echo-files").toFile()
        try {
            val nested = dir.resolve("nested").apply { mkdirs() }
            dir.resolve("echo.m4a").writeText("record")

            assertEquals(1, EchoFiles.deleteAll(dir))
            assertTrue(nested.isDirectory)
        } finally {
            dir.deleteRecursively()
        }
    }
}
