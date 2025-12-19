package net.bikerboys.minemineextraslots.mixin;

import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.bikerboys.minemineextraslots.MineMineExtraSlots;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CRemoveAbilityPacket;
import xyz.pixelatedw.mineminenomi.screens.SelectHotbarAbilitiesScreen;
import xyz.pixelatedw.mineminenomi.screens.extra.buttons.AbilitySlotButton;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = SelectHotbarAbilitiesScreen.class, remap = false)
public abstract class SelectHotbarAbilitiesScreenMixin extends Screen implements INestedGuiEventHandler {


    @Shadow protected PlayerEntity player;
    @Final @Shadow protected List<AbilitySlotButton> abilitySlots;
    @Shadow public int groupSelected;
    @Shadow private IAbilityData abilityDataProps;
    @Shadow public int slotSelected;

    @Unique
    private final List<AbilitySlotButton> extraAbilitySlots = new ArrayList<>();

    protected SelectHotbarAbilitiesScreenMixin(ITextComponent title) {
        super(title);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickExtraSlots(CallbackInfo ci) {
        for (int i = 0; i < this.extraAbilitySlots.size(); i++) {
            int slotId = groupSelected * 8 + 80 + i;
            AbilitySlotButton btn = this.extraAbilitySlots.get(i);

            IAbility ability = null;
            try {
                ability = this.abilityDataProps.getEquippedAbility(slotId);
            } catch (IndexOutOfBoundsException ignored) {}

            btn.setAbility(ability);
        }
    }

    @Inject(method = "updateSlots", at = @At("RETURN"))
    private void addExtraBottomLeftSlots(CallbackInfo ci) {
        try {
            final int extraSlots = MineMineExtraSlots.CLIENT_SLOT_COUNT;

            // --- FIXED COORDINATES ---
            // Anchor to Center (this.width / 2)
            final int baseX = (this.width / 2) + MineMineExtraSlots.xOffset;
            final int baseY = this.height + MineMineExtraSlots.yOffset;
            final int spacing = 25;

            for (int i = 0; i < extraSlots; i++) {
                final int initialSlotId = i + groupSelected * 8 + 80;

                IAbility ability = null;
                try {
                    ability = this.abilityDataProps.getEquippedAbility(initialSlotId);
                } catch (IndexOutOfBoundsException ignored) {
                }

                if (i < this.extraAbilitySlots.size()) {
                    AbilitySlotButton existing = this.extraAbilitySlots.get(i);
                    existing.setAbility(ability);
                    continue;
                }

                final int buttonIndex = i;
                AbilitySlotButton slotButton = new AbilitySlotButton(
                        ability,
                        baseX + i * spacing,
                        baseY,
                        22,
                        21,
                        this.player,
                        (btn) -> this.handleExtraSlotClick((AbilitySlotButton) btn, buttonIndex)
                );

                this.addButton(slotButton);
                this.extraAbilitySlots.add(slotButton);
            }

            while (this.extraAbilitySlots.size() > extraSlots) {
                AbilitySlotButton removed = this.extraAbilitySlots.remove(this.extraAbilitySlots.size() - 1);
                this.children.remove(removed);
                this.buttons.remove(removed);
            }
        } catch (Exception e) {
            System.out.println("why");
        }
    }

    @Unique
    private void handleExtraSlotClick(AbilitySlotButton btn, int buttonIndex) {
        try {
            int currentSlotId = buttonIndex + (this.groupSelected * 8) + 80;

            if (this.slotSelected != currentSlotId) {
                this.slotSelected = currentSlotId;
                for (AbilitySlotButton slotBtn : this.abilitySlots) slotBtn.setIsPressed(false);
                for (AbilitySlotButton extraBtn : this.extraAbilitySlots) extraBtn.setIsPressed(false);
                btn.setIsPressed(true);
            } else {
                IAbility ability = null;
                try {
                    ability = this.abilityDataProps.getEquippedAbility(this.slotSelected);
                } catch (IndexOutOfBoundsException ignored) {
                    return;
                }

                if (ability == null) return;
                if (ability.hasComponent(ModAbilityKeys.COOLDOWN) && ability.getComponent(ModAbilityKeys.COOLDOWN).get().isOnCooldown())
                    return;
                if (ability.hasComponent(ModAbilityKeys.DISABLE) && ability.getComponent(ModAbilityKeys.DISABLE).get().isDisabled())
                    return;
                if (ability.hasComponent(ModAbilityKeys.CONTINUOUS) && ability.getComponent(ModAbilityKeys.CONTINUOUS).get().isContinuous())
                    return;
                if (ability.hasComponent(ModAbilityKeys.CHARGE) && ability.getComponent(ModAbilityKeys.CHARGE).get().isCharging())
                    return;

                WyNetwork.sendToServer(new CRemoveAbilityPacket(this.slotSelected));
                this.abilityDataProps.setEquippedAbility(this.slotSelected, null);
                btn.setAbility(null);
            }
        } catch (Exception e) {
            System.out.println("something else happened selecthotbarabilitiesscreen");
        }
    }
}