package plopp.pipecraft.obj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class objParser {

    public static List<Triangle> loadTrianglesFromObj(ResourceLocation location) {
        List<Vec3> vertices = new ArrayList<>();
        List<Triangle> triangles = new ArrayList<>();

        try (InputStream in = Minecraft.getInstance().getResourceManager().open(location);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("v ")) {
                    String[] parts = line.split("\\s+");
                    float x = Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    float z = Float.parseFloat(parts[3]);
                    vertices.add(new Vec3(x, y, z));
                } else if (line.startsWith("f ")) {
                    String[] parts = line.split("\\s+");
                    int i1 = Integer.parseInt(parts[1].split("/")[0]) - 1;
                    int i2 = Integer.parseInt(parts[2].split("/")[0]) - 1;
                    int i3 = Integer.parseInt(parts[3].split("/")[0]) - 1;
                    triangles.add(new Triangle(vertices.get(i1), vertices.get(i2), vertices.get(i3)));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return triangles;
    }
}