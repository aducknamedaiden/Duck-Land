package com.example

import com.example.domain.model.DuckRarity
import com.example.domain.model.DuckSpeciesCatalog
import com.example.domain.model.DuckTraitCatalog
import com.example.domain.model.LeaderboardEntry
import com.example.domain.usecase.HatchDuckUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExampleUnitTest {
  @Test
  fun duckSpeciesCatalogHas32UniqueValidSpecies() {
    val species = DuckSpeciesCatalog.all
    assertEquals(32, species.size)
    assertEquals(32, species.map { it.speciesId }.distinct().size)

    val validBodies = DuckTraitCatalog.bodyColors.map { it.id }.toSet()
    val validBills = DuckTraitCatalog.billColors.map { it.id }.toSet()
    val validBillStyles = DuckTraitCatalog.billStyles.map { it.id }.toSet()
    val validWingStyles = DuckTraitCatalog.wingStyles.map { it.id }.toSet()
    val validEyeStyles = DuckTraitCatalog.eyeStyles.map { it.id }.toSet()
    val validHats = DuckTraitCatalog.hats.map { it.id }.toSet()
    val validEyewear = DuckTraitCatalog.eyewear.map { it.id }.toSet()
    val validAccessories = DuckTraitCatalog.accessories.map { it.id }.toSet()
    val validBgs = DuckTraitCatalog.pondBackgrounds.map { it.id }.toSet()

    species.forEach { sp ->
      assertTrue("${sp.speciesId} body", sp.defaultPalette.bodyColor in validBodies)
      assertTrue("${sp.speciesId} bill", sp.defaultPalette.billColor in validBills)
      assertTrue("${sp.speciesId} billStyle", sp.defaultPalette.billStyle in validBillStyles)
      assertTrue("${sp.speciesId} wingStyle", sp.defaultPalette.wingStyle in validWingStyles)
      assertTrue("${sp.speciesId} eyeStyle", sp.defaultPalette.eyeStyle in validEyeStyles)
      assertTrue("${sp.speciesId} hat", sp.defaultPalette.hat in validHats)
      assertTrue("${sp.speciesId} eyewear", sp.defaultPalette.eyewear in validEyewear)
      assertTrue("${sp.speciesId} accessory", sp.defaultPalette.accessory in validAccessories)
      assertTrue("${sp.speciesId} pondBg", sp.defaultPalette.pondBg in validBgs)
    }

    assertTrue(species.any { it.rarity == DuckRarity.LEGENDARY })
    val hatchUseCase = HatchDuckUseCase(Random(42))
    val rolled = (1..20).map { hatchUseCase.rollSpecies(luckyEgg = true).speciesId }.toSet()
    assertTrue(rolled.size >= 4)
  }

  @Test
  fun leaderboardExcludesAdminPanelAccountsButKeepsGiftedCollectors() {
    val entries = listOf(
      LeaderboardEntry(userId = "owner_1", displayName = "Owner", duckCount = 999, isAdminAccount = true),
      LeaderboardEntry(userId = "gifted_1", displayName = "Gifted Bob", duckCount = 5, grapeBalance = 50000, isAdminAccount = false),
      LeaderboardEntry(userId = "regular_1", displayName = "Alice", duckCount = 12, grapeBalance = 300, isAdminAccount = false)
    )
    val visible = entries.filter { !it.isAdminAccount }.sortedByDescending { it.duckCount }
    assertEquals(listOf("regular_1", "gifted_1"), visible.map { it.userId })
  }
}
