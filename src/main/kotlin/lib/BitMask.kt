package lib

import abstractions.score.Score
import structure.Fluorite
import kotlin.math.pow

class BitMask {
    private val score = Fluorite.getNewFakeScore("bitmask")

    operator fun set(idx: Int, set: Boolean) {
        if (set) {
            score += 2.0.pow(idx.toDouble()).toInt()
        } else {
            score -= 2.0.pow(idx.toDouble()).toInt()
        }
    }

    fun reset(idx: Int) {
        score += 2.0.pow(idx.toDouble()).toInt()
    }

    operator fun get(idx: Int): Score {
        return score.rem(idx) rshift idx
    }
}