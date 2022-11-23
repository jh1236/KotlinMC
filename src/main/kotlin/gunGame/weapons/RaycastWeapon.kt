package gunGame.weapons

import abstractions.*
import abstractions.flow.If
import abstractions.flow.Tree
import commands.Command
import enums.Blocks
import enums.IParticle
import gunGame.damageSelf
import gunGame.ifPlaying
import gunGame.playingTag
import gunGame.self
import lib.get
import lib.rangeScore
import lib.raycastEntity
import structure.McFunction
import utils.*
import utils.score.Objective
import utils.score.Score
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.random.Random

open class RaycastWeapon(
    name: String,
    damage: Int,
    customModelData: Int,
    cooldown: Double,
    clipSize: Int = 1,
    reloadTime: Double = 0.0,
    private var piercing: Boolean = false,
    protected var particleCount: Int = 1,
    private var sound: List<Pair<String, Double>> = arrayListOf(),
    private var particle: IParticle? = null,
    private var bulletsPerShot: Int = 1,
    private var range: Int = -1,
    private var spread: Double = 0.0,
    private var onWallHit: ((Selector) -> Unit)? = null,
    private var onEntityHit: (RaycastWeapon.(Selector, Selector) -> Unit)? = null,
    private var splashRange: Double = 0.0,
    private var killMessage: String = """'["",{"selector":"@s", "color":"gold"}, " was killed by ", {"selector":"@a[tag = $shootTag]"}]'""",
    secondary: Boolean = false,
    isReward: Boolean = false
) : AbstractCoasWeapon(name, damage, customModelData, cooldown, clipSize, reloadTime, secondary, isReward) {

    companion object {
        private val random = Random(19930749089)
        private val sprayObjective = Objective("spray")
    }

    override fun fire() {

        shootTag.add(self)
        if (spread > 0) {
            val score = sprayObjective[self]
            if (bulletsPerShot > 1) {
                val shootFunction = McFunction { shotWithSpread(score) }
                repeat(bulletsPerShot) {
                    shootFunction()
                }
            } else {
                shotWithSpread(score)
            }
        } else {
            rayCast()
        }
        for ((sound, pitch) in sound) {
            Command.playsound(sound).master(self["tag =! noSound"], rel(), 1.0, pitch)
        }

        shootTag.remove(self)
    }


    private fun splash() {
        var previous = 0.0
        val d = (damage / (4 * splashRange)).roundToInt()
        for (i in 1 until (4 * splashRange).roundToInt()) {
            Command.execute().asat('e'["distance = ${previous}..${i.toDouble() / 4}"].hasTag(playingTag)).run {
                damageSelf((d * (4 * splashRange - i)).roundToInt())
            }
            previous = (i.toDouble() / 4)
        }
    }

    private fun rayCast() {
        Command.data().merge.storage("jh1236:message", "{death: $killMessage}")
        raycastEntity(.25f, {
            particle?.let { particle ->
                Command.particle(particle, rel(),0, 0, 0,0.0, particleCount).force(
                    if (cooldown > .05) 'a'[""] else 'a'[""].notHasTag(
                        shootTag
                    )
                )
            }
            //TODO: stop being lazy
            Command.execute().unless(loc(0, 0, 0.25) isBlock Blocks.tag("jh1236:air"))
                .run { Command.raw("particle dust_color_transition 0.361 0.361 0.361 1 0.871 0.871 0.871 ~ ~ ~ 0 0 0 0 5 normal @a") }
        }, {
            Command.execute().ifPlaying(self.notHasTag(shootTag).notHasTag(safeTag)).run {
                safeTag.add(self)
                damageSelf(damage)
                onEntityHit?.let {
                    this.it(self, 'a'[""].hasTag(shootTag))
                }
                if (splashRange > 0) {
                    splash()
                }
                if (!piercing) {
                    onWallHit?.let { it(self) }
                    rangeScore.set(0)
                }
            }
        }, range, onWallHit = {
            onWallHit?.let { it(self) }
            if (splashRange > 0) {
                splash()
            }
        }
        )
        safeTag.remove('e'[""])
    }

    private fun getRandomOffset(spread: Double): Double {
        return round((random.nextDouble() - .5f) * (random.nextDouble() - .5f) * 1000 * spread) / 100f
    }

    private fun shotWithSpread(score: Score) {
        Tree(score, 0..200) {
            Command.execute().If(score eq it)
                .rotated(Vec2("~${getRandomOffset(spread)}", "~${getRandomOffset(spread)}")).run { rayCast() }
        }
        score -= 1
        If(score lt 0) {
            score.set(200)
        }
    }

}