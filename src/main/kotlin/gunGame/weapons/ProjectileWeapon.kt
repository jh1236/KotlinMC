package gunGame.weapons

import abstractions.PlayerTag
import abstractions.Storage
import abstractions.flow.If
import abstractions.flow.Tree
import abstractions.hasTag
import abstractions.notHasTag
import abstractions.score.Score
import commands.Command
import enums.Anchor
import enums.Entities
import enums.IParticle
import enums.Particles
import gunGame.*
import internal.commands.impl.execute.OnTarget
import lib.Quad
import lib.debug.Log
import lib.doesCollide
import lib.rangeScore
import lib.raycastEntity
import structure.Fluorite
import structure.McFunction
import utils.*
import kotlin.math.round
import kotlin.math.roundToInt


open class ProjectileWeapon(
    name: String,
    damage: Int,
    customModelData: Int,
    cooldown: Double,
    clipsize: Int = 1,
    reload: Double = 0.0,
    protected val projectileEntity: Entities = Entities.MARKER,
    protected var ableBeShot: Boolean = false,
    var activationDelay: Int = -1,
    protected var maxAllowed: Int = -1,
    protected var projectilesPerShot: Int = 1,
    protected var spread: Double = 0.0,
    protected var canHitOwner: Boolean = false,
    protected var sound: ArrayList<Pair<String, Double>> = arrayListOf(),
    private var particleArray: List<Quad<IParticle, Int, Number, Number>> = listOf(),
    var range: Int = -1,
    var onWallHit: ((Selector) -> Unit)? = null,
    protected var onProjectileTick: (ProjectileWeapon.(Selector) -> Unit)? = null,
    var onEntityHit: (ProjectileWeapon.(Selector, Selector) -> Unit)? = null,
    protected var projectileSpeed: Int = -1,
    protected var splashRange: Double = 0.0,
    protected var killMessage: String = """'["",{"selector":"@s", "color":"gold"}, " was killed by ", {"selector":"@a[tag = $shootTag]"}]'""",
    protected var piercing: Boolean = false,
    secondary: Boolean = false,
    isReward: Boolean = false
) : AbstractCoasWeapon(name, damage, customModelData, cooldown, clipsize, reload, secondary, isReward) {

    companion object {
        private val random = java.util.Random(199283047)
        val universalProjectile = PlayerTag("projectile")
        private val hit = Fluorite.getNewFakeScore("hit", 0)
        private const val splashHitsOwner = true
        private val hitTag: PlayerTag = PlayerTag("hit")
    }

    val projectile = PlayerTag("projectile$myId")


    override fun fire() {
        if (maxAllowed > 0) {
            val processingTag = PlayerTag("temp")
            val tempScore = Fluorite.reuseFakeScore("id")
            tempScore.set(idScore[self])
            Command.execute().asat('e'[""].hasTag(projectile)).If(idScore[self] eq tempScore).run {
                processingTag.add(self)
            }
            If('e'[""].hasTag(processingTag).count() gte maxAllowed) {
                val lowestHealth = Fluorite.reuseFakeScore("temp", range)
                Command.execute().asat('e'[""].hasTag(processingTag)).run {
                    lowestHealth.minOf(health[self])
                }
                Command.execute().asat('e'[""].hasTag(processingTag)).If(health[self] eq lowestHealth).run.kill()
            }
            processingTag.remove('e'[""])
        }
        shootTag.add(self)
        if (spread > 0) {
            val score = sprayObjective[self]
            if (projectilesPerShot > 1) {
                val shootFunction = McFunction { shotWithSpread(score) }
                repeat(projectilesPerShot) {
                    shootFunction()
                }
            } else {
                shotWithSpread(score)
            }
        } else {
            projectile()
        }
        val file = McFunction("$basePath/projectile") {
            hit.set(0)
        }
        if (projectileSpeed > 1) {
            val file2 = McFunction("$basePath/projectile/tick") { projectileTick() }
            repeat(projectileSpeed) {
                file += { Command.execute().at(self).If(hit eq 0).run(file2) }
            }
        } else {
            file += { projectileTick() }
        }
        Fluorite.tickFile += { Command.execute().asat('e'[""].hasTag(projectile)).run(file) }
        for ((sound, pitch) in sound) {
            Command.playsound(sound).master(self["tag =! noSound"], rel(), 1.0, pitch)
        }
        shootTag.remove(self)
    }

    private fun getRandomOffset(spread: Double): Double {
        return round((random.nextDouble() - .5f) * (random.nextDouble() - .5f) * 1000 * spread) / 100f
    }

    private fun shotWithSpread(score: Score) {
        Tree(score, 0..200) {
            Command.execute().If(score eq it)
                .rotated(Vec2("~${getRandomOffset(spread)}", "~${getRandomOffset(spread)}")).run { projectile() }
        }
        score -= 1
        If(score lt 0) {
            score.set(200)
        }
    }

    private fun projectile() {

        val entity = if (ableBeShot) Entities.ARMOR_STAND else projectileEntity
        val tag = PlayerTag("newProjectile")

        Command.execute().summon(entity).run {
            if (!ableBeShot) {
                self.data["{}"] = "{NoGravity:1b}"
            } else {
                self.data["{}"] = "{Small:1b, Invulnerable:1b, NoGravity:1b, Invisible:1b}"
            }
            universalProjectile.add(self)
            projectile.add(self)
            if (ableBeShot) {
                playingTag.add(self)
            }
            idScore[self].set(idScore['a'["limit = 1"].hasTag(shootTag)])
            if (range > 0) {
                health[self] = range
            } else {
                health[self] = rangeScore
            }
            if (projectileSpeed > 0) {
                tag.add(self)
            }
        }
        if (projectileSpeed > 0) {
            Command.execute().anchored(Anchor.EYES).run.tp(
                'e'[""].hasTag(tag), loc(), Vec2("~", "~")
            )
            tag.remove('e'[""])
        }
    }

    private fun projectileTick() {
        val tempScore = Fluorite.reuseFakeScore("tempID")

        with(Command) {

            tempScore.set(idScore[self])
            execute().As('a'[""].hasTag(playingTag)).If(idScore[self] eq tempScore).run {
                shootTag.add(self)
            }
            particleArray.forEach { (particle, count, radius, speed) ->
                particle(particle, rel(), radius, radius, radius, speed, count).force(
                    if (cooldown > .05) 'a'[""] else 'a'[""].notHasTag(
                        shootTag
                    )
                )

            }
            onProjectileTick?.let { this@ProjectileWeapon.it(self) }
            hit.set(0)
            if (projectileSpeed > 0) {
                lateinit var check: Score
                execute().positioned(loc(0.0, 0.0, .25)).run { check = doesCollide() }
                If(check eq 0) {
                    tp(
                        self, loc(0, 0, .25)
                    )
                }.Else {
                    hit.set(1)
                    onWallHit?.let { it(self) }
                    health[self].reset()
                    kill()
                }
            }
            val ex = if (activationDelay > 0) {
                execute().unless(health[self] gt range - activationDelay)
            } else {
                execute()
            }
            ex.unless(hit eq 1).asIntersects('e'[""].hasTag(playingTag))
            ex.asIntersects('e'[""].notHasTag(projectile).hasTag(playingTag))

            if (!canHitOwner) {
                ex.unless(idScore[self] eq tempScore)
            }

            ex.run {
                Storage("jh1236:message")["{}"] = "{death: $killMessage}"
                hit.set(1)
                if (splashRange > 0.0) {
                    hitTag.add(self)
                }
                damageSelf(damage)
                onEntityHit?.let { this@ProjectileWeapon.it(self, 'a'["limit = 1"].hasTag(shootTag)) }
            }
            health[self] -= 1
            If(hit eq 1) {
                health[self] = 0
            }
            If(health[self] lte 0) {
                onWallHit?.let { it('a'[""].hasTag(shootTag)) }
                if (splashRange > 0) {
                    splash()
                }
                execute().on(OnTarget.PASSENGERS).run.ride(self).dismount
                tp(self, rel(0, -1024, 0))
                kill()
            }
            shootTag.remove('e'[""])
        }

    }

    private fun splash() {
        playingTag.remove(self["type = marker"])
        val temp = PlayerTag("tempProjectile")
        deathStorage["death"] = killMessage
        var previous = 0.0
        val collide = Fluorite.getNewFakeScore("blocks", 0)
        val d = (damage / (4 * splashRange)).roundToInt()
        for (i in 1 until (4 * splashRange).roundToInt()) {
            var selector = 'e'["distance = ${previous}..${i.toDouble() / 4}"].hasTag(playingTag).notHasTag(hitTag)
            if (!canHitOwner && !splashHitsOwner) {
                selector = selector.notHasTag(shootTag)
            }
            Command.execute().As(selector).facing(self, Anchor.EYES).run {
                temp.add(self)
                hit.set(0)
                collide.set(0)
                Command.particle(Particles.TOTEM_OF_UNDYING, loc(0, 0, 2), 0, 0, 0, 0, 0)
                raycastEntity(.25f, { Command.particle(Particles.END_ROD) }, {
                    If(self.hasTag(temp)) {
                        hit.set(1)
                        rangeScore.set(0)
                    }
                }, (splashRange * 4).roundToInt(),
                    {
                        If(rangeScore eq 0) {
                            collide += 1
                            If(collide lte 4) {
                                rangeScore.set((splashRange * 4).roundToInt())
                            }
                        }


                    })
                If(hit eq 1) {
                    hitTag.add(self)
                    damageSelf((d * (4 * splashRange - i)).roundToInt())
                }
                Log.trace("player: ", self, ", hit: ", hit, ", collide: ", collide)
            }
            previous = (i.toDouble() / 4)
        }
        hitTag.remove('e'[""])
    }

}