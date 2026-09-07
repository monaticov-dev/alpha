package com.alpha;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Пробный забег: новый мир, бег вперёд с прыжком 15 секунд, выход.
 * Запуск: runClient25 --mcJvmArgs="-Dalpha.walk=1". Без флага неактивен.
 */
public class WalkTest {
    private static final Logger LOG = LogManager.getLogger("alpha-walk");
    private int phase;
    private int phaseTick;
    private double x0;
    private double z0;

    public static void maybeStart() {
        if (System.getProperty("alpha.walk") != null) {
            cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new WalkTest());
            LOG.info("[walk] armed");
        }
    }

    private void hold(boolean down) {
        KeyBinding.setKeyBindState(17, down); // W
        KeyBinding.setKeyBindState(57, down); // SPACE
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        phaseTick++;
        switch (phase) {
        case 0: // ждём главное меню, создаём новый мир
            if (mc.currentScreen instanceof GuiMainMenu && mc.theWorld == null) {
                mc.gameSettings.pauseOnLostFocus = false; // без автопаузы заранее
                String folder = "walk-" + System.currentTimeMillis();
                WorldSettings settings = new WorldSettings(System.currentTimeMillis(),
                        WorldSettings.GameType.SURVIVAL, true, false, WorldType.DEFAULT);
                mc.launchIntegratedServer(folder, folder, settings);
                LOG.info("[walk] create {}", folder);
                phase++;
                phaseTick = 0;
            } else if (phaseTick > 1200) {
                LOG.info("[walk] FAIL no-menu");
                mc.shutdown();
            }
            break;
        case 1: // ждём появления игрока, устаканиваем спавн, бежим
            if (mc.theWorld != null && mc.thePlayer != null && phaseTick > 40) {
                if (mc.currentScreen != null) {
                    mc.displayGuiScreen(null);
                }
                x0 = mc.thePlayer.posX;
                z0 = mc.thePlayer.posZ;
                hold(true);
                LOG.info("[walk] run from {} {}", x0, z0);
                phase++;
                phaseTick = 0;
            } else if (phaseTick > 3600) {
                LOG.info("[walk] FAIL no-world");
                mc.shutdown();
            }
            break;
        case 2: // 15 секунд бега (300 тиков), сравниваем координаты
            if (mc.currentScreen != null) {
                mc.displayGuiScreen(null);
            }
            hold(true);
            if (phaseTick > 300) {
                hold(false);
                double moved = Math.hypot(mc.thePlayer.posX - x0, mc.thePlayer.posZ - z0);
                LOG.info("[walk] {} moved={}", moved > 1.0 ? "PASS" : "FAIL",
                        String.format("%.2f", moved));
                mc.displayGuiScreen(new GuiIngameMenu()); // ESC
                phase++;
                phaseTick = 0;
            }
            break;
        case 3: // выход из мира в меню
            if (phaseTick > 20) {
                mc.theWorld.sendQuittingDisconnectingPacket();
                mc.loadWorld(null);
                mc.displayGuiScreen(new GuiMainMenu());
                phase++;
                phaseTick = 0;
            }
            break;
        default: // в меню — выйти из игры
            if (mc.currentScreen instanceof GuiMainMenu && mc.theWorld == null && phaseTick > 40) {
                LOG.info("[walk] quit");
                mc.shutdown();
            } else if (phaseTick > 1200) {
                mc.shutdown();
            }
            break;
        }
    }
}
