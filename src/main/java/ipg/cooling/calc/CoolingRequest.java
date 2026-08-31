package ipg.cooling.calc;

/**
 * Laser plus heat-sink tube inputs. Null geometry/flow fields mean "choose automatically".
 *
 * <p>Heat load is {@code P_laser/η − P_laser}. Recommended flow is 5 L/min per kW of laser power.
 */
public record CoolingRequest(
        double laserPowerW,
        double efficiency,
        double powerFactor,
        double inletTempC,
        double maxWallTempC,
        double maxWaterRiseK,
        TubeMaterial material,
        double wallThicknessM,
        Double innerDiameterM,
        Double lengthM,
        Double volumeFlowM3s,
        double maxPressureDropPa,
        int bendCount,
        double bendRadiusM
) {
    public static final double TARGET_VELOCITY_MPS = 2.0;
    public static final double MAX_VELOCITY_MPS = 6.0;
    public static final double MIN_VELOCITY_MPS = 0.4;
    /** Spec rule of thumb: 5 L/min per kW of laser output. */
    public static final double FLOW_LMIN_PER_KW = 5.0;

    public double maxPowerConsumptionW() {
        return laserPowerW / efficiency;
    }

    public double apparentPowerVa() {
        return maxPowerConsumptionW() / powerFactor;
    }

    public double heatLoadW() {
        return maxPowerConsumptionW() - laserPowerW;
    }

    public double recommendedFlowM3s() {
        return laserPowerW / 1000.0 * FLOW_LMIN_PER_KW / 60_000.0;
    }

    public int uBendCount() {
        return Math.max(0, bendCount);
    }
}
