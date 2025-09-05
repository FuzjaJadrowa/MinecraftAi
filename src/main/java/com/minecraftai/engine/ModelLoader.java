package com.minecraftai.engine;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class ModelLoader {

    public static class Model {
        public List<Cube> cubes = new ArrayList<>();
        public int textureId;
    }

    public static class Cube {
        public float x, y, z;
        public float width, height, depth;
        public Face north, south, east, west, up, down;
    }

    public static class Face {
        public float u0, v0, u1, v1;
    }

    public static Model loadModel(String jsonPath, String texturePath) {
        Model model = new Model();
        try {
            InputStream jsonStream = ModelLoader.class.getClassLoader().getResourceAsStream(jsonPath);
            if (jsonStream == null) throw new RuntimeException("Nie znaleziono pliku: " + jsonPath);
            JsonObject root = new Gson().fromJson(new InputStreamReader(jsonStream), JsonObject.class);

            InputStream texStream = ModelLoader.class.getClassLoader().getResourceAsStream(texturePath);
            if (texStream == null) throw new RuntimeException("Nie znaleziono tekstury: " + texturePath);
            BufferedImage image = ImageIO.read(texStream);
            model.textureId = loadTexture(image);
            int texWidth = image.getWidth();
            int texHeight = image.getHeight();

            JsonArray elements = root.getAsJsonArray("elements");
            for (int i = 0; i < elements.size(); i++) {
                JsonObject e = elements.get(i).getAsJsonObject();
                Cube c = new Cube();
                JsonArray from = e.getAsJsonArray("from");
                JsonArray to = e.getAsJsonArray("to");

                c.x = from.get(0).getAsFloat();
                c.y = from.get(1).getAsFloat();
                c.z = from.get(2).getAsFloat();

                c.width = to.get(0).getAsFloat() - c.x;
                c.height = to.get(1).getAsFloat() - c.y;
                c.depth = to.get(2).getAsFloat() - c.z;

                JsonObject faces = e.getAsJsonObject("faces");
                c.north = parseFace(faces.getAsJsonObject("north"), texWidth, texHeight);
                c.south = parseFace(faces.getAsJsonObject("south"), texWidth, texHeight);
                c.east  = parseFace(faces.getAsJsonObject("east"), texWidth, texHeight);
                c.west  = parseFace(faces.getAsJsonObject("west"), texWidth, texHeight);
                c.up    = parseFace(faces.getAsJsonObject("up"), texWidth, texHeight);
                c.down  = parseFace(faces.getAsJsonObject("down"), texWidth, texHeight);

                model.cubes.add(c);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return model;
    }

    private static Face parseFace(JsonObject faceObj, int texWidth, int texHeight) {
        Face f = new Face();
        JsonArray uv = faceObj.getAsJsonArray("uv");
        f.u0 = uv.get(0).getAsFloat() / texWidth;
        f.v0 = uv.get(1).getAsFloat() / texHeight;
        f.u1 = uv.get(2).getAsFloat() / texWidth;
        f.v1 = uv.get(3).getAsFloat() / texHeight;
        return f;
    }

    private static int loadTexture(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixelsRaw = new int[width * height];
        image.getRGB(0, 0, width, height, pixelsRaw, 0, width);

        ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixelsRaw[y * width + x];
                pixels.put((byte) ((pixel >> 16) & 0xFF));
                pixels.put((byte) ((pixel >> 8) & 0xFF));
                pixels.put((byte) (pixel & 0xFF));
                pixels.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        pixels.flip();

        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        return texId;
    }
}