package com.example.domain.usecase

import com.example.domain.model.DuckRarity
import com.example.domain.model.DuckSpecies
import com.example.domain.model.DuckSpeciesCatalog
import kotlin.random.Random

class HatchDuckUseCase(private val random: Random = Random.Default) {

    fun rollSpecies(luckyEgg: Boolean = false): DuckSpecies {
        val pool = DuckSpeciesCatalog.all
        val weighted = pool.flatMap { species ->
            val rarity = species.rarity
            val weight = when {
                luckyEgg && rarity == DuckRarity.LEGENDARY -> rarity.hatchWeight * 3
                luckyEgg && rarity == DuckRarity.EPIC -> rarity.hatchWeight * 2
                else -> rarity.hatchWeight
            }
            List(weight) { species }
        }
        return weighted[random.nextInt(weighted.size)]
    }

    fun generateSerialNumber(): Long = random.nextLong(100L, 99999L)
}
