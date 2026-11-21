package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ThemePreference(val label: String) {
    SYSTEM("System"),
    LIGHT("Sunlit Vineyard"),
    DARK("Midnight Vineyard")
}

enum class DuckRarity(
    val id: String,
    val label: String,
    val hatchWeight: Int,
    val suggestedPrice: Long
) {
    COMMON("common", "Common", 58, 45L),
    RARE("rare", "Rare", 28, 110L),
    EPIC("epic", "Epic", 11, 240L),
    LEGENDARY("legendary", "Legendary", 3, 520L);

    companion object {
        fun fromId(raw: String?): DuckRarity =
            entries.firstOrNull { it.id == raw?.lowercase() } ?: COMMON
    }
}

data class DuckPalette(
    val bodyColor: String = "sunny_yellow",
    val billColor: String = "coral_orange",
    val billStyle: String = "classic",
    val wingStyle: String = "classic",
    val eyeStyle: String = "bright",
    val hat: String = "none",
    val eyewear: String = "none",
    val accessory: String = "none",
    val pondBg: String = "grape_pond"
) {
    fun toMap(): Map<String, String> = mapOf(
        "bodyColor" to bodyColor,
        "billColor" to billColor,
        "billStyle" to billStyle,
        "wingStyle" to wingStyle,
        "eyeStyle" to eyeStyle,
        "hat" to hat,
        "eyewear" to eyewear,
        "accessory" to accessory,
        "pondBg" to pondBg
    )

    companion object {
        fun default(): DuckPalette = DuckPalette()

        fun fromMap(map: Map<String, String>?): DuckPalette {
            if (map == null) return DuckPalette()
            return DuckPalette(
                bodyColor = map["bodyColor"]?.takeIf { it.isNotBlank() } ?: "sunny_yellow",
                billColor = map["billColor"]?.takeIf { it.isNotBlank() } ?: "coral_orange",
                billStyle = map["billStyle"]?.takeIf { it.isNotBlank() } ?: "classic",
                wingStyle = map["wingStyle"]?.takeIf { it.isNotBlank() } ?: "classic",
                eyeStyle = map["eyeStyle"]?.takeIf { it.isNotBlank() } ?: "bright",
                hat = map["hat"]?.takeIf { it.isNotBlank() } ?: "none",
                eyewear = map["eyewear"]?.takeIf { it.isNotBlank() } ?: "none",
                accessory = map["accessory"]?.takeIf { it.isNotBlank() } ?: "none",
                pondBg = map["pondBg"]?.takeIf { it.isNotBlank() } ?: "grape_pond"
            )
        }
    }
}

data class TraitOption(
    val id: String,
    val label: String,
    val hex: Long = 0xFFFFD54FL
)

object DuckTraitCatalog {
    val bodyColors = listOf(
        TraitOption("sunny_yellow", "Sunny", 0xFFFFD54F),
        TraitOption("mint_foam", "Mint", 0xFF7BE3D0),
        TraitOption("bubblegum", "Berry", 0xFFFF9EBB),
        TraitOption("lavender", "Lilac", 0xFFC5B4F8),
        TraitOption("sky_cloud", "Sky", 0xFF90D5FF),
        TraitOption("peach_cream", "Peach", 0xFFFFBE8F),
        TraitOption("pearl_white", "Pearl", 0xFFF5F7FA),
        TraitOption("golden_glow", "Gold", 0xFFFFC107),
        TraitOption("grape_crush", "Concord Grape", 0xFF7B2CBF),
        TraitOption("teal_splash", "Teal", 0xFF2EC4B6),
        TraitOption("rose_quartz", "Rose", 0xFFFF6B8B),
        TraitOption("obsidian_night", "Midnight", 0xFF2D3142)
    )

    val billColors = listOf(
        TraitOption("coral_orange", "Coral", 0xFFFF7A45),
        TraitOption("butter_gold", "Butter", 0xFFFFB300),
        TraitOption("rose_berry", "Rose", 0xFFFF5C8A),
        TraitOption("mint_bill", "Seafoam", 0xFF2EC4B6)
    )

    val billStyles = listOf(
        TraitOption("classic", "Classic Oval"),
        TraitOption("wide", "Plump Bill"),
        TraitOption("chirp", "Cheerful Chirp")
    )

    val wingStyles = listOf(
        TraitOption("classic", "Smooth Wing"),
        TraitOption("flappy", "Perky Wing"),
        TraitOption("angel", "Angelic Wings")
    )

    val eyeStyles = listOf(
        TraitOption("bright", "Bright Eyes"),
        TraitOption("wink", "Happy Wink"),
        TraitOption("starry", "Starry Sparkle")
    )

    val hats = listOf(
        TraitOption("none", "None"),
        TraitOption("sprout", "Lily Sprout"),
        TraitOption("sailor", "Sailor Cap"),
        TraitOption("beanie", "Cozy Beanie"),
        TraitOption("crown", "Pond Crown"),
        TraitOption("halo", "Starlight Halo")
    )

    val eyewear = listOf(
        TraitOption("none", "None"),
        TraitOption("shades", "Teal Shades"),
        TraitOption("round", "Scholar Glasses"),
        TraitOption("star", "Star Specs"),
        TraitOption("monocle", "Royal Monocle")
    )

    val accessories = listOf(
        TraitOption("none", "None"),
        TraitOption("bowtie", "Coral Bowtie"),
        TraitOption("scarf", "Mint Scarf"),
        TraitOption("floatie", "Pool Ring"),
        TraitOption("medal", "Pond Medal")
    )

    val pondBackgrounds = listOf(
        TraitOption("grape_pond", "Grape Pond", 0xFFD9B8FF),
        TraitOption("ph_pond", "pH Spectrum", 0xFF9C4DFF),
        TraitOption("mint_mist", "Mint Pond", 0xFFCBEFE9),
        TraitOption("sunset_peach", "Golden Hour", 0xFFFFDFC4),
        TraitOption("lavender_dusk", "Lilac Dusk", 0xFFE3DCFF),
        TraitOption("starlight", "Starlight", 0xFF1F3B4D)
    )

    fun randomizePalette(): DuckPalette = DuckPalette(
        bodyColor = bodyColors.random().id,
        billColor = billColors.random().id,
        billStyle = billStyles.random().id,
        wingStyle = wingStyles.random().id,
        eyeStyle = eyeStyles.random().id,
        hat = hats.random().id,
        eyewear = eyewear.random().id,
        accessory = accessories.random().id,
        pondBg = pondBackgrounds.random().id
    )
}

data class DuckSpecies(
    val speciesId: String,
    val defaultName: String,
    val rarity: DuckRarity,
    val description: String,
    val defaultPalette: DuckPalette
)

object DuckSpeciesCatalog {
    val all: List<DuckSpecies> = listOf(
        // Original 12 species (with explicit modular DuckPalette parameters)
        DuckSpecies(
            "sunny_classic", "Sunny Classic", DuckRarity.COMMON,
            "A timeless cheerful bath-time legend who loves warm ripples.",
            DuckPalette(bodyColor = "sunny_yellow", billColor = "coral_orange", pondBg = "mint_mist")
        ),
        DuckSpecies(
            "mint_sprout", "Mint Sprout", DuckRarity.COMMON,
            "Freshly sprouted by the lily pads with a leafy tiny topper.",
            DuckPalette(bodyColor = "mint_foam", billColor = "butter_gold", hat = "sprout", pondBg = "mint_mist")
        ),
        DuckSpecies(
            "berry_pop", "Berry Pop", DuckRarity.COMMON,
            "Sweet pastel strawberry duckling with a dapper bowtie.",
            DuckPalette(bodyColor = "bubblegum", billColor = "butter_gold", accessory = "bowtie", pondBg = "sunset_peach")
        ),
        DuckSpecies(
            "sky_drifter", "Sky Drifter", DuckRarity.COMMON,
            "Floats carefree on sunny clouds with a cozy teal scarf.",
            DuckPalette(bodyColor = "sky_cloud", billColor = "coral_orange", accessory = "scarf", pondBg = "mint_mist")
        ),
        DuckSpecies(
            "sailor_quack", "Captain Splash", DuckRarity.RARE,
            "Commands every paper boat across the pond with nautical flair.",
            DuckPalette(bodyColor = "pearl_white", billColor = "coral_orange", billStyle = "wide", wingStyle = "flappy", hat = "sailor", accessory = "scarf", pondBg = "mint_mist")
        ),
        DuckSpecies(
            "cool_breeze", "Breeze Shades", DuckRarity.RARE,
            "Always chill in signature teal shades and a bright pool ring.",
            DuckPalette(bodyColor = "sunny_yellow", billColor = "coral_orange", eyeStyle = "wink", eyewear = "shades", accessory = "floatie", pondBg = "sunset_peach")
        ),
        DuckSpecies(
            "lavender_scholar", "Professor Lilac", DuckRarity.RARE,
            "Reads pond poetry wearing tiny round glasses and a beanie.",
            DuckPalette(bodyColor = "lavender", billColor = "butter_gold", hat = "beanie", eyewear = "round", accessory = "bowtie", pondBg = "lavender_dusk")
        ),
        DuckSpecies(
            "peach_champion", "Peach Medal", DuckRarity.EPIC,
            "Three-time synchronized splashing champion of the North Pond.",
            DuckPalette(bodyColor = "peach_cream", billColor = "rose_berry", billStyle = "chirp", wingStyle = "flappy", hat = "beanie", eyewear = "shades", accessory = "medal", pondBg = "sunset_peach")
        ),
        DuckSpecies(
            "star_popstar", "Nova Quack", DuckRarity.EPIC,
            "Lights up twilight ripples with star specs and sparkling glow.",
            DuckPalette(bodyColor = "lavender", billColor = "mint_bill", billStyle = "chirp", wingStyle = "flappy", eyeStyle = "starry", hat = "halo", eyewear = "star", accessory = "bowtie", pondBg = "starlight")
        ),
        DuckSpecies(
            "sakura_float", "Sakura Lotus", DuckRarity.EPIC,
            "Graceful blossom traveler drifting beneath lantern light.",
            DuckPalette(bodyColor = "bubblegum", billColor = "coral_orange", wingStyle = "angel", eyeStyle = "wink", hat = "sprout", eyewear = "round", accessory = "floatie", pondBg = "sunset_peach")
        ),
        DuckSpecies(
            "golden_monarch", "King Ducky", DuckRarity.LEGENDARY,
            "Ruler of Duck Land wearing the radiant Pond Crown and monocle.",
            DuckPalette(bodyColor = "golden_glow", billColor = "coral_orange", billStyle = "wide", wingStyle = "angel", eyeStyle = "starry", hat = "crown", eyewear = "monocle", accessory = "medal", pondBg = "sunset_peach")
        ),
        DuckSpecies(
            "celestial_emperor", "Astral Oracle", DuckRarity.LEGENDARY,
            "Born under a shooting star, shimmering above the midnight pond.",
            DuckPalette(bodyColor = "pearl_white", billColor = "mint_bill", billStyle = "chirp", wingStyle = "angel", eyeStyle = "starry", hat = "halo", eyewear = "star", accessory = "medal", pondBg = "starlight")
        ),

        // 20 Brand-New Ducks!
        DuckSpecies(
            "grape_fizz", "Grape Soda", DuckRarity.COMMON,
            "Fizzy Concord grape duckling who bubbles happily across the vineyard pond.",
            DuckPalette(bodyColor = "grape_crush", billColor = "rose_berry", billStyle = "chirp", wingStyle = "flappy", accessory = "bowtie", pondBg = "grape_pond")
        ),
        DuckSpecies(
            "lilac_napper", "Lilac Napper", DuckRarity.COMMON,
            "Always ready for a cozy sunset nap in a fluffy pink beanie.",
            DuckPalette(bodyColor = "lavender", billColor = "butter_gold", billStyle = "wide", eyeStyle = "wink", hat = "beanie", pondBg = "lavender_dusk")
        ),
        DuckSpecies(
            "peach_bubbles", "Peach Bubbles", DuckRarity.COMMON,
            "Bobs cheerfully in its favorite striped pool ring all afternoon.",
            DuckPalette(bodyColor = "peach_cream", billColor = "coral_orange", wingStyle = "flappy", accessory = "floatie", pondBg = "sunset_peach")
        ),
        DuckSpecies(
            "pearl_swimmer", "Pearl Diver", DuckRarity.COMMON,
            "Glides sleekly through minty shallows with cool teal shades.",
            DuckPalette(bodyColor = "pearl_white", billColor = "mint_bill", eyewear = "shades", pondBg = "mint_mist")
        ),
        DuckSpecies(
            "buttercup_pip", "Buttercup Pip", DuckRarity.COMMON,
            "Chirps hello to every butterfly while tending a tiny lily sprout.",
            DuckPalette(bodyColor = "sunny_yellow", billColor = "butter_gold", billStyle = "chirp", eyeStyle = "starry", hat = "sprout", pondBg = "grape_pond")
        ),
        DuckSpecies(
            "aqua_splash", "Aqua Splash", DuckRarity.COMMON,
            "Zooms along pH-balanced currents wrapped in a cozy mint scarf.",
            DuckPalette(bodyColor = "teal_splash", billColor = "coral_orange", billStyle = "wide", wingStyle = "flappy", accessory = "scarf", pondBg = "ph_pond")
        ),
        DuckSpecies(
            "rosy_blossom", "Rosy Blossom", DuckRarity.COMMON,
            "Blushing rose-quartz duckling dressed up in a coral bowtie.",
            DuckPalette(bodyColor = "rose_quartz", billColor = "rose_berry", eyeStyle = "wink", accessory = "bowtie", pondBg = "sunset_peach")
        ),
        DuckSpecies(
            "vineyard_sommelier", "Vineyard Baron", DuckRarity.RARE,
            "Inspects only the finest Concord grape harvests with a golden monocle.",
            DuckPalette(bodyColor = "grape_crush", billColor = "butter_gold", eyeStyle = "wink", eyewear = "monocle", accessory = "bowtie", pondBg = "grape_pond")
        ),
        DuckSpecies(
            "ph_chemist", "Professor pH", DuckRarity.RARE,
            "Measures litmus pond chemistry and keeps every ripple vibrant!",
            DuckPalette(bodyColor = "mint_foam", billColor = "mint_bill", billStyle = "chirp", wingStyle = "flappy", eyeStyle = "starry", hat = "sprout", eyewear = "round", accessory = "medal", pondBg = "ph_pond")
        ),
        DuckSpecies(
            "cloud_mariner", "Sky Mariner", DuckRarity.RARE,
            "Sails sky-blue lagoons wearing crisp sailor stripes and shades.",
            DuckPalette(bodyColor = "sky_cloud", billColor = "coral_orange", billStyle = "wide", hat = "sailor", eyewear = "shades", accessory = "scarf", pondBg = "mint_mist")
        ),
        DuckSpecies(
            "sunset_surfer", "Sunset Surfer", DuckRarity.RARE,
            "Rides golden evening ripples with a pool ring and fearless grin.",
            DuckPalette(bodyColor = "peach_cream", billColor = "butter_gold", billStyle = "chirp", wingStyle = "flappy", eyeStyle = "wink", eyewear = "shades", accessory = "floatie", pondBg = "sunset_peach")
        ),
        DuckSpecies(
            "berry_skater", "Berry Skater", DuckRarity.RARE,
            "Tears up the pond boardwalk in a beanie, shades, and breezy scarf.",
            DuckPalette(bodyColor = "bubblegum", billColor = "mint_bill", billStyle = "wide", wingStyle = "flappy", eyeStyle = "starry", hat = "beanie", eyewear = "shades", accessory = "scarf", pondBg = "ph_pond")
        ),
        DuckSpecies(
            "gilded_swimmer", "Gilded Swimmer", DuckRarity.RARE,
            "Glitters in the sun while lounging in luxury with a fancy monocle.",
            DuckPalette(bodyColor = "golden_glow", billColor = "coral_orange", billStyle = "wide", eyewear = "monocle", accessory = "floatie", pondBg = "grape_pond")
        ),
        DuckSpecies(
            "velvet_connoisseur", "Concord Duke", DuckRarity.EPIC,
            "Grape Pond aristocracy with angelic wings and a gleaming crown.",
            DuckPalette(bodyColor = "grape_crush", billColor = "butter_gold", wingStyle = "angel", eyeStyle = "wink", hat = "crown", eyewear = "monocle", accessory = "scarf", pondBg = "grape_pond")
        ),
        DuckSpecies(
            "obsidian_dj", "Midnight DJ", DuckRarity.EPIC,
            "Spins starry pond beats until dawn wearing superstar star specs.",
            DuckPalette(bodyColor = "obsidian_night", billColor = "mint_bill", billStyle = "chirp", wingStyle = "flappy", eyeStyle = "starry", hat = "beanie", eyewear = "star", accessory = "medal", pondBg = "starlight")
        ),
        DuckSpecies(
            "cloud_seraph", "Cloud Seraph", DuckRarity.EPIC,
            "Floats softly above lilac dusk mists on feathery golden wings.",
            DuckPalette(bodyColor = "pearl_white", billColor = "rose_berry", billStyle = "chirp", wingStyle = "angel", eyeStyle = "starry", hat = "halo", accessory = "scarf", pondBg = "lavender_dusk")
        ),
        DuckSpecies(
            "litmus_alchemist", "Litmus Wizard", DuckRarity.EPIC,
            "Brews rainbow pH potions that turn twilight water emerald and violet.",
            DuckPalette(bodyColor = "teal_splash", billColor = "rose_berry", billStyle = "wide", wingStyle = "angel", eyeStyle = "starry", hat = "sprout", eyewear = "star", accessory = "medal", pondBg = "ph_pond")
        ),
        DuckSpecies(
            "rose_duchess", "Rose Duchess", DuckRarity.EPIC,
            "Hosts royal tea parties on rose petals wearing a tiny golden crown.",
            DuckPalette(bodyColor = "rose_quartz", billColor = "butter_gold", wingStyle = "angel", eyeStyle = "wink", hat = "crown", eyewear = "round", accessory = "bowtie", pondBg = "sunset_peach")
        ),
        DuckSpecies(
            "archduke_vineyard", "Grape Emperor", DuckRarity.LEGENDARY,
            "Supreme guardian of the Grape Harvest with crowned angel wings.",
            DuckPalette(bodyColor = "grape_crush", billColor = "butter_gold", billStyle = "wide", wingStyle = "angel", eyeStyle = "starry", hat = "crown", eyewear = "star", accessory = "medal", pondBg = "ph_pond")
        ),
        DuckSpecies(
            "eclipse_phoenix", "Eclipse Phoenix", DuckRarity.LEGENDARY,
            "Mythic midnight duckling crowned with a glowing starlight halo.",
            DuckPalette(bodyColor = "obsidian_night", billColor = "coral_orange", billStyle = "chirp", wingStyle = "angel", eyeStyle = "starry", hat = "halo", eyewear = "monocle", accessory = "medal", pondBg = "starlight")
        )
    )

    fun byId(speciesId: String): DuckSpecies =
        all.firstOrNull { it.speciesId == speciesId } ?: all.first()
}

@Keep
@IgnoreExtraProperties
data class CollectorProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarConfig: Map<String, String> = DuckPalette.default().toMap(),
    val duckCount: Long = 0L,
    val tradesCompleted: Long = 0L,
    val banned: Boolean = false,
    val banReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Wallet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val coins: Long = 250L,
    val lastClaimId: String = "",
    val lastClaimType: String = "none",
    @ServerTimestamp val lastDailyClaimAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RubberDuck(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val speciesId: String = "sunny_classic",
    val name: String = "Sunny Classic",
    val rarity: String = "common",
    val serialNumber: Long = 1L,
    val palette: Map<String, String> = DuckPalette.default().toMap(),
    val listed: Boolean = false,
    val listingId: String = "",
    val lastTradeId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketplaceListing(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerAvatar: Map<String, String> = DuckPalette.default().toMap(),
    val buyerId: String = "",
    val duckId: String = "",
    val duckName: String = "",
    val duckSpeciesId: String = "",
    val duckRarity: String = "common",
    val duckSerialNumber: Long = 1L,
    val duckPalette: Map<String, String> = DuckPalette.default().toMap(),
    val askingCoins: Long = 50L,
    val wantsTrade: Boolean = true,
    val desiredRarity: String = "any",
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TradeOffer(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val participantIds: List<String> = emptyList(),
    val listingId: String = "",
    val offeredDuckId: String = "",
    val offeredDuckName: String = "",
    val offeredDuckRarity: String = "common",
    val offeredDuckPalette: Map<String, String> = DuckPalette.default().toMap(),
    val requestedDuckId: String = "",
    val requestedDuckName: String = "",
    val requestedDuckRarity: String = "common",
    val requestedDuckPalette: Map<String, String> = DuckPalette.default().toMap(),
    val offeredCoins: Long = 0L,
    val note: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CoinClaim(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val sourceId: String = "",
    val sourceType: String = "listing_sale",
    val amount: Long = 0L,
    val claimed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WeeklyEvent(
    @DocumentId val id: String = "",
    val title: String = "",
    val themeTag: String = "",
    val description: String = "",
    val bonusRarity: String = "any",
    val active: Boolean = true,
    val weekNumber: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EventEntry(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val ownerDisplayName: String = "",
    val ownerAvatar: Map<String, String> = DuckPalette.default().toMap(),
    val duckId: String = "",
    val duckName: String = "",
    val duckSpeciesId: String = "",
    val duckRarity: String = "common",
    val duckPalette: Map<String, String> = DuckPalette.default().toMap(),
    val caption: String = "",
    val voteCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PondNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "event_started",
    val title: String = "",
    val body: String = "",
    val referenceId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminGrant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ownerCode: String = "",
    @ServerTimestamp val grantedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RedeemedCode(
    @DocumentId val id: String = "",
    val userId: String = "",
    val code: String = "",
    val rewardSummary: String = "",
    @ServerTimestamp val redeemedAt: Timestamp? = null
)

sealed interface PromoReward {
    val summary: String

    data class DuckReward(
        val code: String,
        val species: DuckSpecies,
        val customName: String,
        val rarity: DuckRarity,
        val duck: RubberDuck? = null
    ) : PromoReward {
        override val summary: String
            get() = "${rarity.label} $customName unlocked!"
    }

    data class CoinReward(
        val code: String,
        val coins: Long
    ) : PromoReward {
        override val summary: String
            get() = "+$coins Grapes added!"
    }

    data object OwnerUnlockReward : PromoReward {
        override val summary: String = "Owner Mode Unlocked!"
    }
}

data class PromoCodeDefinition(
    val code: String,
    val title: String,
    val subtitle: String,
    val isSecret: Boolean = false,
    val reward: PromoReward
)

object PromoCodeCatalog {
    const val OWNER_CODE = "*OWNERDUCK#1*"

    val allCodes: List<PromoCodeDefinition> = listOf(
        PromoCodeDefinition(
            code = "BETA",
            title = "Beta Tester Epic Duck",
            subtitle = "Unlocks the limited Epic Beta Test Lotus",
            reward = PromoReward.DuckReward(
                code = "BETA",
                species = DuckSpeciesCatalog.byId("sakura_float"),
                customName = "Beta Test Lotus",
                rarity = DuckRarity.EPIC
            )
        ),
        PromoCodeDefinition(
            code = "QUACK100",
            title = "Starter Grape Harvest",
            subtitle = "Grants +100 Grapes",
            reward = PromoReward.CoinReward("QUACK100", 100L)
        ),
        PromoCodeDefinition(
            code = "SPLASH250",
            title = "Pond Vineyard Gift",
            subtitle = "Grants +250 Grapes",
            reward = PromoReward.CoinReward("SPLASH250", 250L)
        ),
        PromoCodeDefinition(
            code = "POND500",
            title = "Royal Grape Jackpot",
            subtitle = "Grants +500 Grapes",
            reward = PromoReward.CoinReward("POND500", 500L)
        ),
        PromoCodeDefinition(
            code = "SAILOR",
            title = "Harbor Crew Duck",
            subtitle = "Unlocks Rare Captain Splash",
            reward = PromoReward.DuckReward(
                code = "SAILOR",
                species = DuckSpeciesCatalog.byId("sailor_quack"),
                customName = "Captain Splash",
                rarity = DuckRarity.RARE
            )
        ),
        PromoCodeDefinition(
            code = "COZYDUCK",
            title = "Cozy Pond Scholar",
            subtitle = "Unlocks Rare Professor Lilac",
            reward = PromoReward.DuckReward(
                code = "COZYDUCK",
                species = DuckSpeciesCatalog.byId("lavender_scholar"),
                customName = "Professor Lilac",
                rarity = DuckRarity.RARE
            )
        ),
        PromoCodeDefinition(
            code = "STARLIGHT",
            title = "Twilight Concert Duck",
            subtitle = "Unlocks Epic Nova Quack",
            reward = PromoReward.DuckReward(
                code = "STARLIGHT",
                species = DuckSpeciesCatalog.byId("star_popstar"),
                customName = "Nova Quack",
                rarity = DuckRarity.EPIC
            )
        ),
        PromoCodeDefinition(
            code = "GOLDCROWN",
            title = "Royal Monarch Duck",
            subtitle = "Unlocks Legendary King Ducky",
            reward = PromoReward.DuckReward(
                code = "GOLDCROWN",
                species = DuckSpeciesCatalog.byId("golden_monarch"),
                customName = "King Ducky",
                rarity = DuckRarity.LEGENDARY
            )
        ),
        PromoCodeDefinition(
            code = "ASTRAL777",
            title = "Shooting Star Oracle",
            subtitle = "Unlocks Legendary Astral Oracle",
            reward = PromoReward.DuckReward(
                code = "ASTRAL777",
                species = DuckSpeciesCatalog.byId("celestial_emperor"),
                customName = "Astral Oracle",
                rarity = DuckRarity.LEGENDARY
            )
        ),
        PromoCodeDefinition(
            code = OWNER_CODE,
            title = "Owner Secret Key",
            subtitle = "Elevates account to Owner Admin Panel",
            isSecret = true,
            reward = PromoReward.OwnerUnlockReward
        )
    )

    val publicCodes: List<PromoCodeDefinition> = allCodes.filter { !it.isSecret }

    fun lookup(rawCode: String): PromoCodeDefinition? {
        val normalized = rawCode.trim().uppercase()
        if (normalized.isEmpty()) return null
        return allCodes.firstOrNull { it.code == normalized }
    }
}

data class DuckLoreCard(
    val duckId: String,
    val title: String,
    val backstory: String,
    val eventCaption: String,
    val tradeTip: String
)

data class AvatarSuggestion(
    val palette: DuckPalette,
    val styleTitle: String,
    val reason: String
)

data class DailyMissionDefinition(
    val missionId: String,
    val title: String,
    val description: String,
    val target: Long,
    val rewardCoins: Long
)

object DailyMissionCatalog {
    const val MISSION_VISIT_POND = "visit_pond"
    const val MISSION_HATCH_EGG = "hatch_egg"
    const val MISSION_TRADE_DUCK = "trade_duck"
    const val MISSION_CHEER_EVENT = "cheer_event"

    val all: List<DailyMissionDefinition> = listOf(
        DailyMissionDefinition(
            missionId = MISSION_VISIT_POND,
            title = "Visit the Pond",
            description = "Splash into your pond collection today",
            target = 1L,
            rewardCoins = 40L
        ),
        DailyMissionDefinition(
            missionId = MISSION_HATCH_EGG,
            title = "Hatch a Duck Egg",
            description = "Hatch any Standard or Lucky egg in the Hatchery",
            target = 1L,
            rewardCoins = 60L
        ),
        DailyMissionDefinition(
            missionId = MISSION_TRADE_DUCK,
            title = "Trade One Duck",
            description = "Send a trade offer or accept a swap in the Marketplace",
            target = 1L,
            rewardCoins = 100L
        ),
        DailyMissionDefinition(
            missionId = MISSION_CHEER_EVENT,
            title = "Cheer a Showcase Duck",
            description = "Cheer for a collector's entry in Weekly Pond Events",
            target = 1L,
            rewardCoins = 50L
        )
    )

    fun byId(missionId: String): DailyMissionDefinition? =
        all.firstOrNull { it.missionId == missionId }

    fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun docId(dateKey: String, missionId: String): String =
        "${dateKey}_${missionId}"
}

@Keep
@IgnoreExtraProperties
data class DailyMissionProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val missionId: String = "",
    val dateKey: String = "",
    val title: String = "",
    val description: String = "",
    val progress: Long = 0L,
    val target: Long = 1L,
    val rewardCoins: Long = 40L,
    val claimed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isCompleted: Boolean
        get() = progress >= target
}

@Keep
@IgnoreExtraProperties
data class AppVersionConfig(
    @DocumentId val id: String = "version",
    val minRequiredVersionCode: Long = 3L,
    val latestVersionCode: Long = 3L,
    val latestVersionName: String = "1.2",
    val updateMessage: String = "A newer version of Duck Land is ready with fresh ducks, collector rankings, and pond upgrades!",
    val storeUrl: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val avatarConfig: Map<String, String> = DuckPalette.default().toMap(),
    val duckCount: Long = 0L,
    val uniqueSpeciesCount: Long = 0L,
    val tradesCompleted: Long = 0L,
    val grapeBalance: Long = 0L,
    val isAdminAccount: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
