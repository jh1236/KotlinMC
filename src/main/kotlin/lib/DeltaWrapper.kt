package lib

import abstractions.score.Objective
import abstractions.score.Score
import commands.Command
import utils.Vec3
import utils.typeInterfaces.IPosition

object Delta {
    private val input = Objective("delta.api.launch")
    private val particleInput = Objective("delta.api.particle")
    const val EXPLODE_SOUND = "minecraft:delta.entity.generic.explode"

    init {
        input.addedToInit = true
        particleInput.addedToInit = true
    }

    fun launchXYZ(x: Score, y: Score, z: Score) {
        input["\$x"] = x
        input["\$z"] = y
        input["\$y"] = z
        Command.function("delta:api/launch_xyz")
    }

    fun launchFacing(strength: Double) {
        input["\$strength"] = (strength * 10000).toInt()
        Command.function("delta:api/launch_looking")
    }

    fun launchFacing(strength: Score) {
        input["\$strength"] = strength
        Command.function("delta:api/launch_looking")
    }

    fun explosionParticle(
        pos: IPosition,
        dx: Number,
        dy: Number,
        dz: Number,
        count: Int
    ) {
        particleInput["\$dx"] = (dx.toDouble() * 100).toInt()
        particleInput["\$dy"] = (dy.toDouble() * 100).toInt()
        particleInput["\$dz"] = (dz.toDouble() * 100).toInt()
        particleInput["\$count"] = count
        Command.execute().positioned(pos as Vec3<*>).run {
            Command.function("delta:api/explosion_particle")
        }
    }

    fun explosionEmitterParticle(
        pos: IPosition,
        dx: Number,
        dy: Number,
        dz: Number,
        count: Int
    ) {
        particleInput["\$dx"] = (dx.toDouble() * 100).toInt()
        particleInput["\$dy"] = (dy.toDouble() * 100).toInt()
        particleInput["\$dz"] = (dz.toDouble() * 100).toInt()
        particleInput["\$count"] = count
        Command.execute().positioned(pos as Vec3<*>).run {
            Command.function("delta:api/explosion_emitter_particle")
        }
    }


}