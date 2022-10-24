package gunGame.weapons.impl

import abstractions.If
import abstractions.PlayerTag
import abstractions.asat
import abstractions.flow.If
import abstractions.hasTag
import commands.Command
import enums.Anchor
import enums.Particles
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


class Minigun : ModularCoasWeapon("Minigun", 240) {

    companion object {
        var myId: Int = 0
        val minigunTag = PlayerTag("minigun")
    }


    init {
        Minigun.myId = myId
        withClipSize(50)
        withParticle(Particles.CRIT)
        withCustomModelData(4)
        withRange(400)
        withSpread(3.0)
        withReload(5.0)
        withCooldown(0.0)
        onReload {
            minigunTag.remove(self)
        }
        addSound("block.note_block.hat", 2.0)
        withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was peppered down by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        done()
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
                    fire()
                }
            Command.execute().asat('a'["scores = {$score = 1}", "predicate = jh1236:ready"].hasTag(playingTag)).run {
                resetAmmoForId(myId)
                applyCoolDown(10)
                minigunTag.remove(self)
                Log.info(self.data["SelectedItem.tag.jh1236.ammo.value"])
                score[self] = 0
            }
        }
    }

    lateinit var fire: McFunction

    override fun shoot() {
        score[self] = 8
        minigunTag.add(self)
        fire = McFunction("$basePath/shoot") {
            super.shoot()
            If(self["nbt = {SelectedItem:{tag:{jh1236:{ammo:{value:50}}}}}"]) {
                Command.playsound("block.conduit.deactivate").master(self, rel(), 1, 0.0)
            }
        }
    }
}