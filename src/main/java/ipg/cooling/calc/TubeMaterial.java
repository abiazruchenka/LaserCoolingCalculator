package ipg.cooling.calc;

public enum TubeMaterial {
    COPPER(398),
    ALUMINUM(237),
    BRASS(110),
    STAINLESS_STEEL(16);

    private final double conductivityWmk;

    TubeMaterial(double conductivityWmk) {
        this.conductivityWmk = conductivityWmk;
    }

    public double conductivityWmk() {
        return conductivityWmk;
    }
}
