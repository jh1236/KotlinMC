package gunGame.weapons.impl

import abstractions.*
import abstractions.flow.If
import abstractions.variables.NBTTypes
import commands.Command
import enums.*
import gunGame.*
import gunGame.weapons.*
import lib.get
import structure.Fluorite
import structure.McFunction
import utils.Selector
import utils.Vec2
import utils.loc
import utils.rel
import utils.score.Objective


lateinit var sniper: RaycastWeapon
lateinit var shotgun: RaycastWeapon
lateinit var bazooka: ProjectileWeapon
lateinit var miniGun: AbstractWeapon
lateinit var necromancy: RaycastWeapon
lateinit var tomeOfAir: AbstractWeapon
lateinit var rifle: RaycastWeapon
lateinit var laser: RaycastWeapon
lateinit var staff: RaycastWeapon

private fun loadStaff() {
    val staffTag = PlayerTag("staff")
    val fireFunc = McFunction("weapons/primary/staff/fire")
    val bonusDamage = Objective("staffDmg")

    staff = object : RaycastWeapon(
        "Staff",
        30,
        103,
        1.0,
        1,
        1.0,
        particle = Particles.TOTEM_OF_UNDYING,
        range = 25,
        onEntityHit = { _, shooter ->
            staffTag.add(shooter)
            applyCoolDown(2)
            damageSelf(bonusDamage[shooter])
            bonusDamage[shooter] += damage
        },
        killMessage = """'["",{"selector": "@a[tag=$shootTag]","color": "gold"},{"text": " was trash and no-aimed "},{"selector": "@s","color": "gold"}]'""",
        secondary = false
    ) {

        init {
            Fluorite.tickFile += {
                Command.execute().asat(
                    'a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}", "predicate = jh1236:ready"].hasTag(
                        staffTag
                    )
                ).anchored(Anchor.EYES).facing(
                    'e'["sort = nearest", "limit = 1", "distance=.5.."].hasTag(playingTag).notHasTag(invisTag)
                        .notHasTag(
                            deadTag
                        ),
                    Anchor.EYES
                ).run(fireFunc)

                Command.execute().asat(
                    'a'["nbt =! {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(staffTag)
                ).run {
                    staffTag.remove(self)
                    setCooldownForId(myId, (reload * 20).toInt())
                }
            }
            setup()
        }

        override fun fire() {
            bonusDamage[self] = 0
            fireFunc.append {
                applyCoolDown(2)
                staffTag.remove(self)
                Command.execute().facing(
                    'e'["sort = nearest", "limit = 1", "distance=.5.."].hasTag(playingTag).notHasTag(invisTag),
                    Anchor.EYES
                ).run { super.fire() }
                Command.execute()
                    .unless('e'["sort = nearest", "limit = 1", "distance=.5.."].hasTag(playingTag).notHasTag(invisTag))
                    .run {
                        super.fire()
                    }
                Command.execute().unless(self.hasTag(staffTag)).run {
                    bonusDamage[self] = 0
                    applyCoolDown(70)
                }
            }
            fireFunc()
        }

    }
}

private fun blank() {
    object : AbstractWeapon("nope", 0) {
        override fun give(player: Selector) {

        }
    }
}

private fun loadSniper() {
    sniper = RaycastBuilder("Sniper", 4000).withCooldown(5.0).withParticle(Particles.END_ROD)
        .addSound("item.shield.block", 1.2).withCustomModelData(1).withRange(200).withPiercing()
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was no-scoped by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .done()
}

private fun loadShotgun() {
    shotgun = RaycastBuilder("Shotgun", 500).withParticle(Particles.CRIT).withReload(3.75).withCooldown(.15)
        .withClipSize(2).withSpread(5.0).withBulletsPerShot(8).addSound("entity.generic.explode", 1.4)
        .addSound("block.chain.hit", 0.0).withCustomModelData(2).withRange(10)
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was filled with lead by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .done()
}

private fun loadBazooka() {
    fun creeper() {
        Command.summon(
            Entities.CREEPER,
            rel(0,0.2,0),
            "{ExplosionRadius:1b,Fuse:0,ignited:1b}"
        )
    }
    bazooka =
        ProjectileBuilder("Bazooka", 6000).withCooldown(4.0).withParticle(Particles.LARGE_SMOKE, 10).withProjectile(3)
            .withRange(100).withCustomModelData(3).addSound("minecraft:entity.firework_rocket.launch", 0.0)
            .withSplash(3.5).onWallHit {
                bigExplosion()
                Command.execute().As('e'["distance = ..3"].hasTag(playingTag)).facing(self, Anchor.FEET).positioned(self).positioned(loc(0,0,-1.3)).run {
                    creeper()
                }
            }
            .onEntityHit { _, _ ->
                bigExplosion()

            }
            .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was blown away by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
            .done()
}

private fun loadLaser() {
    val bounce = Fluorite.reuseFakeScore("bounce")
    val bonus = Fluorite.reuseFakeScore("dmg")
    val laserTag = PlayerTag("laser")
    val bounceFunc = McFunction("weapons/primary/laser/bounce_z") {
        val angle = Fluorite.reuseFakeScore("angle")
        angle.set(180)
        val dif = Fluorite.reuseFakeScore("rot")
        dif.set(self.data["Rotation[0]"])
        angle -= dif
        self.data["Rotation[0]", NBTTypes.FLOAT] = angle
    }
    val fireFunc = McFunction("weapons/primary/laser/fire")
    laser =


        object : RaycastWeapon(
            "Laser",
            0,
            5,
            3.0,
            sound = listOf("block.conduit.attack.target" to 2.0),
            particle = Particles.FALLING_DUST(Blocks.REDSTONE_BLOCK),
            range = 100,
            onWallHit = {
                If((bounce gt 0) and !(loc(0, 0, .25) isBlock Blocks.tag("jh1236:air"))) {
                    Command.execute().As('e'["limit = 1"].hasTag(laserTag)).run {
                        Command.raw("execute unless block ~ ~0.5 ~ #jh1236:air store result entity @s Rotation[1] float 1 run data get entity @s Rotation[1] -1")
                        Command.raw("execute unless block ~ ~-0.5 ~ #jh1236:air store result entity @s Rotation[1] float 1 run data get entity @s Rotation[1] -1")
                        Command.raw("execute unless block ~0.5 ~ ~ #jh1236:air store result entity @s Rotation[0] float 1 run data get entity @s Rotation[0] -1")
                        Command.raw("execute unless block ~-0.5 ~ ~ #jh1236:air store result entity @s Rotation[0] float 1 run data get entity @s Rotation[0] -1")
                        If(!(rel(0, 0, -.5) isBlock Blocks.tag("jh1236:air")), bounceFunc)
                        If(!(rel(0, 0, .5) isBlock Blocks.tag("jh1236:air")), bounceFunc)
                        bounce -= 1
                        bonus *= 2
                        Command.execute().rotated(self).positioned(loc(0, 0, 0.25)).run(fireFunc)
                    }
                }
            },
            onEntityHit = { hit, _ ->
                Command.execute().As(hit).run {
                    bonus *= 2500
                    damageSelf(bonus)
                }
            },
            killMessage = """'["",{"selector": "@s","color": "gold"},{"text": " was ricocheted by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""",
            secondary = false
        ) {
            init {
                setup()
            }

            override fun fire() {
                Command.summon(Entities.MARKER, rel(), "{Tags:[$laserTag]}")
                Command.execute().As('e'[""].hasTag(laserTag)).run.tp(self, rel(), Vec2("~", "~"))
                bounce.set(6)
                bonus.set(1)
                fireFunc.append {
                    super.fire()
                }
                fireFunc()
                Command.kill('e'[""].hasTag(laserTag))
            }
        }

}

private fun loadNecromancy() {
    necromancy = RaycastBuilder("Necromancy", 2500).withCooldown(2.0).addSound("block.soul_sand.place", .1)
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
            Command.particle(Particles.SOUL, rel(), 0, 0, 0, .1, 20).normal
        }.withCustomModelData(6)
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was bewitched by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .done()

}

private fun loadRifle() {
    rifle = RaycastBuilder("Rifle", 1200).withCooldown(0.5).withParticle(Particles.ENCHANTED_HIT)
        .addSound("minecraft:block.ancient_debris.hit", .25).addSound("minecraft:block.ancient_debris.hit", .25)
        .addSound("minecraft:block.ancient_debris.hit", .25).addSound("minecraft:block.ancient_debris.hit", .25)
        .addSound("minecraft:ui.loom.select_pattern", .75).withCustomModelData(9).withRange(300)
        .onEntityHit { hit, _ -> Command.effect().give(hit, Effects.SLOWNESS, 2, 2, false) }
        .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was hunted by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
        .done()
}

fun loadPrimaries() {
    loadSniper()
    loadShotgun()
    loadBazooka()
    miniGun = Minigun()
    loadLaser()
    loadNecromancy()
    loadStaff()
    loadNinjaSword()
    loadRifle()
}

