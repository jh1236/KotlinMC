package lib

import abstractions.PlayerTag
import abstractions.ReturningMethod
import abstractions.flow.If
import abstractions.hasTag
import abstractions.score.Score
import commands.Command
import enums.Blocks
import enums.Entities
import enums.nbt
import structure.Fluorite
import utils.get
import utils.rel

val panes = Blocks.newTag(
    Blocks.GLASS_PANE,
    Blocks.WHITE_STAINED_GLASS_PANE,
    Blocks.RED_STAINED_GLASS_PANE,
    Blocks.ORANGE_STAINED_GLASS_PANE,
    Blocks.PINK_STAINED_GLASS_PANE,
    Blocks.YELLOW_STAINED_GLASS_PANE,
    Blocks.LIME_STAINED_GLASS_PANE,
    Blocks.GREEN_STAINED_GLASS_PANE,
    Blocks.LIGHT_BLUE_STAINED_GLASS_PANE,
    Blocks.CYAN_STAINED_GLASS_PANE,
    Blocks.MAGENTA_STAINED_GLASS_PANE,
    Blocks.PURPLE_STAINED_GLASS_PANE,
    Blocks.PURPLE_STAINED_GLASS_PANE,
    Blocks.BROWN_STAINED_GLASS_PANE,
    Blocks.GRAY_STAINED_GLASS_PANE,
    Blocks.BLACK_STAINED_GLASS_PANE,
    Blocks.IRON_BARS
)
val rods = Blocks.newTag(Blocks.END_ROD, Blocks.LIGHTNING_ROD)
val skulls = Blocks.newTag(
    Blocks.DRAGON_HEAD,
    Blocks.SKELETON_SKULL,
    Blocks.WITHER_SKELETON_SKULL,
    Blocks.PLAYER_HEAD,
    Blocks.ZOMBIE_HEAD,
    Blocks.CREEPER_HEAD
)
val wall_skulls = Blocks.newTag(
    Blocks.DRAGON_WALL_HEAD,
    Blocks.SKELETON_WALL_SKULL,
    Blocks.WITHER_SKELETON_WALL_SKULL,
    Blocks.PLAYER_WALL_HEAD,
    Blocks.ZOMBIE_WALL_HEAD,
    Blocks.CREEPER_WALL_HEAD
)
val lanterns = Blocks.newTag(Blocks.LANTERN, Blocks.SOUL_LANTERN)
val pistons = Blocks.newTag(Blocks.STICKY_PISTON, Blocks.PISTON)
val split1 = Blocks.newTag(
    Blocks.SCULK_SENSOR,
    Blocks.END_PORTAL_FRAME,
    Blocks.ENCHANTING_TABLE,
    Blocks.STONECUTTER,
    Blocks.DAYLIGHT_DETECTOR,
    Blocks.SCULK_SHRIEKER,
    Blocks.tag("minecraft:beds"),
    Blocks.tag("minecraft:campfires"),
    Blocks.CAKE,
    Blocks.CANDLE_CAKE
)
val split2 = Blocks.newTag(
    rods,
    panes,
    Blocks.PISTON_HEAD,
    Blocks.tag("minecraft:fences"),
    Blocks.tag("minecraft:fence_gates"),
    Blocks.CHAIN,
    lanterns,
    pistons
)
val split3 = Blocks.newTag(
    Blocks.BELL,
    Blocks.tag("minecraft:walls"),
    Blocks.HOPPER,
    Blocks.LECTERN,
    Blocks.CONDUIT,
    Blocks.tag("minecraft:anvil"),
    skulls,
    wall_skulls
)
val all_partials = Blocks.newTag(
    split2,
    split3,
    split1,
    Blocks.tag("minecraft:stairs"),
    Blocks.tag("minecraft:doors"),
    Blocks.tag("minecraft:slabs"),
    Blocks.tag("minecraft:trapdoors"),
)

val tag = PlayerTag("coordinate_fetch")
val doesCollide = ReturningMethod("Lach993:collision_check", 0) {

    // returns 1 if collides. 0 otherwise
    val retScore = Fluorite.reuseFakeScore("collides", 0) // change value to 0
    If(!(rel() isBlock Blocks.tag("jh1236:air"))) {
//        Command.say("true")
        retScore.set(1)
    }
    If(rel() isBlock all_partials) {
        Command.summon(Entities.MARKER, rel(), "{Tags:[$tag]}")
        val x = Fluorite.reuseFakeScore("partialTempx")
        val y = Fluorite.reuseFakeScore("partialTempy")
        val z = Fluorite.reuseFakeScore("partialTempz")
        x.set('e'[""].hasTag(tag)["limit=1"].data["Pos[0]"], 100.0) %= 100
        y.set('e'[""].hasTag(tag)["limit=1"].data["Pos[1]"], 100.0) %= 100
        z.set('e'[""].hasTag(tag)["limit=1"].data["Pos[2]"], 100.0) %= 100
        retScore.set(0)
        If(rel() isBlock Blocks.tag("minecraft:slabs")) {
            If(rel() isBlock Blocks.tag("minecraft:slabs[type = bottom]") and (y lte 50)) {
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:slabs[type = top]") and (y gte 50)) {
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:slabs[type = double]")) {
                retScore.set(1)
            }
        }
        If(rel() isBlock Blocks.tag("minecraft:trapdoors")) {
            If(rel() isBlock Blocks.tag("minecraft:trapdoors[open=false]")) {

                If(rel() isBlock Blocks.tag("minecraft:trapdoors[half=top]") and (y gte 75)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:trapdoors[half=bottom]") and (y lte 25)) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:trapdoors[open=true]")) {
                If(rel() isBlock Blocks.tag("minecraft:trapdoors[facing=north]") and (z gte 75)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:trapdoors[facing=south]") and (z lte 25)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:trapdoors[facing=west]") and (x gte 75)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:trapdoors[facing=east]") and (x lte 25)) {
                    retScore.set(1)
                }
            }
        }
        If(rel() isBlock Blocks.tag("minecraft:doors")) {
            If(rel() isBlock Blocks.tag("minecraft:doors[open=false]")) {

                If(rel() isBlock Blocks.tag("minecraft:doors[facing=north]") and (z gte 75)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=south]") and (z lte 25)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=west]") and (x gte 75)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=east]") and (x lte 25)) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:doors[open=true]")) {
                If(rel() isBlock Blocks.tag("minecraft:doors[hinge=right]")) {
                    If(rel() isBlock Blocks.tag("minecraft:doors[facing=east]") and (z gte 75)) {
                        retScore.set(1)
                    }
                    If(rel() isBlock Blocks.tag("minecraft:doors[facing=west]") and (z lte 25)) {
                        retScore.set(1)
                    }
                    If(rel() isBlock Blocks.tag("minecraft:doors[facing=north]") and (x gte 75)) {
                        retScore.set(1)
                    }
                    If(rel() isBlock Blocks.tag("minecraft:doors[facing=south]") and (x lte 25)) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.tag("minecraft:doors[hinge=left]")) {
                    If(rel() isBlock Blocks.tag("minecraft:doors[facing=west]") and (z gte 75)) {
                        retScore.set(1)
                    }
                    If(rel() isBlock Blocks.tag("minecraft:doors[facing=east]") and (z lte 25)) {
                        retScore.set(1)
                    }
                    If(rel() isBlock Blocks.tag("minecraft:doors[facing=south]") and (x gte 75)) {
                        retScore.set(1)
                    }
                    If(rel() isBlock Blocks.tag("minecraft:doors[facing=north]") and (x lte 25)) {
                        retScore.set(1)
                    }
                }
            }
        }
        If(rel() isBlock Blocks.tag("minecraft:stairs")) {
            //mm yummy, 22 different conditions
            If(rel() isBlock Blocks.tag("minecraft:stairs[half = bottom]") and (y lte 50)) {
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[half = top]") and (y gte 50)) {
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[facing=west]")) {
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=straight]") and (x lte 50)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_left]")) {
                    If(x lte 50) {
                        retScore.set(1)
                    }
                    If(z gte 50) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_right]")) {
                    If(x lte 50) {
                        retScore.set(1)
                    }
                    If(z lte 50) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_left]") and (x lte 50 and (z gte 50))) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_right]") and (x lte 50 and (z lte 50))) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[facing=east]")) {
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=straight]") and (x gte 50)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_left]")) {
                    If(x gte 50) {
                        retScore.set(1)
                    }
                    If(z lte 50) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_right]")) {
                    If(x gte 50) {
                        retScore.set(1)
                    }
                    If(z gte 50) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_left]") and (x gte 50 and (z lte 50))) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_right]") and (x gte 50 and (z gte 50))) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[facing=south]")) {
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=straight]") and (z gte 50)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_left]")) {
                    If(z gte 50) {
                        retScore.set(1)
                    }
                    If(x gte 50) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_right]")) {
                    If(z gte 50) {
                        retScore.set(1)
                    }
                    If(x lte 50) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_left]") and (z gte 50 and (x gte 50))) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_right]") and (z gte 50 and (x lte 50))) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[facing=north]")) {
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=straight]") and (z lte 50)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_left]")) {
                    If(z lte 50) {
                        retScore.set(1)
                    }
                    If(x lte 50) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_right]")) {
                    If(z lte 50) {
                        retScore.set(1)
                    }
                    If(x gte 50) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_left]") and (z lte 50 and (x lte 50))) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_right]") and (z lte 50 and (x gte 50))) {
                    retScore.set(1)
                }
            }

        }
        If(rel() isBlock split1) {
            If(rel() isBlock Blocks.STONECUTTER) {
                If(y lte 62) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.DAYLIGHT_DETECTOR) {
                If(y lte 37) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.SCULK_SENSOR) {
                If(y lte 50) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.SCULK_SHRIEKER) {
                If(y lte 50) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:beds")) {
                If(y lte 56) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:campfires")) {
                If(y lte 43) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.CAKE) {
                If(y lte 50) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.CANDLE_CAKE) {
                If(y lte 50) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.END_PORTAL_FRAME) {
                If(y lte 81) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.ENCHANTING_TABLE) {
                If(y lte 75) {
                    retScore.set(1)
                }
            }
        }
        If(rel() isBlock split2) {
            If(rel() isBlock rods) {
                val centre = (50 - 13)..(50 + 13)
                for (i in listOf("up", "down")) {
                    If(rel() isBlock rods.nbt("[facing=$i]")) {
                        If(x inRange centre and (z inRange centre)) {
                            retScore.set(1)
                        }
                    }
                }
                for (i in listOf("north", "south")) {
                    If(rel() isBlock rods.nbt("[facing=$i]")) {
                        If(x inRange centre and (y inRange centre)) {
                            retScore.set(1)
                        }
                    }
                }
                for (i in listOf("east", "west")) {
                    If(rel() isBlock rods.nbt("[facing=$i]")) {
                        If(z inRange centre and (y inRange centre)) {
                            retScore.set(1)
                        }
                    }
                }
            }
            If(rel() isBlock Blocks.CHAIN) {
                val centre = (50 - 13)..(50 + 13)
                If(rel() isBlock Blocks.CHAIN.nbt("[axis=y]")) {
                    If(x inRange centre and (z inRange centre)) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.CHAIN.nbt("[axis=z]")) {
                    If(x inRange centre and (y inRange centre)) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.CHAIN.nbt("[axis=x]")) {
                    If(z inRange centre and (y inRange centre)) {
                        retScore.set(1)
                    }
                }
            }
            If(rel() isBlock panes) {
                If(x inRange 40..60 and (z inRange 40..60)) {
                    retScore.set(1)
                }
                If(rel() isBlock panes.nbt("[west=true]") and (x inRange 0..50) and (z inRange 40..60)) {
                    retScore.set(1)
                }
                If(rel() isBlock panes.nbt("[east=true]") and (x inRange 50..100) and (z inRange 40..60)) {
                    retScore.set(1)
                }
                If(rel() isBlock panes.nbt("[north=true]") and (z inRange 0..50) and (x inRange 40..60)) {
                    retScore.set(1)
                }
                If(rel() isBlock panes.nbt("[south=true]") and (z inRange 50..100) and (x inRange 40..60)) {
                    retScore.set(1)
                }

            }
            If(rel() isBlock pistons) {
                If(rel() isBlock pistons.nbt("[extended=false]")) {
                    retScore.set(1)
                }
                If(rel() isBlock pistons.nbt("[facing=up]")) {
                    If(y lte 75) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock pistons.nbt("[facing=down]")) {
                    If(y gte 25) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock pistons.nbt("[facing=south]")) {
                    If(z lte 75) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock pistons.nbt("[facing=north]")) {
                    If(z gte 25) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock pistons.nbt("[facing=east]")) {
                    If(x lte 75) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock pistons.nbt("[facing=west]")) {
                    If(x gte 25) {
                        retScore.set(1)
                    }
                }

            }
            If(rel() isBlock Blocks.PISTON_HEAD) {
                val centre = (50 - 13)..(50 + 13)
                If(rel() isBlock Blocks.PISTON_HEAD.nbt("[facing=down]")) {
                    If(x inRange centre and (z inRange centre)) {
                        retScore.set(1)
                    }
                    If(y lte 25) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.PISTON_HEAD.nbt("[facing=up]")) {
                    If(x inRange centre and (z inRange centre)) {
                        retScore.set(1)
                    }
                    If(y gte 75) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.PISTON_HEAD.nbt("[facing=north]")) {
                    If(x inRange centre and (y inRange centre)) {
                        retScore.set(1)
                    }
                    If(z lte 25) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.PISTON_HEAD.nbt("[facing=south]")) {
                    If(x inRange centre and (y inRange centre)) {
                        retScore.set(1)
                    }
                    If(z gte 75) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.PISTON_HEAD.nbt("[facing=east]")) {
                    If(z inRange centre and (y inRange centre)) {
                        retScore.set(1)
                    }
                    If(x gte 75) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock Blocks.PISTON_HEAD.nbt("[facing=west]")) {
                    If(z inRange centre and (y inRange centre)) {
                        retScore.set(1)
                    }
                    If(x lte 25) {
                        retScore.set(1)
                    }
                }

            }
            If(rel() isBlock Blocks.tag("minecraft:fences")) {
                If(x inRange 38..62 and (z inRange 38..62)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:fences[west=true]") and (x inRange 0..50) and (z inRange 38..62) and (y inRange 37..94)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:fences[east=true]") and (x inRange 50..100) and (z inRange 38..62) and (y inRange 37..94)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:fences[north=true]") and (z inRange 0..50) and (x inRange 38..62) and (y inRange 37..94)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:fences[south=true]") and (z inRange 50..100) and (x inRange 38..62) and (y inRange 37..94)) {
                    retScore.set(1)
                }

            }
            If(rel() isBlock Blocks.tag("minecraft:fence_gates")) {
                If(rel() isBlock Blocks.tag("minecraft:fence_gates[in_wall=true]")) {
                    fencegateHelper(19..75, retScore, x, y, z)
                }
                If(rel() isBlock Blocks.tag("minecraft:fence_gates[in_wall=false]")) {
                    fencegateHelper(38..93, retScore, x, y, z)
                }

            }
            If(rel() isBlock lanterns) {
                If(rel() isBlock lanterns.nbt("[hanging=false]")) {
                    If(x inRange 32..68 and (z inRange 32..68) and (y lte 56)) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock lanterns.nbt("[hanging=true]")) {
                    If(x inRange 32..68 and (z inRange 32..68) and (y inRange 7..63)) {
                        retScore.set(1)
                    }
                    If(x inRange 37..63 and (z inRange 37..63) and (y gte 63)) {
                        retScore.set(1)
                    }
                }
            }
        }
        If(rel() isBlock split3) {
            If(rel() isBlock Blocks.LECTERN) {
                If(y lte 20) {
                    retScore.set(1)
                }
                If(y gte 63) {
                    retScore.set(1)
                }
                If(x inRange 25..75) {
                    If(z inRange 25..75) {
                        retScore.set(1)
                    }
                }
            }
            If(rel() isBlock Blocks.HOPPER) {
                If(y gte 63) {
                    retScore.set(1)
                }
                If(x inRange 25..75) {
                    If(z inRange 25..75) {
                        If(y gte 25) {
                            retScore.set(1)
                        }
                    }
                }
                If(rel() isBlock Blocks.HOPPER.nbt("[facing=down]")) {
                    If(x inRange 37..63 and (z inRange 37..63)) {
                        retScore.set(1)
                    }

                }.ElseIf(y inRange 25..50) {
                    If(x inRange 37..63) {
                        If(rel() isBlock Blocks.HOPPER.nbt("[facing=north]")) {
                            If(z lte 50) {
                                retScore.set(1)
                            }
                        }
                        If(rel() isBlock Blocks.HOPPER.nbt("[facing=south]")) {
                            If(z gte 50) {
                                retScore.set(1)
                            }
                        }
                    }
                    If(z inRange 37..63) {
                        If(rel() isBlock Blocks.HOPPER.nbt("[facing=west]")) {
                            If(x lte 50) {
                                retScore.set(1)
                            }
                        }
                        If(rel() isBlock Blocks.HOPPER.nbt("[facing=east]")) {
                            If(x gte 50) {
                                retScore.set(1)
                            }
                        }
                    }
                }
            }
            If(rel() isBlock Blocks.BELL) {
                If(y inRange 32..87) {
                    If(x inRange 32..68 and (z inRange 32..68)) {
                        retScore.set(1)
                        Command.playsound("block.bell.use")
                            .player('a'[""], rel(), 2)
                    }
                }
                If(rel() isBlock Blocks.BELL.nbt("[attachment=floor]")) {
                    If(rel() isBlock Blocks.BELL.nbt("[facing=north]")) {
                        If(!(x inRange 22..78)) {
                            If(z inRange 37..63) {
                                retScore.set(1)
                            }
                        }
                    }
                    If(rel() isBlock Blocks.BELL.nbt("[facing=south]")) {
                        If(!(x inRange 22..78)) {
                            If(z inRange 37..63) {
                                retScore.set(1)
                            }
                        }
                    }
                    If(rel() isBlock Blocks.BELL.nbt("[facing=east]")) {
                        If(!(z inRange 22..78)) {
                            If(x inRange 37..63) {
                                retScore.set(1)
                            }
                        }
                    }
                    If(rel() isBlock Blocks.BELL.nbt("[facing=west]")) {
                        If(!(z inRange 22..78)) {
                            If(x inRange 37..63) {
                                retScore.set(1)
                            }
                        }
                    }
                }
            }
            If(rel() isBlock Blocks.CONDUIT) {
                val n = 32..68
                If(x inRange n and (y inRange n) and (z inRange n)) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:anvil")) {
                If(x inRange 13..87 and (z inRange 13..87) and (y lte 25)) {
                    retScore.set(1)
                }
                for (i in listOf("north", "south")) {
                    If(rel() isBlock Blocks.tag("minecraft:anvil[facing=$i]")) {
                        If(y gte 82) {
                            If(x inRange 30..70) {
                                retScore.set(1)
                            }
                        }
                        If(x inRange 37..63) {
                            If(z inRange 25..75) {
                                retScore.set(1)
                            }
                        }
                    }
                }
                for (i in listOf("east", "west")) {
                    If(rel() isBlock Blocks.tag("minecraft:anvil[facing=$i]")) {
                        If(y gte 82) {
                            If(z inRange 30..70) {
                                retScore.set(1)
                            }
                        }
                        If(z inRange 37..63) {
                            If(x inRange 25..75) {
                                retScore.set(1)
                            }
                        }

                    }
                }

            }
            If(rel() isBlock skulls) {
                If(x inRange 25..75 and (y lte 50) and (z inRange 25..75)) {
                    retScore.set(1)
                }
            }
            If(rel() isBlock wall_skulls) {
                val siderange = 25..75
                If(rel() isBlock wall_skulls.nbt("[facing=north]")) {
                    If(z gte 50 and (y inRange siderange) and (x inRange siderange)) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock wall_skulls.nbt("[facing=south]")) {
                    If(z lte 50 and (y inRange siderange) and (x inRange siderange)) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock wall_skulls.nbt("[facing=east]")) {
                    If(x lte 50 and (y inRange siderange) and (z inRange siderange)) {
                        retScore.set(1)
                    }
                }
                If(rel() isBlock wall_skulls.nbt("[facing=west]")) {
                    If(x gte 50 and (y inRange siderange) and (z inRange siderange)) {
                        retScore.set(1)
                    }
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:walls")) {
                If(x inRange 30..70 and (z inRange 30..70)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:walls[west=low]") and (x inRange 0..50) and (z inRange 35..65) and (y lte 87)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:walls[west=high]") and (x inRange 0..50) and (z inRange 35..65)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:walls[east=low]") and (x inRange 50..100) and (z inRange 35..65) and (y lte 87)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:walls[east=high]") and (x inRange 50..100) and (z inRange 35..65)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:walls[north=low]") and (z inRange 0..50) and (x inRange 35..65) and (y lte 87)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:walls[north=high]") and (z inRange 0..50) and (x inRange 35..65)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:walls[south=low]") and (z inRange 50..100) and (x inRange 35..65) and (y lte 87)) {
                    retScore.set(1)
                }
                If(rel() isBlock Blocks.tag("minecraft:walls[south=high]") and (z inRange 50..100) and (x inRange 35..65)) {
                    retScore.set(1)
                }

            }
        }
        Command.kill('e'["tag=$tag"])
    }



    retScore
}

fun fencegateHelper(ylimits: IntRange = 19..75, retscore: Score, x: Score, y: Score, z: Score) {
    If(y inRange ylimits) {
        If(rel() isBlock Blocks.tag("minecraft:fence_gates[open=false]")) {
            If(rel() isBlock Blocks.tag("minecraft:fence_gates[facing=east]")) {
                If(x inRange 43..57) {
                    retscore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:fence_gates[facing=west]")) {
                If(x inRange 43..57) {
                    retscore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:fence_gates[facing=north]")) {
                If(z inRange 43..57) {
                    retscore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:fence_gates[facing=south]")) {
                If(z inRange 43..57) {
                    retscore.set(1)
                }
            }
        }
        If(rel() isBlock Blocks.tag("minecraft:fence_gates[open=true]")) {
            If(rel() isBlock Blocks.tag("minecraft:fence_gates[facing=east]")) {
                If(!(z inRange 25..75)) {
                    If(x inRange 43..95) {
                        retscore.set(1)
                    }
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:fence_gates[facing=west]")) {
                If(!(z inRange 25..75)) {
                    If(x inRange 5..57) {
                        retscore.set(1)
                    }
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:fence_gates[facing=north]")) {
                If(!(x inRange 25..75)) {
                    If(z inRange 5..57) {
                        retscore.set(1)
                    }
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:fence_gates[facing=south]")) {
                If(!(x inRange 25..75)) {
                    If(z inRange 43..95) {
                        retscore.set(1)
                    }

                }
            }
        }
    }
}
