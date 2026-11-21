package com.example.data.ai

import com.example.domain.model.AvatarSuggestion
import com.example.domain.model.DuckLoreCard
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckSpeciesCatalog
import com.example.domain.model.DuckTraitCatalog
import com.example.domain.model.RubberDuck
import com.example.domain.repository.DuckOracleRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.json.JSONObject

class FirebaseDuckOracleRepository : DuckOracleRepository {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(
                    "You are the Pond Oracle of Duck Land, a friendly, whimsical rubber-duck collector's guide. " +
                        "The in-game currency is Grapes. Help players evaluate duck rarities (Common, Rare, Epic, Legendary), plan fair duck-for-duck trades, " +
                        "craft creative weekly pond showcase ideas, and enjoy playful duck lore. Keep answers concise, warm, and upbeat."
                )
            }
        )
    }

    private val loreSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "A catchy 2-5 word collector title for this duck"),
                "backstory" to Schema.string(description = "A whimsical 2-sentence origin story for this rubber duck"),
                "eventCaption" to Schema.string(description = "A punchy 1-sentence Weekly Pond Event showcase caption under 110 characters"),
                "tradeTip" to Schema.string(description = "A practical 1-sentence Duck Land trading and Grape valuation tip")
            )
        )
    }

    private val loreModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = loreSchema
            }
        )
    }

    private val avatarSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "bodyColor" to Schema.enumeration(
                    values = DuckTraitCatalog.bodyColors.map { it.id },
                    description = "Duck body color ID"
                ),
                "billColor" to Schema.enumeration(
                    values = DuckTraitCatalog.billColors.map { it.id },
                    description = "Duck bill color ID"
                ),
                "billStyle" to Schema.enumeration(
                    values = DuckTraitCatalog.billStyles.map { it.id },
                    description = "Modular duck bill shape ID"
                ),
                "wingStyle" to Schema.enumeration(
                    values = DuckTraitCatalog.wingStyles.map { it.id },
                    description = "Modular duck wing style ID"
                ),
                "eyeStyle" to Schema.enumeration(
                    values = DuckTraitCatalog.eyeStyles.map { it.id },
                    description = "Modular duck eye expression ID"
                ),
                "hat" to Schema.enumeration(
                    values = DuckTraitCatalog.hats.map { it.id },
                    description = "Hat ID"
                ),
                "eyewear" to Schema.enumeration(
                    values = DuckTraitCatalog.eyewear.map { it.id },
                    description = "Eyewear ID"
                ),
                "accessory" to Schema.enumeration(
                    values = DuckTraitCatalog.accessories.map { it.id },
                    description = "Accessory ID"
                ),
                "pondBg" to Schema.enumeration(
                    values = DuckTraitCatalog.pondBackgrounds.map { it.id },
                    description = "Pond background ID"
                ),
                "styleTitle" to Schema.string(description = "Short 2-4 word name for this avatar outfit"),
                "reason" to Schema.string(description = "Short 1-sentence explanation of why these traits match the vibe")
            )
        )
    }

    private val avatarStylistModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = avatarSchema
            }
        )
    }

    override fun streamOracleAnswer(
        prompt: String,
        myDucks: List<RubberDuck>,
        coins: Long,
        activeEventTitle: String?
    ): Flow<String> {
        val flockSummary = if (myDucks.isEmpty()) {
            "0 ducks yet"
        } else {
            myDucks.take(15).joinToString("; ") { "${it.name} (${it.rarity}, #${it.serialNumber})" }
        }
        val contextualPrompt = buildString {
            appendLine("Collector context:")
            appendLine("- Grapes balance: $coins Grapes")
            appendLine("- Flock size: ${myDucks.size} ducks ($flockSummary)")
            if (!activeEventTitle.isNullOrBlank()) {
                appendLine("- Active Weekly Event: $activeEventTitle")
            }
            appendLine()
            appendLine("Collector question: ${prompt.trim()}")
        }
        return chatModel.generateContentStream(contextualPrompt).mapNotNull { chunk ->
            chunk.text?.takeIf { it.isNotEmpty() }
        }
    }

    override suspend fun generateDuckLore(
        duck: RubberDuck,
        activeEventTitle: String?
    ): DuckLoreCard {
        val species = DuckSpeciesCatalog.byId(duck.speciesId)
        val palette = DuckPalette.fromMap(duck.palette)
        val prompt = buildString {
            appendLine("Create collector lore and showcase details for this digital rubber duck in Duck Land:")
            appendLine("- Name: ${duck.name}")
            appendLine("- Species: ${species.defaultName} (${species.description})")
            appendLine("- Rarity: ${duck.rarity}")
            appendLine("- Serial Number: #${duck.serialNumber}")
            appendLine("- Traits: body=${palette.bodyColor}, bill=${palette.billColor}, hat=${palette.hat}, eyewear=${palette.eyewear}, accessory=${palette.accessory}, pond=${palette.pondBg}")
            if (!activeEventTitle.isNullOrBlank()) {
                appendLine("- Current Weekly Event Theme: $activeEventTitle")
            }
        }
        val response = loreModel.generateContent(prompt)
        val jsonText = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
        val json = JSONObject(jsonText)
        return DuckLoreCard(
            duckId = duck.id,
            title = json.getString("title"),
            backstory = json.getString("backstory"),
            eventCaption = json.getString("eventCaption").take(120),
            tradeTip = json.getString("tradeTip")
        )
    }

    override suspend fun suggestAvatarLook(vibePrompt: String): AvatarSuggestion {
        val prompt = "Design a cohesive Duck Land rubber duck avatar outfit matching this vibe: \"${vibePrompt.trim()}\""
        val response = avatarStylistModel.generateContent(prompt)
        val jsonText = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
        val json = JSONObject(jsonText)
        val palette = DuckPalette(
            bodyColor = json.getString("bodyColor"),
            billColor = json.getString("billColor"),
            billStyle = json.optString("billStyle", "classic"),
            wingStyle = json.optString("wingStyle", "classic"),
            eyeStyle = json.optString("eyeStyle", "bright"),
            hat = json.getString("hat"),
            eyewear = json.getString("eyewear"),
            accessory = json.getString("accessory"),
            pondBg = json.getString("pondBg")
        )
        return AvatarSuggestion(
            palette = palette,
            styleTitle = json.getString("styleTitle"),
            reason = json.getString("reason")
        )
    }

    private companion object {
        const val MODEL_NAME = "gemini-3.8-flash"
    }
}
