package ipg.cooling.calc;

/**
 * Input for the cooling-tube calculation. Null geometry/flow fields mean "choose automatically".
 */
public record CoolingRequest(
        double heatPowerW,
        double inletTempC,
        double maxWallTempC,
        double maxWaterRiseK,
        TubeMaterial material,
        double wallThicknessM,
        Double innerDiameterM,
        Double lengthM,
        Double volumeFlowM3s,
        double maxPressureDropPa
) {
    public static final double TARGET_VELOCITY_MPS = 2.0;
    public static final double MAX_VELOCITY_MPS = 4.0;
    public static final double MIN_VELOCITY_MPS = 0.4;
}
