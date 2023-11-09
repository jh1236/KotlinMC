package gunGame

import abstractions.*
import abstractions.flow.If
import abstractions.schedule.Sleep
import abstractions.score.Criteria
import abstractions.score.Objective
import commands.Command
import enums.Anchor
import enums.Blocks
import enums.Particles
import events.EventManager
import gunGame.maps.playTaunt
import gunGame.maps.randomMode
import gunGame.maps.secondarySelectScore
import gunGame.maps.weaponSelectScore
import gunGame.weapons.ProjectileWeapon
import gunGame.weapons.impl.*
import gunGame.weapons.resetAmmo
import gunGame.weapons.resetCooldown
import gunGame.weapons.shootTag
import internal.commands.impl.execute.Execute
import internal.commands.impl.execute.OnTarget
import lib.debug.Log
import structure.Fluorite
import structure.McFunction
import utils.*


val streak = Objective("jh.streak", Criteria.dummy, """{"text":"Streak"}""")
val kills = Objective("jh.kills", Criteria.dummy, """{"text":"Kills"}""")
val coins = Objective("jh.coins", Criteria.dummy, """{"text":"Coins"}""")
val deaths = Objective("jh.deaths", Criteria.dummy, """{"text":"Deaths"}""")
val kdr = Objective("jh.kdr", Criteria.dummy, """{"text":"100 * KDR"}""")
val maxHealth = Objective("maxHealth")
val health = Objective("health")
val timeSinceHit = Objective("timeSinceHit")
val playingTag = PlayerTag("playing")
val deadTag = PlayerTag("dead")
val deathStorage = Storage("jh1236:message")

fun Execute.ifPlaying(player: Selector = self): Execute {
    this.If(player.hasTag(playingTag))
    return this
}

fun healthTick() {
    Command.execute().As('a'[""].hasTag(playingTag)).If(health[self] lt maxHealth[self]).If(timeSinceHit[self] gte 40)
        .run {
            val dif = Fluorite.reuseFakeScore("health")
            dif.set(maxHealth[self])
            dif -= health[self]
            dif.minOf(10)
            health[self] += dif
        }
    Command.execute().As('a'[""].hasTag(playingTag)).run {
        timeSinceHit[self] += 1
        Command.title(self)
            .actionbar("[{\"text\":\"Health: \",\"color\": \"#DD3333\", \"bold\" : true},{\"score\": {\"name\": \"@s\",\"objective\": \"jh.health\"}, \"bold\" : false}]")
    }
    Command.execute().asat('a'[""].hasTag(playingTag)).positioned(Vec3("~", "-30", "~")).As(self["dy = 10"]).run {
        Command.execute().on(OnTarget.ATTACKER).run {
            shootTag.add(self)
        }
        If('a'[""].hasTag(shootTag)) {
            deathStorage["death"] =
                """'["",{"selector":"@s","color":"gold"}," was knocked into the void by ", {"selector":"@a[tag = $shootTag]","color":"gold"}]'"""
        }.Else {
            deathStorage["death"] = """'["",{"selector":"@s","color":"gold"}," slipped and fell"]'"""
        }
        dieFunc()
        shootTag.remove('a'[""])
    }
    Command.execute().asat('a'[""].hasTag(playingTag)).If(rel() isBlock Blocks.STRUCTURE_VOID).run {
        Command.execute().on(OnTarget.ATTACKER).If(self.hasTag(playingTag)).run {
            shootTag.add(self)
        }
        If('a'[""].hasTag(shootTag)) {
            deathStorage["death"] =
                """'["",{"selector":"@s","color":"gold"}," was knocked into the void by ", {"selector":"@a[tag = $shootTag]","color":"gold"}]'"""
        }.Else {
            deathStorage["death"] = """'["",{"selector":"@s","color":"gold"}," slipped and fell"]'"""
        }
        dieFunc()
        shootTag.remove('a'[""])
    }

}

private val calcKDR = McFunction("jh1236:health/kdr") {
    kdr[self] = kills[self]
    kdr[self] *= 100
    kdr[self] /= deaths[self]
}

val respawnFunc = McFunction("health/respawn") {
    streak[self] = 0
    If(randomMode eq 1) {
        Command.tp(self, abs(-64, 17, -65), Vec2(180, 0))
        regenRandom()
        weaponSelectScore.set(primary1)
        secondarySelectScore.set(secondary1)
    }.Else {
        Command.tp(self, abs(-16, 3, -89), Vec2(180, 0))
    }
    val tempScore = Fluorite.reuseFakeScore("temp")
    tempScore.set(idScore[self])
    Command.execute().As('e'[""].hasTag(smokeTag)).If(idScore[self] eq tempScore).run {
        Command.kill(self)
    }
    Command.gamemode().adventure(self)
    deadTag.remove(self)
    playingTag.remove(self)
    Command.clear()
    smgScore[self] = 0
    minigunScore[self] = 0
    guardianScore[self] = 0
    Command.stopsound(self)
    speedyAmmo.remove(self)
    shotCount[self] = 0
    Command.raw("execute at @s run particle minecraft:entity_effect ~ ~ ~ 0.9960784313725490196078431372549 0.9921568627450980392156862745098 1 1 0 force @s")
    Command.effect().clear
    maxHealth[self] = 3000
    health[self].reset()
}

private val dieFunc = McFunction("jh1236:health/die") {
    with(Command) {
        val tempScore = Fluorite.reuseFakeScore("id")
        tempScore.set(idScore[self])
        execute().As('e'[""].hasTag(ProjectileWeapon.universalProjectile)).If(idScore[self] eq tempScore).run {
            kill(self)
        }
        execute().As('e'[""].hasTag(smokeTag)).If(idScore[self] eq tempScore).run {
            kill(self)
        }
        playingTag.remove(self)
        deadTag.add(self)
        clear()
        smgScore[self] = 0
        minigunScore[self] = 0
        guardianScore[self] = 0
        speedyAmmo.remove(self)
        shotCount[self] = 0
        effect().clear
        gamemode().spectator
        spectate('a'["limit = 1"].hasTag(shootTag))
        raw("execute at @s run particle minecraft:entity_effect ~ ~ ~ 0.9960784313725490196078431372549 0.9921568627450980392156862745098 1 1 0 force @s")
        kills['a'["limit = 1"].hasTag(shootTag).hasTag(playingTag)] += 1
        streak['a'["limit = 1"].hasTag(shootTag)] += 1
        streak[self] = 0
        deaths[self] += 1
        If(!self.hasTag(shootTag)) {
            coins[self] += 100
            If(randomMode eq 1) {
                coins[self] += 100
            }
        }.Else {
            deaths[self] += 1
            coins[self] -= 20
        }
        tellraw(
            'a'[""],
            """{"nbt":"death", "storage":"$deathStorage","interpret":true}"""
        )
        playTaunt()

        execute().asat('a'[""].hasTag(shootTag)).run(handleStreak)
        calcKDR()
        execute().asat('a'[""].hasTag(shootTag)).run {
            calcKDR()
            resetAmmo()
            resetCooldown()
        }
        health[self].reset()
        Sleep(Duration(80))
        respawnFunc()
    }
}


val deathEvent = EventManager(dieFunc)

val damageSelf = McMethod("jh1236:health/damage", 1) { (damage) ->
    with(Command) {
        If(damage gt 0 and (self.notHasTag(deadTag))) {
            damage(self[""].notHasTag(swordStabbed), 1.0, "jh1236:shot")
            Log.info("Damaged ", self, " for ", damage, " damage!!")
            If(self.hasTag(medusaTag) and (damage gt 0)) {
                resetMedusa()
                damage /= 10
            }
            If(self.hasTag(invisTag)) {
                invisCooldown[self] = 10
            }
            If(!self.hasTag(dummyTag)) {
                health[self] -= damage
            }.Else {
                Log.warn("Dealt ", damage, " damage!")
            }
            timeSinceHit[self] = 0
            particle(
                Particles.BLOCK(Blocks.REDSTONE_BLOCK),
                rel(),
                0, 0, 0,
                1.0,
                20
            ).force('a'["distance = .75.."])
            execute().asat('a'[""].hasTag(shootTag)).run {
                playsound("entity.arrow.hit_player")
                    .player(self, rel(), 2.0, 1.0, 1.0)
            }
            execute().asat(self.hasTag(ProjectileWeapon.universalProjectile).hasTag(playingTag)).run {
                health[self] = 1
            }
            If(health[self] lte 0) {
                deathEvent.call()
            }
        }
    }

}

fun Execute.asIntersects(entity: Selector): Execute {
    As(entity["dx = 0"]).positioned(rel(-.5, -.5, -.5)).If(self["dx = 0"]).positioned(rel(.5, .5, .5))
    return this
}

fun Execute.lerpFacing(entity: Selector, numerator: Int, denominator: Int): Execute {
    facing(entity, Anchor.EYES).positioned(loc(0, 0, numerator)).rotated(self)
        .positioned(loc(0, 0, denominator)).facing(self, Anchor.EYES).facing(loc(0, 0, -1)).positioned(self)
    return this
}