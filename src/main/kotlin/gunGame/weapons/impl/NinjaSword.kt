package gunGame.weapons.impl

import abstractions.PlayerTag
import abstractions.advancements.Advancement
import abstractions.advancements.EntityHurtPlayer
import abstractions.flow.If
import abstractions.flow.Tree
import abstractions.hasTag
import abstractions.schedule.Sleep
import abstractions.score.Criteria
import abstractions.score.Objective
import commands.Command
import enums.Anchor
import enums.Blocks
import enums.Entities
import enums.Items
import gunGame.damageSelf
import gunGame.deadTag
import gunGame.playingTag
import gunGame.self
import gunGame.weapons.AbstractWeapon
import gunGame.weapons.shootTag
import lib.random.Random
import structure.Fluorite
import structure.McFunction
import utils.*

val ninjaScore = Objective("useIronSword", Criteria.useItem(Items.IRON_SWORD))
val traceScore = Objective("trace")
val swordScore = Objective("useStoneSword", Criteria.useItem(Items.STONE))
val stabbed = PlayerTag("stabbed")

fun parity(int: Int): Int = if (int % 2 == 0) 1 else -1

val tryTp = McFunction("ninja_sword/try_tp") {
    traceScore[self] = 6
    Tree(Random.next.rem(6), 0..5) {
        Command.execute().anchored(Anchor.EYES).facing(
            'a'["sort = nearest", "limit=1", "distance = 0.1..4"].hasTag(playingTag),
            Anchor.FEET
        ).positioned('a'["sort = nearest", "limit=1", "distance = 0.1..4"].hasTag(playingTag)).rotated(
            Vec2("~${parity(it) * ((it / 2) * 30 - 150)}", "0")
        ).run {
            Command.function("jh1236:ninja_sword/tp")
        }
    }
}

val tp = McFunction("ninja_sword/tp") {
    val retScore = Fluorite.reuseFakeScore("test", 0)
    retScore.set {
        Command.execute()
            .If(loc(0, 0, -.5) isBlock Blocks.AIR)
            .If(loc(0, 0, -1) isBlock Blocks.AIR)
            .If(loc(0, 0, -1.5) isBlock Blocks.AIR)
            .If(loc(0, 0, -2) isBlock Blocks.AIR)
        If(retScore eq 0) {
            Command.tp(self, loc(0, 0, -.25), Vec2("~", "~-30"))
        }.Else {
            tryTp()
        }
    }
}

fun loadNinjaSword() {


    val ninjaSword = object : AbstractWeapon("ninja_sword", 1000) {


        override fun give(player: Selector) {
            Command.give(self, Items.IRON_SWORD.nbt("{jh1236:{weapon:$myId}}"))
        }

    }

    val hitFunc = McFunction("ninja_sword/hit") {
        Sleep(Duration(1))
        shootTag.add('a'["scores = {$ninjaScore = 1..}"])
        damageSelf(ninjaSword.damage)
        ninjaScore['a'[""]] = 0
        stabbed.add(self)
        Command.execute().asat('a'[""].hasTag(shootTag)).run {
            tryTp()
            shootTag.remove(self)
        }
        stabbed.remove(self)
        Command.advancement().revoke(self).only("jh1236:ninja_hit")
    }

    Advancement("jh1236:ninja_hit") {
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
                                    nbt = "{jh1236:{weapon:${ninjaSword.myId}}}"
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

}

fun stoneSword() {
    val sword = object : AbstractWeapon("Stone Sword", 600, true) {
        override fun give(player: Selector) {
            Command.give(self, Items.STONE_SWORD.nbt("{jh1236:{weapon:$myId}}"))
        }

    }
    val swordHitFunc = McFunction("sword/hit") {
        Sleep(Duration(1))
        shootTag.add('a'["scores = {$swordScore = 1..}"])
        damageSelf(sword.damage)
        swordScore['a'[""]] = 0
        Command.execute().asat('a'[""].hasTag(shootTag)).run {
            shootTag.remove(self)
        }
        Command.advancement().revoke(self).only("jh1236:sword_hit")
    }

    Advancement("jh1236:sword_hit") {
        criteria {
            condition("hit") {
                trigger(EntityHurtPlayer {
                    damage {
                        sourceEntity {
                            type = Entities.PLAYER
                            equipment {
                                mainhand {
                                    items {
                                        item(Items.STONE_SWORD)
                                    }
                                    nbt = "{jh1236:{weapon:${sword.myId}}}"
                                }
                            }
                        }
                    }
                })
            }
        }
        rewards {
            function = swordHitFunc
        }
    }

    Fluorite.tickFile += {
        Command.execute().asat('a'["scores = {$traceScore = 1..}"]).run {
            // TODO: stop being lazy
            Command.raw("execute anchored eyes facing entity @e[tag=$stabbed,sort=nearest,limit=1, tag =! $deadTag] feet run tp @s ~ ~ ~ ~ ~-30")
            traceScore[self] -= 1
            If(traceScore[self] eq 0) {
                Command.tag('e'["sort=nearest", "limit=1"].hasTag(stabbed))
            }
        }
    }

}
