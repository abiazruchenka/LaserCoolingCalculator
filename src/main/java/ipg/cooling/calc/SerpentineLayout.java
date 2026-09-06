package ipg.cooling.calc;

/**
 * In-plane serpentine that fits a rectangular plate.
 * Centre-to-centre pitch is filled evenly; U-bend radius is {@code max(minR, pitch/2)}.
 */
public record SerpentineLayout(
        TubeOrientation orientation,
        int passes,
        int uBends,
        double pitchM,
        double bendRadiusM,
        double straightLengthM,
        double developedLengthM
) {
    private static final double MIN_GAP_M = 0.002;

    public static SerpentineLayout resolve(CoolingRequest request, double innerDiameterM, double fallbackLengthM) {
        if (request == null || request.plate() == null) {
            return null;
        }
        CoolingPlate plate = request.plate();
        if (plate.fixedLayout() != null) {
            return plate.fixedLayout();
        }
        double outer = innerDiameterM + 2.0 * request.wallThicknessM();
        TubeOrientation orientation = plate.orientation() != null ? plate.orientation() : TubeOrientation.ALONG_WIDTH;
        int passes = plate.passes() > 0 ? plate.passes() : Math.max(1, request.uBendCount() + 1);
        SerpentineLayout layout = tryCreate(plate, orientation, passes, request.bendRadiusM(), outer, 0);
        if (layout != null) {
            return layout;
        }
        if (fallbackLengthM > 0) {
            return new SerpentineLayout(
                    orientation, passes, Math.max(0, passes - 1),
                    0, request.bendRadiusM(), fallbackLengthM, fallbackLengthM);
        }
        return null;
    }

    public static SerpentineLayout tryCreate(
            CoolingPlate plate,
            TubeOrientation orientation,
            int passes,
            double minBendRadiusM,
            double outerDiameterM
    ) {
        return tryCreate(plate, orientation, passes, minBendRadiusM, outerDiameterM, 0);
    }

    public static SerpentineLayout tryCreate(
            CoolingPlate plate,
            TubeOrientation orientation,
            int passes,
            double minBendRadiusM,
            double outerDiameterM,
            double pitchOverrideM
    ) {
        if (plate == null || orientation == null || passes < 1 || minBendRadiusM <= 0 || outerDiameterM <= 0) {
            return null;
        }
        double usableRun = plate.runLengthM(orientation) - 2.0 * plate.edgeMarginM();
        double usableCross = plate.crossLengthM(orientation) - 2.0 * plate.edgeMarginM();
        if (usableRun <= outerDiameterM || usableCross <= outerDiameterM) {
            return null;
        }
        double minPitch = Math.max(2.0 * minBendRadiusM, outerDiameterM + MIN_GAP_M);
        double pitch = passes == 1
                ? 0.0
                : pitchOverrideM > 0 ? pitchOverrideM : usableCross / (passes - 1);
        if (passes > 1 && pitch + 1e-9 < minPitch) {
            return null;
        }
        if (passes > 1 && pitchOverrideM > 0 && pitch * (passes - 1) > usableCross + 1e-9) {
            return null;
        }
        double radius = passes == 1 ? minBendRadiusM : Math.max(minBendRadiusM, pitch / 2.0);
        double straight = passes == 1 ? usableRun : usableRun - 2.0 * radius;
        if (straight <= 0) {
            return null;
        }
        int uBends = Math.max(0, passes - 1);
        double developed = passes * straight + uBends * Math.PI * radius + 2.0 * plate.edgeMarginM();
        return new SerpentineLayout(orientation, passes, uBends, passes == 1 ? 0.0 : pitch, radius, straight, developed);
    }
}
