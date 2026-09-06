package ipg.cooling.catalog;

public record PlateBody(
        String material,
        double widthMm,
        double heightMm,
        double thicknessMm,
        Double massKg,
        boolean massEstimated,
        boolean thicknessEstimated,
        double conductivityWmk,
        double specificHeatJkgK,
        double edgeMarginMm
) {
    public static final double CAST_AL_DENSITY_KGM3 = 2650.0;

    public double resolvedMassKg() {
        if (massKg != null && massKg > 0) {
            return massKg;
        }
        return widthMm / 1000.0 * heightMm / 1000.0 * thicknessMm / 1000.0 * CAST_AL_DENSITY_KGM3;
    }
}
