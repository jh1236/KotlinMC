package gunGame

import lib.Trigger
import lib.debug.Debug

fun addDebug() {
    if (!Debug.debugMode) return
    Trigger("damage") {
        damageSelf(it)
    }
    Trigger("all_gear") {
        allGear()
    }
}