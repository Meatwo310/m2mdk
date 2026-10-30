package net.meatwo310.examplemod.client;

import net.meatwo310.examplemod.Constants;
import net.meatwo310.mdk.config.ui.PlatformConfigScreenRegistrar;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = Constants.MODID, dist = Dist.CLIENT)
public class ModClient {
    public ModClient(ModContainer container) {
        PlatformConfigScreenRegistrar.register(container);
    }
}
