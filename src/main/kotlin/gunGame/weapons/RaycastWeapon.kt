package gunGame.weapons

import abstractions.flow.If
import abstractions.flow.trees.ScoreTree
import abstractions.hasTag
import abstractions.notHasTag
import abstractions.score.Score
import commands.Command
import enums.Blocks
import enums.IParticle
import gunGame.*
import lib.Quad
import lib.rangeScore
import lib.raycastEntity
import structure.McFunction
import utils.Selector
import utils.get
import utils.loc
import utils.rel
import kotlin.math.abs
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
    val particleArray: ArrayList<Quad<IParticle, Int, Double, Double>> = arrayListOf(),
    private var sound: List<Pair<String, Double>> = arrayListOf(),
    private var bulletsPerShot: Int = 1,
    private var range: Int = 0,
    private var spread: Double = 0.0,
    protected var onWallHit: ((Selector) -> Unit)? = null,
    protected var onEntityHit: (RaycastWeapon.(Selector, Selector) -> Unit)? = null,
    private var splashRange: Double = 0.0,
    protected var onFireBullet: (() -> Unit)? = null,
    protected var onRaycastTick: (() -> Unit)? = null,
    protected var killMessage: String = """'["",{"selector":"@s", "color":"gold"}, " was killed by ", {"selector":"@a[tag = $shootTag]"}]'""",
    secondary: Boolean = false,
    isReward: Boolean = false,
) : AbstractCoasWeapon(name, damage, customModelData, cooldown, clipSize, reloadTime, secondary, isReward) {

    companion object {
        private val random = Random(19930749089)
    }

    var showShooterParticle = true

    init {
        if (range != 0) {
            extraLines.add(
                """{"text": "Range: ${abs(range / 4.0)} blocks", "color": "gray", "italic" : false}""".replace(
                    ".0",
                    ""
                )
            )
        }
        if (bulletsPerShot != 1) {
            extraLines.add("""{"text":"Bullets per shot: ${abs(bulletsPerShot)}", "color": "gray", "italic" : false}""")
        }
        if (splashRange != 0.0) {
            extraLines.add("""{"text":"Splash Radius: ${kotlin.math.abs(splashRange)}", "color": "gray", "italic" : false}""")
        }
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
            Command.playsound(sound).master('a'["tag =! noSound"], rel(), 1.0, pitch)
        }

        shootTag.remove(self)
    }


    private fun splash() {
        var previous = 0.0
        val d = (damage / (4 * splashRange)).roundToInt()
        for (i in 1 until (4 * splashRange).roundToInt()) {
            Command.execute().asat('e'["distance = ${previous}..${i.toDouble() / 4}"].hasTag(playingTag)).run {
                if (damage > 0) {
                    damageSelf((d * (4 * splashRange - i)).roundToInt())
                }
                onEntityHit?.let { this@RaycastWeapon.it(self, 'a'["limit = 1"].hasTag(shootTag)) }
            }
            previous = (i.toDouble() / 4)
        }
    }

    private fun rayCast() {
        deathStorage["death"] = killMessage
        onFireBullet?.let { it() }
        raycastEntity(.25f, {
            onRaycastTick?.let { it1 -> it1() }
            If(rel() isBlock Blocks.BELL) {
                Command.playsound("block.bell.use")
                    .player('a'[""], rel(), 2)
            }
            particleArray.forEach { (particle, count, radius, speed) ->
                If(rangeScore.rem(2) eq 0) {
                    Command.particle(particle, rel(), radius, radius, radius, speed, count).force(
                        if (showShooterParticle) 'a'[""] else 'a'[""].notHasTag(
                            shootTag
                        ).hasTag(lagTag)
                    )
                }
                Command.particle(particle, rel(), radius, radius, radius, speed, count).force(
                    if (showShooterParticle) 'a'[""] else 'a'[""].notHasTag(
                        shootTag
                    ).notHasTag(lagTag)
                )
                val force =
                    Command.particle(particle, loc(0, 0, -.0625), radius, radius, radius, speed, count).force(
                        (if (showShooterParticle) 'a'[""] else 'a'[""].notHasTag(
                            shootTag
                        )).notHasTag(lagTag)
                    )
                Command.particle(particle, loc(0, 0, -.125), radius, radius, radius, speed, count).force(
                    (if (showShooterParticle) 'a'[""] else 'a'[""].notHasTag(
                        shootTag
                    )).notHasTag(lagTag)
                )
                Command.particle(particle, loc(0, 0, -.1875), radius, radius, radius, speed, count).force(
                    (if (showShooterParticle) 'a'[""] else 'a'[""].notHasTag(
                        shootTag
                    )).notHasTag(lagTag)
                )

            }
        }, {
            Command.execute().ifPlaying(self.notHasTag(shootTag).notHasTag(safeTag)).run {
                safeTag.add(self)
                if (damage > 0) {
                    damageSelf(damage)
                }
                onEntityHit?.let {
                    this.it(self, 'a'["limit = 1"].hasTag(shootTag))
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
            Command.raw("particle dust_color_transition 0.361 0.361 0.361 1 0.871 0.871 0.871 ~ ~ ~ 0 0 0 0 5 normal @a")
            onWallHit?.let { it(self) }
            if (splashRange > 0) {
                splash()
            }
        })
        safeTag.remove('e'[""])
    }

    private fun getRandomOffset(spread: Double): Double {
        return round((random.nextDouble() - .5f) * (random.nextDouble() - .5f) * 1000 * spread) / 10_000f
    }

    private fun shotWithSpread(score: Score) {
        ScoreTree(score, 0..200) {
            Command.execute().If(score eq it)
                .facing(loc(getRandomOffset(spread), getRandomOffset(spread), 1.0)).run { rayCast() }
        }
        score -= 1
        If(score lt 0) {
            score.set(200)
        }
    }

}