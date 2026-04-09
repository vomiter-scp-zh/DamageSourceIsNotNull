package com.vomiter.damagesourceisnotnull;

import com.mojang.logging.LogUtils;
import com.vomiter.damagesourceisnotnull.debug.NullDamageSourceCommand;
import com.vomiter.damagesourceisnotnull.debug.NullDamageTestAll;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;


// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DamageSourceIsNotNull.MODID)
public class DamageSourceIsNotNull {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "dsnnullbasic";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public DamageSourceIsNotNull(ModContainer mod, IEventBus modBus) {
        EVENT_BUS.addListener(NullDamageSourceCommand::register);
        EVENT_BUS.addListener(NullDamageSourceCommand::onLiving);
        EVENT_BUS.addListener(NullDamageTestAll::onServerTick);
    }
}
