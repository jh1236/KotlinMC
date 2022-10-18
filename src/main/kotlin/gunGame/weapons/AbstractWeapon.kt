package gunGame.weapons

import abstractions.PlayerTag
import structure.McFunction
import utils.Selector

abstract class AbstractWeapon(protected val damage: Int, var secondary: Boolean = false) {
    companion object {
        private val listOfWeapons = arrayListOf<AbstractWeapon>()
        var id = 0


        fun getWeaponById(id: Int): AbstractWeapon {
            return listOfWeapons.stream().filter { it.myId == id }.findAny().orElseThrow()
        }

        @Suppress("UNCHECKED_CAST")
        val allWeapons: List<AbstractWeapon>
            get() {
                return listOfWeapons.clone() as List<AbstractWeapon>
            }
    }

    init {
        // HACK: leaking this could be fixed with like a factory, but its easier to do this and this project isn't serious
        //  enough to justify a fix
        @Suppress("LeakingThis")
        listOfWeapons += this
    }

    protected val safeTag = PlayerTag("safe")
    protected val myId: Int = ++id


    abstract fun give(player: Selector);
}