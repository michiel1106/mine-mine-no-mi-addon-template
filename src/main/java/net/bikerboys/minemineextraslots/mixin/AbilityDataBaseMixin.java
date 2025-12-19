package net.bikerboys.minemineextraslots.mixin;

import org.spongepowered.asm.mixin.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;

@Mixin(AbilityDataBase.class)
public class AbilityDataBaseMixin {
    @Shadow
    private IAbility[] activeAbilities = new IAbility[160];


}
