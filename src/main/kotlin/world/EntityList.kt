package world

import abstractions.asat
import commands.Command

import utils.Selector
import utils.typeInterfaces.ISelector

class EntityList<out T : Entity> : IMcObject, ISelector {
    private val selector: Selector
    private val createT: () -> T

    constructor(selector: Selector, createT: () -> T) {
        this.selector = selector
        this.createT = createT
    }

    constructor(options: Map<String, String>, createT: () -> T) {
        this.createT = createT
        if (options.isEmpty()) {
            selector = Selector(createT().selectorChar)
        } else {
            var selectorString = "@${createT().selectorChar}["
            for ((k, v) in options) {
                selectorString += "$k = $v,"
            }
            selectorString.replace(".$", "]")
            selector = Selector(selectorString)
        }
    }

    fun forEach(func: (T) -> Unit) {
        Command.execute().asat(selector).runAll { func(createT()) }
    }

    override fun print() {
        Command.tellraw(Selector('a'), selector.asTellraw())
    }

    override val selectorString: String
        get() = selector.selectorString

    override fun addArgs(key: String, value: String): ISelector {
        return selector.addArgs(key, value)
    }


}