package gunGame.weapons.primary

import abstractions.flow.If
import abstractions.hasTag
import abstractions.notHasTag
import commands.Command
import enums.Effects
import enums.Items
import enums.Particles
import gunGame.*
import gunGame.weapons.ModularCoasWeapon
import gunGame.weapons.shootTag
import lib.get
import utils.Criteria
import utils.Vec2
import utils.abs
import utils.rel
import utils.score.Objective


lateinit var sniper: ModularCoasWeapon
lateinit var shotgun: ModularCoasWeapon
lateinit var bazooka: ModularCoasWeapon
lateinit var miniGun: ModularCoasWeapon
lateinit var necromancy: ModularCoasWeapon
lateinit var tomeOfAir: ModularCoasWeapon
lateinit var rifle: ModularCoasWeapon

fun loadPrimaries() {
    sniper = ModularCoasWeapon("Sniper", 4000).withCooldown(5.0).withParticle(Particles.END_ROD)
        .addSound("item.shield.block", 1.2).withCustomModelData(1).withRange(200).withPiercing().done()

    shotgun = ModularCoasWeapon("Shotgun", 500).withParticle(Particles.CRIT).withReload(3.75).withCooldown(.15)
        .withClipSize(2).withSpread(5.0).withBulletsPerShot(8).addSound("entity.generic.explode", 1.4)
        .addSound("block.chain.hit", 0.0).withCustomModelData(2).withRange(10).done()

    bazooka =
        ModularCoasWeapon("Bazooka", 6000).withCooldown(4.0).withParticle(Particles.LARGE_SMOKE, 10).withProjectile(2)
            .withRange(100).withCustomModelData(3).addSound("minecraft:entity.firework_rocket.launch", 0.0)
            .withSplash(3.0).onWallHit { bazookaHitsWall() }
            .onEntityHit { _, _ -> bazookaHitsWall() }.done()

    miniGun = Minigun()

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
        }.withCustomModelData(6).done()



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
            }.done()

    rifle = ModularCoasWeapon("Rifle", 1600).withCooldown(0.75).withParticle(Particles.ENCHANTED_HIT)
        .addSound("minecraft:block.ancient_debris.hit", .25).addSound("minecraft:block.ancient_debris.hit", .25)
        .addSound("minecraft:block.ancient_debris.hit", .25).addSound("minecraft:block.ancient_debris.hit", .25)
        .addSound("minecraft:ui.loom.select_pattern", .75).withCustomModelData(9).withRange(300)
        .onEntityHit { hit, _ -> Command.effect().give(hit, Effects.SLOWNESS, 2, 2, false) }.done()
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