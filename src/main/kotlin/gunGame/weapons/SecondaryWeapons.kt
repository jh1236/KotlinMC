package gunGame.weapons

import abstractions.*
import abstractions.flow.If
import commands.Command
import enums.*
import gunGame.deathEvent
import gunGame.playingTag
import gunGame.self
import lib.copyHeldItemToBlockAndRun
import lib.debug.Log
import lib.get
import lib.idScore
import structure.Fluorite
import structure.McFunction
import utils.Selector
import utils.abs
import utils.loc
import utils.rel

lateinit var pistol: ModularCoasWeapon
lateinit var tomeOfPetrification: ModularCoasWeapon
lateinit var teleport: AbstractCoasWeapon
lateinit var smokeCloud: AbstractWeapon
lateinit var boom: AbstractWeapon


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

    teleport = object : AbstractCoasWeapon("Tome of Teleportation", 200) {
        override fun shoot() {
            applyCoolDown(40)
            copyHeldItemToBlockAndRun {
                it["tag.jh1236.range"] = { Command.time().query.gametime }
            }
        }

        override fun give(player: Selector) {

        }
    }

    boom = ModularCoasWeapon("Bang", 0).withCooldown(4.0).withParticle(Particles.DUST(1.0, 0.0, 0.0, 1.2), 10)
        .withProjectile(1)
        .withRange(50).withCustomModelData(4).addSound("minecraft:block.enchantment_table.use", 1.3).onWallHit {
        }.onWallHit {
            Command.summon(Entities.CREEPER, rel(), "{ExplosionRadius:-1, Fuse:0b, ignited:1b}")
        }.onEntityHit { _, _ ->
            Command.summon(Entities.CREEPER, rel(), "{ExplosionRadius:-1, Fuse:0b, ignited:1b}")
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
            Log.info(self.data["PickupDelay"])
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
}