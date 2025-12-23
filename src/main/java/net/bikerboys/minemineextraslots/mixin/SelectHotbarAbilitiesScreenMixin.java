package net.bikerboys.minemineextraslots.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DisableComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CEquipAbilityPacket;
import xyz.pixelatedw.mineminenomi.packets.client.ability.CRemoveAbilityPacket;
import xyz.pixelatedw.mineminenomi.screens.SelectHotbarAbilitiesScreen;
import xyz.pixelatedw.mineminenomi.screens.SelectHotbarAbilitiesScreen.Selection;
import xyz.pixelatedw.mineminenomi.screens.extra.buttons.AbilitySlotButton;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(value = SelectHotbarAbilitiesScreen.class, remap = false)
public abstract class SelectHotbarAbilitiesScreenMixin extends Screen {

    @Shadow protected PlayerEntity player;
    @Final @Shadow protected List<AbilitySlotButton> abilitySlots;
    @Shadow public int groupSelected;
    @Shadow private IAbilityData abilityDataProps;
    @Shadow public int slotSelected;

    @Shadow public abstract Selection getSelectionMode();
    @Shadow public abstract AbilityCore<?> getDraggedAbility();
    @Shadow public abstract void setDraggedAbility(AbilityCore<?> ability);

    @Shadow public abstract boolean hasDraggedAbility();

    @Unique
    private final List<AbilitySlotButton> extraAbilitySlots = new ArrayList<>();

    protected SelectHotbarAbilitiesScreenMixin(ITextComponent title) {
        super(title);
    }

    @Inject(method = "tick()V", at = @At("TAIL"), remap = true)
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
            this.buttons.removeAll(this.extraAbilitySlots);
            this.children.removeAll(this.extraAbilitySlots);
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
                    this.player,
                    (btn) -> this.handleExtraSlotClick((AbilitySlotButton) btn, buttonIndex)
            );

            this.addButton(slotButton);
            this.extraAbilitySlots.add(slotButton);
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V", at = @At("TAIL"), remap = true)
    public void renderExtraSlots(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        for (AbilitySlotButton btn : this.extraAbilitySlots) {
            btn.render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true, remap = true)
    public void mouseClickedExtraSlots(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 1) {
            for (int i = 0; i < this.extraAbilitySlots.size(); i++) {
                AbilitySlotButton btn = this.extraAbilitySlots.get(i);
                if (btn.isMouseOver(mouseX, mouseY)) {
                    int slotId = groupSelected * 8 + 80 + i;

                    if (this.getSelectionMode() == Selection.KEYBIND && this.slotSelected >= 0) {
                        this.slotSelected = -1;
                        btn.setIsPressed(false);
                    }

                    WyNetwork.sendToServer(new CRemoveAbilityPacket(slotId));
                    this.abilityDataProps.setEquippedAbility(slotId, null);
                    btn.setAbility(null);

                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"), cancellable = true, remap = true)
    public void mouseReleasedExtraSlots(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.getSelectionMode() == Selection.DRAG_AND_DROP && this.hasDraggedAbility() && button == 0) {
            for (int i = 0; i < this.extraAbilitySlots.size(); i++) {
                AbilitySlotButton btn = this.extraAbilitySlots.get(i);
                if (btn.isMouseOver(mouseX, mouseY)) {
                    int slotId = groupSelected * 8 + 80 + i;

                    WyNetwork.sendToServer(new CEquipAbilityPacket(slotId, this.getDraggedAbility()));

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

        if (this.getSelectionMode() == Selection.DRAG_AND_DROP) {
            IAbility ability = this.abilityDataProps.getEquippedAbility(slotId);
            if (ability != null) {

                this.setDraggedAbility(ability.getCore());
            }
            WyNetwork.sendToServer(new CRemoveAbilityPacket(slotId));
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

                if (ability.hasComponent(ModAbilityKeys.COOLDOWN) && ability.getComponent(ModAbilityKeys.COOLDOWN).get().isOnCooldown()) return;
                if (ability.hasComponent(ModAbilityKeys.DISABLE) && ability.getComponent(ModAbilityKeys.DISABLE).get().isDisabled()) return;
                if (ability.hasComponent(ModAbilityKeys.CONTINUOUS) && ability.getComponent(ModAbilityKeys.CONTINUOUS).get().isContinuous()) return;
                if (ability.hasComponent(ModAbilityKeys.CHARGE) && ability.getComponent(ModAbilityKeys.CHARGE).get().isCharging()) return;

                WyNetwork.sendToServer(new CRemoveAbilityPacket(this.slotSelected));
                this.abilityDataProps.setEquippedAbility(this.slotSelected, null);
                btn.setAbility(null);
            }
        }
    }
}