package gunGame.weapons

import abstractions.*
import abstractions.flow.If
import commands.Command
import enums.*
import gunGame.*
import lib.*
import structure.Fluorite
import structure.McFunction
import utils.*
import utils.score.Score

lateinit var pistol: ModularCoasWeapon
lateinit var tomeOfPetrification: ModularCoasWeapon
lateinit var teleport: ModularCoasWeapon
lateinit var smokeCloud: AbstractWeapon
lateinit var boom: AbstractWeapon
lateinit var stealth: AbstractWeapon
lateinit var staff: ModularCoasWeapon
lateinit var medusa: 


fun loadSecondaries() {
    pistol = ModularCoasWeapon("Pistol", 600)
        .withCooldown(.15)
        .withClipSize(6)
        .withReload(1.0)
        .withParticle(Particles.CRIT)
        .addSound("ui.loom.select_pattern", 2.0)
        .withCustomModelData(101)
        .withRange(50)
        .asSecondary()
        .done()
    
    staff = object: ModularCoasWeapon("staff", 25) {
        val staffTag = PlayerTag("staff")
        val fireFunc = McFunction("jh1236:secondary/staff/fire")
        val damageScore = Objective("staffDmg")
        init{
            Fluorite.tickFile += {
                Command.execute().asat('a'[""].hasTag(staffTag)).anchored(Anchor.EYES).run(shootFunction)
            }
            withReload(1)
            withParticle(Particles.TOTEM_OF_UNDYING)
            withRange(25)
            asSecondary()
            onEntityHit {(_, shooter) -> 
                staffTag.add(shooter)
            }
            done()
        }

        override fun shoot() {
            damageScore[self] = 0
            fireFunc.append {
                staffTag.remove(self)
                damageScore[self] += damage
                super.shoot()
            }
            fireFunc()
        }

    }

    tomeOfPetrification = ModularCoasWeapon("Tome of Petrification", 500)
        .withCooldown(2.0)
        .addSound("item.hoe.till", .1)
        .withParticle(Particles.SQUID_INK)
        .onEntityHit { playerHit, _ ->
            Command.effect().give(playerHit, Effects.GLOWING, 2, 0, true)
            Command.effect().give(playerHit, Effects.SLOWNESS, 1, 11, true)
            Command.effect().give(playerHit, Effects.JUMP_BOOST, 1, 128, true)
            repeat(6) {
                Command.playsound("entity.elder_guardian.curse").master(playerHit, rel(), 1.0)
                Command.playsound("item.trident.thunder").master(playerHit, rel(), 1.0)
                Command.playsound("entity.enderman.scream").master(playerHit, rel(), 1.0)
            }
        }
        .onWallHit {
            Command.particle(Particles.SQUID_INK, rel(), abs(0, 0, 0), .3, 500)
        }
        .withRange(50)
        .withCustomModelData(106)
        .asSecondary()
        .done()

    teleport = object : ModularCoasWeapon("Tome of Teleportation", 200) {
        private fun calcDistance(): Score {
            val distance = Fluorite.getNewFakeScore("distance")
            val max = Fluorite.getNewFakeScore("max")
            max.set(self.data["SelectedItem.tag.jh1236.cooldown.max"])
            val lastShot = Fluorite.getNewFakeScore("gt")
            distance.set { Command.time().query.gametime }
            lastShot.set(self.data["SelectedItem.tag.jh1236.cooldown.value"])
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
            withPiercing()
            setup()
        }

        override fun shoot() {
            rangeScore.set(40)
            //TODO: stop being lazy
            Command.raw("particle minecraft:reverse_portal ~ ~.5 ~ 0.1 0.5 0.1 0.02 200 force @a")
            rangeScore.set(calcDistance())
            super.shoot()
            copyHeldItemToBlockAndRun {
                it[""] = "{ Count:1 }"
            }
        }
    }

    boom = ModularCoasWeapon("Bang", 0).withCooldown(4.0).withParticle(Particles.DUST(1.0, 0.0, 0.0, 1.2), 10)
        .withProjectile(1)
        .withRange(50).withCustomModelData(4).addSound("minecraft:block.enchantment_table.use", 1.3).onWallHit {
        }.onWallHit {
            val selector = 'a'["distance=..10", "limit = 1", "sort = nearest"].hasTag(playingTag)
            Command.execute().facing.entity(selector, Anchor.EYES).positioned.As(selector.hasTag(playingTag)).rotated(
                Vec2("~", "0")
            )
                .positioned(loc(0, 0, -.3)).run {
                    repeat(1) {
                        Command.summon(Entities.CREEPER, rel(0, 0.4, 0), "{ExplosionRadius:-1, Fuse:0b, ignited:1b}")
                    }
                }
        }.onEntityHit { _, _ ->
            Command.execute().asat('a'["distance=..10"].hasTag(playingTag)).run {
                repeat(1) {
                    Command.summon(Entities.CREEPER, rel(0, .5, 0), "{ExplosionRadius:-1, Fuse:0b, ignited:1b}")
                }
            }
        }.done()


    smokeCloud = object : AbstractWeapon(0) {
        val smokeTag = PlayerTag("smoke")

        private fun smoke(radius: Double) {
            Command.particle(
                Particles.SQUID_INK,
                loc(),
                abs(radius, radius, radius),
                .05,
                (200 * radius / .7).toInt()
            ).force(self)
        }

        private val becomeSmoke = McFunction("secondary/smoke") {
            smokeTag.add(self)
            self.data["{}"] = "{PickupDelay:85s}"
            val uuidScore = Fluorite.getNewFakeScore("uuid")
            uuidScore.set(self.data["Thrower[0]"])
            Command.execute().As('a'[""]).run {
                val myUUID = Fluorite.getNewFakeScore("uuid1")
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
                Command.execute().asat('a'[""])
                    .If(self hasData "{Inventory:[{tag:{jh1236:{weapon:$myId}}}]}").run {
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

    medusa = object : AbstractWeapon(0) {
        
        val medusaTag = PlayerTag("medusa")

        val resetMedusa = McFunction("jh1236:secondary/medusa") {
            applyCooldown(60)
            Command.effect().clear(self, Effects.SLOWNESS)
        }

        init {
            Fluorite.tickFile += {
                Command.execute().asat('a'["predicate = jh1236:ready","nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(playingTag)).run {
                    medusaTag.add(self)
                    Command.effect().give(self, Effects.SLOWNESS, 2, 2, true)
                }
                Command.execute().asat('a'[""].hasTag(playingTag)).run {
                    If (!(self["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}")) {
                        medusaTag.remove(self)
                        Command.effect().clear(self, Effects.SLOWNESS)
                    }
                }
            }
        }

        override fun give(){ 
            Command.give(self, Items.GUNPOWDER.nbt("{jh1236:{weapon:$myId}}"))
        }
    }

    stealth = object : AbstractWeapon(0) {
        val invisTag = PlayerTag("invis")
        val func = McFunction("secondary/stealth") {
            If(self.notHasTag(invisTag)) {
                health[self].minOf(1000)
                maxHealth[self] = 1000
            }
            Command.effect().give(self, Effects.INVISIBILITY, 1, 0, true)
            val gt = Fluorite.getNewFakeScore("gametime")
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
            Fluorite.tickFile += {
                Command.execute().asat('a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(playingTag))
                    .run(func)
                Command.execute().asat('a'["nbt =! {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(invisTag))
                    .run(endFunc)
            }
        }


        override fun give(player: Selector) {
            Command.give(self, Items.SUGAR.nbt("{jh1236:{weapon:$myId}}"))
        }

    }
}
