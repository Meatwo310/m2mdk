package net.meatwo310.examplemod.client;

import net.fabricmc.api.ClientModInitializer;
import net.meatwo310.examplemod.Constants;
import net.meatwo310.mdk.config.ui.PlatformConfigScreenRegistrar;

public class ModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PlatformConfigScreenRegistrar.register(Constants.MODID);
    }
}
