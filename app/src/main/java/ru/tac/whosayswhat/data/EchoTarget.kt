package ru.tac.whosayswhat.data

/**
 * Выбирает животное для режима «Эхо».
 *
 * Явный выбор родителя важнее последнего нажатия ребёнка. Если животное из
 * сохранённой настройки исчезло из каталога, спокойно откатываемся к
 * автоматическому выбору, а не показываем пустой экран.
 */
object EchoTarget {
    fun resolve(
        animals: List<Animal>,
        preferredId: String?,
        lastTapped: Animal?,
    ): Animal? {
        preferredId?.let { id ->
            animals.firstOrNull { it.id == id }?.let { return it }
        }
        if (lastTapped != null) {
            animals.firstOrNull { it.id == lastTapped.id }?.let { return it }
        }
        return animals.firstOrNull()
    }
}
