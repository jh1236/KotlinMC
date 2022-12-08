package gunGame

import abstractions.If
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
import gunGame.weapons.impl.loadExperiments
import gunGame.weapons.impl.loadPrimaries
import lib.Trigger
import lib.debug.Debug
import lib.debug.Log
import lib.get
import lib.getId
import structure.Datapack
import structure.ExternalFile
import structure.Fluorite
import structure.McFunction
import utils.*

val dp = Datapack("Gun Game Experimental", "jh1236")
val self = Selector('s')
val allGear = McFunction("all_gear")

fun main() {
    ExternalFile(
        "G:/Programming/kotlin/KotlinMc/src/main/resources/air.json",
        "data/jh1236/tags/blocks/air.json"
    )
    ExternalFile(
        "G:/Programming/kotlin/KotlinMc/src/main/resources/sneaking.json",
        "data/jh1236/predicates/sneaking.json"
    )

    Log.logLevel = Log.TRACE
    Debug.debugMode = false
    addDebug()

    coasSetup()
    coolDownSetup()

    loadPrimaries()
    loadSecondaries()
    loadFun()
    loadExperiments()
    spawnSetup()

    McFunction("other/open_door") {
        rel(0, 1, 0).data["{}"] = "{Page:0}"
        Command.fill(abs(-92, 15, 11), abs(-92, 14, 10), Blocks.AIR)
        Command.setblock(abs(-91, 14, 10), Blocks.BROWN_CARPET)
        Command.schedule().function("40t") {
            Command.raw(
                """setblock -91 14 10 minecraft:lectern[facing=east,has_book=true,powered=false]{Book:{Count:1b,id:"minecraft:written_book",tag:{author:"Jh1236",filtered_title:"Lorem Ipsum",pages:['{"text":"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean non lacus ac felis semper tempor ut vitae leo. Duis nisl neque, condimentum at gravida ut, iaculis eu ipsum. Etiam quis arcu quis ligula dapibus porttitor. Quisque tempor quam vel felis condimentum, ac eleifend tellus ornare."}','{"text":"Phasellus ex neque, fermentum sed turpis et, sodales efficitur libero. Donec dapibus aliquam tellus nec accumsan. Nullam sit amet cursus enim. Pellentesque non orci ac erat rutrum bibendum. Pellentesque cursus sodales libero eu suscipit. Donec nec quam odio."}','{"text":"Aenean at quam vitae odio placerat placerat ut in tortor. Proin id lorem elit. Phasellus tincidunt, justo nec gravida vestibulum, neque libero vulputate eros, sit amet hendrerit ex augue vel eros. Quisque ac libero eu arcu dapibus sodales sed ac quam."}','{"text":"Open Seasame"}','{"text":"Vestibulum scelerisque elementum tortor, in dignissim ante. Aliquam augue sapien, iaculis eu ultricies eu, hendrerit in mi. Phasellus congue eros non fermentum eleifend. Nam ac mattis purus, vel auctor enim. "}'],resolved:1b,title:"Lorem Ipsum"}},Page:0}"""
            )
            Command.fill(abs(-92, 15, 11), abs(-92, 14, 10), Blocks.RED_MUSHROOM_BLOCK)
        }

    }


    Trigger("map", Selector("Jh1236")) { Fluorite.reuseFakeScore("map").set(it) }

    Fluorite.tickFile += ::healthTick
    Fluorite.tickFile += ::coolDownTick
    Fluorite.tickFile += ::ammoDisplayTick
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
        "Datapack Handling guns etc", delete = true
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
    Command.execute().asat('e'["type = item", "tag = !safe"]).If(self.data["Item.tag.jh1236.weapon"]).If('a'[""].hasTag(playingTag)).run {
        Command.tag(self).add("safe")
        self.data["{}"] = "{PickupDelay:0s}"
    }
    Command.kill('e'["type = item, tag =! safe"])

}
