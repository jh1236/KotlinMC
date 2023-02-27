package gunGame.weapons

import abstractions.*
import abstractions.flow.If
import abstractions.flow.Tree
import abstractions.score.Score
import abstractions.score.ScoreConstant
import abstractions.variables.NBTTypes
import commands.Command
import enums.Items
import gunGame.playingTag
import gunGame.self
import gunGame.weapons.impl.shotCount
import gunGame.weapons.impl.speedyAmmo
import internal.commands.impl.execute.Execute
import internal.conditionals.Conditional
import lib.applyNbtToHeldItem
import lib.copyHeldItemToBlockAndRun
import lib.copyItemfromSlotAndRun
import lib.debug.Log
import structure.ExternalFile
import structure.Fluorite
import structure.McFunction
import utils.Selector
import utils.get
import utils.rel

val shootTag = PlayerTag("shot")
fun coolDownSetup() {
    ExternalFile(
        "G:/Programming/kotlin/GunGame/src/main/resources/ready.json",
        "data/jh1236/predicates/ready.json"
    )
}


val applyCoolDown = McMethod("apply_cd", 1) { (coolDown) ->
    with(Command) {
        If(self["tag = noCooldown"]) { coolDown.set(0) }
        If(self.hasTag(speedyAmmo) and (coolDown gt 0)) {
            coolDown /= 2
            shotCount[self] -= 1
            If(shotCount[self] lte 0) {
                speedyAmmo.remove(self)
            }
        }
        val gameTime = Fluorite.reuseFakeScore("gametime")
        gameTime.set { time().query.gametime }
        gameTime += coolDown
        Log.trace("Cooldown applied was: ", coolDown)
        If(coolDown lte -1) { gameTime.set(-1) }
        copyHeldItemToBlockAndRun {
            it["tag.jh1236.cooldown.max", NBTTypes.INT, 1.0] = coolDown
            it["tag.jh1236.cooldown.value", NBTTypes.INT, 1.0] = gameTime
            it["tag.jh1236"] = "{ready:0b}"
        }
    }
}

fun applyCooldownToSlot(cd: Int, slot: String) {
    applyCooldownToSlot(ScoreConstant(cd), slot)
}

fun applyCooldownToSlot(cd: Score, slot: String) {
    If(self["tag = noCooldown"]) { cd.set(1) }
    val gameTime = Fluorite.getNewGarbageScore()
    gameTime.set { Command.time().query.gametime }
    gameTime += cd
    copyItemfromSlotAndRun(slot) {
        it["tag.jh1236.cooldown.max", NBTTypes.INT, 1.0] = cd
        it["tag.jh1236.cooldown.value", NBTTypes.INT, 1.0] = gameTime
        it["tag.jh1236"] = "{ready:0b}"
    }

}


fun Selector.canShoot() = object : Conditional() {
    override fun addToExecuteIf(ex: Execute) {
        ex.entity(this@canShoot["predicate = jh1236:ready"])
    }
}

val resetAmmoForId = McMethod("jh1236:ammo/reset_special", 1) { (id) ->
    val weaponScore = Fluorite.reuseFakeScore("id")
    repeat(9) {
        weaponScore.set(0)
        weaponScore.set(self.data["Inventory[{Slot:${it}b}].tag.jh1236.weapon"])
        Command.execute().If(id eq weaponScore).run {
            copyItemfromSlotAndRun("hotbar.$it") { itemData ->
                itemData["tag.jh1236.ammo.value"] = itemData["tag.jh1236.ammo.max"]
                itemData["Count"] = itemData["tag.jh1236.ammo.value"]
            }
        }
    }
}
val setAmmoForId = McMethod("jh1236:ammo/reset_special_2", 2) { (id, ammoValue) ->
    val weaponScore = Fluorite.reuseFakeScore("id")
    repeat(9) {
        weaponScore.set(0)
        weaponScore.set(self.data["Inventory[{Slot:${it}b}].tag.jh1236.weapon"])
        Command.execute().If(id eq weaponScore).run {
            copyItemfromSlotAndRun("hotbar.$it") { itemData ->
                itemData["tag.jh1236.ammo.value"] = ammoValue
                itemData["Count"] = ammoValue
            }
        }
    }
}


val setCooldownForId = McMethod("jh1236:cooldown/reset_special", 2) { (id, cooldown) ->
    val weaponScore = Fluorite.reuseFakeScore("id")
    repeat(9) {
        weaponScore.set(0)
        weaponScore.set(self.data["Inventory[{Slot:${it}b}].tag.jh1236.weapon"])
        Command.execute().If(id eq weaponScore).run {
            applyCooldownToSlot(cooldown, "hotbar.$it")
        }
    }
}


fun ammoDisplayTick() {
    with(Command) {
        execute().asat('a'[""].hasTag(playingTag)).If(self.data["SelectedItem.tag.jh1236.ammo"]).run {
            copyHeldItemToBlockAndRun {
                it["Count"] = it["tag.jh1236.ammo.value"]
            }
        }
        execute().asat('a'[""].hasTag(playingTag)).If(self.data["Inventory[{Slot:-106b}]"]).run {
            item().replace.entity(self, "weapon.mainhand").from.entity(self, "weapon.offhand")
            item().replace.entity(self, "weapon.offhand").with(Items.AIR)
            If(self.data[ "SelectedItem.tag.jh1236.ammo"] and self["predicate = jh1236:ready"]) {
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
        execute().asat('a'["predicate = !jh1236:ready"].hasTag(playingTag))
            .If(self.data["SelectedItem.tag.jh1236.weapon"]).run {
                val gameTime = Fluorite.reuseFakeScore("gametime")
                gameTime.set { time().query.gametime }
                val coolDown = Fluorite.reuseFakeScore("cd")
                coolDown.set(self.data["SelectedItem.tag.jh1236.cooldown.value"])
                val max = Fluorite.reuseFakeScore("max")
                max.set(self.data["SelectedItem.tag.jh1236.cooldown.max"])
                If(gameTime gte coolDown and (coolDown gte 0)) {
                    If(max gt 1) { playsound("block.note_block.pling").player(self, rel(), 1, 2.0) }
                    applyNbtToHeldItem("{jh1236:{ready:1b}}")
                }
                xp().set(self, 10).levels
                coolDown -= gameTime
                coolDown *= 25
                coolDown /= max
                coolDown *= -1
                coolDown += 25
                If(gameTime lt 0) {
                    coolDown.set(0)
                }
                Tree(coolDown, 0..25) {
                    xp().set(self, it).points
                }
            }
        execute().asat('a'[""].hasTag(playingTag)).If.predicate("jh1236:ready").run {
            xp().set(self, 0).points
        }
    }
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
        If(self.data["Inventory[{Slot:${it}b}].tag.jh1236.ammo.max"]) {
            copyItemfromSlotAndRun("hotbar.$it") { itemData ->
                itemData["tag.jh1236.ammo.value"] = itemData["tag.jh1236.ammo.max"]
                val temp = Fluorite.reuseFakeScore("count")
                Command.tellraw('a'[""], temp)
                temp.set(itemData["tag.jh1236.ammo.max"])
                temp.maxOf(1)
                itemData["Count"] = temp
            }
        }
    }
}
val resetCooldown = McFunction("cooldown/reset") {
    repeat(9) {
        copyItemfromSlotAndRun("hotbar.$it") { itemData ->
            itemData["tag.jh1236.cooldown"] = "{value:0}"
            itemData["tag.jh1236"] = "{ready:1b}"
        }
    }
}
