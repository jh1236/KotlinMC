package gunGame.weapons.impl

import abstractions.PlayerTag
import abstractions.advancements.Advancement
import abstractions.advancements.EntityHurtPlayer
import abstractions.flow.If
import abstractions.flow.trees.ScoreTree
import abstractions.hasTag
import abstractions.score.Criteria
import abstractions.score.Objective
import commands.Command
import enums.Anchor
import enums.Blocks
import enums.Entities
import enums.Items
import gunGame.damageSelf
import gunGame.deathStorage
import gunGame.playingTag
import gunGame.self
import gunGame.weapons.AbstractWeapon
import gunGame.weapons.LootTableGenerator
import gunGame.weapons.shootTag
import internal.commands.impl.execute.OnTarget
import lib.random.Random
import structure.Fluorite
import structure.McFunction
import utils.Vec2
import utils.get
import utils.loc
import utils.rel

val ninjaScore = Objective("useIronSword", Criteria.useItem(Items.IRON_SWORD))
val traceScore = Objective("trace")
val swordScore = Objective("useStoneSword", Criteria.useItem(Items.STONE))
val stabbed = PlayerTag("stabbed")
val swordStabbed = PlayerTag("swordStabbed")

fun sign(int: Int): Int = if (int % 2 == 0) 1 else -1


fun loadNinjaSword() {

    val ninjaSword = object : AbstractWeapon("Ninja Sword", 1000) {
        init {
            val lore = arrayListOf(
                """{"text" : "Damage: 1000", "color": "gray", "italic" : false}""",
                """{"text" : "Teleports the player around its target on hit", "color": "gray", "italic" : false}"""
            )
            val nbt = """{HideFlags:63, Unbreakable: 1b, jh1236:{weapon:$myId} }"""
            lootTable = LootTableGenerator.genLootTable(basePath, Items.IRON_SWORD, name, lore, nbt)
            setupInternal()
        }
    }

    val tryTp = McFunction("${ninjaSword.basePath}/try_tp") {
        traceScore[self] = 6
        ScoreTree(Random.next(6), 0..5) {
            Command.execute().anchored(Anchor.FEET).facing(
                'a'["sort = nearest", "limit=1"].hasTag(stabbed).hasTag(playingTag), Anchor.FEET
            ).positioned('a'["sort = nearest", "limit=1"].hasTag(stabbed).hasTag(playingTag)).facing(loc(0, 0, -1))
                .rotated(
                    Vec2("~${sign(it) * ((it / 2) * 30 - 150)}", "0")
                ).run {
                    Command.function("${ninjaSword.basePath}/tp")
                }
        }
    }

    val tp = McFunction("${ninjaSword.basePath}/tp") {
        val retScore = Fluorite.reuseFakeScore("test", 0)
        retScore.set {
            Command.execute().If(loc(0, 0, .5) isBlock Blocks.AIR).If(loc(0, 0, 1) isBlock Blocks.AIR)
                .If(loc(0, 0, 1.5) isBlock Blocks.AIR).If(loc(0, 0, 2) isBlock Blocks.AIR)
            If(retScore eq 0) {
                Command.tp(self, loc(0, 0, 1.5), Vec2("~", "~-30"))
            }.Else {
                tryTp()
            }
        }
    }


    val hitFunc = McFunction("${ninjaSword.basePath}/hit") {
        Command.execute().on(OnTarget.ATTACKER).run {
            shootTag.add(self)
        }
        deathStorage["death"] =
            """'["",{"selector": "@s","color": "gold"},{"text": " didn\'t even see "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'"""
        swordStabbed.add(self)
        damageSelf(ninjaSword.damage)
        swordStabbed.remove(self)
        ninjaScore['a'[""]] = 0
        stabbed.add(self)
        Command.execute().asat('a'[""].hasTag(shootTag)).run {
            tryTp()
            shootTag.remove(self)
        }
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
    Fluorite.tickFile += {
        Command.execute().asat('a'["scores = {$traceScore = 1..}"]).run {
            // TODO: stop being lazy
//            Command.raw("execute anchored eyes facing entity @e[tag=$stabbed,sort=nearest,limit=1, tag =! $deadTag] feet run tp @s ~ ~ ~ ~ ~-30")
            Command.execute().anchored(Anchor.EYES)
                .facing('e'["sort = nearest, limit = 1"].hasTag(stabbed), Anchor.FEET).run.tp(
                    self,
                    rel(),
                    Vec2("~", "~-30")
                )
            traceScore[self] -= 1
            If(traceScore[self] eq 0) {
                stabbed.remove('e'["sort=nearest", "limit=1"].hasTag(stabbed))
            }
        }
        swordScore['a'[""]] = 0
        ninjaScore['a'[""]] = 0
    }

}

fun stoneSword() {
    val sword = object : AbstractWeapon("Stone Sword", 600, true) {
        init {
            val lore = arrayListOf(
                """{"text" : "Damage: 600", "color": "gray", "italic" : false}""",
            )
            val nbt = """{HideFlags:63, Unbreakable: 1b, jh1236:{weapon:$myId} }"""
            lootTable = LootTableGenerator.genLootTable(basePath, Items.STONE_SWORD, name, lore, nbt)
            setupInternal()
        }

    }
    val swordHitFunc = McFunction("${sword.basePath}/hit") {
        Command.execute().on(OnTarget.ATTACKER).run {
            shootTag.add(self)
        }
        swordStabbed.add(self)
        deathStorage["death"] =
            """'["",{"selector": "@s","color": "gold"},{"text": " was cut down by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'"""
        damageSelf(sword.damage)
        swordStabbed.remove(self)
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


}
