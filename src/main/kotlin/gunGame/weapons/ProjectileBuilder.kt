package gunGame.weapons

import enums.IParticle
import lib.Quad
import utils.Selector
import kotlin.math.roundToInt

class ProjectileBuilder(val name: String, val damage: Int) {
    private var isReward = false
    private var secondary = false
    private var ableBeShot = false
    private var activationDelay: Int = -1
    private var maxAllowed = -1
    private var piercing = false
    private var particleCount: Int = 1
    private var canHitOwner: Boolean = false
    private var sound = arrayListOf<Pair<String, Double>>()
    private var particleArray = arrayListOf<Quad<IParticle, Int, Number, Number>>()
    private var reload = 0.0
    private var cooldown = 0.0
    private var clipSize = 1
    private var range = -1
    private var onWallHit: ((Selector) -> Unit)? = null
    private var onProjectileTick: (ProjectileWeapon.(Selector) -> Unit)? = null
    private var onEntityHit: (ProjectileWeapon.(Selector, Selector) -> Unit)? = null
    private var customModelData = -1
    private var projectileSpeed = -1
    private var splashRange = 0.0
    private var killMessage =
        """'["",{"selector":"@s", "color":"gold"}, " was killed by ", {"selector":"@a[tag = $shootTag]"}]'"""

    fun withProjectileSpeed(speed: Int): ProjectileBuilder {
        this.projectileSpeed = speed
        return this
    }

    fun withKillMessage(killMessage: String): ProjectileBuilder {
        this.killMessage = killMessage
        return this
    }

    fun withSplash(splashRange: Double): ProjectileBuilder {
        this.splashRange = splashRange
        return this
    }

    fun canBeShot(): ProjectileBuilder {
        this.ableBeShot = true
        return this
    }

    fun asSecondary(): ProjectileBuilder {
        this.secondary = true
        return this
    }


    fun asReward(): ProjectileBuilder {
        this.isReward = true
        return this
    }

    fun canHitOwner(): ProjectileBuilder {
        this.canHitOwner = true
        return this
    }

    fun withPiercing(): ProjectileBuilder {
        this.piercing = true
        return this
    }

    fun withActivationDelay(delay: Double): ProjectileBuilder {
        this.activationDelay = (delay * 20).roundToInt()
        return this
    }


    fun withCustomModelData(data: Int): ProjectileBuilder {
        customModelData = data
        return this
    }

    fun onEntityHit(onHit: ProjectileWeapon.(Selector, Selector) -> Unit): ProjectileBuilder {
        this.onEntityHit = onHit
        return this
    }

    fun onWallHit(onHit: (Selector) -> Unit): ProjectileBuilder {
        this.onWallHit = onHit
        return this
    }


    fun addParticle(particle: IParticle, count: Int = 1, radius: Number = 0.0, speed: Number = 0.0): ProjectileBuilder {
        particleArray.add(Quad(particle, count, radius, speed))
        return this
    }

    fun withClipSize(clipSize: Int): ProjectileBuilder {
        this.clipSize = clipSize
        return this
    }


    fun addSound(sound: String, pitch: Double = 1.0): ProjectileBuilder {
        this.sound.add(sound to pitch)
        return this
    }

    // time between each shot
    fun withCooldown(cooldown: Double): ProjectileBuilder {
        this.cooldown = cooldown
        return this
    }

    //time used when end of clip
    fun withReload(reloadTime: Double): ProjectileBuilder {
        this.reload = reloadTime
        return this
    }

    fun withProjectile(speed: Int, maxAllowed: Int = -1): ProjectileBuilder {
        projectileSpeed = speed
        this.maxAllowed = maxAllowed
        return this
    }

    fun onProjectileTick(func: ProjectileWeapon.(Selector) -> Unit): ProjectileBuilder {
        onProjectileTick = func
        return this
    }

    fun withRange(range: Int): ProjectileBuilder {
        this.range = (range * 4)
        return this
    }

    fun done(): ProjectileWeapon {
        val ret = ProjectileWeapon(
            name = name,
            damage = damage,
            customModelData = customModelData,
            cooldown = cooldown,
            clipsize = clipSize,
            reload = reload,
            ableBeShot = ableBeShot,
            activationDelay = activationDelay,
            maxAllowed = maxAllowed,
            canHitOwner = canHitOwner,
            sound = sound,
            particleArray = particleArray,
            range = range,
            onWallHit = onWallHit,
            onProjectileTick = onProjectileTick,
            onEntityHit = onEntityHit,
            projectileSpeed = projectileSpeed,
            splashRange = splashRange,
            killMessage = killMessage,
            secondary = secondary,
            isReward = isReward
        )
        ret.setup()
        return ret
    }
}