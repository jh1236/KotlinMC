package gunGame

import abstractions.PlayerTag
import abstractions.Trigger
import abstractions.flow.If
import abstractions.flow.Ifv2
import abstractions.hasTag
import abstractions.notHasTag
import abstractions.schedule.Sleep
import abstractions.score.Criteria
import abstractions.score.Objective
import commands.Command
import enums.*
import events.ScoreEventManager
import gunGame.maps.addShop
import gunGame.maps.spawnSetup
import gunGame.weapons.*
import gunGame.weapons.impl.loadExperiments
import gunGame.weapons.impl.loadFun
import gunGame.weapons.impl.loadPrimaries
import gunGame.weapons.impl.loadSecondaries
import internal.versionManagement.Version
import lib.applyNbtToHeldItem
import lib.copyItemfromSlotAndRun
import lib.debug.Debug
import lib.debug.Log
import lib.random.Random
import structure.*
import utils.*

val dp = Datapack("Gun Game Experimental", "jh1236", Version.VERSION_1_19)
val self = Selector('s')
val allGear = McFunction("all_gear")
val lagTag = PlayerTag("lag")
val idScore = Objective("id")
val dummyTag = PlayerTag("dummy")
val regenRandom: McFunction = McFunction("weapons/randomise")
val primary1 = Fluorite.getNewFakeScore("primary1")
val primary2 = Fluorite.getNewFakeScore("primary2")
val primary3 = Fluorite.getNewFakeScore("primary3")
val primary4 = Fluorite.getNewFakeScore("primary4")
val secondary1 = Fluorite.getNewFakeScore("secondary1")
val secondary2 = Fluorite.getNewFakeScore("secondary2")
val secondary3 = Fluorite.getNewFakeScore("secondary3")
val secondary4 = Fluorite.getNewFakeScore("secondary4")


fun main() {
//    If.setIf(Ifv2)
    ExternalFile(
        "G:/Programming/kotlin/GunGame/src/main/resources/air.json",
        "data/jh1236/tags/blocks/air.json"
    )
    ExternalFile(
        "G:/Programming/kotlin/GunGame/src/main/resources/sneaking.json",
        "data/jh1236/predicates/sneaking.json"
    )
    ExternalFile(
        "G:/Programming/kotlin/GunGame/src/main/resources/damage.json",
        "data/jh1236/damage_type/shot.json"
    )
    TagFile(TagFile.TagType.DAMAGE_TYPE, "minecraft:bypasses_cooldown").add("jh1236:shot")

    Log.logLevel = Log.TRACE
    If.setIf(Ifv2)
    Debug.debugMode = true
    addDebug()

    Fluorite.tickFile += ::coolDownTick
    Fluorite.tickFile += ::ammoDisplayTick
    Fluorite.tickFile += ::mcMain
    Fluorite.loadFile += ::mcLoad
    coasSetup()
    coolDownSetup()

    loadPrimaries()
    loadSecondaries()
    loadFun()
    loadExperiments()
    spawnSetup()
    Fluorite.tickFile += ::healthTick

    McFunction("jh1236:other/dummy") {
        Command.execute().summon(Entities.ARMOR_STAND).run {
            playingTag.add(self)
            health[self] = 100000000
            dummyTag.add(self)
        }
    }


    val isDooring = Fluorite.getNewFakeScore("door")
    Fluorite.loadFile += {
        isDooring.set(0)
    }
    McFunction("other/open_door") {
        If(isDooring eq 0) {
            isDooring.set(1)
            Sleep(Duration(5), keepContext = false)
            rel(0, 1, 0).data["{}"] = "{Page:0}"
            Command.fill(abs(-92, 15, 11), abs(-92, 14, 10), Blocks.AIR)
            Command.setblock(abs(-91, 14, 10), Blocks.BROWN_CARPET)
            Sleep(Duration(40), keepContext = false)
            Command.raw(
                """setblock -91 14 10 minecraft:lectern[facing=east,has_book=true,powered=false]{Book:{Count:1b,id:"minecraft:written_book",tag:{author:"Jh1236",filtered_title:"Lorem Ipsum",pages:['{"text":"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean non lacus ac felis semper tempor ut vitae leo. Duis nisl neque, condimentum at gravida ut, iaculis eu ipsum. Etiam quis arcu quis ligula dapibus porttitor. Quisque tempor quam vel felis condimentum, ac eleifend tellus ornare."}','{"text":"Phasellus ex neque, fermentum sed turpis et, sodales efficitur libero. Donec dapibus aliquam tellus nec accumsan. Nullam sit amet cursus enim. Pellentesque non orci ac erat rutrum bibendum. Pellentesque cursus sodales libero eu suscipit. Donec nec quam odio."}','{"text":"Aenean at quam vitae odio placerat placerat ut in tortor. Proin id lorem elit. Phasellus tincidunt, justo nec gravida vestibulum, neque libero vulputate eros, sit amet hendrerit ex augue vel eros. Quisque ac libero eu arcu dapibus sodales sed ac quam."}','{"text":"Open Seasame"}','{"text":"Vestibulum scelerisque elementum tortor, in dignissim ante. Aliquam augue sapien, iaculis eu ultricies eu, hendrerit in mi. Phasellus congue eros non fermentum eleifend. Nam ac mattis purus, vel auctor enim. "}'],resolved:1b,title:"Lorem Ipsum"}},Page:0}"""
            )
            Command.fill(abs(-92, 15, 11), abs(-92, 14, 10), Blocks.RED_MUSHROOM_BLOCK)
            isDooring.set(0)
        }
    }

    McFunction("particles/dagger") {
        for (i in 1..5) {
            Command.particle(Particles.DUST(.3, .3, .3, .5), loc(0, 0, i / 10.0), 0, 0, 0, 0, 0)
        }
        for (i in 0..2) {
            Command.particle(Particles.DUST(.15, .15, .15, .5), loc(0, 0, -i / 10.0), 0, 0, 0, 0, 0)
            Command.particle(Particles.DUST(.15, .15, .15, .5), loc(0, i / 12.0, 0), 0, 0, 0, 0, 0)
            Command.particle(Particles.DUST(.15, .15, .15, .5), loc(0, -i / 12.0, 0), 0, 0, 0, 0, 0)
        }
    }

    McFunction("jh1236:other/visualise") {
        for (x in -5..5) {
            for (y in -5..5) {
                for (z in -5..5) {
                    Command.execute().align("xyz").positioned(rel(x + .5, y + .5, z + .5))
                        .If(rel() isBlock Blocks.STRUCTURE_VOID).run.particle(
//                        Particles.DUST(0.2, 0.7, 1.0, 1.0),
                            Particles.BLOCK_MARKER(Blocks.STRUCTURE_VOID),
                            rel(),
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            1
                        ).force(self)

                }
            }
        }
    }

    McFunction("jh1236:other/jump/small") {
        Command.effect().give(self, Effects.LEVITATION, 1, 20)
        Sleep(Duration(5))
        Command.effect().clear(self, Effects.LEVITATION)
    }

    McFunction("jh1236:other/jump/medium") {
        Command.effect().give(self, Effects.LEVITATION, 1, 30)
        Sleep(Duration(5))
        Command.effect().clear(self, Effects.LEVITATION)
    }
    McFunction("jh1236:other/jump/big") {
        Command.effect().give(self, Effects.LEVITATION, 1, 55)
        Sleep(Duration(5))
        Command.effect().clear(self, Effects.LEVITATION)
    }


    Trigger("map", 'a'[""]) {
        respawnFunc()
        Command.tp(self, abs(-15.5, 3.25, -17.5))
    }


    addShop()


    allGear.append {
        playingTag.add(self)
        AbstractWeapon.allWeapons.forEach { it.give(self) }
    }

    regenRandom.append {
        Command.kill('e'["tag = temp"])
        val primaryCount = AbstractWeapon.allWeapons.stream().filter { !it.secondary && !it.isReward }.count().toInt()
        val secondaryCount = AbstractWeapon.allWeapons.stream().filter { it.secondary && !it.isReward }.count().toInt()


        primary1.set(Random.next(primaryCount))
        primary2.set(Random.next(primaryCount-1))
        If(primary2 gte primary1) {
            primary2 += 1
        }
        If (primary2 lt primary1) {
            primary2.swap(primary1)
        }
        primary3.set(Random.next(primaryCount-2))
        If(primary3 gte primary1) {
            primary3 += 1
        }
        If(primary3 gte primary2) {
            primary3 += 1
        }
        If (primary3 lt primary2) {
            primary3.swap(primary2)
        }
        If (primary2 lt primary1) {
            primary2.swap(primary1)
        }
        primary4.set(Random.next(primaryCount-3))
        If(primary4 gte primary1) {
            primary4 += 1
        }
        If(primary4 gte primary2) {
            primary4 += 1
        }
        If(primary4 gte primary3) {
            primary4 += 1
        }
        secondary1.set(Random.next(secondaryCount))
        secondary2.set(Random.next(secondaryCount-1))
        If(secondary2 gte secondary1) {
            secondary2 += 1
        }
        If (secondary2 lt secondary1) {
            secondary2.swap(secondary1)
        }
        secondary3.set(Random.next(secondaryCount-2))
        If(secondary3 gte secondary1) {
            secondary3 += 1
        }
        If(secondary3 gte secondary2) {
            secondary3 += 1
        }
        If (secondary3 lt secondary2) {
            secondary3.swap(secondary2)
        }
        If (secondary2 lt secondary1) {
            secondary2.swap(secondary1)
        }
        secondary4.set(Random.next(secondaryCount-3))
        If(secondary4 gte secondary1) {
            secondary4 += 1
        }
        If(secondary4 gte secondary2) {
            secondary4 += 1
        }
        If(secondary4 gte secondary3) {
            secondary4 += 1
        }
        
        primary1 += 1
        primary2 += 1
        primary3 += 1
        primary4 += 1
        secondary1 += 1
        secondary2 += 1
        secondary3 += 1
        secondary4 += 1
        Command.execute().positioned(abs(-59, 18, -66)).run {
            summonPrimary(primary1)
        }
        Command.execute().positioned(abs(-59, 18, -63)).run {
            summonPrimary(primary2)
        }
        Command.execute().positioned(abs(-59, 18, -60)).run {
            summonPrimary(primary3)
        }
        Command.execute().positioned(abs(-59, 18, -57)).run {
            summonPrimary(primary4)
        }
        Command.execute().positioned(abs(-69, 18, -66)).run {
            summonSecondary(secondary1)
        }
        Command.execute().positioned(abs(-69, 18, -63)).run {
            summonSecondary(secondary2)
        }
        Command.execute().positioned(abs(-69, 18, -60)).run {
            summonSecondary(secondary3)
        }
        Command.execute().positioned(abs(-69, 18, -57)).run {
            summonSecondary(secondary4)
        }
    }

    //G:/Games/Minecraft Servers/Gun Server/world/datapacks
    //C:/Users/Jared Healy/AppData/Roaming/.minecraft/saves/Datapack Testing/datapacks
    //G:/Games/Minecraft Servers/1.18 test/world/datapacks
    //s

    dp.write(
        "G:/Games/Minecraft Servers/Gun Server/world/datapacks",
        "Datapack Handling guns etc", delete = true
    )
    dp.write(
        "G:/Games/Minecraft Servers/build server/world/datapacks",
        "Datapack Handling guns etc", delete = true
    )
}


fun mcLoad() {
    Command.execute().unless(idScore["%system"] eq idScore["%system"]).run {
        idScore["%system"] = 0
    }
    Command.forceload().add(Vec2(12360, 0))
    Command.setblock(abs(12360, -64, 0), Blocks.YELLOW_SHULKER_BOX)
}

fun coasSetup() {
    val coasManager = ScoreEventManager(Criteria.useItem(Items.CARROT_ON_A_STICK))
    AbstractCoasWeapon.setCoasFunction(coasManager)
    coasManager += {
        AbstractCoasWeapon.currentId.set(
            self.data["SelectedItem.tag.jh1236.weapon"].get()
        )
    }
}


fun mcMain() {
    Command.execute().asat(Selector('a')).run {
        If((idScore[self] eq idScore[self]).not()) {
            idScore[self].set { idScore["%system"] += 1 }
        }
    }
    Command.effect().give('a'["tag =! noSpeed"].hasTag(playingTag), Effects.SPEED, 10, 1, true)
    Command.effect().give('a'["tag =! noSpeed"].notHasTag(playingTag), Effects.SPEED, 10, 3, true)
    Command.effect().give('a'[""], Effects.SATURATION, 10, 100, true)
    Command.effect().give('a'[""], Effects.WATER_BREATHING, 10, 100, true)
    Command.effect().give('a'[""].hasTag(playingTag), Effects.RESISTANCE, 10, 100, true)

    //TODO: convert properly
    Command.raw("execute as @a[tag = !$playingTag, predicate = jh1236:sneaking] at @s if block ~ ~-.25 ~ waxed_oxidized_copper positioned ~ 0 ~ if entity @s[dy = 100] run tp @s ~ ~-2 ~")
    Command.raw("execute as @a[tag = !$playingTag, predicate = jh1236:sneaking] at @s if block ~ ~-.25 ~ waxed_oxidized_copper positioned ~ 0 ~ unless entity @s[dy = 100] run tp @s ~ ~4 ~")
    Command.execute().asat('e'["type = item", "tag = !safe"]).If(self.data["Item.tag.jh1236.weapon"])
        .If('a'[""].hasTag(playingTag)).run {
            Command.tag(self).add("safe")
            self.data["{}"] = "{PickupDelay:0s}"
        }
    Command.kill('e'["type = item, tag =! safe"])

}
