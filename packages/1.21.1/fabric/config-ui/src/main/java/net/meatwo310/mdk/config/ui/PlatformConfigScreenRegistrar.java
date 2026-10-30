package net.meatwo310.mdk.config.ui;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public final class PlatformConfigScreenRegistrar {
    private PlatformConfigScreenRegistrar() {}

    public static void register(String modId) {
        ConfigScreenFactoryRegistry.INSTANCE.register(modId, (registeredModId, parent) ->
                new ConfigurationScreen(registeredModId, parent, KeyedConfigScreen::new));
    }
}
