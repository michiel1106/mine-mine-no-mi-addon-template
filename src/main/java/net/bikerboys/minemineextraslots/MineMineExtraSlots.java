package net.bikerboys.minemineextraslots;

import com.mojang.blaze3d.systems.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.*;
import net.minecraft.util.math.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.client.gui.*;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.*;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.bikerboys.minemineextraslots.config.AddonConfig;
import net.bikerboys.minemineextraslots.networking.AddonNetwork;
import net.bikerboys.minemineextraslots.networking.S2CSyncConfigPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.*;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CUseAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.*;

import java.util.*;

@Mod("moreabilityslots")
public class MineMineExtraSlots {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "minemineextraslots";

    // --- CHANGED OFFSETS ---
    // We reduced xOffset to 95 because we are now adding it to the MIDDLE of the screen.
    // 95 pixels starts right after the standard hotbar ends.
    public static int xOffset = 125;
    public static int yOffset = -23; // Adjusted to sit in line with hotbar

    public static int CLIENT_SLOT_COUNT = 5;
    public static final int MAX_SLOT_CAP = 5;
    public static final KeyBinding[] EXTRA_SLOT_KEYS = new KeyBinding[MAX_SLOT_CAP];

    public MineMineExtraSlots() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AddonConfig.SERVER_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        AddonNetwork.register();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getPlayer().level.isClientSide) {
            int slots = AddonConfig.SERVER.EXTRA_SLOTS.get();
            AddonNetwork.sendToClient(new S2CSyncConfigPacket(slots), (ServerPlayerEntity) event.getPlayer());
        }
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        for (int i = 0; i < MAX_SLOT_CAP; i++) {
            EXTRA_SLOT_KEYS[i] = new KeyBinding("key.moreabilityslots.slot_" + (i + 1), -1, "key.categories.mineminenomi");
            ClientRegistry.registerKeyBinding(EXTRA_SLOT_KEYS[i]);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        private static int colorTicks = 0;


        @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
        public static void renderExtraSlots(RenderGameOverlayEvent.Post event) {
            try {
                if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) return;

                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null || mc.player == null) return;

                PlayerEntity player = mc.player;

                IEntityStats entityStats = EntityStatsCapability.get(player);
                if (entityStats == null || !entityStats.isInCombatMode()) return;

                IAbilityData abilityData = AbilityDataCapability.get(player);
                if (abilityData == null) return;

                // --- Layout Settings (FIXED) ---
                int screenWidth = mc.getWindow().getGuiScaledWidth();
                int screenHeight = mc.getWindow().getGuiScaledHeight();

                // ANCHOR TO CENTER: screenWidth / 2
                int baseX = (screenWidth / 2) + MineMineExtraSlots.xOffset;
                int baseY = screenHeight + MineMineExtraSlots.yOffset;
                int spacing = 25;

                int activeSlots = MineMineExtraSlots.CLIENT_SLOT_COUNT;

                mc.getTextureManager().bind(ModResources.WIDGETS);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                event.getMatrixStack().pushPose();
                event.getMatrixStack().translate(0, 0, 100);

                for (int i = 0; i < activeSlots; i++) {
                    int slotId = 80 + i + (abilityData.getCombatBarSet() * 8);

                    IAbility abl = null;
                    try {
                        abl = abilityData.getEquippedAbility(slotId);
                    } catch (Exception ignored) {
                    }

                    int x = baseX + (i * spacing);
                    int y = baseY;

                    mc.getTextureManager().bind(ModResources.WIDGETS);
                    RenderSystem.enableBlend();

                    // === 1. DRAW EMPTY SLOT ===
                    if (abl == null) {
                        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                        GuiUtils.drawTexturedModalRect(event.getMatrixStack(), x, y, 0, 0, 23, 23, 0.0F);
                    }
                    // === 2. DRAW ABILITY SLOT ===
                    else {
                        boolean hasDisplayText = false;
                        String displayText = "";
                        double maxNumber = 1.0;
                        double number = 0.0;
                        float slotRed = 1.0F, slotGreen = 1.0F, slotBlue = 1.0F;
                        float iconRed = 1.0F, iconGreen = 1.0F, iconBlue = 1.0F, iconAlpha = 1.0F;

                        Optional<SlotDecorationComponent> slotDecoComponent = abl.getComponent(ModAbilityKeys.SLOT_DECORATION);

                        if (slotDecoComponent.isPresent()) {
                            SlotDecorationComponent slotDeco = slotDecoComponent.get();
                            number = (double) slotDeco.getCurrentValue();
                            maxNumber = (double) slotDeco.getMaxValue();
                            hasDisplayText = slotDeco.hasDisplayText();
                            displayText = slotDeco.getDisplayText();

                            slotRed = slotDeco.getSlotRed();
                            slotGreen = slotDeco.getSlotGreen();
                            slotBlue = slotDeco.getSlotBlue();

                            iconRed = slotDeco.getIconRed();
                            iconGreen = slotDeco.getIconGreen();
                            iconBlue = slotDeco.getIconBlue();
                            iconAlpha = slotDeco.getIconAlpha();

                            slotDeco.triggerPreRenderEvents(player, mc, event.getMatrixStack(), (float) x, (float) y, event.getPartialTicks());
                        }

                        RendererHelper.drawTexturedModalRect(event.getMatrixStack(), (float) x, (float) y, 0.0F, 0.0F, 23.0F, 23.0F, 0.0F, slotRed, slotGreen, slotBlue, 1.0F);

                        double slotHeight = MathHelper.clamp(23.0 - number / maxNumber * 23.0, 0.0, 23.0);
                        RendererHelper.drawTexturedModalRect(event.getMatrixStack(), (float) x, (float) y, 24.0F, 0.0F, 23.0F, (float) ((int) slotHeight), 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);

                        try {
                            RendererHelper.drawIcon(abl.getIcon(player), event.getMatrixStack(), (float) (x + 4), (float) (y + 4), 1.0F, 16.0F, 16.0F, iconRed, iconGreen, iconBlue, iconAlpha);
                        } catch (Exception ignored) {
                        }

                        if (slotDecoComponent.isPresent()) {
                            slotDecoComponent.get().triggerPostRenderEvents(player, mc, event.getMatrixStack(), (float) x, (float) y, event.getPartialTicks());
                        }

                        event.getMatrixStack().translate(0, 0, 5);
                        if (number > 0.0) {
                            String numText = hasDisplayText ? displayText : String.format("%.1f", number / 20.0) + " ";
                            int textWidth = mc.font.width(numText);
                            int textX = x + 13 - (textWidth / 2);
                            int textY = y + 9;
                            WyHelper.drawStringWithBorder(mc.font, event.getMatrixStack(), numText, textX, textY, WyHelper.hexToRGB("#FFFFFF").getRGB());
                        }
                        event.getMatrixStack().translate(0, 0, -5);
                    }

                    // === 3. DRAW KEYBIND ===
                    KeyBinding key = (i < EXTRA_SLOT_KEYS.length) ? EXTRA_SLOT_KEYS[i] : null;
                    if (key != null) {
                        StringBuilder sb = new StringBuilder();
                        colorTicks = (colorTicks + 1) % 2000;
                        if (key.isUnbound()) {
                            sb.append(colorTicks >= 1000 ? "§4" : "§c").append("?");
                        } else {
                            sb.append("§7");
                            String keyName = key.getKey().getDisplayName().getString();
                            switch (key.getKeyModifier()) {
                                case ALT:
                                    sb.append("a");
                                    break;
                                case CONTROL:
                                    sb.append("c");
                                    break;
                                case SHIFT:
                                    sb.append("s");
                                    break;
                            }
                            sb.append(keyName.length() > 2 ? keyName.substring(0, 1).toUpperCase() : keyName.toUpperCase());
                        }

                        event.getMatrixStack().pushPose();
                        event.getMatrixStack().translate(x + 18, y + 16, 10.0);
                        event.getMatrixStack().scale(0.66F, 0.66F, 0.66F);
                        WyHelper.drawStringWithBorder(mc.font, event.getMatrixStack(), sb.toString(), 0, 0, -1);
                        event.getMatrixStack().popPose();
                    }
                }

                event.getMatrixStack().popPose();
                RenderSystem.disableBlend();
            } catch (Exception e) {
                System.out.println("something happened");
            }


        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.KeyInputEvent event) {
            Minecraft mc = Minecraft.getInstance();
            PlayerEntity player = mc.player;
            if (player == null) return;
            IEntityStats entityStats = EntityStatsCapability.get(player);
            if (entityStats == null || !entityStats.isInCombatMode()) return;
            IAbilityData abilityData = AbilityDataCapability.get(player);
            if (abilityData == null) return;
            if (EXTRA_SLOT_KEYS == null) return;

            int activeSlots = MineMineExtraSlots.CLIENT_SLOT_COUNT;

            for (int i = 0; i < activeSlots; i++) {
                if (i >= EXTRA_SLOT_KEYS.length || EXTRA_SLOT_KEYS[i] == null) continue;
                if (EXTRA_SLOT_KEYS[i].consumeClick()) {
                    int slotId = 80 + i + (abilityData.getCombatBarSet() * 8);
                    WyNetwork.sendToServer(new CUseAbilityPacket(slotId));
                }
            }
        }
    }
}
