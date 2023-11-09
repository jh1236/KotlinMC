import abstractions.PlayerTag
import abstractions.flow.If
import abstractions.flow.While
import abstractions.hasTag
import abstractions.notHasTag
import abstractions.score.Objective
import commands.Command
import enums.Anchor
import enums.Effects
import enums.Entities
import enums.Particles
import gunGame.*
import gunGame.weapons.ProjectileWeapon
import gunGame.weapons.applyCoolDown
import gunGame.weapons.impl.ticksShot
import gunGame.weapons.shootTag
import lib.*
import lib.debug.Log
import structure.Fluorite
import structure.McFunction
import utils.Vec2
import utils.get
import utils.loc
import utils.rel

val rpgScore = Objective("rpgRC")
val rpgFireStrength = ticksShot
private const val PROJ_PER_STRENGTH = 2
private const val MAX_PROJ = 20
private const val MIN_PROJ = 4
private const val BASE_DAMAGE = 600
private const val DAMAGE_PER_STRENGTH = 600
private const val MAX_DAMAGE = 4000

class RPG : ProjectileWeapon(
    "RPG",
    -600,
    13,
    -2.0,
    range = 800,
    spread = 3.0,
    splashRange = 4.0,
    sound = arrayListOf(
        "minecraft:entity.firework_rocket.launch" to 0.75,
        "minecraft:block.soul_sand.hit" to .4,
        "minecraft:block.soul_sand.hit" to .4,
        "minecraft:block.soul_sand.hit" to .4
    ),
    killMessage = """'["",{"selector": "@s","color": "gold"},{"text": " was blasted by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""",
    projectileSpeed = 1.5,
    projectileEntity = Entities.BLOCK_DISPLAY,
    projectileNBT = "{transformation:{scale:[.5f,.5f,.5f]}, block_state:{Name:\"tnt\"}}",
    projectilesPerShot = 1,
) {

    companion object {
        val rpg = PlayerTag("rpg")
    }


    private val final = Fluorite.reuseFakeScore("firePower")
    val fireFunc = McFunction("$basePath/fire") {

    }

    val func = McFunction("$basePath/sights") {
        shootTag.add(self)
        rangeScore.set(100)
        Command.execute().anchored(Anchor.EYES).run {
            raycast(1.0f, {
                Command.particle(Particles.DUST(1.0, 0.0, 0.0, .5), loc(0, 0, -0.75), 0.0, 0.0, 0.0, 0.0, 0)
                    .force('a'[""].notHasTag(shootTag).notHasTag(lagTag))
                Command.particle(Particles.DUST(1.0, 0.0, 0.0, .5), loc(0, 0, -0.5), 0.0, 0.0, 0.0, 0.0, 0)
                    .force('a'[""].notHasTag(shootTag).notHasTag(lagTag))
                Command.particle(Particles.DUST(1.0, 0.0, 0.0, .5), loc(0, 0, -0.25), 0.0, 0.0, 0.0, 0.0, 0)
                    .force('a'[""].notHasTag(shootTag).notHasTag(lagTag))
                Command.particle(Particles.DUST(1.0, 0.0, 0.0, .5), loc(0, 0, 0), 0.0, 0.0, 0.0, 0.0, 0)
                    .force('a'[""].notHasTag(shootTag))
            }, {
                Command.particle(Particles.DUST(1.0, 0.0, 0.0, 1.0), loc(0, 0, -1), 0.0, 0.0, 0.0, 0.0, 0)
                    .force(self["predicate = jh1236:ready"])
            })
        }
        shootTag.remove(self)
    }

    init {
        damageStr = "$BASE_DAMAGE + $DAMAGE_PER_STRENGTH for each charge level (max $MAX_DAMAGE)"
        extraLines.add("""{"text" : "Projectiles per shot: 3 + $PROJ_PER_STRENGTH for each charge level", "color": "gray", "italic" : false}""")
        onWallHit = {
            Command.playsound(Delta.EXPLODE_SOUND).master('a'[""].notHasTag(shootTag), rel(), 1.0)
            Command.execute().asat(it).run.playsound(Delta.EXPLODE_SOUND).master('a'[""], rel(), .2)
            Command.particle(Particles.EXPLOSION, rel(), 0.05, 0.05, 0.05, 0.01, 1)
            Command.particle(
                Particles.DUST_COLOR_TRANSITION(1.0, .1, .1, 1.0, .3, .3, .3), rel(), 0.5, 0.5, 0.5, 0.01, 40
            )
            Command.particle(Particles.CLOUD, rel(), 0.5, 0.5, 0.5, 0.01, 10)
            Command.particle(Particles.LAVA, rel(), 0.5, 0.5, 0.5, 0.01, 10)
            Log.info("dmg: ", rpgFireStrength[self])
        }

        onEntityHit = { _, _, projectile ->
            Log.info("damage: ", rpgFireStrength[projectile])
            damageSelf(rpgFireStrength[projectile])
        }


        Fluorite.tickFile += {
            Command.execute()
                .asat('a'["nbt = {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(playingTag))
                .run(func)
            rpgScore['a'["scores = {$rpgScore = 1..}"].hasTag(playingTag)] -= 1
            rpgScore['a'["scores = {$rpgScore = 1..}"].notHasTag(playingTag)] = 0
            Command.execute()
                .asat('a'["nbt =! {SelectedItem:{tag:{jh1236:{weapon:$myId}}}}"].hasTag(playingTag).hasTag(rpg)).run {
                    rpg.remove(self)
                    rpgScore[self] = 0
                    Command.effect().clear(self, Effects.SLOWNESS)
                    rpgFireStrength[self] = 0
                    repeat(9) {
                        final.set(self.data["Inventory[{Slot:${it}b}].tag.jh1236.weapon"])
                        Command.execute().If(final eq myId).run {
                            copyItemfromSlotAndRun("hotbar.$it") { itemData ->
                                itemData["Count"] = "1"
                            }
                        }
                    }
                }
            Command.execute().asat('a'["scores = {$rpgScore = 2..}", "predicate = jh1236:ready"].hasTag(playingTag))
                .run {
                    rpgFireStrength[self] += 1
                    Command.effect().give(self, Effects.SLOWNESS, 1, 4, true)
                    final.set(rpgFireStrength[self])
                    final /= 10
                    final += 1
                    final *= PROJ_PER_STRENGTH

                    If(final gte MAX_PROJ) {
                        Command.playsound("minecraft:entity.zombie.destroy_egg").master(self, rel(), 1.0, 0.0)
                    }
                    If(final gt MAX_PROJ + 3 * PROJ_PER_STRENGTH) {
                        repeat(MAX_PROJ) { fireFunc() }
                        final.set(idScore[self])
                        Command.execute().asat('e'[""].hasTag(projectile)).If(idScore[self] eq final).run {
                            health[self] = 0
                        }
                    }
                    final.minOf(MAX_PROJ)
                    final.maxOf(MIN_PROJ)
                    If(rpgFireStrength[self].rem(10) eq 1) {
                        copyHeldItemToBlockAndRun {
                            it["Count"] = final
                        }
                    }
                }
            Command.execute().asat('a'["scores = {$rpgScore = 1}", "predicate = jh1236:ready"].hasTag(playingTag)).run {
                rpgFireStrength[self] /= 10
                final.set(rpgFireStrength[self])
                final += 1
                final.minOf(MAX_PROJ)
                final *= PROJ_PER_STRENGTH
                final.maxOf(MIN_PROJ)
                While(final gte 1) {
                    fireFunc()
                    final -= 1
                }
                applyCoolDown(40)
                Command.effect().clear(self)
            }

        }

        setup()
    }

    override fun fire() {
        If(rpgScore[self] eq 0) {
            rpgFireStrength[self] = 0
        }
        rpgScore[self] = 8
        rpg.add(self)
        fireFunc.append {
            rpgScore[self] = 0
            super.fire()
            shootTag.add(self)
            val processed = PlayerTag("tntProcessed")
            Command.execute().asat('e'[""].hasTag(projectile).notHasTag(processed)).run {
                rpgFireStrength[self] = rpgFireStrength['a'["limit=1"].hasTag(shootTag)]
                rpgFireStrength[self] *= DAMAGE_PER_STRENGTH
                rpgFireStrength[self] += BASE_DAMAGE
                rpgFireStrength[self].minOf(MAX_DAMAGE)
                processed.add(self)
            }
            copyHeldItemToBlockAndRun {
                it["Count"] = "1"
            }
            shootTag.remove(self)
        }
    }
}