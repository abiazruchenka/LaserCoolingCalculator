package ipg.cooling.calc;

public record CoolingResult(
        double innerDiameterM,
        double outerDiameterM,
        double lengthM,
        double volumeFlowM3s,
        double velocityMps,
        double reynolds,
        FlowRegime regime,
        double nusselt,
        double heatTransferCoeffWm2K,
        double waterRiseK,
        double outletTempC,
        double innerWallTempC,
        double outerWallTempC,
        double pressureDropPa,
        double thermalResistanceKw,
        double coolingConductanceWPerK,
        double maxPowerConsumptionW,
        double apparentPowerVa,
        double chillerCapacityW,
        double recommendedFlowM3s,
        int bendCount,
        int uBends,
        double bendRadiusM,
        double localLossK,
        boolean wallLimitOk,
        boolean pressureLimitOk,
        boolean noBoiling,
        String recommendation
) {
}
