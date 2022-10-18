package lib

import abstractions.flow.Do
import abstractions.flow.If
import abstractions.unless
import commands.Command
import enums.Blocks
import enums.Entities
import enums.blockTag
import gunGame.asIntersects
import structure.Fluorite
import utils.Selector
import utils.loc
import utils.rel
import utils.score.Score

fun raycast(change: Float, forEach: (Score) -> Unit = {}, onHit: () -> Unit = {}, count: Int = 1000) {
    val range = Fluorite.getNewFakeScore("count", count)
    Do {
        forEach(range)
        If((rel() isBlock blockTag("jh1236:air")).not()) {
            range.set(0)
            onHit()
        }
    }.moved(loc(0, 0, change)).While(range gte 0)
}

val range = Fluorite.getNewFakeScore("count")
fun raycastEntity(
    change: Float,
    forEach: (Score) -> Unit = {},
    onHit: () -> Unit = {},
    count: Int = 1000,
    onWallHit: (() -> Unit)? = null
) {
    range.set(count)
    with(Command) {
        Do {
            forEach(range)
            execute().unless(rel() isBlock blockTag("jh1236:air")).run {
                onWallHit?.let { it() }
                range.set(0)
            }
            If(rel() isBlock Blocks.REDSTONE_BLOCK) {
                summon(Entities.MARKER, loc(0, 0, -.25), "{Tags:[particle]}")
            }
            execute().asIntersects(Selector('e')).run(onHit)
            range -= 1
        }.moved(loc(0, 0, change)).While(range gte 0)
    }
}
