package lib

import abstractions.flow.Do
import abstractions.flow.If
import abstractions.unless
import commands.Command
import enums.Blocks
import enums.Entities
import enums.blockTag
import gunGame.asIntersects
import lib.debug.Log
import structure.Fluorite
import utils.Selector
import utils.loc
import utils.rel
import utils.score.Score

val rangeScore = Fluorite.getNewFakeScore("count")

fun raycast(change: Float, forEach: (Score) -> Unit = {}, onHit: () -> Unit = {}, count: Int = -1) {
    if (count >= 0) {
        rangeScore.set(count)
    }
    Do {
        forEach(rangeScore)
        If(rangeScore eq 0) {
            onHit()
        }
        If((rel() isBlock blockTag("jh1236:air")).not()) {
            rangeScore.set(0)
            onHit()
        }
        rangeScore -= 1
    }.moved(loc(0, 0, change)).While(rangeScore gte 0)
}

fun raycastEntity(
    change: Float,
    forEach: (Score) -> Unit = {},
    onHit: () -> Unit = {},
    count: Int = -1,
    onWallHit: (() -> Unit)? = null
) {
    if (count >= 0) {
        rangeScore.set(count)
    }
    with(Command) {
        Do {
            forEach(rangeScore)
            If(rangeScore eq 0) {
                onWallHit?.let { it() }
            }
            execute().unless(rel() isBlock blockTag("jh1236:air")).run {
                onWallHit?.let { it() }
                rangeScore.set(0)
            }
            If(rel() isBlock Blocks.REDSTONE_BLOCK) {
                summon(Entities.MARKER, loc(0, 0, -.25), "{Tags:[particle]}")
            }
            execute().asIntersects(Selector('e')).run(onHit)
            rangeScore -= 1
        }.moved(loc(0, 0, change)).While(rangeScore gte 0)
    }
}
