package ru.tac.whosayswhat.audio

import java.io.File

/**
 * Работа с временными файлами «Эхо».
 *
 * Вынесено отдельно от Android-классов, чтобы очистку можно было проверить
 * обычным unit-тестом.
 */
object EchoFiles {
    /** Удаляет все файлы в каталоге и возвращает число удалённых. */
    fun deleteAll(dir: File): Int =
        dir.listFiles()?.count { file -> file.isFile && file.delete() } ?: 0
}
