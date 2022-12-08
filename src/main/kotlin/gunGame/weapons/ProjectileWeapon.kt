package gunGame.weapons

import abstractions.*
import abstractions.flow.If
import commands.Command
import enums.Anchor
import enums.Blocks
import enums.Entities
import enums.IParticle
import gunGame.*
import lib.get
import lib.idScore
import lib.rangeScore
import structure.Fluorite
import structure.McFunction
import utils.Selector
import utils.Vec2
import utils.loc
import utils.rel
import kotlin.math.roundToInt


open class ProjectileWeapon(
    name: String,
    damage: Int,
    customModelData: Int,
    cooldown: Double,
    clipsize: Int = 1,
    reload: Double = 0.0,
    protected var ableBeShot: Boolean = false,
    var activationDelay: Int = -1,
    protected var maxAllowed: Int = -1,
    protected var particleCount: Int = 1,
    protected var canHitOwner: Boolean = false,
    protected var sound: ArrayList<Pair<String, Double>> = arrayListOf(),
    protected var particle: IParticle? = null,
    var range: Int = -1,
    var onWallHit: ((Selector) -> Unit)? = null,
    protected var onProjectileTick: (ProjectileWeapon.(Selector) -> Unit)? = null,
    var onEntityHit: (ProjectileWeapon.(Selector, Selector) -> Unit)? = null,
    protected var projectileSpeed: Int = -1,
    protected var splashRange: Double = 0.0,
    protected var killMessage: String = """'["",{"selector":"@s", "color":"gold"}, " was killed by ", {"selector":"@a[tag = $shootTag]"}]'""",
    secondary: Boolean = false,
    isReward: Boolean = false
) : AbstractCoasWeapon(name, damage, customModelData, cooldown, clipsize, reload, secondary, isReward) {

    companion object {
        val universalProjectile = PlayerTag("projectile")
        private val hit = Fluorite.getNewFakeScore("hit", 0)
    }

    val projectile = PlayerTag("projectile$myId")


    override fun fire() {
        if (maxAllowed > 0) {
            If('e'[""].hasTag(projectile).count() gte maxAllowed) {
                val lowestHealth = Fluorite.reuseFakeScore("temp", range)
                Command.execute().asat('e'[""].hasTag(projectile)).run {
                    lowestHealth.minOf(health[self])
                }
                Command.execute().asat('e'[""].hasTag(projectile)).If(health[self] eq lowestHealth).run.kill()
            }
        }
        shootTag.add(self)
        projectile()
        for ((sound, pitch) in sound) {
            Command.playsound(sound).master(self["tag =! noSound"], rel(), 1.0, pitch)
        }
        shootTag.remove(self)
    }


    private fun projectile() {
        val new = PlayerTag("new")
        if (!ableBeShot) {
            Command.summon(
                Entities.MARKER, rel(), "{Tags:[$projectile,$new, $universalProjectile]}"
            )
        } else {
            Command.summon(
                Entities.ARMOR_STAND,
                rel(),
                "{Tags:[$projectile,$new, $universalProjectile, $playingTag], Small:1b, Invulnerable:1b, NoGravity:1b, Invisible:1b}"
            )
        }

        val newEntity = 'e'[""].hasTag(new).hasTag(projectile)
        Command.execute().As(newEntity).run {
            idScore[self].set(idScore['a'["limit = 1"].hasTag(shootTag)])
            if (range > 0) {
                health[self] = range
            } else {
                health[self] = rangeScore
            }
        }
        if (projectileSpeed > 0) {
            Command.execute().asat('a'[""].hasTag(shootTag)).anchored(Anchor.EYES).run.tp(
                newEntity, loc(), Vec2("~", "~")
            )
        }
        new.remove('e'[""])
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
    }

    private fun projectileTick() {
        val tempScore = Fluorite.reuseFakeScore("tempID")

        with(Command) {

            tempScore.set(idScore[self])
            execute().As('e'[""].hasTag(playingTag)).If(idScore[self] eq tempScore).run {
                shootTag.add(self)
            }
            particle?.let { particle(it, rel(), 0, 0, 0, 0.0, particleCount) }
            onProjectileTick?.let { this@ProjectileWeapon.it(self) }
            hit.set(0)
            if (projectileSpeed > 0) {
                execute().positioned(loc(0.0, 0.0, .25)).If(rel() isBlock Blocks.tag("jh1236:air")).run.tp(
                    self, rel()
                )
                execute().positioned(loc(0.0, 0.0, .25)).unless(rel() isBlock Blocks.tag("jh1236:air")).run {
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

            if (!canHitOwner) {
                ex.unless(idScore[self] eq tempScore)
            }

            ex.run {
                data().merge.storage("jh1236:message", "{death: $killMessage}")
                hit.set(1)
                damageSelf(damage)
                onEntityHit?.let { this@ProjectileWeapon.it(self, 'a'[""].hasTag(shootTag)) }
            }
            health[self] -= 1
            If(hit eq 1) {
                health[self] = 0
            }
            If(health[self] eq 0) {
                onWallHit?.let { it('a'[""].hasTag(shootTag)) }
                if (splashRange > 0) {
                    splash()
                }
                kill()
            }
            shootTag.remove('e'[""])
        }

    }

    private fun splash() {
        playingTag.remove(self["type = marker"])
        Command.data().merge.storage("jh1236:message", "{death: $killMessage}")
        var previous = 0.0
        val d = (damage / (4 * splashRange)).roundToInt()
        for (i in 1 until (4 * splashRange).roundToInt()) {
            if (canHitOwner) {
                Command.execute().asat('e'["distance = ${previous}..${i.toDouble() / 4}"].hasTag(playingTag)).run {
                    damageSelf((d * (4 * splashRange - i)).roundToInt())
                }
            } else {
                Command.execute()
                    .asat('e'["distance = ${previous}..${i.toDouble() / 4}"].hasTag(playingTag).notHasTag(shootTag))
                    .run {
                        damageSelf((d * (4 * splashRange - i)).roundToInt())
                    }
            }
            previous = (i.toDouble() / 4)
        }
    }

}