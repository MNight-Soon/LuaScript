package com.mnight.luascript;

import com.mnight.luascript.core.LuaEngineManager;
import com.mnight.luascript.core.ScriptEventRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(LuaScript.MODID)
public class LuaScript {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "luascript";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "luascript" namespace

    public LuaScript(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerClientReloadListeners);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (LuaScript) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LuaEngineManager.INSTANCE.init();
    }
    private void clientSetup(final FMLClientSetupEvent event) {
        LuaEngineManager.INSTANCE.reloadClientScripts();
    }

    // --- RELOAD LISTENERS ---

    // 1. Server Side Reload (/reload)
    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                LuaEngineManager.INSTANCE.reloadServerScripts();
            }
        });
    }

    // 2. Client Side Reload (F3 + T)
    public void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                LuaEngineManager.INSTANCE.reloadClientScripts();
            }
        });
    }
    // --- GAMEPLAY EVENTS (Triggers for Lua) ---
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        ScriptEventRegistry.INSTANCE.fire("block_break", event);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ScriptEventRegistry.INSTANCE.fire("player_join", event);
    }
}
