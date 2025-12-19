package net.bikerboys.minemineextraslots.mixin;

import org.spongepowered.asm.mixin.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;

@Mixin(value = AbilityDataBase.class, remap = false)
public class AbilityDataBaseMixin {
    @Shadow
    private IAbility[] activeAbilities = new IAbility[160];


}
