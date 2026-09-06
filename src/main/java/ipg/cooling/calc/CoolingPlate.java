package ipg.cooling.calc;

/**
 * Cast-aluminium cooling plate with an embedded heat-exchanger tube.
 * Defaults match a typical 445.5 × 547.9 × 20 mm plate, finished mass 10.34 kg
 * (EN AC-AlSi10Mg(Fe), k ≈ 150 W/(m·K)).
 */
public record CoolingPlate(
        double widthM,
        double heightM,
        double thicknessM,
        double massKg,
        double conductivityWmk,
        double specificHeatJkgK,
        double edgeMarginM,
        TubeOrientation orientation,
        int passes,
        SerpentineLayout fixedLayout
) {
    public static final double DEFAULT_CONDUCTIVITY_WMK = 150.0;
    public static final double DEFAULT_SPECIFIC_HEAT = 900.0;

    public static CoolingPlate typicalCastPlate() {
        return new CoolingPlate(
                0.4455, 0.5479, 0.020, 10.34,
                DEFAULT_CONDUCTIVITY_WMK, DEFAULT_SPECIFIC_HEAT, 0.020,
                TubeOrientation.ALONG_WIDTH, 8, null
        );
    }

    public double heatCapacityJPerK() {
        return massKg * specificHeatJkgK;
    }

    public double runLengthM(TubeOrientation run) {
        return run == TubeOrientation.ALONG_HEIGHT ? heightM : widthM;
    }

    public double crossLengthM(TubeOrientation run) {
        return run == TubeOrientation.ALONG_HEIGHT ? widthM : heightM;
    }
}
