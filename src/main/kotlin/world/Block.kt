package world

import commands.Command
import enums.Blocks
import internal.conditionals.BlockCondition
import utils.Vec3
import utils.abs
import utils.loc
import utils.rel

class Block {
    private val pos: Vec3<out Any>

    infix fun Is(other: Blocks): BlockCondition {
        return pos isBlock other
    }

    constructor(x: Int, y: Int, z: Int) : this(x, y, z, PosType.Absolute)

    internal constructor(x: Number, y: Number, z: Number, type: PosType) {
        pos = when (type) {
            PosType.Local -> loc(x, y, z)
            PosType.Relative -> rel(x, y, z)
            PosType.Absolute -> abs(x, y, z)
        }

    }

    enum class PosType {
        Local,
        Relative,
        Absolute
    }

    fun set(block: Blocks) {
        Command.setblock(pos, block)
    }

    fun getData(path:String = "") {

    }
}
