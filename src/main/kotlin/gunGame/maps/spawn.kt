package gunGame.maps

import abstractions.flow.If
import abstractions.flow.Switch
import commands.Command
import gunGame.playingTag
import gunGame.self
import gunGame.weapons.AbstractWeapon
import lib.get
import structure.Fluorite
import structure.McFunction
import utils.Vec2
import utils.score.Objective

val weaponSelectScore = Objective("select")[self]
val secondarySelectScore = Objective("scnd")[self]
lateinit var spawnFunc: McFunction

fun spawnFuncSetup() {
    spawnFunc = McFunction("spawn") {
        playingTag.add(self)
        val allWeapons = AbstractWeapon.allWeapons

        var i = 0
        Command.comment("Primaries")
        for (weapon in allWeapons) {
            if (weapon.secondary) continue
            i++
            If(weaponSelectScore eq i) {
                weapon.give(self)
            }
        }
        Command.comment("Secondaries")
        i = 0
        for (weapon in allWeapons) {
            if (!weapon.secondary) continue
            i++
            If(secondarySelectScore eq i) {
                weapon.give(self)
            }
        }


        Switch(Fluorite.getNewFakeScore("map"), allowDuplicateMatches = true).case(1) {
            Command.spreadplayers(Vec2(8, 8), 9.0, 9.0).under(10, false, self)
        }.case(2) {
            Command.tp(self, 'e'["tag = spawn_mansion", "limit = 1", "sort = random"])
        }.case(3) {
            Command.tp(self, 'e'["tag = spawn_house", "limit = 1", "sort = random"])
        }.case(4) {
            Command.tp(self, 'e'["tag = spawn_pg3d", "limit = 1", "sort = random"])
        }.case(5) {
            Command.tp(self, 'e'["tag = spawn_japan", "limit = 1", "sort = random"])
        }
    }
}