package net.bikerboys.minemineextraslots;

import com.mojang.blaze3d.systems.RenderSystem;

import net.bikerboys.minemineextraslots.config.AddonConfig;
import net.bikerboys.minemineextraslots.networking.AddonNetwork;
import net.bikerboys.minemineextraslots.networking.S2CSyncConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.server.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SlotDecorationComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import static xyz.pixelatedw.mineminenomi.init.ModKeybindings.changeAbilityMode;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CUseAbilityPacket;
import xyz.pixelatedw.mineminenomi.packets.client.ability.components.*;
import xyz.pixelatedw.mineminenomi.wypi.*;

import java.util.Optional;

@Mod(MineMineExtraSlots.MODID)
public class MineMineExtraSlots {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "minemineextraslots";

    public static int xOffset = 125;
    public static int yOffset = -23;

    public static int CLIENT_SLOT_COUNT = 5;
    public static final int MAX_SLOT_CAP = 5;


    public MineMineExtraSlots() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AddonConfig.SERVER_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        AddonNetwork.register();
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;

            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            int slots = AddonConfig.SERVER.EXTRA_SLOTS.get();

            AddonNetwork.sendTo(new S2CSyncConfigPacket(slots), player);
        }

    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        private static int colorTicks = 0;

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void renderExtraSlots(RenderGameOverlayEvent.Post event) {

            if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            if (mc.options.hideGui) return;

            PlayerEntity player = mc.player;
            IEntityStats entityStats = EntityStatsCapability.get(player);
            if (entityStats == null || !entityStats.isInCombatMode()) return;

            IAbilityData abilityData = AbilityDataCapability.get(player);
            if (abilityData == null) return;

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            int baseX = (screenWidth / 2) + MineMineExtraSlots.xOffset;
            int baseY = screenHeight + MineMineExtraSlots.yOffset;
            int spacing = 25;

            int activeSlots = MineMineExtraSlots.CLIENT_SLOT_COUNT;

            event.getMatrixStack().pushPose();
            event.getMatrixStack().translate(0, 0, 100);

            // RENDER BOTH COMBAT BARS
            for (int bar = 0; bar < 2; bar++) {

                int barYOffset = bar == 0 ? 0 : -26;

                for (int i = 0; i < activeSlots; i++) {

                    int slotId = 80 + (bar * 8) + i;

                    IAbility abl;
                    try {
                        abl = abilityData.getEquippedAbility(slotId);
                    } catch (Exception e) {
                        continue;
                    }

                    int x = baseX + (i * spacing);
                    int y = baseY + barYOffset;

                    mc.getTextureManager().bind(ModResources.WIDGETS);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();

                    if (abl == null) {
                        RenderSystem.color4f(1F, 1F, 1F, 1F);
                        mc.gui.blit(event.getMatrixStack(), x, y, 0, 0, 23, 23);
                    } else {
                        boolean hasDisplayText = false;
                        String displayText = "";
                        double maxNumber = 1.0;
                        double number = 0.0;

                        float slotRed = 1F, slotGreen = 1F, slotBlue = 1F;
                        float iconRed = 1F, iconGreen = 1F, iconBlue = 1F, iconAlpha = 1F;

                        Optional<SlotDecorationComponent> decoOpt =
                                abl.getComponent(ModAbilityKeys.SLOT_DECORATION);

                        if (decoOpt.isPresent()) {
                            SlotDecorationComponent deco = decoOpt.get();
                            number = deco.getCurrentValue();
                            maxNumber = deco.getMaxValue();
                            hasDisplayText = deco.hasDisplayText();
                            displayText = deco.getDisplayText();

                            slotRed = deco.getSlotRed();
                            slotGreen = deco.getSlotGreen();
                            slotBlue = deco.getSlotBlue();

                            iconRed = deco.getIconRed();
                            iconGreen = deco.getIconGreen();
                            iconBlue = deco.getIconBlue();
                            iconAlpha = deco.getIconAlpha();

                            deco.triggerPreRenderEvents(player, mc, event.getMatrixStack(), x, y, event.getPartialTicks());
                        }

                        RendererHelper.drawTexturedModalRect(
                                event.getMatrixStack(), x, y,
                                0F, 0F, 23F, 23F,
                                0F, slotRed, slotGreen, slotBlue, 1F
                        );

                        double slotHeight = MathHelper.clamp(
                                23.0 - number / maxNumber * 23.0,
                                0.0, 23.0
                        );

                        RendererHelper.drawTexturedModalRect(
                                event.getMatrixStack(), x, y,
                                24F, 0F, 23F, (float) slotHeight,
                                0F, 1F, 1F, 1F, 1F
                        );

                        try {
                            RendererHelper.drawIcon(
                                    abl.getIcon(player),
                                    event.getMatrixStack(),
                                    x + 4, y + 4,
                                    1F, 16F, 16F,
                                    iconRed, iconGreen, iconBlue, iconAlpha
                            );
                        } catch (Exception ignored) {}

                        decoOpt.ifPresent(deco ->
                                deco.triggerPostRenderEvents(player, mc, event.getMatrixStack(), x, y, event.getPartialTicks())
                        );

                        if (number > 0.0) {
                            String text = hasDisplayText
                                    ? displayText
                                    : String.format("%.1f", number / 20.0);

                            int textWidth = mc.font.width(text);
                            WyHelper.drawStringWithBorder(
                                    mc.font,
                                    event.getMatrixStack(),
                                    text,
                                    x + 13 - (textWidth / 2),
                                    y + 9,
                                    WyHelper.hexToRGB("#FFFFFF").getRGB()
                            );
                        }
                    }

                    // ONLY DRAW KEYS FOR ACTIVE BAR
                    KeyBinding key =
                            bar == 1
                                    ? net.bikerboys.minemineextraslots.client.ClientEvents.EXTRA_SLOT_KEYS_BAR_1[i]
                                    : net.bikerboys.minemineextraslots.client.ClientEvents.EXTRA_SLOT_KEYS_BAR_0[i];

                    if (key != null) {
                        StringBuilder sb = new StringBuilder();
                        colorTicks = (colorTicks + 1) % 2000;

                        if (key.isUnbound()) {
                            sb.append(colorTicks >= 1000 ? "§4" : "§c").append("?");
                        } else {
                            sb.append("§7");
                            switch (key.getKeyModifier()) {
                                case ALT: sb.append("a"); break;
                                case CONTROL: sb.append("c"); break;
                                case SHIFT: sb.append("s"); break;
                            }
                            String k = key.getKey().getDisplayName().getString();
                            sb.append(k.length() > 2 ? k.substring(0, 1).toUpperCase() : k.toUpperCase());
                        }

                        event.getMatrixStack().pushPose();
                        event.getMatrixStack().translate(x + 18, y + 16, 10);
                        event.getMatrixStack().scale(0.66F, 0.66F, 0.66F);
                        WyHelper.drawStringWithBorder(mc.font, event.getMatrixStack(), sb.toString(), 0, 0, -1);
                        event.getMatrixStack().popPose();
                    }


                }
            }

            event.getMatrixStack().popPose();
            RenderSystem.disableBlend();
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onKeyInput(InputEvent.KeyInputEvent event) {
            Minecraft mc = Minecraft.getInstance();
            PlayerEntity player = mc.player;

            if (player == null || mc.screen != null) return;

            IEntityStats entityStats = EntityStatsCapability.get(player);
            IAbilityData abilityData = AbilityDataCapability.get(player);
            if (entityStats == null || abilityData == null) return;

            if (event.getAction() == 0) return;

            int activeSlots = MineMineExtraSlots.CLIENT_SLOT_COUNT;

            // Loop over both bars (0 and 1)
            for (int bar = 0; bar < 2; bar++) {
                KeyBinding[] keys =
                        bar == 1
                                ? net.bikerboys.minemineextraslots.client.ClientEvents.EXTRA_SLOT_KEYS_BAR_1
                                : net.bikerboys.minemineextraslots.client.ClientEvents.EXTRA_SLOT_KEYS_BAR_0;

                for (int i = 0; i < activeSlots; i++) {
                    if (i >= keys.length || keys[i] == null) continue;

                    if (keys[i].consumeClick()) {
                        int slotId = 80 + (bar * 8) + i;
                        IAbility abl = abilityData.getEquippedAbility(slotId);

                        if (abl != null) {
                            boolean isOnCooldown = abl.hasComponent(ModAbilityKeys.COOLDOWN)
                                    && abl.getComponent(ModAbilityKeys.COOLDOWN)
                                    .map(c -> c.isOnCooldown() && c.getCooldown() > 10.0F)
                                    .orElse(false);

                            if (!isOnCooldown && entityStats.isInCombatMode()) {
                                if (changeAbilityMode.isDown() && abl.hasComponent(ModAbilityKeys.ALT_MODE)) {
                                    WyNetwork.sendToServer(new CChangeAbilityAltModePacket(slotId));
                                } else {
                                    WyNetwork.sendToServer(new CUseAbilityPacket(slotId));
                                }
                            }
                        }
                    }
                }
            }
        }


    }
}