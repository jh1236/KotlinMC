package gunGame.weapons.impl

import abstractions.PlayerTag
import abstractions.asat
import abstractions.flow.If
import abstractions.hasTag
import commands.Command
import enums.Anchor
import gunGame.playingTag
import gunGame.self
import gunGame.weapons.*
import lib.debug.Log
import lib.get
import structure.Fluorite
import structure.McFunction
import utils.rel
import utils.score.Objective

private val score = Objective("shoot")


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
    secondary = false
) {

    companion object {
        var myId: Int = 0
        val minigunTag = PlayerTag("minigun")
    }

    val fireFunc = McFunction("$basePath/shoot")


    init {
        Minigun.myId = myId
        Fluorite.tickFile += {
            score['a'["scores = {$score = 1..}"].hasTag(playingTag)] -= 1
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
                    score[self] = 0
                }
            Command.execute().asat('a'["scores = {$score = 2..}", "predicate = jh1236:ready"].hasTag(playingTag))
                .anchored(Anchor.EYES).run {
                    minigunTag.add(self)
                    fireFunc()
                }
            Command.execute().asat('a'["scores = {$score = 1}", "predicate = jh1236:ready"].hasTag(playingTag)).run {
                resetAmmoForId(myId)
                applyCoolDown(10)
                minigunTag.remove(self)
                Log.info(self.data["SelectedItem.tag.jh1236.ammo.value"])
                score[self] = 0
            }
        }
        setup()
    }

    override fun fire() {
        score[self] = 8
        minigunTag.add(self)
        fireFunc.append {
            super.fire()
            If(decrementClip(0, 1) eq 1) {
                applyCoolDown((reload * 20).toInt())
                Command.playsound("block.conduit.deactivate").master(self, rel(), 1, 0.0)
                minigunTag.remove(self)
                score[self] = 0
            }
        }
        fireFunc()
    }
}
