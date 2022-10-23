package gunGame.weapons

import abstractions.*
import abstractions.flow.If
import abstractions.flow.Tree
import abstractions.variables.NBTTypes
import commands.Command
import enums.Items
import gunGame.playingTag
import gunGame.self
import internal.commands.impl.execute.Execute
import internal.conditionals.Conditional
import lib.applyNbtToHeldItem
import lib.copyHeldItemToBlockAndRun
import lib.copyItemfromSlotAndRun
import lib.debug.Log
import lib.get
import structure.ExternalFile
import structure.Fluorite
import structure.McFunction
import utils.Selector
import utils.rel
import utils.score.ScoreConstant

val shootTag = PlayerTag("shot")
fun coolDownSetup() {
    ExternalFile(
        "G:/Games/Minecraft Servers/Gun Server/world/datapacks/backup/packle/data/jh1236/predicates/ready.json",
        "data/jh1236/predicates/ready.json"
    )
}


val applyCoolDown = McMethod("apply_cd", 1) { (coolDown) ->
    with(Command) {
        If(coolDown gte 10000) { coolDown.set(20) }
        If(self["tag = noCooldown"]) { coolDown.set(1) }
        val gameTime = Fluorite.reuseFakeScore("gametime")
        gameTime.set { time().query.gametime }
        gameTime += coolDown
        copyHeldItemToBlockAndRun {
            it["tag.jh1236.cooldown.max", NBTTypes.INT, 1.0] = coolDown
            it["tag.jh1236.cooldown.value", NBTTypes.INT, 1.0] = gameTime
            it["tag.jh1236"] = "{ready:0b}"
        }
    }
}

fun applyCooldownToSlot(cd: Int, slot: String) {
    val coolDown = ScoreConstant(cd)
    If(coolDown gte 10000) { coolDown.set(20) }
    If(self["tag = noCooldown"]) { coolDown.set(1) }
    val gameTime = Fluorite.getNewGarbageScore()
    gameTime.set { Command.time().query.gametime }
    gameTime += coolDown
    copyItemfromSlotAndRun(slot) {
        it["tag.jh1236.cooldown.max", NBTTypes.INT, 1.0] = coolDown
        it["tag.jh1236.cooldown.value", NBTTypes.INT, 1.0] = gameTime
        it["tag.jh1236"] = "{ready:0b}"
    }

}


fun Selector.canShoot() = object : Conditional() {
    override fun addToExecuteIf(ex: Execute) {
        ex.entity(this@canShoot["predicate = jh1236:ready"])
    }
}


fun ammoDisplayTick() {
    with(Command) {
        execute().asat('a'[""].hasTag(playingTag)).If(self hasData "SelectedItem.tag.jh1236.ammo").run {
            copyHeldItemToBlockAndRun {
                it["Count"] = it["tag.jh1236.ammo.value"]
            }
        }
        execute().asat('a'[""].hasTag(playingTag)).If(self hasData "Inventory[{Slot:-106b}]").run {
            item().replace.entity(self, "weapon.mainhand").from.entity(self, "weapon.offhand")
            item().replace.entity(self, "weapon.offhand").with(Items.AIR)
            If(self hasData "SelectedItem.tag.jh1236.ammo" and self["predicate = jh1236:ready"]) {
                val ammo = Fluorite.reuseFakeScore("ammo")
                ammo.set(self.data["SelectedItem.tag.jh1236.ammo.value"])
                val max = Fluorite.reuseFakeScore("max")
                max.set(self.data["SelectedItem.tag.jh1236.ammo.max"])
                If(!(max eq ammo)) {
                    copyHeldItemToBlockAndRun {
                        it["tag.jh1236.ammo.value"] = it["tag.jh1236.ammo.max"]
                    }
                    applyCoolDown(30)
                }
            }
        }
    }
}

fun coolDownTick() {
    with(Command) {
        execute().asat('a'[""].hasTag(playingTag)).If(self hasData "SelectedItem.tag.jh1236.weapon")
            .unless.predicate("jh1236:ready").run {
                val gameTime = Fluorite.reuseFakeScore("gametime")
                gameTime.set { time().query.gametime }
                val coolDown = Fluorite.reuseFakeScore("cd")
                coolDown.set(self.data["SelectedItem.tag.jh1236.cooldown.value"])
                val max = Fluorite.reuseFakeScore("max")
                max.set(self.data["SelectedItem.tag.jh1236.cooldown.max"])
                If(gameTime gte coolDown) {
                    If(max gt 1) { playsound("block.note_block.pling").player(self, rel(), 1, 2.0) }
                    applyNbtToHeldItem("{jh1236:{ready:1b}}")
                }
                xp().set(self, 10).levels
                coolDown -= gameTime
                coolDown *= 25
                coolDown /= max
                coolDown *= -1
                coolDown += 25
                Tree(coolDown, 0..25) {
                    xp().set(self, it).points
                }
            }
        execute().asat('a'[""].hasTag(playingTag)).If.predicate("jh1236:ready").run {
            xp().set(self, 0).points
        }
    }
    ammoDisplayTick()
}

val decrementClip = ReturningMethod("jh1236:dec_clip", 2) {
    val clipSize = Fluorite.getNewFakeScore("clipSize")
    val retScore = Fluorite.getNewFakeScore("ret")
    copyHeldItemToBlockAndRun { itemData ->
        clipSize.set(itemData["tag.jh1236.ammo.value"])
        clipSize -= 1
        If(clipSize eq 0) {
            Log.trace("clip empty!!")
            itemData["tag.jh1236.ammo.value"] = itemData["tag.jh1236.ammo.max"]
            retScore.set(it[1])
        }.Else {
            itemData["tag.jh1236.ammo.value"] = clipSize
            retScore.set(it[0])
        }
    }
    Log.trace("Clipsize: ", clipSize.asTellraw())
    retScore
}
val resetAmmo = McFunction("ammo/reset") {
    repeat(9) {
        copyItemfromSlotAndRun("hotbar.$it") { itemData ->
            itemData["tag.jh1236.ammo.value"] = itemData["tag.jh1236.ammo.max"]
            itemData["Count"] = itemData["tag.jh1236.ammo.value"]
        }
    }
}
