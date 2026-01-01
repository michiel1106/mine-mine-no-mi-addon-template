package net.bikerboys.minemineextraslots.networking;

import net.bikerboys.minemineextraslots.*;

import net.minecraft.network.*;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.network.*;


import java.util.function.Supplier;

public class S2CSyncConfigPacket {
    public int slotCount;

    public S2CSyncConfigPacket() {} // needed for decode

    public S2CSyncConfigPacket(int slotCount) {
        this.slotCount = slotCount;
    }

    // write to buffer
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.slotCount);
    }

    // read from buffer
    public static S2CSyncConfigPacket decode(FriendlyByteBuf buffer) {
        S2CSyncConfigPacket msg = new S2CSyncConfigPacket();
        msg.slotCount = buffer.readInt();
        return msg;
    }

    // handle packet on client
    public static void handle(S2CSyncConfigPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlerClass.handlePacket(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }
}
