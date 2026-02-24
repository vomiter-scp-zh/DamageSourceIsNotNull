package com.vomiter.damagesourceisnotnull;

import com.mojang.logging.LogUtils;
import com.vomiter.damagesourceisnotnull.debug.NullDamageSourceCommand;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DamageSourceIsNotNull.MODID)
public class DamageSourceIsNotNull {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "damagesourceisnotnull";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public DamageSourceIsNotNull(FMLJavaModLoadingContext context) {
        EVENT_BUS.addListener(NullDamageSourceCommand::register);
        EVENT_BUS.addListener(NullDamageSourceCommand::onLiving);
        context.registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> NetworkConstants.IGNORESERVERONLY,
                        (remoteVersionString, isServer) -> true
                )
        );

    }
}
