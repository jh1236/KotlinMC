package gunGame.weapons

import abstractions.As
import abstractions.PlayerTag
import abstractions.asat
import abstractions.flow.If
import abstractions.flow.Switch
import abstractions.hasTag
import abstractions.variables.NBTTypes
import commands.Command
import enums.Anchor
import enums.Effects
import enums.Entities
import enums.Particles
import gunGame.*
import lib.get
import lib.random.Random
import structure.Fluorite
import structure.McFunction
import utils.Vec2
import utils.loc
import utils.rel

lateinit var catGun: ModularCoasWeapon
lateinit var danceGun: ModularCoasWeapon
val handleStreak = McFunction("jh1236:health/streak")


fun loadCat() {
    val catTag = PlayerTag("cat")
    Fluorite.tickFile += {
        Command.execute().asat('e'[""].hasTag(catTag)).run {
            health[self] += 1
            If(health[self] gt 200) {
                Command.tp(self, rel(0, -1000, 0))
            }
        }
        Command.execute().asat('a'["nbt={ActiveEffects:[{Id:17}]}"].hasTag(playingTag)).run {
            Command.effect().clear(self, Effects.HUNGER)
            damageSelf(1000)
        }
    }

    catGun = ModularCoasWeapon("Cat Gun", 0).withParticle(Particles.ANGRY_VILLAGER)
        .addSound("minecraft:entity.cat.ambient").withCooldown(1.5).onWallHit {
            Command.summon(
                Entities.HUSK,
                loc(0.3, 0.2, -1),
                "{Invulnerable:1b,IsBaby:1b,Tags:[$catTag],Passengers:[{id:\"minecraft:cat\",Invulnerable:1b,variant:\"tabby\",Tags:[$catTag]}],ArmorItems:[{},{},{},{id:'minecraft:red_carpet',Count:1b}],ActiveEffects:[{Id:1,Amplifier:14b,Duration:400},{Id:14,Amplifier:0b,Duration:400}]}"
            )
            Command.summon(
                Entities.HUSK,
                loc(0.3, -0.2, -1),
                "{Invulnerable:1b,IsBaby:1b,Tags:[$catTag],Passengers:[{id:\"minecraft:cat\",Invulnerable:1b,variant:\"ragdoll\",Tags:[$catTag]}],ArmorItems:[{},{},{},{id:'minecraft:red_carpet',Count:1b}],ActiveEffects:[{Id:1,Amplifier:14b,Duration:400},{Id:14,Amplifier:0b,Duration:400}]}"
            )
            Command.summon(
                Entities.HUSK,
                loc(.3, 0, -1.5),
                "{Invulnerable:1b,IsBaby:1b,Tags:[$catTag],Passengers:[{id:\"minecraft:cat\",Invulnerable:1b,variant:\"siamese\",Tags:[$catTag]}],ArmorItems:[{},{},{},{id:'minecraft:red_carpet',Count:1b}],ActiveEffects:[{Id:1,Amplifier:14b,Duration:400},{Id:14,Amplifier:0b,Duration:400}]}"
            )

        }.withRange(100).asReward().done()

}

fun loadDance() {
    danceGun = ModularCoasWeapon("Dance Gun", 500).withCooldown(1.0).withCustomModelData(112)
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " lost a dance-off with "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .withPiercing()
        .withRange(100)
        .addSound("minecraft:block.beacon.power_select", 2.0)
        .withParticle(Particles.DUST(3.0, 3.0, 3.0, 2.0), 4)
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
            Command.execute().facing(shoot, Anchor.EYES).run.tp(hit, rel(), Vec2("~180", "~"))
            Command.playsound("custom.gangnum").master(hit, rel(), 100.0)
            //TODO: convert to script
            Command.raw("execute at @s run particle minecraft:entity_effect ~ ~ ~ 0.9960784313725490196078431372549 0.9921568627450980392156862745098 0.0039215686274509803921568627451 1 0 force @s")
            Command.schedule().As(self, 4960) {
                Command.raw("particle minecraft:entity_effect ~ ~ ~ 0.9960784313725490196078431372549 0.9921568627450980392156862745098 1 1 0 force @s")
            }
        }.asReward().done()
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
            catGun.give(self)
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
    loadCat()
    handleStreak.append { loadStreakRewards() }
}
