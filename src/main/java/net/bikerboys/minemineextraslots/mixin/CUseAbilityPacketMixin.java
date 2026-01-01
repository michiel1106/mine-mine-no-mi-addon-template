package net.bikerboys.minemineextraslots.mixin;

import net.minecraft.network.chat.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xyz.pixelatedw.mineminenomi.abilities.doa.*;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.events.ability.*;
import xyz.pixelatedw.mineminenomi.config.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;

import xyz.pixelatedw.mineminenomi.packets.client.ability.CUseAbilityPacket;

import java.util.function.Supplier;

@Mixin(value = CUseAbilityPacket.class, remap = false)
public class CUseAbilityPacketMixin {


        /**
         * @author
         * @reason
         */
        @Overwrite
        public static void handle(CUseAbilityPacket message, Supplier<NetworkEvent.Context> ctx) {
            if (((NetworkEvent.Context)ctx.get()).getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ((NetworkEvent.Context)ctx.get()).enqueueWork(() -> {
                    ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
                    player.level().getProfiler().push("abilityUse");
                    IAbilityData abilityData = (IAbilityData)AbilityCapability.get(player).orElse((IAbilityData) null);
                    if (abilityData != null) {
                        Ability abl = (Ability)abilityData.getEquippedAbility(((GetSlot)message).getSlot());
                        if (abl != null && !player.isSpectator()) {
                            AbilityCanUseEvent pre = new AbilityCanUseEvent(player, abl);
                            if (!MinecraftForge.EVENT_BUS.post(pre)) {
                                abl.use(player);
                                player.getCommandSenderWorld().getProfiler().pop();
                            }
                        }
                    }
                }).exceptionally((e) -> {
                    e.printStackTrace();
                        ((NetworkEvent.Context)ctx.get()).getNetworkManager().disconnect(Component.translatable("mineminenomi.networking.failed", new Object[]{e.getMessage()}));
                    return null;
                });
            }

            ((NetworkEvent.Context)ctx.get()).setPacketHandled(true);
        }
    }
