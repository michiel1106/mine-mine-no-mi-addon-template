package net.bikerboys.minemineextraslots.mixin;

import net.bikerboys.minemineextraslots.MineMineExtraSlots;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.player.*;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

import xyz.pixelatedw.mineminenomi.init.*;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CEquipAbilityPacket;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CRemoveAbilityPacket;
import xyz.pixelatedw.mineminenomi.ui.screens.*;
import xyz.pixelatedw.mineminenomi.ui.widget.*;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(value = AbilitiesListScreen.class, remap = false)
public abstract class SelectHotbarAbilitiesScreenMixin extends Screen {

    @Shadow protected Player player;
    @Final @Shadow protected List<AbilitySlotButton> abilitySlots;
    @Shadow public int groupSelected;
    @Shadow private IAbilityData abilityDataProps;
    @Shadow public int slotSelected;

    @Shadow public abstract AbilitiesListScreen.Selection getSelectionMode();
    @Shadow public abstract AbilityCore<?> getDraggedAbility();
    @Shadow public abstract void setDraggedAbility(AbilityCore<?> ability);

    @Shadow public abstract boolean hasDraggedAbility();

    @Unique
    private final List<AbilitySlotButton> extraAbilitySlots = new ArrayList<>();

    protected SelectHotbarAbilitiesScreenMixin(Component title) {
        super(title);
    }


    @Inject(method = "m_86600_", at = @At("TAIL"), remap = false)
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

    @Inject(method = "updateSlots()V", at = @At("RETURN"))
    private void addExtraBottomLeftSlots(CallbackInfo ci) {
        if (!this.extraAbilitySlots.isEmpty()) {
            this.renderables.removeAll(this.extraAbilitySlots);
            this.children().removeAll(this.extraAbilitySlots);
            this.extraAbilitySlots.clear();
        }

        final int extraSlots = MineMineExtraSlots.CLIENT_SLOT_COUNT;
        final int baseX = (this.width / 2) + MineMineExtraSlots.xOffset;
        final int baseY = this.height + MineMineExtraSlots.yOffset;
        final int spacing = 25;

        for (int i = 0; i < extraSlots; i++) {
            final int slotId = i + groupSelected * 8 + 80;

            IAbility ability = null;
            try {
                ability = this.abilityDataProps.getEquippedAbility(slotId);
            } catch (IndexOutOfBoundsException ignored) {}

            final int buttonIndex = i;

            AbilitySlotButton slotButton = new AbilitySlotButton(
                    ability,
                    baseX + i * spacing,
                    baseY,
                    22,
                    21,
                    (AbstractClientPlayer) this.player,
                    (btn) -> this.handleExtraSlotClick((AbilitySlotButton) btn, buttonIndex)
            );

            this.addWidget(slotButton);
            this.extraAbilitySlots.add(slotButton);
        }
    }

    @Inject(method = "m_88315_", at = @At("TAIL"), remap = false)
    public void renderExtraSlots(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        for (AbilitySlotButton btn : this.extraAbilitySlots) {
            btn.render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }


    @Inject(method = "m_6375_", at = @At("HEAD"), cancellable = true, remap = false)
    public void mouseClickedExtraSlots(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 1) {
            for (int i = 0; i < this.extraAbilitySlots.size(); i++) {
                AbilitySlotButton btn = this.extraAbilitySlots.get(i);
                if (btn.isMouseOver(mouseX, mouseY)) {
                    int slotId = groupSelected * 8 + 80 + i;

                    if (this.getSelectionMode() == AbilitiesListScreen.Selection.KEYBIND && this.slotSelected >= 0) {
                        this.slotSelected = -1;
                        btn.setIsPressed(false);
                    }

                    ModNetwork.sendToServer(new CRemoveAbilityPacket(slotId));
                    this.abilityDataProps.setEquippedAbility(slotId, null);
                    btn.setAbility(null);

                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "m_6348_", at = @At("HEAD"), cancellable = true, remap = false)
    public void mouseReleasedExtraSlots(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.getSelectionMode() == AbilitiesListScreen.Selection.DRAG_AND_DROP && this.hasDraggedAbility() && button == 0) {
            for (int i = 0; i < this.extraAbilitySlots.size(); i++) {
                AbilitySlotButton btn = this.extraAbilitySlots.get(i);
                if (btn.isMouseOver(mouseX, mouseY)) {
                    int slotId = groupSelected * 8 + 80 + i;

                    ModNetwork.sendToServer(new CEquipAbilityPacket(slotId, this.getDraggedAbility()));

                    this.abilityDataProps.setEquippedAbility(slotId, this.getDraggedAbility().createAbility());
                    btn.setAbility(this.abilityDataProps.getEquippedAbility(slotId));

                    this.setDraggedAbility(null);

                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }


    @Unique
    private void handleExtraSlotClick(AbilitySlotButton btn, int buttonIndex) {
        int slotId = buttonIndex + (this.groupSelected * 8) + 80;

        if (this.getSelectionMode() == AbilitiesListScreen.Selection.DRAG_AND_DROP) {
            IAbility ability = this.abilityDataProps.getEquippedAbility(slotId);
            if (ability != null) {
                this.setDraggedAbility(ability.getCore());
            }
            ModNetwork.sendToServer(new CRemoveAbilityPacket(slotId));
            this.abilityDataProps.setEquippedAbility(slotId, null);
            btn.setAbility(null);
        }
        else {
            if (this.slotSelected != slotId) {
                this.slotSelected = slotId;

                for (AbilitySlotButton slotBtn : this.abilitySlots) slotBtn.setIsPressed(false);
                for (AbilitySlotButton extraBtn : this.extraAbilitySlots) extraBtn.setIsPressed(false);

                btn.setIsPressed(true);
            } else {
                IAbility ability = this.abilityDataProps.getEquippedAbility(this.slotSelected);
                if (ability == null) return;

                ModNetwork.sendToServer(new CRemoveAbilityPacket(this.slotSelected));
                this.abilityDataProps.setEquippedAbility(this.slotSelected, null);
                btn.setAbility(null);
            }
        }
    }
}