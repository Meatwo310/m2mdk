package net.meatwo310.examplemod;

import net.fabricmc.api.ModInitializer;
import net.meatwo310.examplemod.config.ModConfigs;
import net.meatwo310.mdk.config.adapter.PlatformConfigRegistrar;
import net.meatwo310.mdk.config.adapter.VersionedConfigSpec;

public class ModMain implements ModInitializer {
    @Override
    public void onInitialize() {
        Constants.LOGGER.debug(Constants.INITIALIZING, ModUtils.loc("1.18.2-fabric"));
        PlatformConfigRegistrar.registerAll(Constants.MODID, VersionedConfigSpec.bindAll(ModConfigs.ALL));
    }
}
