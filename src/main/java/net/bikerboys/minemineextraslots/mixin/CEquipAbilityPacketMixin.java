package net.bikerboys.minemineextraslots.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xyz.pixelatedw.mineminenomi.api.ModRegistries;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CEquipAbilityPacket;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SEquipAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

import java.util.function.Supplier;

@Mixin(value = CEquipAbilityPacket.class, remap = false)
public class CEquipAbilityPacketMixin {


    @Overwrite
    public static void handle(CEquipAbilityPacket message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            context.enqueueWork(() -> {
                try {
                    PlayerEntity player = context.getSender();
                    if (player != null) {

                        int slot = ((CEquipAbilityPacketAccessor) message).getSlot();
                        ResourceLocation abilityId = ((CEquipAbilityPacketAccessor) message).getAbilityId();



                        IAbilityData abilityDataProps = AbilityDataCapability.get(player);
                        Ability oldAbility = (Ability) abilityDataProps.getEquippedAbility(slot);

                        if (oldAbility != null) {
                            if (oldAbility.hasComponent(ModAbilityKeys.COOLDOWN) && ((CooldownComponent) oldAbility.getComponent(ModAbilityKeys.COOLDOWN).get()).isOnCooldown()) return;
                            if (oldAbility.hasComponent(ModAbilityKeys.DISABLE) && ((DisableComponent) oldAbility.getComponent(ModAbilityKeys.DISABLE).get()).isDisabled()) return;
                            if (oldAbility.hasComponent(ModAbilityKeys.CONTINUOUS) && ((ContinuousComponent) oldAbility.getComponent(ModAbilityKeys.CONTINUOUS).get()).isContinuous()) return;
                            if (oldAbility.hasComponent(ModAbilityKeys.CHARGE) && ((ChargeComponent) oldAbility.getComponent(ModAbilityKeys.CHARGE).get()).isCharging()) return;
                        }

                        AbilityCore core = (AbilityCore) ModRegistries.ABILITIES.getValue(abilityId);
                        if (core != null) {
                            if (abilityDataProps.hasUnlockedAbility(core)) {
                                IAbility ability = core.createAbility();
                                abilityDataProps.setEquippedAbility(slot, ability);
                                WyNetwork.sendToAllTrackingAndSelf(new SEquipAbilityPacket(player.getId(), slot, core), player);
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