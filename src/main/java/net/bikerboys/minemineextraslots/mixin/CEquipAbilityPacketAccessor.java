package net.bikerboys.minemineextraslots.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CEquipAbilityPacket;

@Mixin(value = CEquipAbilityPacket.class, remap = false)
public interface CEquipAbilityPacketAccessor {
    @Accessor("slot")
    int getSlot();

    @Accessor("abilityId")
    ResourceLocation getAbilityId();
}