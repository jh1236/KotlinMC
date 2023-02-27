package gunGame.weapons.impl

import abstractions.PlayerTag
import abstractions.asat
import abstractions.flow.If
import abstractions.flow.Tree
import abstractions.hasTag
import abstractions.notHasTag
import abstractions.score.Objective
import commands.Command
import enums.Anchor
import enums.Effects
import enums.Particles
import gunGame.damageSelf
import gunGame.lagTag
import gunGame.playingTag
import gunGame.self
import gunGame.weapons.RaycastWeapon
import gunGame.weapons.applyCoolDown
import gunGame.weapons.setCooldownForId
import gunGame.weapons.shootTag
import internal.structure.format
import lib.debug.Log
import structure.Fluorite
import structure.McFunction
import utils.Vec2
import utils.get
import utils.loc
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val guardianScore = Objective("guardianRC")
private val ticksShot = Objective("ticksShot")


class Raygun : RaycastWeapon(
    "Raygun",
    120,
    4,
    0.0,
    range = 400,
    killMessage = """'["",{"selector": "@s","color": "gold"},{"text": " was zapped by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""",
    secondary = false,
) {

    companion object {
        var myId: Int = 0
        val guardianTag = PlayerTag("guardian")
    }

    val fireFunc = McFunction("$basePath/shoot")


    init {
        Raygun.myId = myId
        Fluorite.tickFile += {
            guardianScore['a'["scores = {$guardianScore = 1..}"].hasTag(playingTag)] -= 1
            Command.execute()
                .asat(
                    'a'["nbt =! {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(
                        playingTag
                    ).hasTag(guardianTag)
                )
                .run {
                    setCooldownForId(myId, 15)
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
                    applyCoolDown(15)
                    guardianTag.remove(self)
                    guardianScore[self] = 0
                    ticksShot[self] = 0
                    Command.effect().clear(self, Effects.SLOWNESS)
                }
        }
        this.onRaycastTick = {
            Command.particle(Particles.BUBBLE, loc(), 0, 0, 0, 0, 1).force('a'[""].notHasTag(shootTag))
            Command.particle(Particles.BUBBLE, loc(0, 0, -0.0625), 0, 0, 0, 0, 1)
                .force('a'[""].notHasTag(shootTag).notHasTag(lagTag))
            Command.particle(Particles.BUBBLE, loc(0, 0, -0.125), 0, 0, 0, 0, 1)
                .force('a'[""].notHasTag(shootTag).notHasTag(lagTag))
            Command.particle(Particles.BUBBLE, loc(0, 0, -0.1875), 0, 0, 0, 0, 1)
                .force('a'[""].notHasTag(shootTag).notHasTag(lagTag))
        }
        this.onWallHit = {
            Command.particle(Particles.BUBBLE, loc(0, 0, -.25), 0, 0, 0, 0, 1).force('a'[""].hasTag(shootTag))
        }
        this.onEntityHit = { shooter, hit ->
            If(ticksShot[shooter] gt 200) {
                Command.execute().asat(hit).run {
                    damageSelf(3000)
                }
            }
        }
        setup()
    }

    override fun fire() {
        guardianScore[self] = 8
        guardianTag.add(self)
        fireFunc.append {
            ticksShot[self] += 1
            Log.info("bang")
            val shoot = McFunction {
                super.fire()
            }
            If(ticksShot[self] gte 200) {
                applyCoolDown(15)
                guardianTag.remove(self)
                Command.effect().clear(self, Effects.SLOWNESS)
                guardianScore[self] = 0
                ticksShot[self] = 0
            }.ElseIf(ticksShot[self] gte 120) {
                Tree(ticksShot[self].take(120), 0..80) {
                    val t = (it) / 2.0
                    val x = (.1 * t) * (.1 * t) * sin((.09 * t) * (.09 * t))
                    val y = (.1 * t) * (.1 * t) * cos((.09 * t) * (.09 * t))
                    val x2 = (.1 * t) * (.1 * t) * sin((.09 * t) * (.09 * t) + 2 * PI / 3)
                    val y2 = (.1 * t) * (.1 * t) * cos((.09 * t) * (.09 * t) + 2 * PI / 3)
                    val x3 = (.1 * t) * (.1 * t) * sin((.09 * t) * (.09 * t) + 4 * PI / 3)
                    val y3 = (.1 * t) * (.1 * t) * cos((.09 * t) * (.09 * t) + 4 * PI / 3)
                    Command.execute().rotated(Vec2("~${x.format()}", "~${y.format()}")).run {
                        shoot()
                    }
                    Command.execute().rotated(Vec2("~${x2.format()}", "~${y2.format()}")).run {
                        shoot()
                    }
                    Command.execute().rotated(Vec2("~${x3.format()}", "~${y3.format()}")).run {
                        shoot()
                    }
                }
            }.ElseIf(ticksShot[self] lt 78) {
                Tree(ticksShot[self], 0..80) {
                    val t = (80 - it) / 2.0
                    val x = (.1 * t) * (.1 * t) * sin((.09 * t) * (.09 * t))
                    val y = (.1 * t) * (.1 * t) * cos((.09 * t) * (.09 * t))
                    val x2 = (.1 * t) * (.1 * t) * sin((.09 * t) * (.09 * t) + 2 * PI / 3)
                    val y2 = (.1 * t) * (.1 * t) * cos((.09 * t) * (.09 * t) + 2 * PI / 3)
                    val x3 = (.1 * t) * (.1 * t) * sin((.09 * t) * (.09 * t) + 4 * PI / 3)
                    val y3 = (.1 * t) * (.1 * t) * cos((.09 * t) * (.09 * t) + 4 * PI / 3)
                    Command.execute().rotated(Vec2("~${x.format()}", "~${y.format()}")).run {
                        shoot()
                    }
                    Command.execute().rotated(Vec2("~${x2.format()}", "~${y2.format()}")).run {
                        shoot()
                    }
                    Command.execute().rotated(Vec2("~${x3.format()}", "~${y3.format()}")).run {
                        shoot()
                    }
                }
            }.Else {
                //TODO: these three raycasts should be one
                shoot()
                shoot()
                shoot()
            }
            If(ticksShot[self].rem(2) eq 0) {
                Command.raw("playsound minecraft:block.amethyst_cluster.step master @s")
            }
            Command.effect().give(self, Effects.SLOWNESS, 1, 3, true)
        }
        fireFunc()
    }
}
