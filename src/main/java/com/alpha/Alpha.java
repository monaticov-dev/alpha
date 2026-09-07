package com.alpha;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Alpha.MOD_ID, name = "Alpha", version = "1.0.0", useMetadata = true)
public class Alpha {
    public static final String MOD_ID = "alpha";
    private static final Logger LOG = LogManager.getLogger(MOD_ID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("Alpha preInit");
        WalkTest.maybeStart();
    }
}
