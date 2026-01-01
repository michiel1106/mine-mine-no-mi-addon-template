package net.bikerboys.minemineextraslots.networking;

import net.bikerboys.minemineextraslots.MineMineExtraSlots;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.*;

public class AddonNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MineMineExtraSlots.MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        int id = 0;
        CHANNEL.registerMessage(
                id++,
                S2CSyncConfigPacket.class,
                S2CSyncConfigPacket::encode,
                S2CSyncConfigPacket::decode,
                S2CSyncConfigPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT) // ensure client-only
        );
    }

    public static void sendTo(S2CSyncConfigPacket packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(S2CSyncConfigPacket packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
