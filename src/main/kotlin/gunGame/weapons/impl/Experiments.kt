package gunGame.weapons.impl

import FlameThrower
import abstractions.flow.If
import abstractions.hasTag
import abstractions.score.Objective
import abstractions.score.Score
import commands.Command
import enums.Anchor
import enums.Particles
import gunGame.self
import gunGame.weapons.*
import lib.Delta
import lib.debug.Log
import utils.Vec2
import utils.get

lateinit var grapple: ProjectileWeapon
lateinit var carabine: RaycastWeapon

fun loadGrapplingHook() {
    val strength = Objective("strength")
    val trueCooldown = 3.5
    grapple =
        ProjectileBuilder("Grappling Hook", 0).addParticle(Particles.DUST(0.0, 0.0, 0.0, 1.0)).withRange(50)
            .withProjectile(3.0).asSecondary()
            .withCooldown(9999.0).onWallHit {
                Command.execute().asat(it)
                    .facing('e'["limit = 1"].hasTag(ProjectileWeapon.currentProjectile), Anchor.FEET).rotated(
                        Vec2("~", "~-10")
                    ).run {
                        If(ProjectileWeapon.hit eq 1) {
                            setCooldownForId(this.myId, (trueCooldown * 20).toInt())
                            Delta.launchFacing(strength['e'["limit = 1"].hasTag(ProjectileWeapon.currentProjectile)])
                        }.Else {
                            setCooldownForId(this.myId, 0)
                            Command.playsound("minecraft:block.note_block.didgeridoo").master(self)
                        }
                    }
            }.onProjectileSpawn {
                Command.execute().asat(it).run {
                    applyCoolDown(Score.LOWER)
                }
                strength[self] = 16000
                Log.info(strength[self])
            }.onProjectileTick {
                Command.playsound("minecraft:item.crossbow.loading_middle").master(it)
                strength[self] += 10
                this.cooldownStr = "$trueCooldown seconds"
            }.addSound("minecraft:entity.fishing_bobber.throw", .8)
            .addSound("minecraft:entity.fishing_bobber.retrieve", .5)
            .withCustomModelData(115).done()

}


fun loadCarbine() {
    carabine =
        RaycastBuilder("Carbine", 700).addParticle(Particles.CRIT).withReload(1.5).withCooldown(0.15).withClipSize(10)
            .addSound("minecraft:entity.camel.dash_ready", 1.0).withRange(200)
            .withKillMessage("""'["",{"selector": "@s","color": "gold"},{"text": " was filled with bullets by "},{"selector": "@a[tag=$shootTag]","color": "gold"}]'""")
            .withCustomModelData(15)
            .done()
}

fun loadExperiments() {
    FlameThrower()
    loadGrapplingHook()
    loadCarbine()
    Minigun()
}
