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

val minigunScore = Objective("minigunRC")


class Minigun : RaycastWeapon(
    "Minigun",
    240,
    4,
    0.0,
    50,
    5.0,
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
        Minigun.myId = myId
        Fluorite.tickFile += {
            minigunScore['a'["scores = {$minigunScore = 1..}"].hasTag(playingTag)] -= 1
            Command.execute()
                .asat(
                    'a'["nbt =! {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(
                        playingTag
                    ).hasTag(minigunTag)
                )
                .run {
                    resetAmmoForId(myId)
                    setCooldownForId(myId, 10)
                    minigunTag.remove(self)
                    minigunScore[self] = 0
                }
            Command.execute().asat('a'["scores = {$minigunScore = 2..}", "predicate = jh1236:ready"].hasTag(playingTag))
                .anchored(Anchor.EYES).run {
                    minigunTag.add(self)
                    fireFunc()
                }
            Command.execute().asat('a'["scores = {$minigunScore = 1}", "predicate = jh1236:ready"].hasTag(playingTag))
                .run {
                    resetAmmoForId(myId)
                    applyCoolDown(10)
                    minigunTag.remove(self)
                    Log.info(self.data["SelectedItem.tag.jh1236.ammo.value"])
                    minigunScore[self] = 0
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
        minigunScore[self] = 8
        minigunTag.add(self)
        fireFunc.append {
            super.fire()
            If(decrementClip(0, 1) eq 1) {
                applyCoolDown((reload * 20).toInt())
                Command.playsound("block.conduit.deactivate").master(self, rel(), 1, 0.0)
                minigunTag.remove(self)
                minigunScore[self] = 0
            }
        }
        fireFunc()
    }
}
