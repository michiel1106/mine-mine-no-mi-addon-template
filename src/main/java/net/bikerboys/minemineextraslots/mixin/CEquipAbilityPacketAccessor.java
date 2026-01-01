package net.bikerboys.minemineextraslots.mixin;

import net.minecraft.resources.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CEquipAbilityPacket;

@Mixin(value = CEquipAbilityPacket.class, remap = false)
public interface CEquipAbilityPacketAccessor {
    @Accessor("slot")
    int getSlot();

    @Accessor("abilityId")
    net.minecraft.resources.ResourceLocation getAbilityId();
}