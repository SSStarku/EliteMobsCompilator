package com.magmaguy.elitemobs.config.customloot.premade;

import com.magmaguy.elitemobs.config.customloot.CustomLootConfigFields;
import org.bukkit.Material;

import java.util.Arrays;

public class TheFellerConfig extends CustomLootConfigFields {
    public TheFellerConfig() {
        super("the_feller",
                true,
                Material.DIAMOND_AXE,
                "&2The Feller",
                Arrays.asList("&aEven in your sleep,", "&ayou can feel this axe''s", "&asaplust"),
                Arrays.asList("LOOT_BONUS_BLOCKS,4", "SILK_TOUCH,1", "DURABILITY,6", "DIG_SPEED,6", "VANISHING_CURSE,1"),
                Arrays.asList("FAST_DIGGING,2,self,continuous", "NIGHT_VISION,1,self,continuous"),
                "dynamic",
                "dynamic",
                ItemType.UNIQUE);
    }
}
