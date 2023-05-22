package events

import abstractions.score.Criteria
import abstractions.score.Objective
import commands.Command
import gunGame.self
import structure.Fluorite
import utils.Selector

class ScoreEventManager(scoreType: Criteria, selector: Selector = Selector('a')) :
    EventManager("${scoreType.uniqueName}_${++count}") {
    companion object {
        var count = -1
    }


    val obj = Objective("${scoreType.uniqueName}_$count", scoreType)

    init {
        Fluorite.tickFile += {
            Command.execute().asat(selector).If(obj[Selector('s')] gte 1).run {
                this.call()
            }
        }
        this += { obj[self] = 0 }
    }
}

