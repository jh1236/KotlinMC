package gunGame.weapons.primary

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
import gunGame.weapons.ModularCoasWeapon
import gunGame.weapons.applyCoolDown
import gunGame.weapons.applyCooldownToSlot
import lib.copyItemfromSlotAndRun
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

    val resetMgAmmo = McFunction("jh1236:minigun/reset") {
        repeat(9) {
            Command.execute().If(self hasData "Inventory[{Slot:${it}b, tag:{jh1236:{weapon:$myId}}}]").run {
                copyItemfromSlotAndRun("hotbar.$it") { itemData ->
                    itemData["tag.jh1236.ammo.value"] = itemData["tag.jh1236.ammo.max"]
                    itemData["Count"] = itemData["tag.jh1236.ammo.value"]
                }
            }
            minigunTag.remove(self)
        }
    }

    init {
        Minigun.myId = myId
        withClipSize(50)
        withParticle(Particles.CRIT)
        withCustomModelData(4)
        withRange(400)
        withSpread(3.0)
        withReload(5.0)
        addSound("block.note_block.hat", 2.0)
        done()
        Fluorite.tickFile += {
            score['a'["scores = {$score = 1..}"].hasTag(playingTag)] -= 1
            Command.execute()
                .asat(
                    'a'["nbt =! {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}", "predicate = jh1236:ready"].hasTag(
                        playingTag
                    ).hasTag(minigunTag)
                )
                .run {
                    resetMgAmmo()
                    repeat(9) {
                        Command.execute().If(self hasData "Inventory[{Slot:${it}b, tag:{jh1236:{weapon:$myId}}}]").run {
                            applyCooldownToSlot(cooldown, "hotbar.$it")
                        }
                    }
                    score[self] = 0
                }
            Command.execute().asat('a'["scores = {$score = 2..}", "predicate = jh1236:ready"].hasTag(playingTag))
                .anchored(Anchor.EYES).run {
                    fire()
                    minigunTag.add(self)
                }
            Command.execute().asat('a'["scores = {$score = 1}", "predicate = jh1236:ready"].hasTag(playingTag)).run {
                If(self["nbt = {jh1236:{ammo:{value:50}}}"]) {
                    applyCoolDown(reload)
                    Command.playsound("block.conduit.deactivate").master(self, rel(), 1.0, 0.0)
                }.Else {
                    resetMgAmmo()
                    applyCoolDown(cooldown)
                }

                Log.info(self.data["SelectedItem.tag.jh1236.ammo.value"])
                score[self] = 0
            }
        }
    }

    lateinit var fire: McFunction

    override fun shoot() {
        score[self] = 8
        minigunTag.add(self)
        fire = McFunction("jh1236:primary/minigun/shoot") {
            super.shoot()
        }
    }
}
