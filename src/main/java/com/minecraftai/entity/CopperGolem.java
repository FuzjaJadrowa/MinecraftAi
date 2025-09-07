package com.minecraftai.entity;

import com.google.gson.*;

import java.io.InputStream;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

public class CopperGolem {

    private static class Face {
        float[] uv;
        String texture;
    }

    private static class Element {
        String name;
        float[] from;
        float[] to;
        Map<String, Face> faces;
    }

    private List<Element> elements = new ArrayList<>();
    private float x, y, z;

    public CopperGolem(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        loadModel("assets/models/copper_golem.json");
    }

    private void loadModel(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Model not found: " + path);
            JsonObject root = JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("elements");

            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                Element el = new Element();
                el.name = obj.get("name").getAsString();
                el.from = toFloatArray(obj.getAsJsonArray("from"));
                el.to = toFloatArray(obj.getAsJsonArray("to"));
                el.faces = new HashMap<>();

                JsonObject faces = obj.getAsJsonObject("faces");
                for (String dir : faces.keySet()) {
                    JsonObject f = faces.getAsJsonObject(dir);
                    Face face = new Face();
                    face.uv = toFloatArray(f.getAsJsonArray("uv"));
                    face.texture = f.get("texture").getAsString();
                    el.faces.put(dir, face);
                }
                elements.add(el);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float[] toFloatArray(JsonArray arr) {
        float[] out = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            out[i] = arr.get(i).getAsFloat();
        }
        return out;
    }

    public void render() {
        glPushMatrix();
        glTranslatef(x, y, z);

        for (Element el : elements) {
            switch (el.name) {
                case "head":
                    glColor3f(1f, 0f, 0f);
                    break;
                case "body":
                    glColor3f(0.8f, 0.4f, 0.1f);
                    break;
                case "left_arm":
                case "right_arm":
                case "left_leg":
                case "right_leg":
                    glColor3f(0f, 0f, 0f);
                    break;
            }
            drawElement(el);
        }

        glColor3f(1f, 1f, 1f);
        glPopMatrix();
    }

    private void drawElement(Element el) {
        float x1 = el.from[0] / 16f;
        float y1 = el.from[1] / 16f;
        float z1 = el.from[2] / 16f;
        float x2 = el.to[0] / 16f;
        float y2 = el.to[1] / 16f;
        float z2 = el.to[2] / 16f;

        glBegin(GL_QUADS);

        if (el.faces.containsKey("north")) {
            glVertex3f(x1, y1, z1);
            glVertex3f(x2, y1, z1);
            glVertex3f(x2, y2, z1);
            glVertex3f(x1, y2, z1);
        }

        if (el.faces.containsKey("east")) {
            glVertex3f(x2, y1, z1);
            glVertex3f(x2, y1, z2);
            glVertex3f(x2, y2, z2);
            glVertex3f(x2, y2, z1);
        }

        if (el.faces.containsKey("south")) {
            glVertex3f(x2, y1, z2);
            glVertex3f(x1, y1, z2);
            glVertex3f(x1, y2, z2);
            glVertex3f(x2, y2, z2);
        }

        if (el.faces.containsKey("west")) {
            glVertex3f(x1, y1, z2);
            glVertex3f(x1, y1, z1);
            glVertex3f(x1, y2, z1);
            glVertex3f(x1, y2, z2);
        }

        if (el.faces.containsKey("up")) {
            glVertex3f(x1, y2, z1);
            glVertex3f(x2, y2, z1);
            glVertex3f(x2, y2, z2);
            glVertex3f(x1, y2, z2);
        }

        if (el.faces.containsKey("down")) {
            glVertex3f(x1, y1, z2);
            glVertex3f(x2, y1, z2);
            glVertex3f(x2, y1, z1);
            glVertex3f(x1, y1, z1);
        }

        glEnd();
    }
}