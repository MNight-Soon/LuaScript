package com.mnight.luascript.core;

import net.neoforged.fml.loading.FMLPaths;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class LuaEngineManager {
    public static final LuaEngineManager INSTANCE = new LuaEngineManager();

    private Globals globals;
    private final Path rootPath;

    private LuaEngineManager() {
        // Points to config/lua_scripts
        this.rootPath = FMLPaths.CONFIGDIR.get().resolve("lua_script");
    }

    public void init(){
        // Initialize Lua standard library
        this.globals = JsePlatform.standardGlobals();

        // Bind the Event Registry to Lua as a global variable "_REGISTRY"
        globals.set("_REGISTRY", CoerceJavaToLua.coerce(this));

        // Create folders
        ensureDirectory("server");
        ensureDirectory("client");

        // Auto-generate api.lua if missing
        createApiFile();
    }

    /**
     * Reloads all scripts in the 'server' folder.
     */

    public void reloadServerScripts(){
        System.out.println("[LuaScript] Reloading SERVER scripts");
        // Important: Clear old listeners!

        // Load API wrapper first
        loadApiFile();

        // Load user scripts
        loadScriptsFromFolder("server");

    }

    /**
     * Reloads all scripts in the 'client' folder.
     */
    public void reloadClientScripts(){
        System.out.println("[LuaScript] Reloading CLIENT scripts");
        // Note: For client visuals, we might not need to clear event registry
        // if we separate client/server registries later, but for now this is fine.
        loadScriptsFromFolder("client");
    }

    private void loadScriptsFromFolder(String subFolder){
        File folder = rootPath.resolve(subFolder).toFile();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".lua"));

        if (files == null) return;

        for (File file : files) {
            try {
                LuaValue chunk = globals.loadfile(file.getAbsolutePath());
                chunk.call();
                System.out.println("[LuaScript] Loaded: " + subFolder + "/" + file.getName());
            } catch (Exception e) {
                System.err.println("[LuaScript] Failed to load: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    private void createApiFile(){
        File apiFile = rootPath.resolve("api.json").toFile();
        if(!apiFile.exists()){
            try {
                String content =
                        "-- LuaMod API Wrapper (Auto-generated)\n" +
                                "events = {}\n\n" +
                                "function events.listen(eventName, callback)\n" +
                                "    _REGISTRY:register(eventName, callback)\n" +
                                "end\n\n" +
                                "-- Shortcuts\n" +
                                "function events.onBlockBreak(callback) events.listen('block_break', callback) end\n" +
                                "function events.onPlayerJoin(callback) events.listen('player_join', callback) end\n";
                Files.write(apiFile.toPath(), content.getBytes());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void loadApiFile(){
        File apiFile = rootPath.resolve("api.json").toFile();
        if (apiFile.exists()){
            try {
                globals.loadfile(apiFile.getAbsolutePath()).call();
            } catch (Exception e) {
                System.err.println("[LuaScript] Failed to load api.lua");
            }
        }
    }

    private void ensureDirectory(String subFolder){
        File dir = rootPath.resolve(subFolder).toFile();
        if(!dir.exists()) dir.mkdirs();
    }


}