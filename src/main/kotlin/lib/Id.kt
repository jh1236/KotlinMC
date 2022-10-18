package lib

import abstractions.flow.If
import gunGame.self
import utils.score.Objective

val idScore = Objective("jh.id")
var system = idScore["%system"]

fun getId() {
    val myId = idScore[self]
    If((myId eq myId).not()) {
        myId.set { system += 1 }
    }
}

