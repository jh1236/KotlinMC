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
import utils.Selector
import kotlin.math.roundToInt

abstract class AbstractCoasWeapon(
    name: String,
    damage: Int,
    val customModelData: Int = 0,
    val cooldown: Double,
    val clipsize: Int = 1,
    val reload: Double = 0.0,
    secondary: Boolean = false,
    isReward: Boolean = false
) : AbstractWeapon(name, damage, secondary, isReward) {
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
    }

    private fun shoot() {
        Log.debug("shot weapon with id $myId!!")
        If(!self["nbt = {SelectedItem:{tag:{jh1236:{ready:0b}}}}"]) {
            cdBeforeShot.set(self.data["SelectedItem.tag.jh1236.cooldown.value"])
            if (clipsize != 1) {
                applyCoolDown(decrementClip((cooldown * 20).roundToInt(), (reload * 20).roundToInt()))
            } else {
                applyCoolDown((cooldown * 20).roundToInt())
            }
        }
        fire()
    }

    abstract fun fire()

    protected val itemNBT: String
        get() {
            val sb = StringBuilder("{jh1236:{ready:1b, weapon:$myId, cooldown: {value: 0, max : 0}, ")
            if (clipsize > 1) {
                sb.append("ammo: {value: $clipsize, max:$clipsize},")
            }
            sb.append("}, ")
            if (customModelData > 0) {
                sb.append("CustomModelData:${customModelData}, ")
            }
            sb.append("display:{ Name: '{\"text\":\"$name\",\"italic\" : false}'}, ")
            sb.append("}")
            return sb.toString()
        }

    override fun give(player: Selector) {
        Command.give(
            player, Items.CARROT_ON_A_STICK.nbt(itemNBT)
        )
    }


}