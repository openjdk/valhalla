package org.openjdk.bench.valhalla.thesis_benchmarks;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;

@ImplicitlyConstructible
@LooselyConsistentValue
value class Particle {
    private final double x, y, z, vx, vy, vz;

    public Particle(double x, double y, double z, double vx, double vy, double vz) {
        this.x = x; this.y = y; this.z = z;
        this.vx = vx; this.vy = vy; this.vz = vz;
    }

    public Particle update() {
        return new Particle(x + vx, y + vy, z + vz, vx, vy, vz);
    }
}