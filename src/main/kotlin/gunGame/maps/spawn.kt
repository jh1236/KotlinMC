package gunGame.maps

import abstractions.Trigger
import abstractions.asat
import abstractions.flow.If
import abstractions.flow.Switch
import abstractions.score.Objective
import commands.Command
import enums.Effects
import gunGame.*
import gunGame.weapons.AbstractWeapon.Companion.allWeapons
import lib.debug.Debug
import lib.debug.Log
import lib.random.Random
import structure.Fluorite
import structure.McFunction
import utils.Selector
import utils.Vec2
import utils.get

val weaponSelectScore = Objective("select")[self]
val secondarySelectScore = Objective("secnd")[self]
lateinit var spawnFunc: McFunction

fun spawnSetup() {


    val primaryCount = allWeapons.stream().filter { !it.secondary && !it.isReward }.count().toInt()
    val secondaryCount = allWeapons.stream().filter { it.secondary && !it.isReward }.count().toInt()
    val mapFunc = McFunction("spawn/map") {
        Command.effect().clear(self, Effects.SPEED)
        If(!(Fluorite.reuseFakeScore("map") eq 0)) {
            Command.tellraw(
                'a'[""],
                "",
                """{"selector":"@s", "color":"gold"}""",
                """{"text": " has spawned in!", "color":"white"}"""
            )
        }
        health[self] = 3000
        maxHealth[self] = 3000
        Command.gamemode().adventure
        Switch(Fluorite.reuseFakeScore("map"), allowDuplicateMatches = true)
            .case(0) {
                respawnFunc()
                Command.tellraw(self, "No map is selected!")
            }
            .case(1) {
                Command.spreadplayers(Vec2(8, 8), 9, 9.0).under(10, false, self)
            }.case(2) {
                Command.tp(self, 'e'["tag = spawn_mansion", "limit = 1", "sort = random"])
            }.case(3) {
                Command.tp(self, 'e'["tag = spawn_house", "limit = 1", "sort = random"])
            }.case(4) {
                Command.tp(self, 'e'["tag = spawn_pg3d", "limit = 1", "sort = random"])
            }.case(5) {
                Command.tp(self, 'e'["tag = spawn_japan", "limit = 1", "sort = random"])
            }.case(6) {
                Command.tp(self, 'e'["tag = spawn_spain", "limit = 1", "sort = random"])
            }
    }

    val primaryDirectory = McFunction("spawn/primary") {
        var i = 0
        for (weapon in allWeapons) {
            if (weapon.secondary || weapon.isReward) continue
            i++
            If(weaponSelectScore eq i) {
                weapon.give(self)
            }
        }
    }
    val secondaryDirectory = McFunction("spawn/secondary") {
        var i = 0
        for (weapon in allWeapons) {
            if (!weapon.secondary || weapon.isReward) continue
            i++
            If(secondarySelectScore eq i) {
                weapon.give(self)
            }
        }
    }

    spawnFunc = McFunction("start") {
        playingTag.add(self)
        primaryDirectory()
        secondaryDirectory()
        mapFunc()

    }
    val randomFunc = McFunction("spawn/random") {
        playingTag.add(self)
        //save old weapon selection
        val one = Fluorite.reuseFakeScore("primary")
        val two = Fluorite.reuseFakeScore("secondary")
        one.set(weaponSelectScore)
        two.set(secondarySelectScore)

        //select 1 random primary, 2 random secondaries
        val random = Random.next(primaryCount)
        random += 1
        Log.info(random)
        weaponSelectScore.set(random)
        primaryDirectory()
        random.set(Random.next(secondaryCount))
        random += 1
        Log.info(random)
        secondarySelectScore.set(random)
        secondaryDirectory()

        random.set(Random.next(secondaryCount))
        random += 1
        Log.info(random)
        secondarySelectScore.set(random)
        secondaryDirectory()

        // tp in
        mapFunc()

        //restore old selection
        weaponSelectScore.set(one)
        secondarySelectScore.set(two)
    }

    Fluorite.tickFile += {
        if (!Debug.debugMode) {
            McFunction("scoreboard") {
                val gt = Fluorite.reuseFakeScore("gametime")
                gt.set { Command.time().query.gametime }
                gt %= 320
                gt /= 80
                Switch(gt, allowDuplicateMatches = true)
                    .case(0) {
                        streak.setDisplay("sidebar")
                    }
                    .case(1) {
                        kills.setDisplay("sidebar")
                    }
                    .case(2) {
                        deaths.setDisplay("sidebar")
                    }
                    .case(3) {
                        kdr.setDisplay("sidebar")
                    }
            }()
        }
    }
    Trigger("spawn") {
        respawnFunc()
    }
    Trigger("random", 'a'[""]) {
        respawnFunc()
        randomFunc()
    }
    Trigger("reset", Selector("Jh1236")) {
        deaths["*"].reset()
        streak["*"].reset()
        kills["*"].reset()
        kdr["*"].reset()
        Command.execute().asat('a'[""]).run {
            respawnFunc()
            deaths[self] = 0
            streak[self] = 0
            kills[self] = 0
            kdr[self] = 0
        }
    }
}