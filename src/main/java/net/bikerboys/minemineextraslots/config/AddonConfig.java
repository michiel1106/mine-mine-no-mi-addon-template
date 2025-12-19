package net.bikerboys.minemineextraslots.config;

import net.minecraftforge.common.*;
import org.apache.commons.lang3.tuple.Pair;

public class AddonConfig {
    public static final Common SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue EXTRA_SLOTS;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("General Settings");

            EXTRA_SLOTS = builder
                    .comment("How many extra ability slots should be available? (Default: 5, Max: 5)")
                    .defineInRange("extra_slots", 5, 0, 5);

            builder.pop();
        }
    }
}