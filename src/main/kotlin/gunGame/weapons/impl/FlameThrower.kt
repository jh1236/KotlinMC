import abstractions.PlayerTag
import abstractions.flow.If
import abstractions.hasTag
import abstractions.notHasTag
import abstractions.variables.NBTTypes
import commands.Command
import enums.Blocks
import enums.Entities
import enums.Particles
import gunGame.*
import gunGame.weapons.ProjectileWeapon
import gunGame.weapons.shootTag
import lib.debug.Log
import structure.Fluorite
import structure.McFunction
import utils.Duration
import utils.get
import utils.loc
import utils.rel

val processedTag = PlayerTag("processed")

class FlameThrower : ProjectileWeapon(
    "Flame Thrower",
    -100,
    14,
    0.0,
    1,
    0.0,
    sound = arrayListOf(
        "minecraft:block.fire.ambient" to 0.2,
        "minecraft:block.fire.ambient" to 0.2,
        "minecraft:block.fire.ambient" to 0.2,
        "minecraft:block.fire.ambient" to 0.2,
        "minecraft:block.fire.ambient" to 0.2,
        "minecraft:block.fire.ambient" to 0.2
    ),
    range = 100,
    spread = 3.0,
    killMessage = """'["",{"selector": "@s","color": "gold"},{"text": " was burnt to a crisp by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""",
    projectileSpeed = -1.0,
    projectileEntity = Entities.AREA_EFFECT_CLOUD,
    projectileNBT = "{Particle:\"flame\",Radius:2f,Duration:80,WaitTime:3,Effects:[{Id:26,Amplifier:5b,Duration:60,ShowParticles:0b}]}",
    secondary = true
) {

    companion object {
        var myId: Int = 0
    }

    val fireFunc = McFunction("$basePath/shoot")


    init {
        damageStr = "70 damage every 0.15 seconds for 3 seconds"
        extraLines[0] = """{"text": "Range: 8 blocks", "color": "gray", "italic" : false}"""
        Fluorite.loadFile += {
            McFunction("$basePath/tick") {
                val tempScore = Fluorite.reuseFakeScore("tempID")
                Command.execute().asat('e'["nbt = {ActiveEffects:[{Id:26}]}"].notHasTag(shootTag).hasTag(playingTag))
                    .run {
                        tempScore.set(self.data["ActiveEffects[{Id:26}].Amplifier"])
                        Log.info(tempScore)
                        Command.execute().As('a'[""].hasTag(playingTag)).If(idScore[self] eq tempScore).run {
                            shootTag.add(self)
                        }
                        deathStorage["death"] = killMessage
                        If(!self.hasTag(shootTag)) {
                            damageSelf(70)
                        }
                    }
                shootTag.remove('a'[""])
                Command.schedule().function(this, Duration(3))
            }()
        }
        FlameThrower.myId = myId

        this.onProjectileTick = {
            Command.execute().asat(it).If(rel(0, -.5, 0) isBlock Blocks.tag("jh1236:air")).run {
                Command.tp(self, rel(0, -.5, 0))
            }
            Command.execute().asat(it).unless(rel(0, .2, 0) isBlock Blocks.tag("jh1236:air")).run {
                Command.tp(self, rel(0, .2, 0))
            }
        }

        setup()
    }

    override fun fire() {
        fireFunc.append {
            super.fire()
            Command.execute().asat('e'[""].hasTag(projectile).notHasTag(processedTag)).run {
                self.data["Effects[0].Amplifier", NBTTypes.BYTE] = idScore[self]
            }
        }
        processedTag.toggle(self)
        If(self.hasTag(processedTag)) {
            fireFunc()
            Command.execute().asat('e'[""].notHasTag(processedTag).hasTag(projectile)).rotated('p'[""]).run {
                    Command.tp(self, loc(0, 0, 2))
                    self.data["Radius"] = "1f"
                }
            Command.particle(Particles.LAVA, loc(0, 0, 2), 1.0, 1.0, 1.0, .01, 20)
            processedTag.add('e'[""].hasTag(projectile))
            fireFunc()
            Command.execute().asat('e'[""].notHasTag(processedTag).hasTag(projectile)).rotated('p'[""]).run {
                    Command.tp(self, loc(0, 0, 4))
                    self.data["Radius"] = "1.5f"
                }
            Command.particle(Particles.LAVA, loc(0, 0, 4), 1.0, 1.0, 1.0, .01, 20)
            processedTag.add('e'[""].hasTag(projectile))
            fireFunc()
            Command.execute().asat('e'[""].notHasTag(processedTag).hasTag(projectile)).rotated('p'[""]).run {
                    Command.tp(self, loc(0, 0, 6))
                }
            Command.particle(Particles.LAVA, loc(0, 0, 6), 1.0, 1.0, 1.0, .01, 20)
            processedTag.add('e'[""].hasTag(projectile))
        }
    }
}