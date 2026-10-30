package net.meatwo310.mdk.config.ui;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class PlatformConfigScreenRegistrar {
    private PlatformConfigScreenRegistrar() {}

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) ->
                new ConfigurationScreen(mod, parent, KeyedConfigScreen::new));
    }
}
