package lib

import commands.Command
import gunGame.self
import utils.Data
import internal.DataProvider
import utils.abs

private class ItemData(blank: Boolean = false) : DataProvider("block $storeBlock Items[0].") {
    override val sourceString: String = if (blank) {
        super.sourceString.substring(0..super.sourceString.length - 2)
    } else {
        super.sourceString
    }

    override fun get(path: String, scale: Number): Data {
        if (path.trim().isBlank()) {
            return Data(ItemData(true), path)
        }
        return super.get(path, scale)
    }

}

val storeBlock = abs(12360, -64, 0)
fun applyNbtToHeldItem(nbt: String) {
    Command.item().replace.block(storeBlock, "container.0").from.entity(self, "weapon.mainhand")
    Command.data().modify.block(storeBlock, "Items[0].tag").merge.value(nbt)
    Command.item().replace.entity(self, "weapon.mainhand").from.block(storeBlock, "container.0")
}

fun copyItemfromSlotAndRun(slot: String, func: (DataProvider) -> Unit) {
    Command.item().replace.block(storeBlock, "container.0").from.entity(self, slot)
    func(ItemData())
    Command.item().replace.entity(self, slot).from.block(storeBlock, "container.0")
}

fun copyHeldItemToBlockAndRun(func: (DataProvider) -> Unit) {
    Command.item().replace.block(storeBlock, "container.0").from.entity(self, "weapon.mainhand")
    func(ItemData())
    Command.item().replace.entity(self, "weapon.mainhand").from.block(storeBlock, "container.0")
}