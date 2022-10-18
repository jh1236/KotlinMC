package events

import internal.commands.BaseCommand
import structure.McFunction

open class EventManager(function: McFunction) {

    protected val eventFunc: McFunction = function

    constructor(eventName: String) : this(McFunction(eventName))

    fun call() {
        eventFunc()
    }

    operator fun plusAssign(other: McFunction) {
        eventFunc += { other() }
    }

    operator fun plusAssign(other: () -> Unit) {
        eventFunc += other
    }

    operator fun plusAssign(other: BaseCommand) {
        eventFunc += other
    }
}