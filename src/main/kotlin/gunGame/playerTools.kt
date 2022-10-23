package gunGame

import abstractions.*
import abstractions.flow.If
import commands.Command
import enums.Anchor
import enums.Blocks
import enums.Particles
import events.EventManager
import gunGame.weapons.handleStreak
import gunGame.weapons.medusaTag
import gunGame.weapons.resetMedusa
import gunGame.weapons.shootTag
import internal.commands.impl.execute.Execute
import lib.debug.Log
import lib.get
import structure.McFunction
import utils.*
import utils.score.Objective

val streak = Objective("jh.streak", Criteria.dummy, """{"text":"Streak"}""")
val kills = Objective("jh.kills", Criteria.dummy, """{"text":"Kills"}""")
val deaths = Objective("jh.deaths", Criteria.dummy, """{"text":"Deaths"}""")
val kdr = Objective("jh.kdr", Criteria.dummy, """{"text":"100 * KDR"}""")
val maxHealth = Objective("maxHealth")
val health = Objective("health")
val timeSinceHit = Objective("timeSinceHit")
val playingTag = PlayerTag("playing")
val deadTag = PlayerTag("dead")


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
            health[self] += 10
        }
    Command.execute().As('a'[""].hasTag(playingTag)).run {
        timeSinceHit[self] += 1
        Command.title(self)
            .actionbar("[{\"text\":\"Health: \",\"color\": \"#DD3333\", \"bold\" : true},{\"score\": {\"name\": \"@s\",\"objective\": \"jh.health\"}, \"bold\" : false}]")
    }

}

private val calcKDR = McFunction("jh1236:health/kdr") {
    kdr[self] = kills[self]
    kdr[self] *= 100
    kdr[self] /= deaths[self]
}

val respawnFunc = McFunction("health/respawn") {
    Command.gamemode().adventure(self)
    deadTag.remove(self)
    playingTag.remove(self)
    Command.clear()
    Command.effect().clear
    health[self] = 3000
    maxHealth[self] = 3000
    Command.tp(self, abs(-16, 3, -89))
}
private val dieFunc = McFunction("jh1236:health/die") {
    with(Command) {
        playingTag.remove(self)
        deadTag.add(self)
        clear()
        effect().clear
        gamemode().spectator
        spectate('a'["limit = 1"].hasTag(shootTag))
        kills['a'["limit = 1"].hasTag(shootTag)] += 1
        streak['a'["limit = 1"].hasTag(shootTag)] += 1
        streak[self] = 0
        deaths[self] += 1
        execute().If('a'[""].hasTag(shootTag).notHasTag(deadTag)).run.tellraw(
            'a'[""],
            """{"nbt":"death", "storage":"jh1236:message","interpret":true}"""
        )
        execute().unless('a'[""].hasTag(shootTag).notHasTag(deadTag)).run.tellraw(
            'a'[""],
            "",
            """{"selector": "@s","color": "gold"}""",
            """{"text": " didn't want to live in the same world as "}""",
            """{"selector": "@a[tag=!$shootTag]","color": "gold"}"""
        )
        execute().asat('a'[""].hasTag(shootTag)).run(::handleStreak)
        calcKDR()
        execute().asat('a'[""].hasTag(shootTag)).run(calcKDR)
        schedule().As(self, 80, respawnFunc)
    }
}


val deathEvent = EventManager(dieFunc)

val damageSelf = McMethod("jh1236:health/damage", 1) { (damage) ->
    with(Command) {
        Log.info("Damaged ", self, " for ", damage, " damage!!")
        If(self.hasTag(medusaTag)) {
            resetMedusa()
            damage /= 10
        }
        health[self] -= damage
        timeSinceHit[self] = 0
        particle(
            Particles.BLOCK(Blocks.REDSTONE_BLOCK),
            rel(),
            abs(0, 0, 0),
            1.0,
            20
        ).force('a'["distance = .75.."])
        playsound("entity.generic.hurt").player(self["tag =! noSound"], rel(), 2.0, 1.0, 1.0)
        execute().asat('a'[""].hasTag(shootTag)).run {
            playsound("entity.arrow.hit_player")
                .player(self, rel(), 2.0, 1.0, 1.0)
        }
        If(health[self] lte 0) {
            deathEvent.call()
        }
    }

}

fun Execute.asIntersects(entity: Selector): Execute {
    As(entity["dx = 0"]).positioned(rel(-.5, -.5, -.5)).If(self["dx = 0"]).positioned(rel(.5, .5, .5))
    return this
}

fun Execute.lerpFacing(entity: Selector, numerator: Int, denominator: Int): Execute {
    facing(entity, Anchor.EYES).positioned(loc(0, 0, numerator)).rotated.As(self)
        .positioned(loc(0, 0, denominator)).facing(self, Anchor.EYES).facing(loc(0, 0, -1)).positioned.As(self)
    return this
}