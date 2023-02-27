package gunGame.weapons

import abstractions.PlayerTag
import enums.IParticle
import lib.Quad
import structure.Fluorite
import utils.Selector

class RaycastBuilder(val name: String, val damage: Int) {

    companion object {
        val universalProjectile = PlayerTag("projectile")
        private val hit = Fluorite.getNewFakeScore("hit", 0)
    }

    private var particleArray = arrayListOf<Quad<IParticle, Int, Double, Double>>()

    private var isReward: Boolean = false
    private var secondary: Boolean = false
    protected var piercing = false
    protected var particleCount: Int = 1
    protected var sound = arrayListOf<Pair<String, Double>>()
    protected var particle: IParticle? = null
    protected var reload = 0.0
    protected var cooldown = 0.0
    protected var clipSize = 1
    protected var bulletsPerShot = 1
    var range = -1
    protected var spread = 0.0
    protected var onWallHit: ((Selector) -> Unit)? = null
    protected var onRaycastTick: (() -> Unit)? = null
    protected var onFireBullet: (() -> Unit)? = null
    var onEntityHit: (RaycastWeapon.(Selector, Selector) -> Unit)? = null
    protected var customModelData = -1
    protected var splashRange = 0.0
    protected var killMessage =
        """'["",{"selector":"@s", "color":"gold"}, " was killed by ", {"selector":"@a[tag = $shootTag]"}]'"""

    fun withKillMessage(killMessage: String): RaycastBuilder {
        this.killMessage = killMessage
        return this
    }

    fun withSplash(splashRange: Double): RaycastBuilder {
        this.splashRange = splashRange
        return this
    }


    fun asSecondary(): RaycastBuilder {
        this.secondary = true
        return this
    }


    fun asReward(): RaycastBuilder {
        this.isReward = true
        return this
    }


    fun withPiercing(): RaycastBuilder {
        this.piercing = true
        return this
    }


    fun withCustomModelData(data: Int): RaycastBuilder {
        customModelData = data
        return this
    }

    fun onEntityHit(onHit: RaycastWeapon.(Selector, Selector) -> Unit): RaycastBuilder {
        this.onEntityHit = onHit
        return this
    }

    fun onWallHit(onHit: (Selector) -> Unit): RaycastBuilder {
        this.onWallHit = onHit
        return this
    }

    fun onRaycastTick(onRaycastTick: () -> Unit): RaycastBuilder {
        this.onRaycastTick = onRaycastTick
        return this
    }

    fun onFireBullet(onFireBullet: () -> Unit): RaycastBuilder {
        this.onFireBullet = onFireBullet
        return this
    }

    fun withBulletsPerShot(bps: Int): RaycastBuilder {
        bulletsPerShot = bps
        return this
    }

    fun addParticle(particle: IParticle, count: Int = 1, radius: Double = 0.0, speed: Double = 0.0): RaycastBuilder {
        particleArray.add(Quad(particle, count, radius, speed))
        return this
    }

    fun withClipSize(clipSize: Int): RaycastBuilder {
        this.clipSize = clipSize
        return this
    }

    fun withSpread(spread: Double): RaycastBuilder {
        this.spread = spread
        return this
    }


    fun addSound(sound: String, pitch: Double = 1.0): RaycastBuilder {
        this.sound.add(sound to pitch)
        return this
    }

    // time between each shot
    fun withCooldown(cooldown: Double): RaycastBuilder {
        this.cooldown = cooldown
        return this
    }

    //time used when end of clip
    fun withReload(reloadTime: Double): RaycastBuilder {
        this.reload = reloadTime
        return this
    }


    fun withRange(range: Int): RaycastBuilder {
        this.range = (range * 4)
        return this
    }

    fun done(): RaycastWeapon {

        val ret = RaycastWeapon(
            name = name,
            damage = damage,
            particleArray = particleArray,
            customModelData = customModelData,
            cooldown = cooldown,
            clipSize = clipSize,
            reloadTime = reload,
            piercing = piercing,
            sound = sound,
            bulletsPerShot = bulletsPerShot,
            range = range,
            spread = spread,
            onWallHit = onWallHit,
            onEntityHit = onEntityHit,
            onFireBullet = onFireBullet,
            onRaycastTick = onRaycastTick,
            splashRange = splashRange,
            killMessage = killMessage,
            secondary = secondary,
            isReward = isReward
        )
        ret.setup()
        return ret
    }


}