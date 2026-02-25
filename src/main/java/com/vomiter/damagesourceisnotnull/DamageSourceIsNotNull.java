package com.vomiter.damagesourceisnotnull;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
public class DamageSourceIsNotNull {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "damagesourceisnotnull";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
}
