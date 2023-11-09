package gunGame.weapons

import abstractions.PlayerTag
import commands.Command
import enums.Entities
import lib.storeBlock
import structure.McFunction
import utils.Selector
import utils.get
import utils.rel

val secondSkinTag = PlayerTag("custom")

abstract class AbstractWeapon(
    val name: String,
    val damage: Int,
    var secondary: Boolean = false,
    var isReward: Boolean = false,
    val customModelData: Int = 0
) {
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


    val spawnFuncTemp = McFunction("$basePath/spawn_item_temp")
    val spawnFunc = McFunction("$basePath/spawn_item") {
        spawnFuncTemp()
        Command.tag('e'["type=item", "sort=nearest", "limit = 1"]).remove("temp")
        Command.tag('e'["type=text_display", "sort=nearest", "limit = 1"]).remove("temp")
    }

    protected fun setupInternal() {
        spawnFuncTemp.append {
            Command.loot().replace.block(storeBlock, "container.0").loot(lootTable)
            Command.execute().align("xyz").run.summon(
                Entities.ITEM,
                rel(.5, .25, .5),
                "{NoGravity:1b,Age:-32768,PickupDelay:32767,Tags:[\"temp\",\"edit\",\"safe\"],Item:{id:\"minecraft:dirt\",Count:1b}}"
            )
            'e'["tag = edit", "limit = 1"].data["Item"] = storeBlock.data["Items[0]"]
            Command.tag('e'["tag = edit"]).remove("edit")
            Command.execute().align("xyz").run.summon(
                Entities.TEXT_DISPLAY,
                rel(0.5, 1.0, 0.5),
                "{billboard:\"center\",default_background:1b,Tags:[\"hg\", temp],text:'{\"text\":\"${
                    name.replace(
                        "\'",
                        "\\\'"
                    )
                }\"}'}"
            )
        }
    }

    var lootTable: String = ""


    init {
        // HACK: leaking this could be fixed with like a factory, but its easier to do this and this project isn't serious
        //  enough to justify a fix
        @Suppress("LeakingThis")
        listOfWeapons += this
    }

    val basePath
        get() = "jh1236:weapons/${if (isReward) "reward" else if (secondary) "secondary" else "primary"}/${
            name.replace(
                " ",
                "_"
            )
        }"
    protected val safeTag = PlayerTag("safe")
    val myId: Int = ++id


    fun give(player: Selector) {
        Command.loot().give(player).loot(lootTable)
    }


}