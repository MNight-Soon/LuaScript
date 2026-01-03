package com.mnight.luascript.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.HashMap;
import java.util.Map;

public class ScriptCommandRegistry {
    public static final ScriptCommandRegistry INSTANCE = new ScriptCommandRegistry();

    private final Map<String, LuaValue> pendingCommands = new HashMap<>();

    public void register(String commandName, LuaValue callback){
        pendingCommands.put(commandName, callback);
        System.out.println("[LuaScript] Registered command '/" +  commandName + "'");
    }

    public void registerAll(CommandDispatcher<CommandSourceStack>  dispatcher){
        for (Map.Entry<String, LuaValue> entry : pendingCommands.entrySet()) {
            String name = entry.getKey();
            LuaValue callback = entry.getValue();

            dispatcher.register(Commands.literal(name)
                    .executes(context -> executeLuaCommand(context, callback))
            );
        }
    }

    public void clear(){
        pendingCommands.clear();
    }

    private int executeLuaCommand(CommandContext<CommandSourceStack> context, LuaValue callback){
        try {
            callback.call(CoerceJavaToLua.coerce(context));
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cLua Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}
