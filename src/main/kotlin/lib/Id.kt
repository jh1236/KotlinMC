package lib

import abstractions.flow.If
import internal.commands.impl.execute.Execute
import internal.conditionals.Conditional
import utils.Selector
import utils.score.Objective
import world.Entity

val idScore = Objective("jh.id")
var system = idScore["%system"]

fun getId() {
    val myId = idScore[Selector('s')]
    If().not(myId eq myId) {
        myId.set { system += 1 }
        myId.set(10)
    }
}

infix fun Entity.idEq(other: Entity): IdMatchConditional {
    return IdMatchConditional(this, other)
}

class IdMatchConditional(private val entity: Entity, private val other: Entity) : Conditional() {
    override fun addToExecuteIf(ex: Execute) {
        ex.score(idScore[entity]).isEqual(idScore[other])
    }
}