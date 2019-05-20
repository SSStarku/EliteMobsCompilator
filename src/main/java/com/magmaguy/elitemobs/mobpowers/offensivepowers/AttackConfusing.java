package com.magmaguy.elitemobs.mobpowers.offensivepowers;

import com.magmaguy.elitemobs.api.PlayerDamagedByEliteMobEvent;
import com.magmaguy.elitemobs.mobpowers.MinorPower;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Created by MagmaGuy on 30/04/2017.
 */
public class AttackConfusing extends MinorPower implements Listener {

    public AttackConfusing() {
        super("AttackConfusing", Particle.SPELL_MOB);
    }

    @EventHandler
    public void attackConfusing(PlayerDamagedByEliteMobEvent event) {
        AttackConfusing attackConfusing = (AttackConfusing) event.getEliteMobEntity().getPower(this);
        if (attackConfusing == null) return;
        if (attackConfusing.isCooldown()) return;

        attackConfusing.doCooldown(20 * 10);
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 10, 3));
    }

}
