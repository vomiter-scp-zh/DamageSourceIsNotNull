package com.vomiter.damagesourceisnotnull;
import com.vomiter.damagesourceisnotnull.debug.NullDamageSourceCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DSNNULLFabric  implements ModInitializer{
    public static final String MOD_ID = DamageSourceIsNotNull.MODID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> NullDamageSourceCommand.register(dispatcher));
    }
}
