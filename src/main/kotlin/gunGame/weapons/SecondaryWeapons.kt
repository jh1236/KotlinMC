package gunGame.weapons

import abstractions.*
import abstractions.flow.If
import abstractions.flow.Tree
import commands.Command
import enums.*
import gunGame.*
import gunGame.weapons.primary.stoneSword
import lib.*
import structure.Fluorite
import structure.McFunction
import utils.*
import utils.score.Objective
import utils.score.Score
import kotlin.math.cos
import kotlin.math.sin

lateinit var pistol: ModularCoasWeapon
lateinit var tomeOfPetrification: ModularCoasWeapon
lateinit var teleport: ModularCoasWeapon
lateinit var leap: ModularCoasWeapon
lateinit var smokeCloud: AbstractWeapon
lateinit var invis: AbstractWeapon
lateinit var compass: AbstractWeapon
lateinit var staff: ModularCoasWeapon
lateinit var medusa: AbstractWeapon
const val pi = Math.PI
val medusaTag = PlayerTag("medusa")
val invisTag = PlayerTag("invis")

val resetMedusa = McFunction("jh1236:secondary/medusa") {
    applyCoolDown(160)
    Command.effect().clear(self, Effects.SLOWNESS)
    medusaTag.remove(self)
}

private fun loadPistol() {
    pistol =
        ModularCoasWeapon("Pistol", 600).withCooldown(.15).withClipSize(6).withReload(1.0).withParticle(Particles.CRIT)
            .addSound("ui.loom.select_pattern", 2.0).withCustomModelData(101).withRange(50).asSecondary()
            .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was popped by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""").done()
}

private fun loadStaff() {
    staff = object : ModularCoasWeapon("Staff", 25) {
        val staffTag = PlayerTag("staff")
        val fireFunc = McFunction("jh1236:secondary/staff/fire")
        val bonusDamage = Objective("staffDmg")

        init {
            withReload(1.0)
            withParticle(Particles.TOTEM_OF_UNDYING)
            withRange(25)
            withCustomModelData(103)
            withCooldown(.1)
            withKillMessage("""'["",{"selector": "@a[tag=$shootTag]","color": "gold"},{"text": " was trash and no-aimed "},{"selector": "@s","color": "gold"}]'""")
            asSecondary()
            onEntityHit { _, shooter ->
                staffTag.add(shooter)
                damageSelf(bonusDamage[shooter])
                bonusDamage[shooter] += damage
            }
            done()
            Fluorite.tickFile += {
                Command.execute().asat(
                    'a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}", "predicate = jh1236:ready"].hasTag(
                        staffTag
                    )
                )
                    .anchored(Anchor.EYES).facing(
                        'e'["sort = nearest", "limit = 1", "distance=.5.."].hasTag(playingTag).notHasTag(invisTag),
                        Anchor.EYES
                    ).run(fireFunc)

                staffTag.remove(
                    'a'["nbt =! {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"]
                )
            }

        }

        override fun shoot() {
            bonusDamage[self] = 0
            fireFunc.append {
                staffTag.remove(self)
                Command.execute().facing(
                    'e'["sort = nearest", "limit = 1", "distance=.5.."].hasTag(playingTag).notHasTag(invisTag),
                    Anchor.EYES
                ).run { super.shoot() }
                Command.execute().unless(
                    'e'["sort = nearest", "limit = 1", "distance=.5.."].hasTag(playingTag).notHasTag(invisTag)
                ).run {
                    staffTag.remove(self)
                    bonusDamage[self] = 0
                }
            }
            fireFunc()
        }

    }
}

private fun loadCompass() {
    compass = object : AbstractWeapon(0) {
        val func = McFunction("secondary/compass") {
            Command.execute().anchored(Anchor.EYES)
                .facing('e'["limit = 1", "sort = nearest", "distance = .5.."].hasTag(playingTag), Anchor.EYES)
                .positioned(loc(0, -.5, .25))
                .run {
                    raycast(
                        .25f,
                        { Command.particle(Particles.ELECTRIC_SPARK, rel(), abs(0, 0, 0), 0, 1).force(self) },
                        { },
                        4
                    )
                }
        }

        init {
            secondary = true
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
    teleport = object : ModularCoasWeapon("Tome of Teleportation", 200) {
        private fun calcDistance(): Score {
            val distance = Fluorite.getNewFakeScore("distance")
            val lastShot = Fluorite.reuseFakeScore("gametime")
            distance.set { Command.time().query.gametime }
            lastShot.set(self.data["SelectedItem.tag.jh1236.cooldown.value"])
            If(self["tag = noCooldown"]) { lastShot.set(0) }
            distance -= lastShot
            distance.minOf(100)
            distance *= 2
            distance /= 5
            distance.maxOf(4)
            return distance
        }

        val func = McFunction("secondary/tome_of_teleportation/tick") {
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
            withParticle(Particles.FALLING_DUST(Blocks.LIGHT_BLUE_CONCRETE))
            asSecondary()
            withCustomModelData(105)
            withCooldown(1.0)
            onWallHit {
                Command.tp(self, loc(0, 0, -.25))
                Command.playsound("entity.enderman.teleport").master(self)
            }
            onShoot {
                rangeScore.set(40)
                //TODO: stop being lazy
                Command.raw("particle minecraft:reverse_portal ~ ~.5 ~ 0.1 0.5 0.1 0.02 200 force @a")
                rangeScore.set(calcDistance())
            }
            afterShot {
                copyHeldItemToBlockAndRun {
                    it[""] = "{ Count:1 }"
                }
            }
            withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was displaced by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
            withPiercing()
            setup()
        }

    }
}

private fun loadPetrify() {
    tomeOfPetrification =
        ModularCoasWeapon("Tome of Petrification", 500).withCooldown(2.0).addSound("item.hoe.till", .1)
            .withParticle(Particles.SQUID_INK).onEntityHit { playerHit, _ ->
                Command.effect().give(playerHit, Effects.GLOWING, 2, 0, true)
                Command.effect().give(playerHit, Effects.SLOWNESS, 1, 11, true)
                Command.effect().give(playerHit, Effects.JUMP_BOOST, 1, 128, true)
                repeat(6) {
                    Command.playsound("entity.elder_guardian.curse").master(playerHit, rel(), 1.0)
                    Command.playsound("item.trident.thunder").master(playerHit, rel(), 1.0)
                    Command.playsound("entity.enderman.scream").master(playerHit, rel(), 1.0)
                }
            }.withProjectile(10).withSplash(0.5).onWallHit {
                Command.particle(Particles.SQUID_INK, rel(), abs(0, 0, 0), .3, 500)
            }.withRange(50).withCustomModelData(106).asSecondary()
            .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was turned to stone by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""").done()

}

private fun loadTraps() {
    object : AbstractWeapon(0) {
        init {
            secondary = true
        }

        override fun give(player: Selector) {
        }

    }
}

private fun loadAura() {

    smokeCloud = object : AbstractWeapon(0) {
        val smokeTag = PlayerTag("smoke")

        private fun smoke(radius: Double) {
            Command.execute().anchored(Anchor.EYES).run.particle(
                Particles.SQUID_INK, loc(), abs(radius, radius, radius), .05, (200 * radius / .7).toInt()
            ).force(self)
        }

        private val becomeSmoke = McFunction("secondary/smoke") {
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

        private val smokeTick = McFunction("secondary/smoke/tick") {
            val id = Fluorite.getNewFakeScore("id")
            id.set(idScore[self])
            Command.execute().As('a'[""].hasTag(playingTag)).unless(idScore[self] eq id).run { smoke(1.4) }
            Command.execute().As('a'[""].hasTag(playingTag)).If(idScore[self] eq id).run {
                Command.particle(Particles.DUST(0.0, 0.0, 0.0, .7), rel(), abs(1.4, 1.4, 1.4), 0.0, 100)
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
            Command.give(self, Items.BLACK_DYE.nbt("{jh1236:{weapon:$myId}}"))
        }

    }
}

private fun loadMedusa() {
    medusa = object : AbstractWeapon(0) {
        val medusaMatch = McFunction("secondary/medusa/particle") {
            medusaTag.add(self)
            Command.effect().give(self, Effects.SLOWNESS, 2, 4, true)
            val gt = Fluorite.reuseFakeScore("gametime")
            gt.set { Command.time().query.gametime }
            gt %= 50
            Tree(gt, 0..50) {
                val y = kotlin.math.abs(2 - ((it.toDouble() / 12.5) % 4))
                val coords =
                    rel(.7 * cos(8 * (it.toDouble() * pi) / 50), y, .7 * sin(8 * (it.toDouble() * pi) / 50))
                val coords2 = rel(.7 * sin(8 * ((25 + it) * pi) / 50), y, .7 * cos(8 * ((25 + it) * pi) / 50))
                Command.particle(
//                Particles.DUST_COLOR_TRANSITION(0.0, 0.0, 1.0, 1.0, 0.6, 1.0, 1.0),
                    Particles.SCRAPE,
                    coords,
                    abs(0, 0, 0),
                    0.0,
                    1
                )
                Command.particle(
//                Particles.DUST_COLOR_TRANSITION(0.0, 0.0, 1.0, 1.0, 0.6, 1.0, 1.0),
                    Particles.SCRAPE,
                    coords2,
                    abs(0, 0, 0),
                    0.0,
                    1
                )
            }
        }

        init {
            secondary = true
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
    invis = object : AbstractWeapon(0) {

        val func = McFunction("secondary/stealth") {
            If(self.notHasTag(invisTag)) {
                health[self].minOf(1000)
                maxHealth[self] = 1000
            }
            Command.effect().give(self, Effects.INVISIBILITY, 1, 0, true)
            val gt = Fluorite.reuseFakeScore("gametime")
            gt.set { Command.time().query.gametime }
            gt %= 10
            If(gt eq 0) {
                Command.particle(Particles.END_ROD, rel(0, .5, 0), abs(0.2, .5, 0.2), 0.0, 1)
            }
            invisTag.add(self)
        }
        val endFunc = McFunction("secondary/stealth/end") {
            Command.effect().clear(self, Effects.INVISIBILITY)
            maxHealth[self] = 3000
            invisTag.remove(self)
        }

        init {
            secondary = true
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

    leap = object : ModularCoasWeapon("Tome of Propulsion", 200) {
        private fun calcDistance(): Score {
            val distance = Fluorite.reuseFakeScore("distance")
            val lastShot = Fluorite.reuseFakeScore("gametime")
            distance.set { Command.time().query.gametime }
            lastShot.set(self.data["SelectedItem.tag.jh1236.cooldown.value"])
            If(self["tag = noCooldown"]) { lastShot.set(0) }
            distance -= lastShot
            distance.minOf(75)
            distance *= 3
            distance /= 5
            distance.maxOf(4)
            return distance
        }

        val func = McFunction("secondary/tome_of_propulsion/tick") {
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
            withParticle(Particles.SCRAPE)
            asSecondary()
            withCustomModelData(111)
            withCooldown(1.5)
            withProjectile(3)
            onWallHit {
                Command.playsound("minecraft:entity.generic.extinguish_fire").master('a'[""], rel(), .6, 1.0)
                Command.particle(Particles.TOTEM_OF_UNDYING, rel(), abs(.2, .2, .2), 1.0, 100)
                Command.effect().clear('a'[""].hasTag(shootTag), Effects.LEVITATION)
            }
            onProjectileTick {
                Command.tp('a'[""].hasTag(shootTag), rel())
                Command.execute().rotated.As('a'[""].hasTag(shootTag)).run.tp(self, rel(), Vec2("~", "~"))
                Command.effect().give('a'[""].hasTag(shootTag), Effects.LEVITATION, 1000, 255, true)
                Command.execute().unless('a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(shootTag))
                    .run {
                        onWallHit!!('a'[""].hasTag(shootTag))
                        Command.kill()
                    }
            }
            onShoot {
                rangeScore.set(calcDistance())
                Command.kill('e'[""].hasTag(projectile))
            }
            afterShot {
                copyHeldItemToBlockAndRun {
                    it[""] = "{ Count:1 }"
                }
            }
            withPiercing()
            setup()
        }
    }
}

fun loadSecondaries() {
    loadPistol()
    loadStaff()
    loadCompass()
    stoneSword()
    loadTp()
    loadPetrify()
    loadTraps()
    loadAura()
    loadMedusa()
    loadInvis()
    loadLeap()
}
