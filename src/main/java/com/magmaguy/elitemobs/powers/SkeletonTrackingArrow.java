package com.magmaguy.elitemobs.powers;

import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.api.EliteMobTargetPlayerEvent;
import com.magmaguy.elitemobs.config.powers.PowersConfig;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.magmaguy.elitemobs.powers.meta.MajorPower;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SkeletonTrackingArrow extends MajorPower implements Listener {

    public SkeletonTrackingArrow() {
        super(PowersConfig.getPower("skeleton_tracking_arrow.yml"));
    }

    private static void trackingArrowLoop(Player player, Arrow arrow) {
        new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                if (player.isValid() && arrow.isValid() && arrow.getWorld().equals(player.getWorld())
                        && player.getLocation().distanceSquared(arrow.getLocation()) < 900 && !arrow.isOnGround()) {
                    if (counter % 10 == 0)
                        arrow.setVelocity(arrow.getVelocity().add(arrowAdjustmentVector(arrow, player)));
                    arrow.getWorld().spawnParticle(Particle.FLAME, arrow.getLocation(), 10, 0.01, 0.01, 0.01, 0.01);
                } else {
                    arrow.setGravity(true);
                    cancel();
                }
                if (counter > 20 * 60) {
                    arrow.setGravity(true);
                    cancel();
                }
                counter++;
            }
        }.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
    }

    private static Vector arrowAdjustmentVector(Arrow arrow, Player player) {
        return player.getEyeLocation().clone().subtract(new Vector(0, 0.5, 0)).subtract(arrow.getLocation()).toVector().normalize().multiply(0.1);
    }

    @EventHandler
    public void targetEvent(EliteMobTargetPlayerEvent event) {
        SkeletonTrackingArrow skeletonTrackingArrow = (SkeletonTrackingArrow) event.getEliteMobEntity().getPower(this);
        if (skeletonTrackingArrow == null) return;
        if (skeletonTrackingArrow.isFiring()) return;

        skeletonTrackingArrow.setFiring(true);
        repeatingTrackingArrowTask(event.getEliteMobEntity(), skeletonTrackingArrow);
    }

    private void repeatingTrackingArrowTask(EliteEntity eliteEntity, SkeletonTrackingArrow skeletonTrackingArrow) {
        new BukkitRunnable() {

            @Override
            public void run() {
                if (!eliteEntity.isValid()) {
                    skeletonTrackingArrow.setFiring(false);
                    cancel();
                    return;
                }
                for (Entity nearbyEntity : eliteEntity.getLivingEntity().getNearbyEntities(20, 20, 20))
                    if (nearbyEntity instanceof Player)
                        if (((Player) nearbyEntity).getGameMode().equals(GameMode.ADVENTURE) ||
                                ((Player) nearbyEntity).getGameMode().equals(GameMode.SURVIVAL)) {
                            Arrow arrow = AttackArrow.shootArrow(eliteEntity.getLivingEntity(), (Player) nearbyEntity);
                            arrow.setVelocity(arrow.getVelocity().multiply(0.2));
                            arrow.setGravity(false);
                            trackingArrowLoop((Player) nearbyEntity, arrow);
                        }
            }

        }.runTaskTimer(MetadataHandler.PLUGIN, 0, 20 * 8);
    }

}
