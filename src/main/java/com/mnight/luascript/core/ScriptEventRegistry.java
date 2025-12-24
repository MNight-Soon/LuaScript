package com.mnight.luascript.core;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptEventRegistry {
    public static final ScriptEventRegistry INSTANCE = new ScriptEventRegistry();

    // Map storing event names and their list of Lua callback functions
    private final Map<String, List<LuaValue>> listeners = new HashMap<>();

    /**
     * Registers a Lua function to listen for a specific event.
     */
    public void register(String eventName, LuaValue callback){
        listeners.computeIfAbsent(eventName, k -> new ArrayList<>()).add(callback);
    }

    /**
     * Clears all listeners. Used during reload to prevent duplicates.
     */
    public void clear(){
        listeners.clear();
    }

    /**
     * Fires an event from Java to all registered Lua callbacks.
     */
    public void fire(String eventName, Object eventData){
        List<LuaValue> callbacks = listeners.get(eventName);
        if (callbacks != null && !callbacks.isEmpty()){
            LuaValue luaData = CoerceJavaToLua.coerce(eventData);
            for (LuaValue callback : callbacks){
                try {
                    callback.call(luaData);
                } catch (Exception e){
                    System.err.println("[LuaScript] Error in event handler for " + eventName);
                    e.printStackTrace();
                }
            }

        }
    }
}
