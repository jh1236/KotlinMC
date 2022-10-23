package lib

import utils.Vec3

fun parametric(func: (Double) -> Vec3<*>, closedRange: ClosedRange<Double>, step: Double): Iterator<Vec3<*>> {
    var i = closedRange.start
    return object : AbstractIterator<Vec3<*>>() {
        override fun computeNext() {
            if (i > closedRange.endInclusive) {
                done()
            }
            setNext(func(i))
            i += step
        }
    }
}