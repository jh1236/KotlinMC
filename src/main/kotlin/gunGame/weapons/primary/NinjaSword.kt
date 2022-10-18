package gunGame.weapons.primary

import abstractions.As
import abstractions.advancements.Advancement
import abstractions.advancements.EntityHurtPlayer
import commands.Command
import enums.Entities
import enums.Items
import gunGame.damageSelf
import gunGame.self
import gunGame.weapons.shootTag
import lib.get
import structure.McFunction

val hitFunc = McFunction("ninja_sword/hit") {
    Command.schedule().As(self, 1) {
        shootTag.add('a'["scores = {$ninjaSword = 1..}"])
        damageSelf(1000)
        ninjaSword['a'[""]] = 0
    }
    Command.advancement().revoke(self).only("jh1236:ninja_hit")
}

val adv = Advancement("jh1236:ninja_hit") {
    criteria {
        condition("hit") {
            trigger(EntityHurtPlayer {
                damage {
                    sourceEntity {
                        type = Entities.PLAYER
                        equipment {
                            mainhand {
                                items {
                                    item(Items.IRON_SWORD)
                                }
                                nbt = "{jh1236:{weapon:8b}}"
                            }
                        }
                    }
                }
            })
        }
    }
    rewards {
        function = hitFunc
    }
}

fun swordInit() {
    adv
    hitFunc
}