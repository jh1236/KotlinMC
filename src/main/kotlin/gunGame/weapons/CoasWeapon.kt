package gunGame.weapons

import abstractions.If
import abstractions.flow.If
import abstractions.hasTag
import commands.Command
import enums.Anchor
import events.ScoreEventManager
import gunGame.playingTag
import gunGame.self
import structure.Fluorite
import structure.McFunction
import utils.score.Score

abstract class AbstractCoasWeapon(name: String, damage: Int) : AbstractWeapon(name, damage) {
    companion object {
        val currentId: Score = Fluorite.getNewFakeScore("id")

        @JvmStatic
        protected lateinit var coasManager: ScoreEventManager
            private set

        fun setCoasFunction(coas: ScoreEventManager) {
            coasManager = coas
        }
    }



    lateinit var shootFunction: McFunction

    fun setup() {
        shootFunction = McFunction(basePath)
        shootFunction.append {
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

    abstract fun shoot()

}