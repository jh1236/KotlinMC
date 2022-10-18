package gunGame

import abstractions.*
import abstractions.flow.If
import commands.Command
import enums.Anchor
import enums.Blocks
import enums.Particles
import events.EventManager
import gunGame.weapons.shootTag
import internal.commands.impl.execute.Execute
import lib.get
import structure.McFunction
import utils.Selector
import utils.abs
import utils.loc
import utils.rel
import utils.score.Objective

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

private val dieFunc = McFunction("jh1236:health/die") {
    with(Command) {

        playingTag.remove(self)
        deadTag.add(self)
        clear()
        effect().clear
        gamemode().spectator
        spectate('a'["limit = 1"].hasTag(shootTag))
        schedule().As(self, 80) {
            gamemode().adventure(self)
            deadTag.remove(self)
            health[self] = 3000
            maxHealth[self] = 3000
            tp(self, abs(-16, 4, -69))
        }
    }
}

val deathEvent = EventManager(dieFunc)

val damageSelf = McMethod("jh1236:health/damage", 1) { (damage) ->
    with(Command) {
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
    facing.entity(entity, Anchor.EYES).positioned(loc(0, 0, numerator)).rotated.As(self)
        .positioned(loc(0, 0, denominator)).facing.entity(self, Anchor.EYES).facing(loc(0, 0, -1)).positioned.As(self)
    return this
}