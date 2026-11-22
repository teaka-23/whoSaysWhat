package ru.tac.whosayswhat.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Одно животное из каталога. Поля повторяют структуру из дизайн-документа
 * (раздел 10), плюс расширения для будущих уровней.
 */
@Serializable
data class Animal(
    val id: String,
    val name: String,
    val image: String? = null,
    @SerialName("image_transparent") val imageTransparent: String? = null,
    @SerialName("sound_file") val soundFile: String,
    @SerialName("onomatopoeia_file") val onomatopoeiaFile: String? = null,
    @SerialName("onomatopoeia_text") val onomatopoeiaText: String = "",
    @SerialName("simple_onomatopoeia") val simpleOnomatopoeia: String? = null,
    @SerialName("hint_detail") val hintDetail: String? = null,
    val level: Int = 1,
    @SerialName("praise_variants") val praiseVariants: List<String> = emptyList(),
) {
    /** Основной звук для игры «Угадай»: человеческое подражание, если есть. */
    val gameSoundFile: String get() = onomatopoeiaFile ?: soundFile
}

@Serializable
data class Catalog(
    val version: String = "1.0",
    val animals: List<Animal> = emptyList(),
)
