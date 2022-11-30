package lib

import abstractions.PlayerTag
import abstractions.ReturningMethod
import abstractions.asat
import abstractions.flow.If
import abstractions.hasTag
import commands.Command
import enums.Blocks
import enums.Entities
import structure.Fluorite
import utils.rel

val getX = ReturningMethod("Lach993:fetch_x_partial", 0) {
    val tag = PlayerTag("Partialx")
    val tempScore = Fluorite.reuseFakeScore("partialTemp")
    Command.summon(Entities.MARKER, rel(), "{Tags:[$tag]}")
    tempScore.set('e'[""].hasTag(tag)["limit=1"].data["Pos[0]"], 100.0)
    Command.kill('e'["tag=$tag"])
    Command.execute().asat('p'[""]).run(Command.scoreboard().players.get(tempScore))
    tempScore %= 100
    tempScore
}

val getY = ReturningMethod("Lach993:fetch_y_partial", 0) {
    val tag = PlayerTag("Partialy")
    val tempScore = Fluorite.reuseFakeScore("partialTemp")
    Command.summon(Entities.MARKER, rel(), "{Tags:[$tag]}")
    tempScore.set('e'[""].hasTag(tag)["limit=1"].data["Pos[1]"], 100.0)
    Command.kill('e'["tag=$tag"])
    Command.execute().asat('p'[""]).run(Command.scoreboard().players.get(tempScore))
    tempScore %= 100
    tempScore
}

val getZ = ReturningMethod("Lach993:fetch_Z_partial", 0) {
    val tag = PlayerTag("Partialz")
    val tempScore = Fluorite.reuseFakeScore("partialTemp")
    Command.summon(Entities.MARKER, rel(), "{Tags:[$tag]}")
    tempScore.set('e'[""].hasTag(tag)["limit=1"].data["Pos[2]"], 100.0)
    Command.kill('e'["tag=$tag"])
    Command.execute().asat('p'[""]).run(Command.scoreboard().players.get(tempScore))
    tempScore %= 100
    tempScore
}


val doesCollide = ReturningMethod("Lach993:collision_check", 0) {
    // returns 1 if collides. 0 otherwise
    val retScore = Fluorite.reuseFakeScore("collides", 1)
    If(rel() isBlock Blocks.tag("jh1236:air")){
        retScore.set(0)
    }.ElseIf(rel() isBlock Blocks.tag("minecraft:slabs")){
        val y = getY()
        If(rel() isBlock Blocks.tag("minecraft:slabs[type = bottom]") and (y gte 45)){
            retScore.set(0)
        }.ElseIf(rel() isBlock Blocks.tag("minecraft:slabs[type = top]") and (y lte 55)) {
            retScore.set(0)
        }
    }.ElseIf(rel() isBlock Blocks.tag("minecraft:trapdoors")) {
        If(rel() isBlock Blocks.tag("minecraft:trapdoors[open=false]")){
            val y = getY()
            If(rel() isBlock Blocks.tag("minecraft:trapdoors[half=top]") and (y lte 75)){
                retScore.set(0)
            }.ElseIf(rel() isBlock Blocks.tag("minecraft:trapdoors[half=bottom]") and (y gte 25)){
                retScore.set(0)
            }
        }.Else{
            If(rel() isBlock Blocks.tag("minecraft:trapdoors[facing=north]") and (getZ() lte 70)){
                retScore.set(0)
            }.ElseIf(rel() isBlock Blocks.tag("minecraft:trapdoors[facing=south]") and (getZ() gte 30)){
                retScore.set(0)
            }.ElseIf(rel() isBlock Blocks.tag("minecraft:trapdoors[facing=west]") and (getX() lte 70)){
                retScore.set(0)
            }.ElseIf(rel() isBlock Blocks.tag("minecraft:trapdoors[facing=east]") and (getX() gte 30)){
                retScore.set(0)
            }
        }
    }.ElseIf(rel() isBlock Blocks.tag("minecraft:doors")) {
        If(rel() isBlock Blocks.tag("minecraft:doors[open=false]")) {
            If(rel() isBlock Blocks.tag("minecraft:doors[facing=north]") and (getZ() lte 70)){
                retScore.set(0)
            }
            If(rel() isBlock Blocks.tag("minecraft:doors[facing=south]") and (getZ() gte 30)){
                retScore.set(0)
            }
            If(rel() isBlock Blocks.tag("minecraft:doors[facing=west]") and (getX() lte 70)){
                retScore.set(0)
            }
            If(rel() isBlock Blocks.tag("minecraft:doors[facing=east]") and (getX() gte 30)){
                retScore.set(0)
            }
        }
        If(rel() isBlock Blocks.tag("minecraft:doors[open=true]")){
            If(rel() isBlock Blocks.tag("minecraft:doors[hinge=right]")){
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=east]") and (getZ() lte 70)){
                    retScore.set(0)
                }
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=west]") and (getZ() gte 30)){
                    retScore.set(0)
                }
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=north]") and (getX() lte 70)){
                    retScore.set(0)
                }
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=south]") and (getX() gte 30)){
                    retScore.set(0)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:doors[hinge=left]")){
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=west]") and (getZ() lte 70)){
                    retScore.set(0)
                }
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=east]") and (getZ() gte 30)){
                    retScore.set(0)
                }
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=south]") and (getX() lte 70)){
                    retScore.set(0)
                }
                If(rel() isBlock Blocks.tag("minecraft:doors[facing=north]") and (getX() gte 30)){
                    retScore.set(0)
                }
            }
        } // optimise
    }.ElseIf(rel() isBlock Blocks.tag("minecraft:walls")){ //fuck walls so fucking much holy shit please jump off cliff
        val z = Fluorite.getNewFakeScore("tempz", getZ().copy())
        val x = Fluorite.getNewFakeScore("tempx", getX().copy())
        val y = getY()
        retScore.set(0)
        If(x inRange 30..70 and (z inRange 30..70)) {
            retScore.set(1)
        }
        If(rel() isBlock Blocks.tag("minecraft:walls[west=low]") and (x inRange 0..50) and (z inRange 35..65) and (y lte 87)) {
            retScore.set(1)
        }.ElseIf(rel() isBlock Blocks.tag("minecraft:walls[west=high]") and (x inRange 0..50) and (z inRange 35..65)){
            retScore.set(1)
        }
        If(rel() isBlock Blocks.tag("minecraft:walls[east=low]") and (x inRange 50..100) and (z inRange 35..65) and (y lte 87)) {
            retScore.set(1)
        }.ElseIf(rel() isBlock Blocks.tag("minecraft:walls[east=high]") and (x inRange 50..100) and (z inRange 35..65)) {
            retScore.set(1)
        }
        If(rel() isBlock Blocks.tag("minecraft:walls[north=low]") and (z inRange 0..50) and (x inRange 35..65) and (y lte 87)) {
            retScore.set(1)
        }.ElseIf(rel() isBlock Blocks.tag("minecraft:walls[north=high]") and (z inRange 0..50) and (x inRange 35..65)) {
            retScore.set(1)
        }
        If(rel() isBlock Blocks.tag("minecraft:walls[south=low]") and (z inRange 50..100) and (x inRange 35..65) and (y lte 87)) {
            retScore.set(1)
        }.ElseIf(rel() isBlock Blocks.tag("minecraft:walls[south=high]") and (z inRange 50..100) and (x inRange 35..65)) {
            retScore.set(1)
        }

    }.ElseIf(rel() isBlock Blocks.tag("minecraft:fences")) {
        val z = Fluorite.getNewFakeScore("tempz", getZ().copy())
        val x = Fluorite.getNewFakeScore("tempx", getX().copy())
        val y = getY()
        retScore.set(0)
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

    }.ElseIf(rel() isBlock Blocks.tag("c:glass_panes")) {
        val z = Fluorite.getNewFakeScore("tempz", getZ().copy())
        val x = Fluorite.getNewFakeScore("tempx", getX().copy())
        val y = getY()
        retScore.set(0)
        If(x inRange 40..60 and (z inRange 40..60)) {
            retScore.set(1)
        }
        If(rel() isBlock Blocks.tag("c:glass_panes[west=true]") and (x inRange 0..50) and (z inRange 40..60)) {
            retScore.set(1)
        }
        If(rel() isBlock Blocks.tag("c:glass_panes[east=true]") and (x inRange 50..100) and (z inRange 40..60)) {
            retScore.set(1)
        }
        If(rel() isBlock Blocks.tag("c:glass_panes[north=true]") and (z inRange 0..50) and (x inRange 40..60)) {
            retScore.set(1)
        }
        If(rel() isBlock Blocks.tag("c:glass_panes[south=true]") and (z inRange 50..100) and (x inRange 40..60)) {
            retScore.set(1)
        }

    }.ElseIf(rel() isBlock Blocks.tag("minecraft:stairs")){
        retScore.set(0)
        val z = Fluorite.getNewFakeScore("tempz", getZ().copy())
        val x = Fluorite.getNewFakeScore("tempx", getX().copy())
        val y = getY()
        //mm yummy, 22 different conditions
        If(rel() isBlock Blocks.tag("minecraft:stairs[half = bottom]") and (y lte 50)){
            retScore.set(1)
        }
        If(rel() isBlock Blocks.tag("minecraft:stairs[half = top]") and (y gte 50)) {
            retScore.set(1)
        }
        If(rel() isBlock Blocks.tag("minecraft:stairs[facing=west]")){
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=straight]") and (x lte 50)){
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_left]")){
                If(x lte 50) {
                    retScore.set(1)
                }
                If(z gte 50){
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_right]")){
                If(x lte 50) {
                    retScore.set(1)
                }
                If(z lte 50){
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_left]") and (x  lte 50 and (z gte 50))){
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_right]") and (x lte 50 and (z lte 50))){
                retScore.set(1)
            }
        }
        If(rel() isBlock Blocks.tag("minecraft:stairs[facing=east]")){
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=straight]") and (x gte 50)){
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_left]")){
                If(x gte 50) {
                    retScore.set(1)
                }
                If(z lte 50){
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_right]")){
                If(x gte 50) {
                    retScore.set(1)
                }
                If(z gte 50){
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_left]") and (x  gte 50 and (z lte 50))){
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_right]") and (x gte 50 and (z gte 50))){
                retScore.set(1)
            }
        }
        If(rel() isBlock Blocks.tag("minecraft:stairs[facing=south]")){
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=straight]") and (z gte 50)){
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_left]")){
                If(z gte 50) {
                    retScore.set(1)
                }
                If(x gte 50){
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_right]")){
                If(z gte 50) {
                    retScore.set(1)
                }
                If(x lte 50){
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_left]") and (z  gte 50 and (x gte 50))){
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_right]") and (z gte 50 and (x lte 50))){
                retScore.set(1)
            }
        }
        If(rel() isBlock Blocks.tag("minecraft:stairs[facing=north]")){
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=straight]") and (z lte 50)){
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_left]")){
                If(z lte 50) {
                    retScore.set(1)
                }
                If(x lte 50){
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=inner_right]")){
                If(z lte 50) {
                    retScore.set(1)
                }
                If(x gte 50){
                    retScore.set(1)
                }
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_left]") and (z  lte 50 and (x lte 50))){
                retScore.set(1)
            }
            If(rel() isBlock Blocks.tag("minecraft:stairs[shape=outer_right]") and (z lte 50 and (x gte 50))){
                retScore.set(1)
            }
        }

    }

    retScore
}
