package gunGame.weapons.impl

import abstractions.PlayerTag
import abstractions.flow.If
import abstractions.hasTag
import abstractions.notHasTag
import abstractions.score.Objective
import commands.Command
import enums.Anchor
import enums.Particles
import gunGame.lagTag
import gunGame.playingTag
import gunGame.self
import gunGame.weapons.*
import lib.debug.Log
import structure.Fluorite
import structure.McFunction
import utils.get
import utils.loc
import utils.rel
import kotlin.math.roundToInt

val smgScore = Objective("smgRC")


class SMG : RaycastWeapon(
    "SMG",
    300,
    4,
    -0.5,
    40,
    1.5,
    sound = listOf("block.note_block.hat" to 2.0),
    range = 400,
    spread = 3.0,
    killMessage = """'["",{"selector": "@s","color": "gold"},{"text": " was peppered down by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""",
    secondary = false,
) {

    companion object {
        var myId: Int = 0
        val minigunTag = PlayerTag("minigun")
    }

    val fireFunc = McFunction("$basePath/shoot")


    init {
        showShooterParticle = false
        SMG.myId = myId
        Fluorite.tickFile += {
            smgScore['a'["scores = {$smgScore = 1..}"].hasTag(playingTag)] -= 1
            Command.execute()
                .asat(
                    'a'["nbt =! {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(
                        playingTag
                    ).hasTag(minigunTag)
                )
                .run {
                    setCooldownForId(myId, 10)
                    minigunTag.remove(self)
                    smgScore[self] = 0
                }
            Command.execute().asat('a'["scores = {$smgScore = 2..}", "predicate = jh1236:ready"].hasTag(playingTag))
                .anchored(Anchor.EYES).run {
                    minigunTag.add(self)
                    decrementClip(0,0)
                    fireFunc()
                }
            Command.execute().asat('a'["scores = {$smgScore = 1}", "predicate = jh1236:ready"].hasTag(playingTag))
                .run {
                    applyCoolDown(10)
                    minigunTag.remove(self)
                    Log.info(self.data["SelectedItem.tag.jh1236.ammo.value"])
                    smgScore[self] = 0
                }
        }
        this.onRaycastTick = {
            Command.particle(Particles.CRIT, loc(), 0, 0, 0, 0, 1).force('a'[""].notHasTag(shootTag))
            Command.particle(Particles.CRIT, loc(0, 0, -0.0625), 0, 0, 0, 0, 1).force(
                'a'[""].notHasTag(shootTag).notHasTag(
                    lagTag
                )
            )
            Command.particle(Particles.CRIT, loc(0, 0, -0.125), 0, 0, 0, 0, 1).force(
                'a'[""].notHasTag(shootTag).notHasTag(
                    lagTag
                )
            )
            Command.particle(Particles.CRIT, loc(0, 0, -0.1875), 0, 0, 0, 0, 1).force(
                'a'[""].notHasTag(shootTag).notHasTag(
                    lagTag
                )
            )
        }
        this.onWallHit = {
            Command.particle(Particles.CRIT, loc(0, 0, -0.25), 0, 0, 0, 0, 1).force('a'[""].hasTag(shootTag))
        }
        setup()
    }

    override fun fire() {
        smgScore[self] = 8
        minigunTag.add(self)
        fireFunc.append {
            super.fire()
            val ammoScore = Fluorite.reuseFakeScore("ammo")
            ammoScore.set(self.data["SelectedItem.tag.jh1236.ammo.value"])
            Log.info(ammoScore)
            If(ammoScore eq clipsize) {
                applyCoolDown((reload * 20).toInt())
                Command.playsound("minecraft:item.spyglass.use").master(self, rel(), 1, 0.7)
                Command.playsound("minecraft:item.spyglass.use").master(self, rel(), 1, 0.7)
                Command.playsound("minecraft:item.spyglass.use").master(self, rel(), 1, 0.7)
                Command.playsound("minecraft:item.spyglass.use").master(self, rel(), 1, 0.7)
                Command.playsound("minecraft:item.spyglass.use").master(self, rel(), 1, 0.7)
                Command.playsound("minecraft:item.spyglass.use").master(self, rel(), 1, 0.7)
                minigunTag.remove(self)
                smgScore[self] = 0
            }.Else {
                applyCoolDown(2)
            }
        }
        fireFunc()
    }
}
