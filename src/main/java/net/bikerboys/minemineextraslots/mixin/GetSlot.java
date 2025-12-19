package net.bikerboys.minemineextraslots.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.*;
import xyz.pixelatedw.mineminenomi.packets.client.ability.*;

@Mixin(value = CUseAbilityPacket.class, remap = false)
public interface GetSlot {


    @Accessor("slot")
    int getSlot();

}
