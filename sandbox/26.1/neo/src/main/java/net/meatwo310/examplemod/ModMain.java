package net.meatwo310.examplemod;

import net.meatwo310.examplemod.config.ModConfigs;
import net.meatwo310.mdk.config.adapter.PlatformConfigRegistrar;
import net.meatwo310.mdk.config.adapter.VersionedConfigSpec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MODID)
public class ModMain {
    public ModMain(IEventBus modEventBus, ModContainer modContainer) {
        Constants.LOGGER.debug(Constants.INITIALIZING, ModUtils.id("26.1-neo"));
        PlatformConfigRegistrar.registerAll(modContainer, VersionedConfigSpec.bindAll(ModConfigs.ALL));
    }
}
