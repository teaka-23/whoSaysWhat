package ru.tac.whosayswhat.game

import ru.tac.whosayswhat.data.Animal
import kotlin.random.Random

/** Один раунд: правильное животное и варианты на экране. */
data class Round(
    val target: Animal,
    val options: List<Animal>,
    val targetPosition: Int,
)

sealed interface Answer {
    data class Correct(val animal: Animal) : Answer
    data object Wrong : Answer
}

/**
 * Чистая логика игры «Угадай», без Android-зависимостей — покрывается unit-тестами.
 *
 * Правила:
 *  - правильное животное не повторяется два раунда подряд (если есть альтернатива);
 *  - позиция правильного ответа балансируется, чтобы ребёнок не выучил «всегда слева»;
 *  - набор карточек не повторяется подряд, если пул позволяет;
 *  - ошибка не сдвигает раунд и не штрафует.
 */
class SessionEngine(
    animals: List<Animal>,
    requestedOptions: Int = 2,
    private val random: Random = Random.Default,
) {
    private val pool = animals.distinctBy { it.id }

    val optionsCount: Int = requestedOptions.coerceAtLeast(2).coerceAtMost(pool.size)

    private val positionCounts = IntArray(optionsCount)
    private var lastTargetId: String? = null
    private var lastOptionIds: Set<String> = emptySet()

    var current: Round? = null
        private set

    var correctCount: Int = 0
        private set

    val correctByAnimal: MutableMap<String, Int> = linkedMapOf()

    init {
        require(pool.size >= 2) { "Для игры нужно минимум 2 животных, получено ${pool.size}" }
    }

    /** Начинает новый раунд, если текущий ещё не завершён — возвращает его же. */
    fun start(): Round {
        current?.let { return it }
        val round = buildRound()
        current = round
        lastTargetId = round.target.id
        lastOptionIds = round.options.mapTo(linkedSetOf()) { it.id }
        positionCounts[round.targetPosition]++
        return round
    }

    /** Ответ ребёнка. При ошибке раунд остаётся тем же. */
    fun answer(animalId: String): Answer {
        val round = current ?: start()
        return if (animalId == round.target.id) {
            correctCount++
            correctByAnimal.merge(round.target.id, 1, Int::plus)
            current = null
            Answer.Correct(round.target)
        } else {
            Answer.Wrong
        }
    }

    /** Животные, которых ребёнок угадал не меньше [minCorrect] раз. */
    fun masteredIds(minCorrect: Int = 2): Set<String> =
        correctByAnimal.filterValues { it >= minCorrect }.keys

    private fun buildRound(): Round {
        val targetCandidates = pool.filter { it.id != lastTargetId }.ifEmpty { pool }
        var attempt = 0

        while (true) {
            val target = targetCandidates.random(random)
            val distractors = pool
                .filter { it.id != target.id }
                .shuffled(random)
                .take(optionsCount - 1)
            val position = leastUsedPosition()

            val slots = MutableList<Animal?>(optionsCount) { null }
            slots[position] = target
            var index = 0
            for (slot in slots.indices) {
                if (slots[slot] == null) slots[slot] = distractors[index++]
            }
            val options = slots.filterNotNull()
            val optionIds = options.mapTo(linkedSetOf()) { it.id }

            attempt++
            val canChangeSet = pool.size > optionsCount
            if (!canChangeSet || optionIds != lastOptionIds || attempt >= MAX_ATTEMPTS) {
                return Round(target = target, options = options, targetPosition = position)
            }
        }
    }

    private fun leastUsedPosition(): Int {
        val min = positionCounts.min()
        val candidates = positionCounts.indices.filter { positionCounts[it] == min }
        return candidates.random(random)
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}
