package world

import abstractions.variables.NBTTypes
import commands.Command
import enums.Effects
import structure.Fluorite
import utils.Selector
import utils.abs
import utils.score.Score
import utils.typeInterfaces.ISelector

open class Entity : IMcObject, ISelector {
    open val selectorChar = 'e'
    private val self = Selector('s')

    override fun print() {
        Command.tellraw(Selector('a'), self.asTellraw())
    }

    fun say(string: String) {
        Command.say(string)
    }

    fun effect(effect: Effects, time: Int, amplifier: Int = 0, hideParticles: Boolean = false) {
        Command.effect().give(self, effect, time, amplifier, hideParticles)
    }

    fun teleport(x: Int, y: Int, z: Int) {
        Command.teleport(abs(x, y, z))
    }

    fun addTag(tagName: String) {
        Command.tag(self).add(tagName)
    }

    fun removeTag(tagName: String) {
        Command.tag(self).remove(tagName)
    }

    fun getData(path: String): Score {
        val ret = Fluorite.getGarbageScore()
        ret.set { Command.data().get.entity(self, path) }
        return ret
    }

    fun setData(path: String, score: Score, type: NBTTypes, scale: Double) {
        Command.execute().store.result.entity(this, path, type, scale)
    }

    fun getRelativeBlock(x: Number, y: Number, z: Number): Block {
        return Block(x, y, z, Block.PosType.Relative)
    }

    fun getLocalBlock(x: Number, y: Number, z: Number): Block {
        return Block(x, y, z, Block.PosType.Local)
    }

    override val selectorString: String
        get() = self.selectorString

    override fun addArgs(key: String, value: String): ISelector {
        return self.addArgs(key, value)
    }


}
