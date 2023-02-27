package gunGame.weapons.impl

import abstractions.PlayerTag
import abstractions.advancements.Advancement
import abstractions.advancements.PlayerInteractedWithEntity
import abstractions.asat
import abstractions.flow.If
import abstractions.hasTag
import abstractions.notHasTag
import abstractions.score.Objective
import abstractions.score.ScoreConstant
import commands.Command
import enums.Entities
import enums.Items
import enums.Particles
import gunGame.*
import gunGame.weapons.*
import lib.debug.Log
import structure.Fluorite
import structure.McFunction
import utils.get
import utils.rel

lateinit var clipOfDexterity: AbstractCoasWeapon
lateinit var mines: ProjectileWeapon
lateinit var spinGun: RaycastWeapon

val speedyAmmo = PlayerTag("speed_ammo")

val shotCount = Objective("shotsRemaining")
val spinsCount = Objective("spinsRemaining")


fun loadClip() {
    clipOfDexterity = object : AbstractCoasWeapon("Clip of Dexterity", 0, 113, -1.0) {
        init {
            secondary = true
            setup()
            Fluorite.tickFile += {
                Command.execute().asat('a'[""].hasTag(playingTag)).run {
                    McFunction("$basePath/ammo") {
                        val temp = Fluorite.reuseFakeScore("temp")
                        temp.set(shotCount[self])
                        temp.maxOf(1)
                        setAmmoForId(ScoreConstant(myId), temp)
                    }()
                }
            }
        }


        override fun fire() {
            Log.info("asdasd")
            Command.playsound("minecraft:item.spyglass.use").master(self, rel(), 1.0, 0.0)
            Command.playsound("minecraft:item.spyglass.use").master(self, rel(), 1.0, 0.0)
            Command.playsound("minecraft:item.spyglass.use").master(self, rel(), 1.0, 0.0)
            Command.playsound("minecraft:item.spyglass.use").master(self, rel(), 1.0, 0.0)
            speedyAmmo.add(self)
            shotCount[self] += 6
        }


    }
}

fun loadLandMines() {
    fun projectileTick(range: Int, activationDelay: Int, id: Int) {
        with(Command) {
            If(health[self] gt range - activationDelay) {
                particle(Particles.DUST(0.6, 0.3, 0.3, 1.0), rel(), 0, 0, 0, 1.0, 5)
            }.ElseIf(health[self] gte 100) {
                If(health[self].rem(40) eq 0) {
                    particle(Particles.DUST(1.0, 0.0, 0.0, 1.0), rel(0, 0.2, 0), 0, 0, 0, 1.0, 5)
                    playsound("minecraft:block.note_block.bit").master('a'[""], rel(), .1, 1.0)
                }.Else {
                    particle(Particles.DUST(0.6, 0.3, 0.3, 1.0), rel(), 0, 0, 0, 1.0, 5)
                }
            }.ElseIf(health[self] gte 40) {
                If(health[self].rem(10) eq 0) {
                    particle(Particles.DUST(1.0, 0.0, 0.0, 1.0), rel(0, 0.2, 0), 0, 0, 0, 1.0, 5)
                    playsound("minecraft:block.note_block.bit").master('a'[""], rel(), .3, 1.0)
                }.Else {
                    particle(Particles.DUST(0.6, 0.3, 0.3, 1.0), rel(), 0, 0, 0, 1.0, 5)
                }
            }.ElseIf(health[self] gte 20) {
                If(health[self].rem(5) eq 0) {
                    particle(Particles.DUST(1.0, 0.0, 0.0, 1.0), rel(0, 0.2, 0), 0, 0, 0, 1.0, 5)
                    playsound("minecraft:block.note_block.bit").master('a'[""], rel(), 1.0, 2.0)
                }.Else {
                    particle(Particles.DUST(0.6, 0.3, 0.3, 1.0), rel(), 0, 0, 0, 1.0, 5)
                }
            }.Else {
                particle(Particles.DUST(1.0, 0.0, 0.0, 2.0), rel(), 0, 0, 0, 1.0, 5)
                playsound("minecraft:block.note_block.bit").master('a'[""], rel(), 1.0, 2.0)
            }
            If('e'["distance=..5"].notHasTag(shootTag).hasTag(playingTag)) {
                val gt = Fluorite.reuseFakeScore("gametime")
                gt.set { time().query.gametime }
                gt %= 4
                If(gt eq 0) {
                    execute()
                        .As('a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$id}}}}"].hasTag(shootTag)).run {
                            execute().at(self).run.playsound("minecraft:block.note_block.didgeridoo")
                                .master(
                                    self, rel(), 1.0, 2.0
                                )
                        }

                }
            }
        }

    }



    mines = ProjectileBuilder("Mine", 10000)

        .withRange(30 * 5)
        .canHitOwner()
        .withProjectile(0, 5)
        .withActivationDelay(2.0)
        .withSplash(8.0)
        .onProjectileTick {
            If(health[self] inRange range - activationDelay..range - activationDelay + 3) {
                Command.execute()
                    .asat('a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(shootTag)).run.playsound("minecraft:block.note_block.xylophone")
                    .master(self, rel(), 1.0, 1.8)
            }
            projectileTick(range, activationDelay, myId)
        }
        .withCooldown(3.0)
        .withCustomModelData(11)
        .canBeShot()
        .addSound("minecraft:block.anvil.use", 0.7)
        .onEntityHit { _, _ ->
            bigExplosion()
        }
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was blown to smithereens by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .onWallHit {
            bigExplosion()
        }
        .done()
    val detonateFunc = McFunction("weapons/primary/mine/detonate") {
        Command.tag(self).add("safe")
        self.data["{}"] = "{PickupDelay:0s}"

        val uuidScore = Fluorite.reuseFakeScore("uuid")
        uuidScore.set(self.data["Thrower[0]"])
        Command.execute().As('a'[""]).run {
            val myUUID = Fluorite.reuseFakeScore("uuid1")
            myUUID.set(self.data["UUID[0]"])
            If(uuidScore eq myUUID) {
                Command.clear(self, Items.CARROT_ON_A_STICK.nbt("{jh1236:{weapon:${mines.myId}}}"))
                val tempScore = Fluorite.reuseFakeScore("id")
                tempScore.set(idScore[self])
                Command.execute().asat('e'[""].hasTag(mines.projectile)).If(idScore[self] eq tempScore)
                    .If(health[self] lt mines.range - mines.activationDelay).run {
                        playingTag.remove(self)
                        health[self] = 5
                    }
            }
        }
    }
    Fluorite.tickFile += {
        Command.execute().asat('e'["type = item", "nbt = {Item:{tag:{jh1236:{weapon:${mines.myId}}}}}"])
            .run(detonateFunc)
        Command.execute().asat('a'[""].hasTag(playingTag)).run {
            McFunction("${mines.basePath}/ammo") {
                val countScore = Fluorite.reuseFakeScore("temp", 0)
                val tempScore = Fluorite.reuseFakeScore("id")
                tempScore.set(idScore[self])
                Command.execute().As('e'[""].hasTag(mines.projectile)).If(idScore[self] eq tempScore).run {
                    countScore += 1
                }
                countScore.maxOf(1)
                setAmmoForId(ScoreConstant(mines.myId), countScore)
            }()
        }
    }
}

fun loadExperiments() {
    loadClip()
    loadLandMines()
    Raygun()
}
