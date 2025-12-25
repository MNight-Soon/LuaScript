package com.mnight.luascript.core;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Field;
import java.util.*;

public class ScriptRecipeRegistry {
    public static final ScriptRecipeRegistry INSTANCE = new ScriptRecipeRegistry();

    private final List<Runnable> pendingTasks =  new ArrayList<>();

    // ==========================================
    // 1. Custom Exception: แจ้งเตือนเมื่อ Lua ผิด
    // ==========================================
    public static class LuaRecipeException extends RuntimeException {
        public LuaRecipeException(String message) {
            super(message);
        }
    }

    // ==========================================
    // 2. Record: เก็บข้อมูล Recipe แบบสะอาดๆ
    // ==========================================
    private record ShapedRecipeData(ItemStack output, ShapedRecipePattern pattern){}

    // ==========================================
    // 3. Public API (Lua เรียกใช้)
    // ==========================================
    public void addShaped(String name, LuaTable rawData){
        pendingTasks.add(() -> {
            try {
                ShapedRecipeData data = parseShapedData(rawData);


            }
        });
    }

    // ==========================================
    // 4. Parsing Logic (แยกออกมาให้อ่านง่าย)
    // ==========================================
    private ShapedRecipeData parseShapedData(LuaTable table){
        // --- Parse Output ---
        LuaValue outputVal = table.get("output");
        if (outputVal.isnil()) throw new LuaRecipeException("Missing 'output' field");

        String outputId = outputVal.checkjstring();
        int count = table.get("count").optint(1);

        Item outputItem = getItem(outputId);
        if (outputItem == Items.AIR) throw new LuaRecipeException("Unknown output item: " + outputId);

        ItemStack outputStack = new ItemStack(outputItem, count);

        // --- Parse Key (Legend) ---
        LuaTable keyTable = table.get("key").checktable();
        Map<Character, Ingredient> keyMap = new HashMap<>();

        for (LuaValue k : keyTable.keys()){
            String charKey = k.checkjstring();
            if (charKey.length() != 1) throw new LuaRecipeException("Key must be a single character: " + charKey);

            String itemId = keyTable.get(k).checkjstring();
            Item ingItem = getItem(itemId);
            keyMap.put(charKey.charAt(0), Ingredient.of(ingItem));
        }

        // --- Parse Pattern ---
        LuaTable patternTable = table.get("pattern").checktable();
        int height = patternTable.length();
        if (height == 0) throw new LuaRecipeException("Pattern cannot be empty");

        String firstRow = patternTable.get(1).checkjstring();
        int width = firstRow.length();

        NonNullList<Ingredient> inputs = NonNullList.withSize(height * width, Ingredient.EMPTY);

        for (int r = 0; r < height; r++){
            String row = patternTable.get(r + 1).checkjstring();
            if (row.length() != width) throw new LuaRecipeException("Pattern rows must have same width");
            for (int c = 0; c < width; c++){
                char symbol = row.charAt(c);
                if (symbol != ' '){
                    Ingredient ing = keyMap.get(symbol);
                    if (ing == null) throw new LuaRecipeException("Unknown symbol in pattern: '" + symbol + "'");
                    inputs.set(r + c, ing);
                }
            }
        }
        return new ShapedRecipeData(outputStack, new ShapedRecipePattern(width, height, inputs, Optional.empty()));
    }

    // ==========================================
    // 5. Registration Logic (สร้าง Recipe จริง)
    // ==========================================

    private void registerShapedRecipe(String name, ShapedRecipeData data){
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath( "lua_script",name);

        ShapedRecipe recipe = new ShapedRecipe(
                "", // Group
                CraftingBookCategory.MISC,
                data.pattern(),
                data.output
        );

        injectRecipeIntoManager(id, recipe);
        System.out.println("[LuaScript] Registered shaped recipe " + id);
    }

    // ==========================================
    // 6. Injection Logic (Reflection โหดๆ รวมไว้ที่นี่)
    // ==========================================
    private RecipeManager currentManager;

    public void apply(RecipeManager manager){
        this.currentManager = manager;
        pendingTasks.forEach(Runnable::run);
        pendingTasks.clear();
        this.currentManager = null;
    }

    private void injectRecipeIntoManager(ResourceLocation id, Recipe<?> recipe){
        if (currentManager == null) return;
        try {
            Field recipeField = null;
            try {
                recipeField = RecipeManager.class.getDeclaredField("recipes");
            } catch (NoSuchFieldException e) {
                recipeField = RecipeManager.class.getDeclaredField("byType");
            }

            recipeField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>> recipeMap =
                    (Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>>) recipeField.get(currentManager);
            RecipeHolder<?> holder = new RecipeHolder<>(id, (Recipe) recipe);
            recipeMap.computeIfAbsent(recipe.getType(), t -> new HashMap<>()).put(id, holder);
            } catch (Exception e){
                throw new RuntimeException("Injection Failed: " + e.getMessage(), e);
        }
    }

}
