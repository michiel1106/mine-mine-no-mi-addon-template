package net.bikerboys.minemineextraslots.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.*;
import xyz.pixelatedw.mineminenomi.packets.client.ability.*;

@Mixin(CUseAbilityPacket.class)
public interface GetSlot {


    @Accessor("slot")
    int getSlot();

}
