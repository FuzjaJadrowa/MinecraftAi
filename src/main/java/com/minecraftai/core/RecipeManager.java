package com.minecraftai.core;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeManager {
    public static class Recipe {
        public String type;
        public List<String> pattern;
        public Map<String, RecipeKey> key;
        public RecipeResult result;
    }

    public static class RecipeKey {
        public String item;
    }

    public static class RecipeResult {
        public String item;
        public int count;
    }

    private static final List<Recipe> recipes = new ArrayList<>();

    public static void loadRecipes() {
        recipes.clear();
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            writeDefaultRecipes(dataDir);
            files = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
        }
        
        if (files != null) {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            for (File f : files) {
                try {
                    String content = Files.readString(f.toPath());
                    Recipe recipe = gson.fromJson(content, Recipe.class);
                    if (recipe != null) {
                        recipes.add(recipe);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load recipe: " + f.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    private static void writeDefaultRecipes(File dataDir) {
        try {
            // 1. Planks
            Files.writeString(new File(dataDir, "planks.json").toPath(), 
                "{\n" +
                "  \"type\": \"minecraft:crafting_shaped\",\n" +
                "  \"pattern\": [\n" +
                "    \"#\"\n" +
                "  ],\n" +
                "  \"key\": {\n" +
                "    \"#\": {\n" +
                "      \"item\": \"minecraft:log\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"result\": {\n" +
                "    \"item\": \"minecraft:planks\",\n" +
                "    \"count\": 4\n" +
                "  }\n" +
                "}");

            // 2. Crafting Table
            Files.writeString(new File(dataDir, "crafting_table.json").toPath(), 
                "{\n" +
                "  \"type\": \"minecraft:crafting_shaped\",\n" +
                "  \"pattern\": [\n" +
                "    \"##\",\n" +
                "    \"##\"\n" +
                "  ],\n" +
                "  \"key\": {\n" +
                "    \"#\": {\n" +
                "      \"item\": \"minecraft:planks\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"result\": {\n" +
                "    \"item\": \"minecraft:crafting_table\",\n" +
                "    \"count\": 1\n" +
                "  }\n" +
                "}");

            // 3. Stick
            Files.writeString(new File(dataDir, "stick.json").toPath(), 
                "{\n" +
                "  \"type\": \"minecraft:crafting_shaped\",\n" +
                "  \"pattern\": [\n" +
                "    \"#\",\n" +
                "    \"#\"\n" +
                "  ],\n" +
                "  \"key\": {\n" +
                "    \"#\": {\n" +
                "      \"item\": \"minecraft:planks\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"result\": {\n" +
                "    \"item\": \"minecraft:stick\",\n" +
                "    \"count\": 4\n" +
                "  }\n" +
                "}");

            // 4. Furnace
            Files.writeString(new File(dataDir, "furnace.json").toPath(), 
                "{\n" +
                "  \"type\": \"minecraft:crafting_shaped\",\n" +
                "  \"pattern\": [\n" +
                "    \"###\",\n" +
                "    \"# #\",\n" +
                "    \"###\"\n" +
                "  ],\n" +
                "  \"key\": {\n" +
                "    \"#\": {\n" +
                "      \"item\": \"minecraft:cobblestone\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"result\": {\n" +
                "    \"item\": \"minecraft:furnace\",\n" +
                "    \"count\": 1\n" +
                "  }\n" +
                "}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ItemType parseItemType(String name) {
        if (name == null) return null;
        if (name.startsWith("minecraft:")) {
            name = name.substring("minecraft:".length());
        }
        name = name.toUpperCase();
        try {
            return ItemType.valueOf(name);
        } catch (IllegalArgumentException e) {
            if (name.equals("OAK_LOG")) return ItemType.LOG;
            if (name.equals("OAK_PLANKS")) return ItemType.PLANKS;
            return null;
        }
    }

    public static ItemStack findMatchingRecipe(ItemStack[] grid, int gridW, int gridH) {
        int minCol = gridW, maxCol = -1, minRow = gridH, maxRow = -1;
        int itemCount = 0;
        for (int r = 0; r < gridH; r++) {
            for (int c = 0; c < gridW; c++) {
                ItemStack stack = grid[r * gridW + c];
                if (stack != null && stack.getCount() > 0) {
                    itemCount++;
                    if (c < minCol) minCol = c;
                    if (c > maxCol) maxCol = c;
                    if (r < minRow) minRow = r;
                    if (r > maxRow) maxRow = r;
                }
            }
        }

        if (itemCount == 0) return null;

        int subW = maxCol - minCol + 1;
        int subH = maxRow - minRow + 1;

        ItemStack[] subgrid = new ItemStack[subW * subH];
        for (int r = 0; r < subH; r++) {
            for (int c = 0; c < subW; c++) {
                subgrid[r * subW + c] = grid[(minRow + r) * gridW + (minCol + c)];
            }
        }

        for (Recipe r : recipes) {
            if (matchesShaped(r, subgrid, subW, subH, gridW, gridH)) {
                ItemType resultType = parseItemType(r.result.item);
                if (resultType != null) {
                    return new ItemStack(resultType, r.result.count);
                }
            }
        }

        return null;
    }

    private static boolean matchesShaped(Recipe recipe, ItemStack[] subgrid, int subW, int subH, int maxGridW, int maxGridH) {
        if (recipe.pattern == null || recipe.pattern.isEmpty()) return false;

        int patH = recipe.pattern.size();
        int patW = recipe.pattern.get(0).length();

        if (patW != subW || patH != subH) return false;
        if (patW > maxGridW || patH > maxGridH) return false;

        for (int r = 0; r < patH; r++) {
            String row = recipe.pattern.get(r);
            for (int c = 0; c < patW; c++) {
                char ch = row.charAt(c);
                ItemStack stack = subgrid[r * subW + c];
                if (ch == ' ') {
                    if (stack != null && stack.getCount() > 0) return false;
                } else {
                    if (stack == null || stack.getCount() <= 0) return false;
                    RecipeKey keyVal = recipe.key.get(String.valueOf(ch));
                    if (keyVal == null) return false;
                    ItemType requiredType = parseItemType(keyVal.item);
                    if (stack.getType() != requiredType) return false;
                }
            }
        }
        return true;
    }
}