package gunGame.weapons

import abstractions.*
import abstractions.flow.If
import abstractions.flow.Tree
import commands.Command
import enums.*
import gunGame.*
import gunGame.weapons.impl.stoneSword
import gunGame.weapons.impl.tomeOfAir
import lib.*
import structure.Fluorite
import structure.McFunction
import utils.Selector
import utils.Vec2
import utils.loc
import utils.rel
import utils.score.Score
import kotlin.math.cos
import kotlin.math.sin

lateinit var pistol: RaycastWeapon
lateinit var tomeOfPetrification: ProjectileWeapon
lateinit var teleport: RaycastWeapon
lateinit var leap: ProjectileWeapon
lateinit var smokeCloud: AbstractWeapon
lateinit var trapPlanter: AbstractWeapon
lateinit var invis: AbstractWeapon
lateinit var compass: AbstractWeapon
lateinit var medusa: AbstractWeapon
const val pi = Math.PI
val medusaTag = PlayerTag("medusa")
val invisTag = PlayerTag("invis")

val resetMedusa = McFunction("jh1236:weapons/secondary/medusa") {
    applyCoolDown(160)
    Command.effect().clear(self, Effects.SLOWNESS)
    medusaTag.remove(self)
}

private fun loadPistol() {
    pistol =
        RaycastBuilder("Pistol", 600).withCooldown(.15).withClipSize(6).withReload(1.0).withParticle(Particles.CRIT)
            .addSound("ui.loom.select_pattern", 2.0).withCustomModelData(101).withRange(50).asSecondary()
            .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was popped by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
            .done()
}

private fun loadCompass() {
    compass = object : AbstractWeapon("Compass", 0, true) {
        val func = McFunction(basePath) {
            Command.execute().anchored(Anchor.EYES)
                .facing('a'["limit = 1", "sort = nearest", "distance = .5.."].hasTag(playingTag), Anchor.EYES)
                .positioned(loc(0, -.5, .25)).run {
                    raycast(
                        .25f,
                        { Command.particle(Particles.ELECTRIC_SPARK, rel(), 0, 0, 0, 0, 1).force(self) },
                        { },
                        4
                    )
                }
        }

        init {
            Fluorite.tickFile += {
                Command.execute().asat('a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(playingTag))
                    .run(func)
            }
        }


        override fun give(player: Selector) {
            Command.give(
                self,
                Items.COMPASS.nbt("""{jh1236:{weapon:$myId}, display : {Name:'{"text":"Tracking Compass", "italic":false}'}}""")
            )
        }

    }
}

private fun loadTp() {
    teleport = object : RaycastWeapon(
        "Tome of Teleportation",
        200,
        105,
        1.0,
        particle = Particles.FALLING_DUST(Blocks.LIGHT_BLUE_CONCRETE),
        onWallHit = {
            Command.tp(self, loc(0, 0, -.25))
            Command.playsound("entity.enderman.teleport").master(self)
        },
        secondary = true,
        piercing = true,
        killMessage = """'["",{"selector": "@s","color": "gold"},{"text": " was displaced by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""",
        range = -1
    ) {
        private fun calcDistance(): Score {
            val distance = Fluorite.getNewFakeScore("distance")
            distance.set { Command.time().query.gametime }
            If(self["tag = noCooldown"]) { cdBeforeShot.set(0) }
            distance -= cdBeforeShot
            distance.minOf(100)
            distance *= 2
            distance /= 5
            distance.maxOf(4)
            return distance
        }

        val func = McFunction("$basePath/tick") {
            cdBeforeShot.set(self.data["SelectedItem.tag.jh1236.cooldown.value"])
            val ammoDisplay = calcDistance()
            rangeScore.set(ammoDisplay)
            //TODO: stop being lazy
            Command.execute().anchored(Anchor.EYES).run {
                raycast(.25f,
                    {},
                    { Command.raw("particle minecraft:soul_fire_flame ^ ^ ^-0.25 0.2 0.4 0.2 0.02 0 force @s") })
            }
            ammoDisplay /= 4
            copyHeldItemToBlockAndRun {
                it["Count"] = ammoDisplay
            }
        }

        init {
            Fluorite.tickFile += {
                Command.execute()
                    .asat('a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}", "predicate = jh1236:ready"])
                    .run(func)
            }
            setup()
        }

        override fun fire() {
            //TODO: stop being lazy
            Command.raw("particle minecraft:reverse_portal ~ ~.5 ~ 0.1 0.5 0.1 0.02 200 force @a")
            rangeScore.set(calcDistance())
            super.fire()
            copyHeldItemToBlockAndRun {
                it[""] = "{Count:1}"
            }
        }
    }
}

private fun loadPetrify() {
    tomeOfPetrification =
        ProjectileBuilder("Tome of Petrification", 500).withCooldown(2.0).addSound("item.hoe.till", .1)
            .withParticle(Particles.SQUID_INK).onEntityHit { playerHit, _ ->
                Command.effect().give(playerHit, Effects.GLOWING, 2, 0, true)
                Command.effect().give(playerHit, Effects.SLOWNESS, 1, 11, true)
                Command.effect().give(playerHit, Effects.JUMP_BOOST, 1, 128, true)
                repeat(6) {
                    Command.playsound("entity.elder_guardian.curse").master(playerHit, rel(), 1.0)
                    Command.playsound("item.trident.thunder").master(playerHit, rel(), 1.0)
                    Command.playsound("entity.enderman.scream").master(playerHit, rel(), 1.0)
                }
            }.withProjectile(15).withSplash(0.5).onWallHit {
                Command.particle(Particles.SQUID_INK, rel(), 0, 0, 0, .3, 500)
            }.withRange(50).withCustomModelData(106).asSecondary()
            .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was turned to stone by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
            .done()

}

private fun loadTraps() {


    trapPlanter =
        ProjectileBuilder("Trap Planter", 500).canBeShot().withRange(300).withProjectile(0, 3).onProjectileTick {
            If(health[self] gte range - 60) {
                Command.particle(Particles.DUST(.4, 0.8, 0.9, 0.7), rel(), 0, 0, 0, 1.0, 5)
            }.Else {
                Command.particle(Particles.DUST(.7, 0.7, 0.7, .45), rel(), 0, 0, 0, 1.0, 5)
            }
            If(
                health[self] lte range - activationDelay and 'a'["distance=..1.5"].notHasTag(shootTag)
                    .hasTag(playingTag)
            ) {
                Command.execute().asat('a'["distance=..1.5"].notHasTag(shootTag).hasTag(playingTag)).run {
                    onEntityHit?.let { it1 -> this.it1(self, 'a'[""].hasTag(shootTag)) }
                    damageSelf(damage)
                }
                Command.kill(self)
            }
        }
            .withActivationDelay(3.0)
            .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was trapped by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
            .withCooldown(3.0).withCustomModelData(108).addSound("minecraft:ui.stonecutter.take_result", 0.8)
            .onEntityHit { _, _ ->
                Command.effect().give(self, Effects.BLINDNESS, 2, 0, true)
                Command.effect().give(self, Effects.SLOWNESS, 2, 11, true)
                Command.effect().give(self, Effects.JUMP_BOOST, 2, 128, true)
                Command.playsound("minecraft:entity.zombie.attack_iron_door").master(self, rel(), 1, .7)
                Command.playsound("minecraft:entity.zombie.attack_iron_door")
                    .master('a'["distance=.1.."], rel(), .7, .7)
                Command.title(self).times(0, 20, 0)
                Command.title(self)
                    .title("""[{"text":"You have been ", "color": "gold"}, {"text": "TRAPPED","color":"dark_red", "bold": true}]""")
                Command.tellraw(
                    'a'[""],
                    """["",{"selector":"@s", "color" : "gold"}," has been ", {"text": "TRAPPED","color":"dark_red"}]"""
                )

            }.asSecondary().done()
}


private fun loadAura() {

    smokeCloud = object : AbstractWeapon("Sinister Aura", 0, true) {
        val smokeTag = PlayerTag("smoke")

        private fun smoke(radius: Double) {
            Command.execute().anchored(Anchor.EYES).run.particle(
                Particles.SQUID_INK, loc(), radius, radius, radius, .05, (200 * radius / .7).toInt()
            ).force(self)
        }

        private val becomeSmoke = McFunction(basePath) {
            smokeTag.add(self)
            Command.tag(self).add("safe")
            self.data["{}"] = "{PickupDelay:85s}"
            val uuidScore = Fluorite.reuseFakeScore("uuid")
            uuidScore.set(self.data["Thrower[0]"])
            Command.execute().As('a'[""]).run {
                val myUUID = Fluorite.reuseFakeScore("uuid1")
                myUUID.set(self.data["UUID[0]"])
                If(uuidScore eq myUUID) {
                    idScore['e'["sort = nearest", "limit = 1"]] = idScore[self]
                }
            }
        }

        private val smokeTick = McFunction("$basePath/tick") {
            val id = Fluorite.getNewFakeScore("id")
            id.set(idScore[self])
            Command.execute().As('a'[""].hasTag(playingTag)).unless(idScore[self] eq id).run { smoke(1.4) }
            Command.execute().As('a'[""].hasTag(playingTag)).If(idScore[self] eq id).run {
                Command.particle(Particles.DUST(0.0, 0.0, 0.0, .7), rel(), 1.4, 1.4, 1.4, 0.0, 100)
            }
            If(self["nbt = {PickupDelay:5s}"]) {
                Command.execute().at('a'[""].hasTag(playingTag)).If(idScore['p'[""]] eq id).run {

                    Command.tp(rel())
                    self.data["{}"] = "{PickupDelay:0s}"
                }
            }
        }

        init {
            secondary = true
            deathEvent += {
                smokeTag.remove(self)
            }
            Fluorite.tickFile += {
                Command.execute().asat('a'[""]).If(self hasData "{Inventory:[{tag:{jh1236:{weapon:$myId}}}]}").run {
                    Command.execute().As('a'["distance = 0.1.."].hasTag(playingTag)).run { smoke(.7) }
                }
                Command.execute()
                    .asat('e'["type = item"].notHasTag(smokeTag)["nbt = {Item:{tag:{jh1236:{weapon:$myId}}}}"])
                    .run { becomeSmoke() }

                Command.execute().asat('e'[""].hasTag(smokeTag)).run { smokeTick() }
            }
        }

        override fun give(player: Selector) {
            Command.give(
                self,
                Items.BLACK_DYE.nbt("""{jh1236:{weapon:$myId}, display:{Name:'{"text":"Sinister Aura","italic":false}'}}""")
            )
        }

    }
}

private fun loadMedusa() {
    medusa = object : AbstractWeapon("Medusas Curse", 0, true) {
        val medusaMatch = McFunction("$basePath/particle") {
            medusaTag.add(self)
            Command.effect().give(self, Effects.SLOWNESS, 2, 4, true)
            val gt = Fluorite.reuseFakeScore("gametime")
            gt.set { Command.time().query.gametime }
            gt %= 50
            Tree(gt, 0..50) {
                val y = kotlin.math.abs(2 - ((it.toDouble() / 12.5) % 4))
                val coords = rel(.7 * cos(8 * (it.toDouble() * pi) / 50), y, .7 * sin(8 * (it.toDouble() * pi) / 50))
                val coords2 = rel(.7 * sin(8 * ((25 + it) * pi) / 50), y, .7 * cos(8 * ((25 + it) * pi) / 50))
                Command.particle(
//                Particles.DUST_COLOR_TRANSITION(0.0, 0.0, 1.0, 1.0, 0.6, 1.0, 1.0),
                    Particles.SCRAPE, coords, 0, 0, 0, 0.0, 1
                )
                Command.particle(
//                Particles.DUST_COLOR_TRANSITION(0.0, 0.0, 1.0, 1.0, 0.6, 1.0, 1.0),
                    Particles.SCRAPE, coords2, 0, 0, 0, 0.0, 1
                )
            }
        }

        init {
            Fluorite.tickFile += {
                Command.execute().asat(
                    'a'["predicate = jh1236:ready", "nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(
                        playingTag
                    )
                ).run(medusaMatch)


                Command.execute().asat('a'[""].hasTag(playingTag).hasTag(medusaTag)).run {
                    If(!(self["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"])) {
                        medusaTag.remove(self)
                        Command.effect().clear(self, Effects.SLOWNESS)
                    }
                }
            }
        }

        override fun give(player: Selector) {
            Command.give(
                player,
                Items.GUNPOWDER.nbt("{jh1236:{weapon:$myId}, display : {Name:'{\"text\":\"Medusa\\'s curse\", \"italic\":false}'}}")
            )
        }
    }
}

private fun loadInvis() {
    invis = object : AbstractWeapon("stealth", 0, true) {

        val func = McFunction(basePath) {
            If(self.notHasTag(invisTag)) {
                If(health[self] gt maxHealth[self]) {
                    val dif = Fluorite.reuseFakeScore("health")
                    dif.set(maxHealth[self])
                    dif -= 1000
                    health[self] -= dif
                }.Else {
                    health[self].minOf(1000)
                }
                maxHealth[self] = 1000
            }
            Command.effect().give(self, Effects.INVISIBILITY, 1, 0, true)
            val gt = Fluorite.reuseFakeScore("gametime")
            gt.set { Command.time().query.gametime }
            gt %= 10
            If(gt eq 0) {
                Command.particle(Particles.END_ROD, rel(0, .5, 0), 0.2, .5, 0.2, 0.0, 1)
            }
            invisTag.add(self)
        }
        val endFunc = McFunction("$basePath/end") {
            Command.effect().clear(self, Effects.INVISIBILITY)
            maxHealth[self] = 3000
            invisTag.remove(self)
        }

        init {
            Fluorite.tickFile += {
                Command.execute().asat('a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(playingTag))
                    .run(func)
                Command.execute().asat('a'["nbt =! {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(invisTag))
                    .run(endFunc)
            }
        }


        override fun give(player: Selector) {
            Command.give(
                self,
                Items.SUGAR.nbt("""{jh1236:{weapon:$myId}, display : {Name:'{"text":"Veil Of Stealth", "italic":false}'}}""")
            )
        }

    }
}

private fun loadLeap() {

    leap =
        object : ProjectileWeapon(
            "Tome of Propulsion",
            0, 111, 1.5,
            projectileSpeed = 3,
            particle = Particles.SCRAPE,
            secondary = true,
            onWallHit = {
                Command.playsound("minecraft:entity.generic.extinguish_fire").master('a'[""], rel(), .6, 1.0)
                Command.particle(Particles.TOTEM_OF_UNDYING, rel(), .2, .2, .2, 1.0, 100)
                Command.effect().clear('a'[""].hasTag(shootTag), Effects.LEVITATION)
            },
            onProjectileTick = {
                Command.tp('a'[""].hasTag(shootTag), rel())
                Command.execute().rotated.As('a'[""].hasTag(shootTag)).run.tp(self, rel(), Vec2("~", "~"))
                Command.effect().give('a'[""].hasTag(shootTag), Effects.LEVITATION, 1000, 255, true)
                Command.execute().unless('a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(shootTag))
                    .run {
                        this.onWallHit!!('a'[""].hasTag(shootTag))
                        Command.kill()
                    }
            }
        ) {
            private fun calcDistance(): Score {
                val distance = Fluorite.reuseFakeScore("distance")

                distance.set { Command.time().query.gametime }

                If(self["tag = noCooldown"]) { cdBeforeShot.set(0) }
                distance -= cdBeforeShot
                distance.minOf(75)
                distance *= 3
                distance /= 5
                distance.maxOf(4)
                return distance
            }

            val func = McFunction("$basePath/tick") {
                cdBeforeShot.set(self.data["SelectedItem.tag.jh1236.cooldown.value"])
                val ammoDisplay = calcDistance()
                ammoDisplay /= 3
                copyHeldItemToBlockAndRun {
                    it["Count"] = ammoDisplay
                }
            }

            init {
                Fluorite.tickFile += {
                    Command.execute()
                        .asat('a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}", "predicate = jh1236:ready"])
                        .run(func)
                }
                setup()
            }

            override fun fire() {
                rangeScore.set(calcDistance())
                Command.kill('e'[""].hasTag(projectile))
                super.fire()
                copyHeldItemToBlockAndRun {
                    it[""] = "{ Count:1 }"
                }
            }
        }
}

private fun loadTome() {

    fun tomeHitsWall() {
        Command.particle(Particles.FLASH, rel(), 0, 0, 0, 1.0, 20)
        Command.particle(Particles.CLOUD, rel(), 0.1, 0.1, 0.1, 1.0, 20)
        Command.playsound("minecraft:item.firecharge.use").master('a'[""], rel(), 1.0)
    }
    tomeOfAir =
        ProjectileBuilder("Tome of Fire", 3000).withCooldown(4.0).withParticle(Particles.FLAME, 10).withProjectile(1, 1)
            .withRange(50).withCustomModelData(7).addSound("minecraft:entity.tnt.primed", 1.3).onWallHit {
                tomeHitsWall()
            }.onEntityHit { _, _ ->
                tomeHitsWall()
            }.onProjectileTick {
                Command.particle(Particles.DUST(1.0, 0.0, 0.0, 2.0), rel(), .2, .2, .2, 1, 10)
                Command.particle(Particles.FLAME, rel(), .2, .2, .2, .1, 10)
                Command.execute().lerpFacing('p'[""].notHasTag(shootTag).hasTag(playingTag), 1, 7).run.tp(
                    self, rel(), Vec2("~", "~")
                )
            }
            .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " got smoked by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
            .asSecondary().done()

}


fun loadSecondaries() {
    loadPistol()
    loadTome()
    stoneSword()
    loadCompass()
    loadTp()
    loadPetrify()
    loadAura()
    loadTraps()
    loadMedusa()
    loadInvis()
    loadLeap()
}
