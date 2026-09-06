package ipg.cooling.calc;

public record CoolingResult(
        double innerDiameterM,
        double lengthM,
        double volumeFlowM3s,
        double velocityMps,
        double reynolds,
        FlowRegime regime,
        double heatTransferCoeffWm2K,
        double waterRiseK,
        double outletTempC,
        double outerWallTempC,
        double pressureDropPa,
        double inletPressurePa,
        double maxPowerConsumptionW,
        double apparentPowerVa,
        double chillerCapacityW,
        double recommendedFlowM3s,
        int uBends,
        double localLossK,
        double plateTempC,
        double timeConstantS
) {
}
