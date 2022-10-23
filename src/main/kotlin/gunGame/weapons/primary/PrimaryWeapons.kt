package gunGame.weapons.primary

import abstractions.PlayerTag
import abstractions.flow.If
import abstractions.hasTag
import abstractions.notHasTag
import abstractions.variables.NBTTypes
import commands.Command
import enums.*
import gunGame.*
import gunGame.weapons.ModularCoasWeapon
import gunGame.weapons.shootTag
import lib.get
import structure.Fluorite
import structure.McFunction
import utils.Vec2
import utils.abs
import utils.loc
import utils.rel


lateinit var sniper: ModularCoasWeapon
lateinit var shotgun: ModularCoasWeapon
lateinit var bazooka: ModularCoasWeapon
lateinit var miniGun: ModularCoasWeapon
lateinit var necromancy: ModularCoasWeapon
lateinit var tomeOfAir: ModularCoasWeapon
lateinit var rifle: ModularCoasWeapon
lateinit var laser: ModularCoasWeapon

fun loadPrimaries() {
    sniper = ModularCoasWeapon("Sniper", 4000).withCooldown(5.0).withParticle(Particles.END_ROD)
        .addSound("item.shield.block", 1.2).withCustomModelData(1).withRange(200).withPiercing()
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was no-scoped by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .done()

    shotgun = ModularCoasWeapon("Shotgun", 500).withParticle(Particles.CRIT).withReload(3.75).withCooldown(.15)
        .withClipSize(2).withSpread(5.0).withBulletsPerShot(8).addSound("entity.generic.explode", 1.4)
        .addSound("block.chain.hit", 0.0).withCustomModelData(2).withRange(10)
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was filled with lead by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .done()

    bazooka =
        ModularCoasWeapon("Bazooka", 6000).withCooldown(4.0).withParticle(Particles.LARGE_SMOKE, 10).withProjectile(3)
            .withRange(100).withCustomModelData(3).addSound("minecraft:entity.firework_rocket.launch", 0.0)
            .withSplash(3.0).onWallHit { bazookaHitsWall() }
            .onEntityHit { _, _ -> bazookaHitsWall() }
            .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was blown away by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
            .done()

    miniGun = Minigun()

    laser =
        object : ModularCoasWeapon("Laser", 2500) {
            val bounce = Fluorite.reuseFakeScore("bounce")
            val bonus = Fluorite.reuseFakeScore("dmg")
            val laserTag = PlayerTag("laser")
            val bounceFunc = McFunction("primary/laser/bounce_z") {
                val angle = Fluorite.reuseFakeScore("angle")
                angle.set(180)
                val dif = Fluorite.reuseFakeScore("rot")
                dif.set(self.data["Rotation[0]"])
                angle -= dif
                self.data["Rotation[0]", NBTTypes.FLOAT] = angle
            }
            val fireFunc = McFunction("primary/laser/fire")

            init {
                withCooldown(3.0)
                withParticle(Particles.FALLING_DUST(Blocks.REDSTONE_BLOCK))
                withPiercing()
                withCustomModelData(5)
                addSound("block.conduit.attack.target", 2.0)
                withRange(100)
                onWallHit {
                    If((bounce gt 0) and !(loc(0, 0, .25) isBlock blockTag("jh1236:air"))) {
                        Command.execute().As('e'["limit = 1"].hasTag(laserTag)).run {
                            Command.raw("execute unless block ~ ~0.5 ~ #jh1236:air store result entity @s Rotation[1] float 1 run data get entity @s Rotation[1] -1")
                            Command.raw("execute unless block ~ ~-0.5 ~ #jh1236:air store result entity @s Rotation[1] float 1 run data get entity @s Rotation[1] -1")
                            Command.raw("execute unless block ~0.5 ~ ~ #jh1236:air store result entity @s Rotation[0] float 1 run data get entity @s Rotation[0] -1")
                            Command.raw("execute unless block ~-0.5 ~ ~ #jh1236:air store result entity @s Rotation[0] float 1 run data get entity @s Rotation[0] -1")
                            If(!(rel(0, 0, -.5) isBlock blockTag("jh1236:air")), bounceFunc)
                            If(!(rel(0, 0, .5) isBlock blockTag("jh1236:air")), bounceFunc)
                            bounce -= 1
                            bonus *= 2
                            Command.execute().rotated(self).positioned(loc(0, 0, 0.25)).run(fireFunc)
                        }
                    }
                }
                onEntityHit { hit, _ ->
                    Command.execute().As(hit).run {
                        If(bonus gt 1) {
                            bonus *= 2500
                            damageSelf(bonus)
                        }
                    }
                }
                withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was ricocheted by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
                done()
            }

            override fun shoot() {
                Command.summon(Entities.MARKER, rel(), "{Tags:[$laserTag]}")
                Command.execute().As('e'[""].hasTag(laserTag)).run.tp(self, rel(), Vec2("~", "~"))
                bounce.set(6)
                bonus.set(1)
                fireFunc.append {
                    super.shoot()
                }
                fireFunc()
                Command.kill('e'[""].hasTag(laserTag))
            }
        }

    necromancy = ModularCoasWeapon("Necromancy", 2500).withCooldown(2.0).addSound("block.soul_sand.place", .1)
        .addSound("particle.soul_escape")
        .withParticle(Particles.DUST_COLOR_TRANSITION(0.0, 0.0, 0.0, 1.0, 0.431, 0.431, 0.431), 10)
        .onEntityHit { playerHit, playerShooting ->
            If(playerHit.hasTag(deadTag)) {
                health[playerShooting] += 1000
                Command.playsound("block.amethyst_block.fall").master(self, rel(), 1.0, 1.0)
                Command.playsound("block.enchantment_table.use").master(self, rel(), 1.0, 1.8)
            }.Else {
                health[playerShooting] += 500
            }
        }.withRange(200).onWallHit {
            Command.particle(Particles.SOUL, rel(), abs(0, 0, 0), .1, 20).normal
        }.withCustomModelData(6)
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was bewitched by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .done()



    tomeOfAir =
        ModularCoasWeapon("Tome of Air", 3000).withCooldown(4.0).withParticle(Particles.CLOUD, 10).withProjectile(1)
            .withRange(50).withCustomModelData(7).addSound("minecraft:block.enchantment_table.use", 1.3).onWallHit {
                tomeHitsWall()
            }.onEntityHit { _, _ ->
                tomeHitsWall()
            }.onProjectileTick {
                Command.execute().lerpFacing('p'[""].notHasTag(shootTag).hasTag(playingTag), 1, 7).run.tp(
                    self, rel(), Vec2("~", "~")
                )
            }
            .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " got smoked by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
            .done()

    ninjaSword()

    rifle = ModularCoasWeapon("Rifle", 1200).withCooldown(0.5).withParticle(Particles.ENCHANTED_HIT)
        .addSound("minecraft:block.ancient_debris.hit", .25).addSound("minecraft:block.ancient_debris.hit", .25)
        .addSound("minecraft:block.ancient_debris.hit", .25).addSound("minecraft:block.ancient_debris.hit", .25)
        .addSound("minecraft:ui.loom.select_pattern", .75).withCustomModelData(9).withRange(300)
        .onEntityHit { hit, _ -> Command.effect().give(hit, Effects.SLOWNESS, 2, 2, false) }
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was hunted by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .done()


}

private fun bazookaHitsWall() {
    Command.particle(Particles.EXPLOSION, rel(), abs(1.5, 1.5, 1.5), 0.0, 100).force('a'[""])
    Command.particle(Particles.EXPLOSION_EMITTER, rel(), abs(1.5, 1.5, 1.5), 0.0, 10).force('a'[""])
    Command.particle(Particles.FLAME, rel(), abs(1.5, 1.5, 1.5), 0.0, 100).force('a'[""])
    Command.particle(Particles.CAMPFIRE_COSY_SMOKE, rel(), abs(1.5, 1.5, 1.5), 0.0, 100).force('a'[""])
    Command.particle(Particles.ASH, rel(), abs(1.5, 1.5, 1.5), 0.0, 750).force('a'[""])
    Command.particle(
        Particles.DUST_COLOR_TRANSITION(0.988, 0.0, 0.0, 1.0, 1.0, 0.529, 0.122), rel(), abs(1.5, 1.5, 1.5), 0.0, 500
    ).force('a'[""])
    Command.playsound("minecraft:entity.firework_rocket.blast").master('a'[""], rel(), 4.0, 0.0)
    Command.playsound("minecraft:entity.generic.explode").master('a'[""], rel(), 4.0, 0.0)
}

private fun tomeHitsWall() {
    Command.particle(Particles.FLASH, rel(), abs(0, 0, 0), 1.0, 20)
    Command.particle(Particles.CLOUD, rel(), abs(0.1, 0.1, 0.1), 1.0, 20)
    Command.playsound("minecraft:block.beacon.deactivate").master('a'[""], rel(), 1.0)
}