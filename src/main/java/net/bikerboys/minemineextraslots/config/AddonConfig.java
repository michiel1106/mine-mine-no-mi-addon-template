package net.bikerboys.minemineextraslots.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class AddonConfig {
    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static class Server {
        public final ForgeConfigSpec.IntValue EXTRA_SLOTS;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("General Settings");

            EXTRA_SLOTS = builder
                    .comment("How many extra ability slots should be available? (Default: 5, Max: 5)")
                    .defineInRange("extra_slots", 5, 0, 5);

            builder.pop();
        }
    }
}