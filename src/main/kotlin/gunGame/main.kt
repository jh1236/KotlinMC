package gunGame

import abstractions.asat
import abstractions.hasTag
import abstractions.notHasTag
import commands.Command
import enums.Blocks
import enums.Effects
import enums.Items
import events.ScoreEventManager
import gunGame.maps.spawnSetup
import gunGame.weapons.*
import gunGame.weapons.primary.loadPrimaries
import lib.Trigger
import lib.debug.Debug
import lib.debug.Log
import lib.get
import lib.getId
import structure.Datapack
import structure.ExternalFile
import structure.Fluorite
import structure.McFunction
import utils.Criteria
import utils.Selector
import utils.Vec2
import utils.abs

val dp = Datapack("Gun Game", "jh1236")
val self = Selector('s')
val allGear = McFunction("all_gear")

fun main() {
    ExternalFile(
        "G:/Games/Minecraft Servers/Gun Server/world/datapacks/backup/packle/data/jh1236/tags/blocks/air.json",
        "data/jh1236/tags/blocks/air.json"
    )
    ExternalFile(
        "G:/Games/Minecraft Servers/Gun Server/world/datapacks/backup/packle/data/jh1236/predicates/sneaking.json",
        "data/jh1236/predicates/sneaking.json"
    )

    Log.logLevel = Log.TRACE
    Debug.debugMode = true
    addDebug()

    coasSetup()
    coolDownSetup()

    loadPrimaries()
    loadSecondaries()


    spawnSetup()


    Trigger("map", Selector("Jh1236")) { Fluorite.reuseFakeScore("map").set(it) }

    Fluorite.tickFile += ::healthTick
    Fluorite.tickFile += ::coolDownTick
    Fluorite.tickFile += ::mcMain
    Fluorite.loadFile += ::mcLoad

    allGear.append {
        playingTag.add(self)
        AbstractWeapon.allWeapons.forEach { it.give(self) }
    }


    //G:/Games/Minecraft Servers/Gun Server/world/datapacks
    //C:/Users/Jared Healy/AppData/Roaming/.minecraft/saves/Datapack Testing/datapacks
    //G:/Games/Minecraft Servers/1.18 test/world/datapacks
    //s

    dp.write(
        "G:/Games/Minecraft Servers/Gun Server/world/datapacks",
        "Datapack Handling guns etc",
    )
}


fun mcLoad() {
    Command.forceload().add(Vec2(12360, 0))
    Command.setblock(abs(12360, -64, 0), Blocks.YELLOW_SHULKER_BOX)
}

fun coasSetup() {
    val coasManager = ScoreEventManager(Criteria.useItem(Items.CARROT_ON_A_STICK))
    AbstractCoasWeapon.setCoasFunction(coasManager)
    coasManager += {
        AbstractCoasWeapon.currentId.set(
            Command.data().get.entity(
                self,
                "SelectedItem.tag.jh1236.weapon"
            )
        )
    }
}


fun mcMain() {
    Command.execute().asat(Selector('a')).run {
        getId()
    }
    Command.effect().give('a'["tag =! noSpeed"].hasTag(playingTag), Effects.SPEED, 10, 1, true)
    Command.effect().give('a'["tag =! noSpeed"].notHasTag(playingTag), Effects.SPEED, 10, 3, true)
    Command.effect().give('a'[""].hasTag(playingTag), Effects.SATURATION, 10, 100, true)
    Command.effect().give('a'[""].hasTag(playingTag), Effects.RESISTANCE, 10, 100, true)

    //TODO: convert properly
    Command.raw("execute as @a[tag = !$playingTag, predicate = jh1236:sneaking] at @s if block ~ ~-.25 ~ waxed_oxidized_copper positioned ~ 0 ~ if entity @s[dy = 100] run tp @s ~ ~-2 ~")
    Command.raw("execute as @a[tag = !$playingTag, predicate = jh1236:sneaking] at @s if block ~ ~-.25 ~ waxed_oxidized_copper positioned ~ 0 ~ unless entity @s[dy = 100] run tp @s ~ ~4 ~")
    Command.raw("execute as @e[tag = block] run data merge entity @s {Time: -10000}")
    Command.kill('e'["type = item, tag =! safe"])

}
