package com.github.shynixn.blockball.bukkit.logic.business.nms.v1_16_R3

import com.github.shynixn.blockball.api.BlockBallApi
import com.github.shynixn.blockball.api.business.service.ConcurrencyService
import com.github.shynixn.blockball.api.business.service.LoggingService
import com.github.shynixn.blockball.api.persistence.entity.BallMeta
import net.minecraft.server.v1_16_R3.EntitySlime
import net.minecraft.server.v1_16_R3.EntityTypes
import net.minecraft.server.v1_16_R3.NBTTagCompound
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityTeleport
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Slime implementation for hitbox calculation.
 * <p>
 * Version 1.3
 * <p>
 * MIT License
 * <p>
 * Copyright (c) 2020 by Shynixn
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
class BallHitBox(
    private val ballDesign: BallDesign,
    ballMeta: BallMeta,
    location: Location
) : EntitySlime(EntityTypes.SLIME, (location.world as CraftWorld).handle) {

    // BukkitEntity has to be self cached since 1.14.
    private var entityBukkit: Any? = null

    /**
     * Initializes the hitbox.
     */
    init {
        val compound = NBTTagCompound()
        compound.setBoolean("Invulnerable", true)
        compound.setBoolean("PersistenceRequired", true)
        compound.setBoolean("NoAI", true)
        compound.setInt("Size", ballMeta.hitBoxSize.toInt() - 1)
        this.loadData(compound)

        bukkitEntity.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false))

        val mcWorld = (location.world as CraftWorld).handle
        this.setPosition(location.x, location.y - 200, location.z)
        mcWorld.addEntity(this, CreatureSpawnEvent.SpawnReason.CUSTOM)

        // Fix the spawn location afterwards.
        val targetLocation = location.clone()
        BlockBallApi.resolve(ConcurrencyService::class.java).runTaskSync(20L) {
            this.setPosition(targetLocation.x, targetLocation.y, targetLocation.z)
            updatePosition()
        }
    }

    /**
     * 1. Prevent slime from dying in Peaceful difficulty
     * 2. Constantly move hitbox to overlap with ballDesign
     */
    override fun tick() {
        super.tick()
        this.dead = false

        val loc = ballDesign.bukkitEntity.location
        val lastX = ballDesign.lastX
        val lastY = ballDesign.lastY
        val lastZ = ballDesign.lastZ

        if (!loc.x.equals(lastX) || !loc.y.equals(lastY) || !loc.z.equals(lastZ)) {
            if (ballDesign.isSmall) {
                this.setPositionRotation(loc.x, loc.y + 0.5, loc.z, loc.yaw, loc.pitch)
            } else {
                this.setPositionRotation(loc.x, loc.y + 1.05, loc.z, loc.yaw, loc.pitch)
            }

            updatePosition()
            debugPosition()
        }
    }

    /**
     * Disable health.
     */
    override fun setHealth(f: Float) {}

    /**
     * Gets the bukkit entity.
     */
    override fun getBukkitEntity(): CraftHitboxSlime {
        if (this.entityBukkit == null) {
            this.entityBukkit = CraftHitboxSlime(this.world.server, this)
        }

        return this.entityBukkit as CraftHitboxSlime
    }

    /**
     * Updates the position of the entity manually.
     */
    private fun updatePosition() {
        val packet = PacketPlayOutEntityTeleport(this)
        this.world.players.forEach { p -> (p.bukkitEntity as CraftPlayer).handle.playerConnection.sendPacket(packet) }
    }

    /**
     * Prints a debugging message for this entity.
     */
    private fun debugPosition() {
        val loc = bukkitEntity.location
        BlockBallApi.resolve(LoggingService::class.java)
            .debug("Hitbox at ${loc.x.toFloat()} ${loc.y.toFloat()} ${loc.z.toFloat()}")
    }
}
