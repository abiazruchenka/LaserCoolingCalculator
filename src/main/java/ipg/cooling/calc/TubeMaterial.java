package ipg.cooling.calc;

public enum TubeMaterial {
    COPPER("Cooper", 398),
    ALUMINUM("Aluminium", 237),
    BRASS("Brass", 110),
    STAINLESS_STEEL("Stainless Steel", 16);

    private final String displayName;
    private final double conductivityWmk;

    TubeMaterial(String displayName, double conductivityWmk) {
        this.displayName = displayName;
        this.conductivityWmk = conductivityWmk;
    }

    public String displayName() {
        return displayName;
    }

    public double conductivityWmk() {
        return conductivityWmk;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
