package gunGame

import abstractions.*
import abstractions.flow.If
import abstractions.score.Criteria
import abstractions.score.Objective
import commands.Command
import enums.Anchor
import enums.Blocks
import enums.Particles
import events.EventManager
import gunGame.weapons.ProjectileWeapon
import gunGame.weapons.impl.*
import gunGame.weapons.resetAmmo
import gunGame.weapons.resetCooldown
import gunGame.weapons.shootTag
import internal.commands.impl.execute.Execute
import lib.debug.Log
import structure.Fluorite
import structure.McFunction
import utils.*


val streak = Objective("jh.streak", Criteria.dummy, """{"text":"Streak"}""")
val kills = Objective("jh.kills", Criteria.dummy, """{"text":"Kills"}""")
val deaths = Objective("jh.deaths", Criteria.dummy, """{"text":"Deaths"}""")
val kdr = Objective("jh.kdr", Criteria.dummy, """{"text":"100 * KDR"}""")
val maxHealth = Objective("maxHealth")
val health = Objective("health")
val timeSinceHit = Objective("timeSinceHit")
val playingTag = PlayerTag("playing")
val deadTag = PlayerTag("dead")
val deathStorage = Storage("jh1236:message")

fun setPlaying(playing: Boolean) {
    if (playing) {
        playingTag.add(self)
    } else {
        playingTag.remove(self)
    }
}

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
        deathStorage["death"] = """'["",{"selector":"@s","color":"gold"}," slipped and fell"]'"""
        dieFunc()
    }

}

private val calcKDR = McFunction("jh1236:health/kdr") {
    kdr[self] = kills[self]
    kdr[self] *= 100
    kdr[self] /= deaths[self]
}

val respawnFunc = McFunction("health/respawn") {
    Command.tp(self, abs(-16, 3, -89))
    Command.gamemode().adventure(self)
    deadTag.remove(self)
    playingTag.remove(self)
    Command.clear()
    Command.stopsound(self)
    speedyAmmo.remove(self)
    shotCount[self] = 0
    Command.raw("execute at @s run particle minecraft:entity_effect ~ ~ ~ 0.9960784313725490196078431372549 0.9921568627450980392156862745098 1 1 0 force @s")
    Command.effect().clear
    health[self] = 3000
    maxHealth[self] = 3000
}
private val dieFunc = McFunction("jh1236:health/die") {
    with(Command) {
        val tempScore = Fluorite.reuseFakeScore("id")
        tempScore.set(idScore[self])
        execute().As('e'[""].hasTag(ProjectileWeapon.universalProjectile)).If(idScore[self] eq tempScore).run {
            kill(self)
        }
        playingTag.remove(self)
        deadTag.add(self)
        clear()
        speedyAmmo.remove(self)
        shotCount[self] = 0
        effect().clear
        gamemode().spectator
        spectate('a'["limit = 1"].hasTag(shootTag))
        raw("execute at @s run particle minecraft:entity_effect ~ ~ ~ 0.9960784313725490196078431372549 0.9921568627450980392156862745098 1 1 0 force @s")
        kills['a'["limit = 1"].hasTag(shootTag)] += 1
        streak['a'["limit = 1"].hasTag(shootTag)] += 1
        streak[self] = 0
        deaths[self] += 1
        tellraw(
            'a'[""],
            """{"nbt":"death", "storage":"$deathStorage","interpret":true}"""
        )

        execute().asat('a'[""].hasTag(shootTag)).run(handleStreak)
        calcKDR()
        execute().asat('a'[""].hasTag(shootTag)).run {
            calcKDR
            resetAmmo()
            resetCooldown()
        }
        schedule().As(self, 80, respawnFunc).append
    }
}


val deathEvent = EventManager(dieFunc)

val damageSelf = McMethod("jh1236:health/damage", 1) { (damage) ->
    with(Command) {
        If(damage gt 0) {
            damage(self, 1.0, "jh1236:shot")
            Log.info("Damaged ", self, " for ", damage, " damage!!")
            If(self.hasTag(medusaTag) and (damage gt 0)) {
                resetMedusa()
                damage /= 10
            }
            health[self] -= damage
            timeSinceHit[self] = 0
            particle(
                Particles.BLOCK(Blocks.REDSTONE_BLOCK),
                rel(),
                0, 0, 0,
                1.0,
                20
            ).force('a'["distance = .75.."])
            playsound("entity.generic.hurt").player(self["tag =! noSound"], rel(), 2.0, 1.0, 1.0)
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