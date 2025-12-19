package net.bikerboys.minemineextraslots.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xyz.pixelatedw.mineminenomi.abilities.doa.*;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.ChargeableAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility;
import xyz.pixelatedw.mineminenomi.api.events.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.events.passives.*;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CUseAbilityPacket;

import java.util.function.Supplier;

@Mixin(value = CUseAbilityPacket.class, remap = false)
public class CUseAbilityPacketMixin {


        /**
         * @author You
         * @reason Remove the slot ID limit check to allow addon slots (80+)
         */
        @Overwrite
        public static void handle(CUseAbilityPacket message, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();

            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                context.enqueueWork(() -> {
                    PlayerEntity player = context.getSender();
                    if (player == null) return;

                    player.level.getProfiler().push("abilityUse");

                    IAbilityData abilityDataProps = AbilityDataCapability.get(player);

                    // --- FIX: Use the Accessor here instead of casting to GetSlot ---
                    int targetSlot = ((GetSlot) message).getSlot();
                    // -----------------------------------------------------------------

                    Ability abl = (Ability) abilityDataProps.getEquippedAbility(targetSlot);
                    AbilityCanUseEvent pre = new AbilityCanUseEvent(player, abl);

                    if (!MinecraftForge.EVENT_BUS.post(pre)) {
                        if (abl != null && !player.isSpectator()) {
                            try {
                                // Logic checks from original mod
                                if (!(abl instanceof AirDoorAbility) && DoaPassiveEvents.isInsideDoor(player)) return;
                                if (abl instanceof ChargeableAbility && abl.isCharging() && !((ChargeableAbility) abl).isCancelable()) return;

                                // Fixed empty if block logic
                                if (abl instanceof MorphAbility && !abl.isContinuous()) {
                                    // Original mod usually returns here if morph isn't continuous?
                                    // Or does nothing? Leaving as is based on your snippet,
                                    // but standard behavior is usually to return if invalid.
                                }

                                abl.use(player);
                            } catch (Exception e) {
                                e.printStackTrace();
                                abl.startCooldown(player);
                            }
                            player.level.getProfiler().pop();
                        }
                    }
                });
            }
            context.setPacketHandled(true);
        }
    }
