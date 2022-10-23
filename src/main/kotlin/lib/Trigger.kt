package lib

import abstractions.asat
import abstractions.unless
import commands.Command
import gunGame.self
import structure.Fluorite
import structure.McFunction
import utils.Criteria
import utils.Selector
import utils.score.Objective
import utils.score.Score

class Trigger(name: String, selector: Selector, func: (Score) -> Unit) {
    companion object {
        private val directoryFunc = McFunction("trigger")

        init {
            Fluorite.tickFile += {
                Command.execute().asat('a'[""]).run(directoryFunc)
            }
        }
    }

    private val obj = Objective(name, Criteria.trigger)

    constructor(name: String, func: (Score) -> Unit) : this(name, 'a'[""], func)

    init {
        val triggerFunc = McFunction("trigger/$name") {
            func(obj[self])
        }
        directoryFunc += {
            Command.scoreboard().players.enable(obj[selector])
            Command.execute().unless(obj[self] eq 0).run(triggerFunc)
            obj[self] = 0
        }
    }
}