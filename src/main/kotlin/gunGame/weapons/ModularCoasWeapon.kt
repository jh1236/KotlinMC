package gunGame.weapons

import abstractions.*
import abstractions.flow.If
import abstractions.flow.Tree
import commands.Command
import enums.*
import gunGame.*
import lib.*
import lib.debug.Log
import structure.Fluorite
import structure.McFunction
import utils.*
import utils.score.Score
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.random.Random

private val random = Random(19930749089)

open class ModularCoasWeapon(name: String, damage: Int) : AbstractCoasWeapon(name, damage) {

    companion object {
        val universalProjectile = PlayerTag("projectile")
        private val hit = Fluorite.getNewFakeScore("hit", 0)
    }


    protected var ableBeShot = false
    var activationDelay: Int = -1
    protected var maxAllowed = -1
    protected var piercing = false
    val projectile = PlayerTag("projectile$myId")
    protected var particleCount: Int = 1
    protected var canHitOwner: Boolean = false
    protected var sound = arrayListOf<Pair<String, Double>>()
    protected var particle: IParticle? = null
    protected var reload = 0
    protected var cooldown = 0
    protected var clipSize = 1
    protected var bulletsPerShot = 1
    var range = -1
    protected var spread = 0.0
    protected var onWallHit: ((Selector) -> Unit)? = null
    protected var afterShot: (() -> Unit)? = null
    protected var onShootFunc: (ModularCoasWeapon.() -> Unit)? = null
    protected var onReloadFunc: (ModularCoasWeapon.() -> Unit)? = null
    protected var onProjectileTick: (ModularCoasWeapon.(Selector) -> Unit)? = null
    var onEntityHit: (ModularCoasWeapon.(Selector, Selector) -> Unit)? = null
    protected var customModelData = -1
    protected var projectileSpeed = -1
    protected var splashRange = 0.0
    protected var killMessage =
        """'["",{"selector":"@s", "color":"gold"}, " was killed by ", {"selector":"@a[tag = $shootTag]"}]'"""

    fun withKillMessage(killMessage: String): ModularCoasWeapon {
        this.killMessage = killMessage
        return this
    }

    fun withSplash(splashRange: Double): ModularCoasWeapon {
        this.splashRange = splashRange
        return this
    }

    fun canBeShot(): ModularCoasWeapon {
        this.ableBeShot = true
        return this
    }

    fun asSecondary(): ModularCoasWeapon {
        this.secondary = true
        return this
    }


    fun onShoot(function: ModularCoasWeapon.() -> Unit): ModularCoasWeapon {
        this.onShootFunc = function
        return this
    }

    fun onReload(function: ModularCoasWeapon.() -> Unit): ModularCoasWeapon {
        this.onReloadFunc = function
        return this
    }

    fun asReward(): ModularCoasWeapon {
        this.isReward = true
        return this
    }

    fun canHitOwner(): ModularCoasWeapon {
        this.canHitOwner = true
        return this
    }

    fun withPiercing(): ModularCoasWeapon {
        this.piercing = true
        return this
    }

    fun withActivationDelay(delay: Double): ModularCoasWeapon {
        this.activationDelay = (delay * 20).roundToInt()
        return this
    }


    fun withCustomModelData(data: Int): ModularCoasWeapon {
        customModelData = data
        return this
    }

    fun onEntityHit(onHit: ModularCoasWeapon.(Selector, Selector) -> Unit): ModularCoasWeapon {
        this.onEntityHit = onHit
        return this
    }

    fun onWallHit(onHit: (Selector) -> Unit): ModularCoasWeapon {
        this.onWallHit = onHit
        return this
    }

    fun afterShot(func: () -> Unit): ModularCoasWeapon {
        this.afterShot = func
        return this
    }

    fun withBulletsPerShot(bps: Int): ModularCoasWeapon {
        bulletsPerShot = bps
        return this
    }

    fun withParticle(particle: IParticle, count: Int = 1): ModularCoasWeapon {
        this.particle = particle
        this.particleCount = count
        return this
    }

    fun withClipSize(clipSize: Int): ModularCoasWeapon {
        this.clipSize = clipSize
        return this
    }

    fun withSpread(spread: Double): ModularCoasWeapon {
        this.spread = spread
        return this
    }


    fun addSound(sound: String, pitch: Double = 1.0): ModularCoasWeapon {
        this.sound.add(sound to pitch)
        return this
    }

    // time between each shot
    fun withCooldown(cooldown: Double): ModularCoasWeapon {
        this.cooldown = (cooldown * 20).toInt()
        return this
    }

    //time used when end of clip
    fun withReload(reloadTime: Double): ModularCoasWeapon {
        this.reload = (reloadTime * 20).toInt()
        return this
    }

    fun withProjectile(speed: Int, maxAllowed: Int = -1): ModularCoasWeapon {
        projectileSpeed = speed
        this.maxAllowed = maxAllowed
        return this
    }

    fun onProjectileTick(func: ModularCoasWeapon.(Selector) -> Unit): ModularCoasWeapon {
        onProjectileTick = func
        return this
    }

    fun withRange(range: Int): ModularCoasWeapon {
        this.range = (range * 4)
        return this
    }

    fun done(): ModularCoasWeapon {
        setup()
        return this
    }


    override fun shoot() {
        onShootFunc?.let { this.it() }
        if (maxAllowed > 0) {
            If('e'[""].hasTag(projectile).count() gte maxAllowed) {
                val lowestHealth = Fluorite.reuseFakeScore("temp", range)
                Command.execute().asat('e'[""].hasTag(projectile)).run {
                    lowestHealth.minOf(health[self])
                }
                Command.execute().asat('e'[""].hasTag(projectile)).If(health[self] eq lowestHealth).run.kill()
            }
        }
        if (clipSize != 1) {
            applyCoolDown(decrementClip(cooldown, reload))
            onReloadFunc?.let {
                val ammo = Fluorite.reuseFakeScore("ammo")
                ammo.set(self.data["SelectedItem.tag.jh1236.ammo.value"])
                val max = Fluorite.reuseFakeScore("max")
                max.set(self.data["SelectedItem.tag.jh1236.ammo.max"])
                If(ammo eq max) {
                    this.it()
                }
            }
        } else {
            applyCoolDown(cooldown)
        }
        Log.debug("shot weapon with id $id!!")
        shootTag.add(self)
        if (spread > 0) {
            val score = Fluorite.reuseFakeScore("spray")
            score.set(self.data["SelectedItem.tag.jh1236.shotCount"])
            if (bulletsPerShot > 1) {
                val shootFunction = McFunction { shotWithSpread(score) }
                repeat(bulletsPerShot) {
                    shootFunction()
                }
            } else {
                shotWithSpread(score)
            }
            copyHeldItemToBlockAndRun {
                it["tag.jh1236.shotCount"] = score
            }
        } else {
            singleShot()
        }
        for ((sound, pitch) in sound) {
            Command.playsound(sound).master(self["tag =! noSound"], rel(), 1.0, pitch)
        }
        afterShot?.let { it() }
        shootTag.remove(self)
    }

    protected open fun singleShot() {
        if (projectileSpeed >= 0) {
            projectile()
        } else {
            rayCast()
        }
    }

    private fun projectile() {
        val new = PlayerTag("new")
        if (!ableBeShot) {
            Command.summon(
                Entities.MARKER,
                rel(),
                "{Tags:[$projectile,$new, $universalProjectile]}"
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
                newEntity,
                loc(),
                Vec2("~", "~")
            )
        }
        new.remove('e'[""])
        val file = McFunction("$basePath/projectile")
        if (projectileSpeed > 1) {
            val file2 = McFunction("$basePath/projectile/tick") { projectileTick() }
            repeat(projectileSpeed) {
                file += { Command.execute().at(self).run(file2) }
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
            particle?.let { particle(it, rel(), abs(0, 0, 0), 0.0, particleCount) }
            onProjectileTick?.let { this@ModularCoasWeapon.it(self) }
            if (projectileSpeed > 0) {
                execute().positioned(loc(0.0, 0.0, .25)).If(rel() isBlock blockTag("jh1236:air")).run.tp(
                    self,
                    rel()
                )
                execute().positioned(loc(0.0, 0.0, .25)).unless(rel() isBlock blockTag("jh1236:air"))
                    .run {
                        if (splashRange > 0) {
                            Command.execute().positioned(rel(0, 1, 0)).run { splash() }
                        }
                        onWallHit?.let { it(self) }
                        health[self].reset()
                        kill()
                    }
            }
            hit.set(0)
            val ex = if (activationDelay > 0) {
                execute().unless(health[self] gt range - activationDelay)
            } else {
                execute()
            }
            ex.asIntersects('e'[""].hasTag(playingTag))

            if (!canHitOwner) {
                ex.unless(idScore[self] eq tempScore)
            }

            ex.run {
                data().merge.storage("jh1236:message", "{death: $killMessage}")
                hit.set(1)
                damageSelf(damage)
                if (splashRange > 0) {
                    splash()
                }
                onEntityHit?.let { this@ModularCoasWeapon.it(self, 'a'[""].hasTag(shootTag)) }
            }
            if (!piercing) {
                If(hit eq 1) {
                    kill()
                }
            }
            health[self] -= 1
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
            Command.execute()
                .asat('e'["distance = ${previous}..${i.toDouble() / 4}"].hasTag(playingTag))
                .run {
                    damageSelf((d * (4 * splashRange - i)).roundToInt())
                }
            previous = (i.toDouble() / 4)
        }
    }

    private fun rayCast() {
        Command.data().merge.storage("jh1236:message", "{death: $killMessage}")
        raycastEntity(.25f, {
            particle?.let { particle ->
                Command.particle(particle, rel(), abs(0, 0, 0), 0.0, particleCount).force(
                    if (cooldown > 1) 'a'[""] else 'a'[""].notHasTag(
                        shootTag
                    )
                )
            }
            //TODO: stop being lazy
            Command.execute().unless(loc(0, 0, 0.25) isBlock blockTag("jh1236:air"))
                .run { Command.raw("particle dust_color_transition 0.361 0.361 0.361 1 0.871 0.871 0.871 ~ ~ ~ 0 0 0 0 5 normal @a") }
        }, {
            Command.execute().ifPlaying(self.notHasTag(shootTag).notHasTag(safeTag)).run {
                safeTag.add(self)
                damageSelf(damage)
                onEntityHit?.let {
                    this.it(self, 'a'[""].hasTag(shootTag))
                }
                if (!piercing) {
                    onWallHit?.let { it(self) }
                    rangeScore.set(0)
                }
            }
        }, range, onWallHit = { onWallHit?.let { it(self) } })
        safeTag.remove('e'[""])
    }

    private fun getRandomOffset(spread: Double): Double {
        return round((random.nextDouble() - .5f) * (random.nextDouble() - .5f) * 1000 * spread) / 100f
    }

    private fun shotWithSpread(score: Score) {
        Tree(score, 0..200) {
            Command.execute().If(score eq it)
                .rotated(Vec2("~${getRandomOffset(spread)}", "~${getRandomOffset(spread)}"))
                .run {
                    singleShot()
                }
        }
        score -= 1
        If(score lt 0) {
            score.set(200)
        }
    }


    override fun give(player: Selector) {
        val sb = StringBuilder("{jh1236:{ready:1b, weapon:$myId, cooldown: {value: 0, max : 0}, ")
        if (spread > 0) {
            sb.append("shotCount:0, ")
        }
        if (clipSize > 1) {
            sb.append("ammo: {value: $clipSize, max:$clipSize},")
        }
        sb.append("}, ")
        if (customModelData > 0) {
            sb.append("CustomModelData:${customModelData}, ")
        }
        sb.append("display:{ Name: '{\"text\":\"$name\",\"italic\" : false}'}, ")
        sb.append("}")
        Command.give(
            player,
            Items.CARROT_ON_A_STICK.nbt(sb.toString())
        )
    }

}