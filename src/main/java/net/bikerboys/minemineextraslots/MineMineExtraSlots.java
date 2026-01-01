package net.bikerboys.minemineextraslots;

import com.mojang.blaze3d.systems.RenderSystem;
import static net.bikerboys.minemineextraslots.client.ClientEvents.EXTRA_SLOT_KEYS;
import net.bikerboys.minemineextraslots.config.AddonConfig;
import net.bikerboys.minemineextraslots.networking.AddonNetwork;
import net.bikerboys.minemineextraslots.networking.S2CSyncConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.*;
import net.minecraftforge.api.distmarker.Dist;


import net.minecraftforge.client.event.*;
import net.minecraftforge.common.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.server.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponentKey;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SlotDecorationComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.api.WyHelper;

import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

import xyz.pixelatedw.mineminenomi.data.entity.stats.*;
import xyz.pixelatedw.mineminenomi.init.ModAbilityComponents;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CUseAbilityPacket;
import xyz.pixelatedw.mineminenomi.init.ModNetwork;

import net.minecraft.client.KeyMapping;

import java.util.Optional;

@SuppressWarnings("removal")
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
            if (!(event.getEntity() instanceof ServerPlayer)) return;

            ServerPlayer player = (ServerPlayer) event.getEntity();
            int slots = AddonConfig.SERVER.EXTRA_SLOTS.get();

            AddonNetwork.sendTo(new S2CSyncConfigPacket(slots), player);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        private static int colorTicks = 0;

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void renderExtraSlots(RenderGuiOverlayEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            if (mc.options.hideGui) return;

            Player player = mc.player;
            IEntityStats entityStats = EntityStatsCapability.get(player).orElse(null);
            if (entityStats == null || !entityStats.isInCombatMode()) return;

            IAbilityData abilityData = AbilityCapability.get(player).orElse(null);
            if (abilityData == null) return;

            GuiGraphics graphics = event.getGuiGraphics();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            int baseX = (screenWidth / 2) + MineMineExtraSlots.xOffset;
            int baseY = screenHeight + MineMineExtraSlots.yOffset;
            int spacing = 25;

            int activeSlots = MineMineExtraSlots.CLIENT_SLOT_COUNT;

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 100);

            for (int i = 0; i < activeSlots; i++) {
                int slotId = 80 + i + (abilityData.getCombatBarSet() * 8);

                IAbility abl = null;
                try {
                    abl = abilityData.getEquippedAbility(slotId);
                } catch (Exception ignored) {}

                int x = baseX + (i * spacing);
                int y = baseY;

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                if (abl == null) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    graphics.blit(ModResources.WIDGETS, x, y, 0, 0, 23, 23);
                } else {
                    String displayText = "";
                    double maxNumber = 1.0;
                    double number = 0.0;
                    float slotRed = 1.0F, slotGreen = 1.0F, slotBlue = 1.0F;
                    float iconRed = 1.0F, iconGreen = 1.0F, iconBlue = 1.0F, iconAlpha = 1.0F;

                    Optional<SlotDecorationComponent> slotDecoComponent = abl.getComponent((AbilityComponentKey) ModAbilityComponents.SLOT_DECORATION.get());

                    if (slotDecoComponent.isPresent()) {
                        SlotDecorationComponent slotDeco = slotDecoComponent.get();
                        number = slotDeco.getCurrentValue();
                        maxNumber = slotDeco.getMaxValue();
                        displayText = slotDeco.getDisplayText();

                        slotRed = slotDeco.getSlotRed();
                        slotGreen = slotDeco.getSlotGreen();
                        slotBlue = slotDeco.getSlotBlue();

                        iconRed = slotDeco.getIconRed();
                        iconGreen = slotDeco.getIconGreen();
                        iconBlue = slotDeco.getIconBlue();
                        iconAlpha = slotDeco.getIconAlpha();

                        slotDeco.triggerPreRenderEvents(player, mc, graphics, null, (float) x, (float) y, 0);
                    }

                    graphics.blit(ModResources.WIDGETS, x, y, 0, 0, 23, 23);
                    graphics.setColor(slotRed, slotGreen, slotBlue, 1.0F);

                    double slotHeight = Mth.clamp(23.0 - number / maxNumber * 23.0, 0.0, 23.0);
                    graphics.blit(ModResources.WIDGETS, x, y, 24, 0, 23, (int) slotHeight);
                    graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

                    try {
                        RendererHelper.drawIcon(abl.getIcon(player), graphics, (float) (x + 4), (float) (y + 4), 1.0F, 16.0F, 16.0F, iconRed, iconGreen, iconBlue, iconAlpha);
                    } catch (Exception ignored) {}

                    if (slotDecoComponent.isPresent()) {
                        slotDecoComponent.get().triggerPostRenderEvents(player, mc, graphics, null, (float) x, (float) y, 0);
                    }

                    graphics.pose().translate(0, 0, 5);
                    if (number > 0.0) {
                        String numText = displayText.isEmpty() ? String.format("%.1f", number / 20.0) + " " : displayText;
                        int textWidth = mc.font.width(numText);
                        int textX = x + 13 - (textWidth / 2);
                        int textY = y + 9;
                        RendererHelper.drawStringWithBorder(mc.font, graphics, numText, textX, textY, WyHelper.hexToRGB("#FFFFFF").getRGB());
                    }
                    graphics.pose().translate(0, 0, -5);
                }

                KeyMapping key = (i < EXTRA_SLOT_KEYS.length) ? EXTRA_SLOT_KEYS[i] : null;
                if (key != null) {
                    StringBuilder sb = new StringBuilder();
                    colorTicks = (colorTicks + 1) % 2000;
                    if (key.isUnbound()) {
                        sb.append(colorTicks >= 1000 ? "§4" : "§c").append("?");
                    } else {
                        sb.append("§7");
                        String keyName = key.getKey().getDisplayName().getString();
                        switch (key.getKeyModifier()) {
                            case ALT: sb.append("a"); break;
                            case CONTROL: sb.append("c"); break;
                            case SHIFT: sb.append("s"); break;
                        }
                        sb.append(keyName.length() > 2 ? keyName.substring(0, 1).toUpperCase() : keyName.toUpperCase());
                    }

                    graphics.pose().pushPose();
                    graphics.pose().translate(x + 18, y + 16, 10.0);
                    graphics.pose().scale(0.66F, 0.66F, 0.66F);
                    RendererHelper.drawStringWithBorder(mc.font, graphics, sb.toString(), 0, 0, -1);
                    graphics.pose().popPose();
                }
            }

            graphics.pose().popPose();
            RenderSystem.disableBlend();
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            if (player == null || mc.screen != null) return;

            IEntityStats entityStats = EntityStatsCapability.get(player).orElse(null);
            IAbilityData abilityData = AbilityCapability.get(player).orElse(null);
            if (entityStats == null || !entityStats.isInCombatMode() || abilityData == null) return;

            if (event.getAction() == 0) return;

            int activeSlots = MineMineExtraSlots.CLIENT_SLOT_COUNT;

            for (int i = 0; i < activeSlots; i++) {
                if (i >= EXTRA_SLOT_KEYS.length || EXTRA_SLOT_KEYS[i] == null) continue;

                if (EXTRA_SLOT_KEYS[i].consumeClick()) {
                    int slotId = 80 + i + (abilityData.getCombatBarSet() * 8);
                    IAbility abl = abilityData.getEquippedAbility(slotId);

                    if (abl != null) {
                        boolean isOnCooldown = false;
                        Optional<xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent> cooldownComp =
                                abl.getComponent((AbilityComponentKey) ModAbilityComponents.COOLDOWN.get());

                        if (cooldownComp.isPresent()) {
                            isOnCooldown = cooldownComp.get().isOnCooldown();
                        }

                        if (!isOnCooldown && entityStats.isInCombatMode()) {
                            ModNetwork.sendToServer(new CUseAbilityPacket(slotId));
                        }
                    }
                }
            }
        }
    }
}