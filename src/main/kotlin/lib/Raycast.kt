package lib

import abstractions.flow.Do
import abstractions.flow.If
import abstractions.score.Score
import commands.Command
import gunGame.asIntersects
import structure.Fluorite
import utils.Selector
import utils.loc

val rangeScore = Fluorite.getNewFakeScore("count")

fun raycast(change: Float, forEach: (Score) -> Unit = {}, onHit: () -> Unit = {}, count: Int = -1) {
    if (count >= 0) {
        rangeScore.set(count)
    }
    Command.execute().positioned(loc(0, 0, 0.25)).run {
        Do {
            forEach(rangeScore)
            If(rangeScore eq 0) {
                onHit()
            }
            val a = doesCollide()
            If(a eq 1) {
                rangeScore.set(0)
                onHit()
            }
            rangeScore -= 1
        }.moved(loc(0, 0, change)).While(rangeScore gte 0)
    }
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
        execute().positioned(loc(0, 0, 0.25)).run {
            Do {
                forEach(rangeScore)
                If(rangeScore eq 0) {
                    onWallHit?.let { it() }
                }
                If(doesCollide() eq 1) {
                    rangeScore.set(0)
                    onWallHit?.let { it() }
                }
                execute().asIntersects(Selector('e')).run(onHit)
                rangeScore -= 1
            }.moved(loc(0, 0, change)).While(rangeScore gte 0)
        }
    }
}
