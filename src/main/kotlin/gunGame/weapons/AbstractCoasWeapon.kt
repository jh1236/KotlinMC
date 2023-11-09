package gunGame.weapons

import abstractions.flow.If
import abstractions.hasTag
import abstractions.score.Objective
import abstractions.score.Score
import commands.Command
import enums.Anchor
import enums.Items
import events.ScoreEventManager
import gunGame.playingTag
import gunGame.self
import lib.debug.Log
import structure.Fluorite
import structure.McFunction
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class AbstractCoasWeapon(
    name: String,
    damage: Int,
    customModelData: Int = 0,
    val cooldown: Double,
    val clipsize: Int = 1,
    val reload: Double = 0.0,
    secondary: Boolean = false,
    isReward: Boolean = false
) : AbstractWeapon(name, damage, secondary, isReward, customModelData) {
    companion object {
        val currentId: Score = Fluorite.getNewFakeScore("id")
        val sprayObjective = Objective("spray")

        @JvmStatic
        lateinit var coasManager: ScoreEventManager
            private set

        fun setCoasFunction(coas: ScoreEventManager) {
            coasManager = coas
        }
    }

    open var damageStr = abs(damage).toString()
    open var cooldownStr = abs(cooldown).toString() + " seconds"
    open var reloadStr = abs(reload).toString() + " seconds"
    open var clipsizeStr = abs(clipsize).toString()
    val extraLines = arrayListOf<String>()

    val cdBeforeShot = Fluorite.reuseFakeScore("b4shot")

    val shootFunction: McFunction = McFunction(basePath)

    fun setup() {
        shootFunction += {
            shoot()
        }
        coasManager += {
            Command.execute().anchored(Anchor.EYES).If(self.hasTag(playingTag).canShoot()).run {
                If(currentId eq myId) {
                    shootFunction()
                }
            }
        }
        val lore = arrayListOf(
            """{"text" : "Damage: $damageStr", "color": "gray", "italic" : false}"""
        )
        if (clipsize > 1) {
            lore.add("""{"text":"Cooldown: $cooldownStr", "color": "gray", "italic" : false}""")
            lore.add("""{"text":"Clipsize: $clipsizeStr", "color": "gray", "italic" : false}""")
            lore.add("""{"text":"Reload Time: $reloadStr", "color": "gray", "italic" : false}""")
        } else {
            lore.add("{\"text\":\"Reload Time: $cooldownStr\", \"color\": \"gray\", \"italic\" : false}")
        }
        lore.addAll(extraLines)
        val sb = StringBuilder("{jh1236:{ready:1b, weapon:$myId, cooldown: {value: 0, max : 0}, ")
        if (clipsize > 1) {
            sb.append("ammo: {value: $clipsize, max:$clipsize},")
        }
        sb.append("}, ")
        if (customModelData > 0) {
            sb.append("CustomModelData:${customModelData}, ")
        }
        sb.append("}")
        lootTable = LootTableGenerator.genLootTable(basePath, Items.CARROT_ON_A_STICK, name, lore, sb.toString())
        setupInternal()
    }

    private fun shoot() {
        Log.debug("shot weapon with id $myId!!")
        If(!self["nbt = {SelectedItem:{tag:{jh1236:{ready:0b}}}}"]) {
            cdBeforeShot.set(self.data["SelectedItem.tag.jh1236.cooldown.value"])
            if (clipsize != 1) {
                applyCoolDown(decrementClip((cooldown * 20).roundToInt(), (reload * 20).roundToInt()))
            } else if (cooldown > 0 || (cooldown * 20).roundToInt() == Score.LOWER) {
                applyCoolDown((cooldown * 20).roundToInt())
            }
        }
        fire()
    }

    abstract fun fire()




}