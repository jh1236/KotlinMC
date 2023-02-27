package lib

import commands.Command
import gunGame.self
import internal.DataProvider
import utils.abs

val storeBlock = abs(12360, -64, 0)
fun applyNbtToHeldItem(nbt: String) {
    Command.item().replace.block(storeBlock, "container.0").from.entity(self, "weapon.mainhand")
    Command.data().modify.block(storeBlock, "Items[0].tag").merge.value(nbt)
    Command.item().replace.entity(self, "weapon.mainhand").from.block(storeBlock, "container.0")
}

fun copyItemfromSlotAndRun(slot: String, func: (DataProvider) -> Unit) {
    Command.item().replace.block(storeBlock, "container.0").from.entity(self, slot)
    func(DataProvider("block $storeBlock Items[0]."))
    Command.item().replace.entity(self, slot).from.block(storeBlock, "container.0")
}

fun copyHeldItemToBlockAndRun(func: (DataProvider) -> Unit) {
    Command.item().replace.block(storeBlock, "container.0").from.entity(self, "weapon.mainhand")
    func(DataProvider("block $storeBlock Items[0]."))
    Command.item().replace.entity(self, "weapon.mainhand").from.block(storeBlock, "container.0")
}