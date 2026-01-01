package net.bikerboys.minemineextraslots.mixin;

import net.minecraft.world.entity.player.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xyz.pixelatedw.mineminenomi.abilities.doa.*;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.ChargeableAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility;
import xyz.pixelatedw.mineminenomi.api.events.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.events.passives.*;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CUseAbilityPacket;

import java.util.function.Supplier;

@Mixin(value = CUseAbilityPacket.class, remap = false)
public class CUseAbilityPacketMixin {


        /**
         * @author
         * @reason
         */
        @Overwrite
        public static void handle(CUseAbilityPacket message, Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();

            if (context.getDirection() == net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER) {
                context.enqueueWork(() -> {
                    Player player = context.getSender();
                    if (player == null) return;

                    player.level.getProfiler().push("abilityUse");

                    IAbilityData abilityDataProps = AbilityCapability.get(player).get();

                    int targetSlot = ((GetSlot) message).getSlot();

                    Ability abl = (Ability) abilityDataProps.getEquippedAbility(targetSlot);
                    AbilityCanUseEvent pre = new AbilityCanUseEvent(player, abl);

                    if (!MinecraftForge.EVENT_BUS.post(pre)) {
                        if (abl != null && !player.isSpectator()) {
                            try {
                                if (!(abl instanceof AirDoorAbility) && DoaPassiveEvents.isInsideDoor(player)) return;
                                if (abl instanceof ChargeableAbility && abl.isCharging() && !((ChargeableAbility) abl).isCancelable()) return;

                                if (abl instanceof MorphAbility && !abl.isContinuous()) {

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
