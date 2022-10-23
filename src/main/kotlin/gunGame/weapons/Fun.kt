package gunGame.weapons

import abstractions.PlayerTag
import abstractions.asat
import abstractions.flow.If
import abstractions.flow.Switch
import abstractions.hasTag
import commands.Command
import enums.Effects
import enums.Entities
import enums.Particles
import gunGame.*
import lib.get
import structure.Fluorite
import utils.loc
import utils.rel

lateinit var catGun: ModularCoasWeapon
lateinit var danceGun: ModularCoasWeapon

fun handleStreak() {
    Switch(streak[self], allowDuplicateMatches = true)
        .case(3) {
            Command.tellraw('a'[""], """["",{"selector":"@s", "color":"gold"}," is on ",{"text":"Fire!","color" : "red"}]""")
        }
        .case(5) {
            Command.tellraw('a'[""], """["", {"selector":"@s","color": "gold"}, " is ", {"text": "UNSTOPPABLE!","bold": true,"color":"red"}]""")
        }
        .case(10) {
            Command.tellraw('a'[""], """["", {"selector":"@s","color": "gold"}, " is ", {"text":"aa","obfuscated": true, "color": "dark_purple"}, {"text": "BLOODTHIRSTY!","bold": true,"color":"dark_red"} ,{"text":"aa","obfuscated": true, "color": "dark_purple"}]""")
        }
}


fun funGuns() {
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

    catGun = ModularCoasWeapon("Cat Gun", 2500).withParticle(Particles.ANGRY_VILLAGER).addSound("minecraft:entity.cat.ambient").withCooldown(1.5).onWallHit {

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

    }.withRange(100).done()

}