package gunGame.weapons

import abstractions.*
import abstractions.flow.If
import abstractions.flow.Tree
import commands.Command
import enums.*
import gunGame.*
import lib.copyHeldItemToBlockAndRun
import lib.debug.Log
import lib.get
import lib.idScore
import lib.raycastEntity
import structure.Fluorite
import structure.McFunction
import utils.*
import utils.score.Objective
import utils.score.Score
import kotlin.math.round
import kotlin.random.Random

private val random = Random(19930749089)

open class ModularCoasWeapon(name: String, damage: Int) : AbstractCoasWeapon(name, damage) {

    companion object {
        private val projectileLife = Objective("life")
        private val hit = Fluorite.getNewFakeScore("hit", 0)
    }


    protected val projectile = PlayerTag("projectile$myId")
    protected var particleCount: Int = 1
    protected var sound = arrayListOf<Pair<String, Double>>()
    protected var particle: IParticle = Particles.DOLPHIN
    protected var reload = 0
    protected var cooldown = 0
    protected var clipSize = 1
    protected var bulletsPerShot = 1
    protected var range = 250
    protected var spread = 0.0
    protected var onWallHit: ((Selector) -> Unit)? = null
    protected var afterShot: (() -> Unit)? = null
    protected var onProjectileTick: ((Selector) -> Unit)? = null
    protected var onEntityHit: ((Selector, Selector) -> Unit)? = null
    protected var customModelData = -1
    protected var projectileSpeed = -1
    protected var splashRange = 0.0


    fun withSplash(splashRange: Double): ModularCoasWeapon {
        this.splashRange = splashRange
        return this
    }

    fun asSecondary(): ModularCoasWeapon {
        this.secondary = true
        return this
    }


    fun withCustomModelData(data: Int): ModularCoasWeapon {
        customModelData = data
        return this
    }

    fun onEntityHit(onHit: (Selector, Selector) -> Unit): ModularCoasWeapon {
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

    fun withProjectile(speed: Int): ModularCoasWeapon {
        projectileSpeed = speed
        return this
    }

    fun onProjectileTick(func: (Selector) -> Unit): ModularCoasWeapon {
        onProjectileTick = func
        return this
    }

    fun withRange(range: Int): ModularCoasWeapon {
        this.range = range
        return this
    }

    fun done(): ModularCoasWeapon {
        setup()
        return this
    }


    override fun shoot() {
        if (clipSize != 1) {
            applyCoolDown(decrementClip(cooldown, reload))
        } else {
            applyCoolDown(cooldown)
        }
        Log.debug("shot weapon with id $id!!")
        shootTag.add(self)
        if (spread > 0) {
            val score = Fluorite.getNewFakeScore("spray")
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
            Command.playsound(sound).master(self, rel(), 1.0, pitch)
        }
        afterShot?.let { it() }
        shootTag.remove(self)
    }

    private fun singleShot() {
        if (projectileSpeed > 0) {
            projectile()
        } else {
            rayCast()
        }
    }

    private fun projectile() {
        val new = PlayerTag("new")
        Command.summon(Entities.MARKER, loc(), "{Tags:[$projectile,$new]}")
        val newEntity = 'e'[""].hasTag(new).hasTag(projectile)
        Command.execute().As(newEntity).run {
            idScore[self].set(idScore['a'["limit = 1"].hasTag(shootTag)])
        }
        Command.execute().asat('a'[""].hasTag(shootTag)).anchored(Anchor.EYES).run.tp(newEntity, loc(), Vec2("~", "~"))
        new.remove('e'[""])
        repeat(projectileSpeed) {
            Fluorite.tickFile += ::projectileTick
        }
    }

    private fun projectileTick() {
        val tempScore = Fluorite.getNewFakeScore("tempID")
        with(Command) {
            execute().asat('e'[""].hasTag(projectile)).run {
                tempScore.set(idScore[self])
                execute().As('a'[""].hasTag(playingTag)).If(idScore[self] eq tempScore).run {
                    shootTag.add(self)
                }
                particle(particle, rel(), abs(0, 0, 0), 0.0, particleCount)
                onProjectileTick?.let { it(self) }
                execute().positioned(loc(0.0, 0.0, .3)).If(rel() isBlock blockTag("jh1236:air")).run.tp(
                    self,
                    rel()
                )
                execute().positioned(loc(0.0, 0.0, .3)).unless(rel() isBlock blockTag("jh1236:air"))
                    .run {
                        onWallHit?.let { it(self) }
                        if (splashRange > 0) {
                            splash()
                        }
                        kill()
                    }
                hit.set(0)
                execute().asIntersects('e'[""].hasTag(playingTag)).unless(idScore[self] eq tempScore).run {
                    hit.set(1)
                    if (splashRange <= 0) {
                        damageSelf(damage)
                    } else {
                        splash()
                    }
                    onEntityHit?.let { it(self, 'a'[""].hasTag(shootTag)) }
                }
                If (hit eq 1) {

                }
                kill()
                projectileLife[self] += 1
                If(projectileLife[self] gte (range / (projectileSpeed * .3)).toInt()) {
                    onWallHit?.let { it('a'[""].hasTag(shootTag)) }
                    kill()
                }
                shootTag.remove('e'[""])
            }
        }
    }

    private fun splash() {
        val offset = -(splashRange / 2)
        val dx = splashRange - 1
        Command.execute().positioned(rel(offset, offset, offset))
            .As('e'["dx = $dx", "dy = $dx", "dz = $dx"].hasTag(playingTag)).run {
                damageSelf(damage)
            }
    }

    private fun rayCast() {
        raycastEntity(.25f, { Command.particle(particle, rel(), abs(0, 0, 0), 0.0, particleCount) }, {
            Command.execute().ifPlaying(self.notHasTag(shootTag).notHasTag(safeTag)).run {
                safeTag.add(self)
                damageSelf(damage)
                onEntityHit?.let {
                    it(self, 'a'[""].hasTag(shootTag))
                }
            }
        }, range * 4, onWallHit = { onWallHit?.let { it(self) } })
        safeTag.remove('a'[""])
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
                    score -= 1
                    If(score lt 0) {
                        score.set(200)
                    }
                }
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