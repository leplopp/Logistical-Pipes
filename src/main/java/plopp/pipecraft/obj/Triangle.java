package plopp.pipecraft.obj;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Triangle {
    public final Vec3 a, b, c;

    public Triangle(Vec3 a, Vec3 b, Vec3 c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
    
    
    public AABB getBoundingBox() {
        double minX = Math.min(a.x, Math.min(b.x, c.x));
        double minY = Math.min(a.y, Math.min(b.y, c.y));
        double minZ = Math.min(a.z, Math.min(b.z, c.z));
        double maxX = Math.max(a.x, Math.max(b.x, c.x));
        double maxY = Math.max(a.y, Math.max(b.y, c.y));
        double maxZ = Math.max(a.z, Math.max(b.z, c.z));
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    
    
    public boolean intersectsAABB(AABB box) {

        return box.contains(a) || box.contains(b) || box.contains(c);
    }

  
}