package net.meatwo310.m2mdk;

import org.gradle.api.provider.Property;

/** Metadata shared by the mod's resource templates. */
public abstract class ModExtension {
    public abstract Property<String> getModId();
    public abstract Property<String> getModName();
    public abstract Property<String> getLicense();
}
