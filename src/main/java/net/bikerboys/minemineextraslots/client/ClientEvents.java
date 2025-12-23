package net.bikerboys.minemineextraslots.client;

import static net.bikerboys.minemineextraslots.MineMineExtraSlots.MAX_SLOT_CAP;
import static net.bikerboys.minemineextraslots.MineMineExtraSlots.MODID;
import net.minecraft.client.settings.*;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.client.registry.*;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.event.lifecycle.*;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    public static final KeyBinding[] EXTRA_SLOT_KEYS = new KeyBinding[MAX_SLOT_CAP];

    @SubscribeEvent
    public static void clientSetup(final FMLClientSetupEvent event) {
        for (int i = 0; i < MAX_SLOT_CAP; i++) {
            EXTRA_SLOT_KEYS[i] = new KeyBinding("key.minemineextraslots.slot_" + (i + 1), -1, "key.categories.mineminenomi");
            ClientRegistry.registerKeyBinding(EXTRA_SLOT_KEYS[i]);
        }
    }

}
