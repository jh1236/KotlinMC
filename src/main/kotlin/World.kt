import events.EventManager
import structure.Datapack
import structure.Fluorite
import structure.McFunction
import utils.Selector
import world.Block
import world.Entity
import world.EntityList
import world.Player

object World {
    val dp = Datapack("name", "~", "jh1236")
    val tickEvent = EventManager(Fluorite.tickFile)
    val loadEvent = EventManager(Fluorite.loadFile)

    init {
        Fluorite.init()
        Fluorite.tickFile += { tickEvent.call() }
    }

    fun function(name: String, func: McFunction.() -> Unit) = dp.function(name, func)


    fun getBlock(x: Int, y: Int, z: Int): Block {
        return Block(x, y, z)
    }

    fun getEntityList(options: Map<String, String> = mapOf()): EntityList<Entity> {
        return EntityList(options) { Entity() }
    }

    fun getPlayerList(options: Map<String, String> = mapOf()): EntityList<Player> {
        return EntityList(options) { Player() }
    }

    fun getEntityList(selector: Selector): EntityList<Entity> {
        return EntityList(selector) { Entity() }
    }

    fun getPlayerList(selector: Selector): EntityList<Player> {
        return EntityList(selector) { Player() }
    }

}
