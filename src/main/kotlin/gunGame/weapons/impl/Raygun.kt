package gunGame.weapons.impl

import abstractions.PlayerTag
import abstractions.flow.If
import abstractions.flow.trees.ScoreTree
import abstractions.hasTag
import abstractions.notHasTag
import abstractions.score.Objective
import commands.Command
import enums.Anchor
import enums.Effects
import enums.Particles
import gunGame.damageSelf
import gunGame.playingTag
import gunGame.self
import gunGame.weapons.RaycastWeapon
import gunGame.weapons.applyCoolDown
import gunGame.weapons.setCooldownForId
import gunGame.weapons.shootTag
import lib.debug.Log
import lib.rangeScore
import structure.Fluorite
import structure.McFunction
import utils.get
import utils.loc
import utils.rel
import kotlin.math.cos
import kotlin.math.sin

val guardianScore = Objective("guardianRC")
val ticksShot = Objective("ticksShot")


private const val trueCooldown = 40


class Raygun : RaycastWeapon(
    "Raygun",
    0,
    12,
    -2.0,
    bulletsPerShot = -3,
    range = 400,
    killMessage = """'["",{"selector": "@s","color": "gold"},{"text": " was zapped by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""",
    secondary = false,
) {

    companion object {
        var myId: Int = 0
        val guardianTag = PlayerTag("guardian")
    }

    val fireFunc = McFunction("$basePath/shoot")
    private val a = 0.001
    private val c = 18.8

    init {
        Raygun.myId = myId
        damageStr = "60 per ray"
        Fluorite.tickFile += {
            guardianScore['a'["scores = {$guardianScore = 1..}"].hasTag(playingTag)] -= 1
            Command.execute()
                .asat(
                    'a'["nbt =! {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(
                        playingTag
                    ).hasTag(guardianTag)
                )
                .run {
                    setCooldownForId(myId, trueCooldown)
                    guardianTag.remove(self)
                    Command.effect().clear(self, Effects.SLOWNESS)
                    guardianScore[self] = 0
                    ticksShot[self] = 0
                }
            Command.execute()
                .asat('a'["scores = {$guardianScore = 2..}", "predicate = jh1236:ready"].hasTag(playingTag))
                .anchored(Anchor.EYES).run {
                    guardianTag.add(self)
                    fireFunc()
                }
            Command.execute().asat('a'["scores = {$guardianScore = 1}", "predicate = jh1236:ready"].hasTag(playingTag))
                .run {
                    applyCoolDown(trueCooldown)
                    guardianTag.remove(self)
                    guardianScore[self] = 0
                    ticksShot[self] = 0
                    Command.effect().clear(self, Effects.SLOWNESS)
                }
        }
        this.onRaycastTick = {
            If(rangeScore.rem(6) eq 0) {
                Command.particle(Particles.BUBBLE, loc(), 0, 0, 0, 0, 1).force('a'[""].notHasTag(shootTag))
            }
        }
        this.onWallHit = {
            Command.particle(Particles.BUBBLE, loc(0, 0, -.25), 0, 0, 0, 0, 1).force('a'[""].hasTag(shootTag))
        }
        this.onEntityHit = { hit, shooter ->
            If(ticksShot[shooter] gt 78 and (ticksShot[shooter] lt 120)) {
                Command.execute().As(hit).run {
                    damageSelf(180)
                }
            }.Else {
                Command.execute().As(hit).run {
                    damageSelf(60)
                }
            }
        }
        setup()
    }


    private fun genX(t: Double, offset: Double): Double {
        val t2 = c * t
//        return a * exp(b * t2 - 6.0) * cos(t2 - 6.0 + offset)
        return a * t2 * t2 * t2 * cos(t2 - 6.0 + offset)
    }

    private fun genY(t: Double, offset: Double): Double {
        val t2 = c * t
//        return a * exp(b * t2 - 6.0) * sin(t2 - 6.0 + offset)
        return a * t2 * t2 * t2 * sin(t2 - 6.0 + offset)
    }

    override fun fire() {
        guardianScore[self] = 8
        guardianTag.add(self)
        fireFunc.append {
            ticksShot[self] += 1
            val shoot = McFunction {
                super.fire()
            }
            If(ticksShot[self] gte 200) {
                applyCoolDown(trueCooldown)
                guardianTag.remove(self)
                Command.effect().clear(self, Effects.SLOWNESS)
                guardianScore[self] = 0
                ticksShot[self] = 0
            }.Else {
                val temp = Fluorite.reuseFakeScore("temp")
                temp.set(ticksShot[self])
                If(temp gte 120) {
                    temp -= 120
                    temp *= -1
                    temp += 78
                }
                Log.info(temp)
                If(temp lt 78) {
                    val z = 15
                    ScoreTree(temp.div(2), 0..40) {
                        val t = 1.0 - (it / 40.0)
                        Command.execute().facing(loc(-genY(t, 0.0), -genX(t, 0.0), z)).run {
                            shoot()
                        }
                        Command.execute()
                            .facing(loc(-genY(t, pi * 2 / 3), -genX(t, pi * 2 / 3), z)).run {
                                shoot()
                            }
                        Command.execute()
                            .facing(loc(-genY(t, pi * -2 / 3), -genX(t, pi * -2 / 3), z))
                            .run {
                                shoot()
                            }
                    }
                }.Else {
                    shoot()
                }
            }
            If(ticksShot[self].rem(3) eq 0) {
//                Command.playsound("minecraft:entity.guardian.attack").master(self)
//                Command.playsound("minecraft:entity.guardian.attack").master(self)
//                Command.playsound("minecraft:entity.guardian.attack").master(self)
//                Command.playsound("minecraft:entity.guardian.attack").master(self)
                Command.playsound("minecraft:entity.allay.item_given").master('a'["tag=!noSound"], rel(), 1.0, 1.0)
            }
            Command.effect().give(self, Effects.SLOWNESS, 1, 3, true)
        }
        fireFunc()
    }
}
