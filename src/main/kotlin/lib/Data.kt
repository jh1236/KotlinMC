package lib

import abstractions.variables.IVariableProvider
import abstractions.variables.NBTTypes
import commands.Command
import gunGame.self
import internal.ITellrawable
import internal.commands.BaseCommand
import internal.commands.impl.Data
import internal.commands.impl.execute.Execute
import utils.Vec3
import utils.abs
import utils.typeInterfaces.IData
import utils.typeInterfaces.IDataReceiver

val storeBlock = abs(12360, -64, 0)
fun applyNbtToHeldItem(nbt: String) {
    Command.item().replace.block(storeBlock, "container.0").from.entity(self, "weapon.mainhand")
    Command.data().modify.block(storeBlock, "Items[0].tag").merge.value(nbt)
    Command.item().replace.entity(self, "weapon.mainhand").from.block(storeBlock, "container.0")
}

class ItemData(val blockPath: Vec3<*>) : IDataReceiver, ITellrawable {
    override fun get(path: String, scale: Double): IData {
        return object : IData() {
            override fun addToExecuteIf(ex: Execute) {
                ex.data.block(blockPath, "Items[0].$path")
            }

            override fun asTellraw(): Any {
                return object {
                    val nbt = "Items[0].$path"
                    val block = "12360 -64 0"
                }
            }

            override fun completeDataCommand(data: Data) {
                data.block(blockPath, "Items[0].$path")
            }

            override fun get(scale: Double): BaseCommand {
                return Command.data().get.block(blockPath, "Items[0].$path", scale)
            }


        }
    }

    override operator fun set(
        path: String,
        type: NBTTypes,
        scale: Double,
        provider: IVariableProvider
    ) {
        Command.execute().store.result.block(blockPath, "Items[0].$path", type, scale).run { provider.get() }

    }

    override fun completeDataCommand(data: Data, path: String, scale: Double): Data {
        return data.block(blockPath, "Items[0].$path")
    }

    override fun setFromFunction(path: String, type: NBTTypes, scale: Double, provider: () -> Unit) {
        Command.execute().store.result.block(blockPath, "Items[0].$path", type, scale).run(provider)
    }

    override fun asTellraw(): Any {
        return object {
            val nbt = "Items[0]"
            val block = "12360 -64 0"
        }
    }


}

fun copyItemfromSlotAndRun(slot: String, func: (ItemData) -> Unit) {
    Command.item().replace.block(storeBlock, "container.0").from.entity(self, slot)
    func(ItemData(storeBlock))
    Command.item().replace.entity(self, slot).from.block(storeBlock, "container.0")
}

fun copyHeldItemToBlockAndRun(func: (ItemData) -> Unit) {
    Command.item().replace.block(storeBlock, "container.0").from.entity(self, "weapon.mainhand")
    func(ItemData(storeBlock))
    Command.item().replace.entity(self, "weapon.mainhand").from.block(storeBlock, "container.0")
}