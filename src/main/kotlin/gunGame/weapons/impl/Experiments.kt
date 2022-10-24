package gunGame.weapons.impl

import abstractions.*
import abstractions.flow.If
import commands.Command
import enums.Particles
import gunGame.bigExplosion
import gunGame.health
import gunGame.playingTag
import gunGame.self
import gunGame.weapons.ModularCoasWeapon
import gunGame.weapons.shootTag
import lib.get
import lib.idScore
import structure.Fluorite
import structure.McFunction
import utils.abs
import utils.rel
import utils.score.Objective

lateinit var clipOfDexterity: ModularCoasWeapon
val speedyAmmo = PlayerTag("speed_ammo")
val shotCount = Objective("shotsRemaining")

lateinit var mines: ModularCoasWeapon


fun loadClip() {
    clipOfDexterity = object : ModularCoasWeapon("Clip of Dexterity", 0) {
        init {
            asSecondary()
            withCustomModelData(113)
            addSound("minecraft:item.spyglass.use", 0.0)
            addSound("minecraft:item.spyglass.use", 0.0)
            addSound("minecraft:item.spyglass.use", 0.0)
            addSound("minecraft:item.spyglass.use", 0.0)
            withCooldown(-1.0)
            done()
        }

        override fun singleShot() {
            speedyAmmo.add(self)
            shotCount[self] += 6
        }


    }
}

fun loadLandMines() {
    fun projectileTick(range: Int, activationDelay: Int) {

        If(health[self] gt range - activationDelay) {
            Command.particle(Particles.DUST(0.6, 0.3, 0.3, 1.0), rel(), abs(0, 0, 0), 1.0, 5)
        }.ElseIf(health[self] gte 100) {
            If(health[self].rem(40) eq 0) {
                Command.particle(Particles.DUST(1.0, 0.0, 0.0, 1.0), rel(0, 0.2, 0), abs(0, 0, 0), 1.0, 5)
                Command.playsound("minecraft:block.note_block.bit").master('a'[""], rel(), .1, 1.0)
            }.Else {
                Command.particle(Particles.DUST(0.6, 0.3, 0.3, 1.0), rel(), abs(0, 0, 0), 1.0, 5)
            }
        }.ElseIf(health[self] gte 40) {
            If(health[self].rem(10) eq 0) {
                Command.particle(Particles.DUST(1.0, 0.0, 0.0, 1.0), rel(0, 0.2, 0), abs(0, 0, 0), 1.0, 5)
                Command.playsound("minecraft:block.note_block.bit").master('a'[""], rel(), .3, 1.0)
            }.Else {
                Command.particle(Particles.DUST(0.6, 0.3, 0.3, 1.0), rel(), abs(0, 0, 0), 1.0, 5)
            }
        }.ElseIf(health[self] gte 20) {
            If(health[self].rem(5) eq 0) {
                Command.particle(Particles.DUST(1.0, 0.0, 0.0, 1.0), rel(0, 0.2, 0), abs(0, 0, 0), 1.0, 5)
                Command.playsound("minecraft:block.note_block.bit").master('a'[""], rel(), 1.0, 2.0)
            }.Else {
                Command.particle(Particles.DUST(0.6, 0.3, 0.3, 1.0), rel(), abs(0, 0, 0), 1.0, 5)
            }
        }.Else {
            Command.particle(Particles.DUST(1.0, 0.0, 0.0, 2.0), rel(), abs(0, 0, 0), 1.0, 5)
            Command.playsound("minecraft:block.note_block.bit").master('a'[""], rel(), 1.0, 2.0)
        }
        If(health[self] lt range - activationDelay and 'a'["distance=..2"].notHasTag(shootTag).hasTag(playingTag)) {
            health[self] = 1
        }
        If('a'["distance=..5"].notHasTag(shootTag).hasTag(playingTag)) {
            val gt = Fluorite.reuseFakeScore("gametime")
            gt.set { Command.time().query.gametime }
            gt %= 4
            If (gt eq 0) {
                Command.execute().asat('a'[""].hasTag(shootTag)).run.playsound("minecraft:block.note_block.didgeridoo").master(
                    self, rel(), 1.0, 2.0)
            }
        }

    }

    mines = object : ModularCoasWeapon("Mine", 10000) {
        init {

            withRange(30 * 5)
            withProjectile(0, 5)
            withActivationDelay(2.0)
            withSplash(8.0)
            onProjectileTick {
                If(health[self] inRange range - activationDelay..range - activationDelay + 3) {
                    Command.execute()
                        .asat('a'[""].hasTag(shootTag)).run.playsound("minecraft:block.note_block.xylophone")
                        .master(self, rel(), 1.0, 1.8)
                }
                projectileTick(range, activationDelay)
            }
            withCooldown(3.0)
            withCustomModelData(11)
            canBeShot()
            addSound("minecraft:block.anvil.use", 0.7)
            onEntityHit { _, _ ->
                bigExplosion()
            }
            withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was blown to smithereens by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
            onWallHit {
                bigExplosion()
            }
            done()


            val detonateFunc = McFunction("$basePath/detonate") {
                Command.tag(self).add("safe")
                self.data["{}"] = "{PickupDelay:0s}"
                val uuidScore = Fluorite.reuseFakeScore("uuid")
                uuidScore.set(self.data["Thrower[0]"])
                Command.execute().As('a'[""]).run {
                    val myUUID = Fluorite.reuseFakeScore("uuid1")
                    myUUID.set(self.data["UUID[0]"])
                    If(uuidScore eq myUUID) {
                        val tempScore = Fluorite.reuseFakeScore("id")
                        tempScore.set(idScore[self])
                        Command.execute().asat('e'[""].hasTag(projectile)).If(idScore[self] eq tempScore)
                            .If(health[self] lt range - activationDelay).run {
                                health[self] = 5
                            }
                    }
                }
            }
            Fluorite.tickFile += {
                Command.execute().asat('e'["type = item", "nbt = {Item:{tag:{jh1236:{weapon:$myId}}}}"])
                    .run(detonateFunc)
            }
        }

    }


}

fun loadExperiments() {
    loadClip()
    loadLandMines()
}
