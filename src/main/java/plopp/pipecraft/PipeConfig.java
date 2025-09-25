package plopp.pipecraft;

public final class PipeConfig {
	
    public final int segmentsPerBlock;
    public final double speedBlocksPerTick;
    public final int extractAmount;
    public final double[] defaultSegmentOffsets;

    public PipeConfig(int segmentsPerBlock, double speedBlocksPerTick, int extractAmount, double[] defaultSegmentOffsets) {
        this.segmentsPerBlock = Math.max(1, segmentsPerBlock);
        this.speedBlocksPerTick = Math.max(0.0001, speedBlocksPerTick);
        this.extractAmount = Math.max(1, extractAmount);
        this.defaultSegmentOffsets = defaultSegmentOffsets.clone();
    }

    public static PipeConfig defaultConfig() {
        return new PipeConfig(
            3,         // Segmente pro Block
            0.05,      // Geschwindigkeit Blöcke pro Tick
            1,         // Items pro Extraktion
            new double[] {0.0, 0.5, 1.0} // Segmentpunkte
        );
    }
}


