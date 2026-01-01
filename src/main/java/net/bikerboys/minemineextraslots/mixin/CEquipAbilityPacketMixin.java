package net.bikerboys.minemineextraslots.mixin;

import net.minecraft.resources.*;
import net.minecraft.util.*;

import net.minecraft.world.entity.player.*;
import net.minecraftforge.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xyz.pixelatedw.mineminenomi.api.*;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.*;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CEquipAbilityPacket;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SEquipAbilityPacket;

import java.util.function.Supplier;

@Mixin(value = CEquipAbilityPacket.class, remap = false)
public class CEquipAbilityPacketMixin {

    /**
     * @author You
     * @reason Removes the slot limit check so extra slots (ID 80+) can be equipped.
     */
    @Overwrite
    public static void handle(CEquipAbilityPacket message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            context.enqueueWork(() -> {
                try {
                    Player player = context.getSender();
                    if (player != null) {
                        // --- USE ACCESSOR TO GET PRIVATE FIELDS ---
                        int slot = ((CEquipAbilityPacketAccessor) message).getSlot();
                        ResourceLocation abilityId = ((CEquipAbilityPacketAccessor) message).getAbilityId();
                        // ------------------------------------------

                        // REMOVED: if (message.slot <= maxBars) check

                        IAbilityData abilityDataProps = AbilityCapability.get(player).get();
                        Ability oldAbility = (Ability) abilityDataProps.getEquippedAbility(slot);

                        // Checks regarding the OLD ability (if one was already there)
                        if (oldAbility != null) {
                            if (oldAbility.hasComponent(ModAbilityComponents.COOLDOWN.get()) && ((CooldownComponent) oldAbility.getComponent(ModAbilityComponents.COOLDOWN.get()).get()).isOnCooldown()) return;
                            if (oldAbility.hasComponent(ModAbilityComponents.DISABLE.get()) && ((DisableComponent) oldAbility.getComponent(ModAbilityComponents.DISABLE.get()).get()).isDisabled()) return;
                            if (oldAbility.hasComponent(ModAbilityComponents.CONTINUOUS.get()) && ((ContinuousComponent) oldAbility.getComponent(ModAbilityComponents.CONTINUOUS.get()).get()).isContinuous()) return;
                            if (oldAbility.hasComponent(ModAbilityComponents.CHARGE.get()) && ((ChargeComponent) oldAbility.getComponent(ModAbilityComponents.CHARGE.get()).get()).isCharging()) return;
                        }

                        AbilityCore core = WyRegistry.ABILITIES.get().getValue(abilityId);
                        if (core != null) {
                            if (abilityDataProps.hasUnlockedAbility(core)) {
                                IAbility ability = core.createAbility();
                                abilityDataProps.setEquippedAbility(slot, ability);
                                ModNetwork.sendToAllTrackingAndSelf(new SEquipAbilityPacket(player.getId(), slot, core), player);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        context.setPacketHandled(true);
    }
}