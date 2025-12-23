package net.bikerboys.minemineextraslots;

import net.bikerboys.minemineextraslots.networking.*;
import net.minecraftforge.fml.network.*;

import java.util.function.*;

public class ClientPacketHandlerClass {


    public static void handlePacket(S2CSyncConfigPacket message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().enqueueWork(() -> {
                MineMineExtraSlots.CLIENT_SLOT_COUNT = message.slotCount;

            });
        }

    }
}
