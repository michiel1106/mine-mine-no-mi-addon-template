package net.bikerboys.minemineextraslots.networking;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.bikerboys.minemineextraslots.MineMineExtraSlots;

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
        CHANNEL.registerMessage(id++,
                S2CSyncConfigPacket.class,
                S2CSyncConfigPacket::encode,
                S2CSyncConfigPacket::new,
                S2CSyncConfigPacket::handle
        );
    }

    public static void sendToClient(Object packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
