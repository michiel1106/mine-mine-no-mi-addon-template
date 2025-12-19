package net.bikerboys.minemineextraslots.networking;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.bikerboys.minemineextraslots.MineMineExtraSlots;

import java.util.function.Supplier;

public class S2CSyncConfigPacket {
    private final int slotCount;

    public S2CSyncConfigPacket(int slotCount) {
        this.slotCount = slotCount;
    }
    public S2CSyncConfigPacket(PacketBuffer buffer) {
        this.slotCount = buffer.readInt();
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeInt(this.slotCount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {

            MineMineExtraSlots.CLIENT_SLOT_COUNT = this.slotCount;
            MineMineExtraSlots.LOGGER.info("Synced extra slots from server: " + this.slotCount);
        });
        ctx.get().setPacketHandled(true);
    }
}
