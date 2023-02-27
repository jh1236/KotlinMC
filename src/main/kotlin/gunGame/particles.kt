package gunGame

import commands.Command
import enums.Particles
import utils.get
import utils.rel

fun bigExplosion() {
    Command.particle(Particles.EXPLOSION, rel(),1.5, 1.5, 1.5,0.0, 100).force('a'[""])
    Command.particle(Particles.EXPLOSION_EMITTER, rel(),1.5, 1.5, 1.5,0.0, 10).force('a'[""])
    Command.particle(Particles.FLAME, rel(),1.5, 1.5, 1.5,0.0, 100).force('a'[""])
    Command.particle(Particles.CAMPFIRE_COSY_SMOKE, rel(),1.5, 1.5, 1.5,0.0, 100).force('a'[""])
    Command.particle(Particles.ASH, rel(),1.5, 1.5, 1.5,0.0, 750).force('a'[""])
    Command.particle(
        Particles.DUST_COLOR_TRANSITION(0.988, 0.0, 0.0, 1.0, 1.0, 0.529, 0.122),
        rel(),
        1.5, 1.5, 1.5,
        0.0,
        500
    ).force('a'[""])
    Command.playsound("minecraft:entity.firework_rocket.blast").master('a'[""], rel(), 4.0, 0.0)
    Command.playsound("minecraft:entity.generic.explode").master('a'[""], rel(), 4.0, 0.0)
    Command.playsound("minecraft:item.firecharge.use").master('a'[""], rel(), 4.0, 0.0)
}