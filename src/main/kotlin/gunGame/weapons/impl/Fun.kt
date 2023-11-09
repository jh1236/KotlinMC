package gunGame.weapons.impl

import abstractions.Trigger
import abstractions.flow.If
import abstractions.flow.Switch
import abstractions.flow.trees.ScoreTree
import abstractions.schedule.Sleep
import abstractions.score.Objective
import abstractions.variables.NBTTypes
import commands.Command
import enums.Anchor
import enums.Items
import enums.Particles
import gunGame.self
import gunGame.streak
import gunGame.weapons.RaycastBuilder
import gunGame.weapons.RaycastWeapon
import gunGame.weapons.shootTag
import lib.debug.Log
import lib.random.Random
import structure.McFunction
import utils.Duration
import utils.Vec2
import utils.get
import utils.rel

lateinit var weezer: RaycastWeapon
lateinit var danceGun: RaycastWeapon
val handleStreak = McFunction("jh1236:health/streak")
val song = Objective("song")

val songs = listOf(
    "custom.gangnum" to 4 * 60 + 8,
    "custom.breakeven" to 262,
    "custom.fallen_kingdom" to 4 * 60 + 10,
    "custom.groove" to 5 * 60 + 40,
    "custom.iris" to 4 * 60 + 50,
    "custom.levels" to 1 * 60 + 32,
    "custom.payphone" to 3 * 60 + 52,
    "custom.pompeii" to 3 * 60 + 35,
    "custom.rickroll" to 3 * 60 + 32,
    "custom.save_a_life" to 4 * 60 + 42,
    "custom.unholy" to 2 * 60 + 37,
    "custom.50_ways" to 4 * 60 + 8,
    "custom.dont_you_worry" to 3 * 60 + 33
    )


fun loadDance() {
    Trigger("jukebox") {
        If(it eq 1) {
            Command.tell(self, """{"text":"Songs:", "underline" : true}""")
            for ((i, v) in songs.withIndex()) {
                Command.tellraw(self, """${i + 2}: ${v.first.split(".")[1]}""")
            }
            Command.tellraw(self, """${songs.size + 2}: Random""")
        }.Else {
            song[self] = it
        }
    }


    danceGun = RaycastBuilder("Dance Gun", 500).withCooldown(1.0).withCustomModelData(112)
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " lost a dance-off with "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .withPiercing()
        .withRange(100)
        .addSound("minecraft:block.beacon.power_select", 2.0)
        .addParticle(Particles.DUST(3.0, 3.0, 3.0, 2.0), 4)
        .onWallHit {
            Command.raw("""summon item ~ ~ ~ {Age:5950,PickupDelay:32767,Item:{id:"minecraft:music_disc_13",Count:1b}, Tags:[new, safe]}""")
            Command.raw("""summon item ~ ~ ~ {Age:5950,PickupDelay:32767,Item:{id:"minecraft:music_disc_mellohi",Count:1b}, Tags:[new, safe]}""")
            Command.raw("""summon item ~ ~ ~ {Age:5950,PickupDelay:32767,Item:{id:"minecraft:music_disc_blocks",Count:1b}, Tags:[new, safe]}""")
            Command.raw("""summon item ~ ~ ~ {Age:5950,PickupDelay:32767,Item:{id:"minecraft:music_disc_cat",Count:1b}, Tags:[new, safe]}""")
            Command.raw("""summon item ~ ~ ~ {Age:5950,PickupDelay:32767,Item:{id:"minecraft:music_disc_otherside",Count:1b}, Tags:[new, safe]}""")
            Command.raw("""summon item ~ ~ ~ {Age:5950,PickupDelay:32767,Item:{id:"minecraft:music_disc_far",Count:1b}, Tags:[new, safe]}""")
            Command.raw("""summon item ~ ~ ~ {Age:5950,PickupDelay:32767,Item:{id:"minecraft:music_disc_chirp",Count:1b}, Tags:[new, safe]}""")
            Command.execute().asat('e'["tag = new"]).run {
                repeat(3) {
                    val score = Random.next(40)
                    score -= 20
                    self.data["Motion[$it]", NBTTypes.DOUBLE, .02] = score
                }
            }
        }
        .onEntityHit { hit, shoot ->
            If(!(song[shoot] eq song[shoot])) {
                song[shoot] = 2
            }
            Command.execute().facing(shoot, Anchor.EYES).run.tp(hit, rel(), Vec2("~180", "~"))
            var value = song[shoot].copy()
            If(song[shoot] eq songs.size + 2) {
                value = Random.next(songs.size)
                value += 2
            }
            ScoreTree(value, 2 until songs.size + 2) {
                val sound = songs[it - 2].first
                Log.info("playin $sound")
                val len = songs[it - 2].second
                Command.playsound("minecraft:$sound").master(hit, rel(), 100.0)
                //TODO: convert to script
                Command.raw("execute at @s run particle minecraft:entity_effect ~ ~ ~ 0.9960784313725490196078431372549 0.9921568627450980392156862745098 0.0039215686274509803921568627451 1 0 force @s")
                Sleep(Duration.seconds(len))
                Command.raw("particle minecraft:entity_effect ~ ~ ~ 0.9960784313725490196078431372549 0.9921568627450980392156862745098 1 1 0 force @s")
            }
        }.asReward().done()
    danceGun.extraLines.add("""{"text":"Forces the user to listen to music on hit.","color" : "gray","italic":false}""")
    danceGun.extraLines.add("""{"text":"(Select the song using /trigger jukebox).","color" : "gray","italic":false}""")
}

fun loadWeezerGun() {
    weezer = RaycastBuilder("Weezer Gun", 500).withCooldown(1.0).withCustomModelData(114)
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was Weezed by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .withPiercing()
        .withRange(100)
        .addSound("minecraft:block.note_block.guitar", 1.4)
        .addParticle(Particles.DUST(.3, .1, 3.0, 1.3), 4)
        .onEntityHit { hit, _ ->
            Command.playsound("custom.weezer").master(hit, rel(), 1.0)
            Sleep(Duration(41))
            Command.item().replace.entity(self, "armor.head")
                .with(Items.CARVED_PUMPKIN.nbt("{Enchantments:[{id:\"curse_of_binding\"}]}"))
        }.asReward().done()
    weezer.extraLines.add("""{"text":"Weezes the victim.","color" : "gray","italic":false}""")
}

fun loadStreakRewards() {
    Switch(streak[self], allowDuplicateMatches = true)
        .case(3) {
            Command.tellraw(
                'a'[""],
                """["",{"selector":"@s", "color":"gold"}," is on ",{"text":"Fire!","color" : "red"}]"""
            )
            danceGun.give(self)

        }
        .case(5) {
            Command.tellraw(
                'a'[""],
                """["", {"selector":"@s","color": "gold"}, " is ", {"text": "UNSTOPPABLE!","bold": true,"color":"red"}]"""
            )
            weezer.give(self)
        }
        .case(10) {
            Command.tellraw(
                'a'[""],
                """["", {"selector":"@s","color": "gold"}, " is ", {"text":"aa","obfuscated": true, "color": "dark_purple"}, {"text": "BLOODTHIRSTY!","bold": true,"color":"dark_red"} ,{"text":"aa","obfuscated": true, "color": "dark_purple"}]"""
            )
        }
}

fun loadFun() {
    loadDance()
    loadWeezerGun()
    handleStreak.append { loadStreakRewards() }
}
