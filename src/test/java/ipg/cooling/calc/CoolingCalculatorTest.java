package ipg.cooling.calc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoolingCalculatorTest {

    private final CoolingCalculator calculator = new CoolingCalculator();

    @Test
    void waterPropertiesAt20CMatchTables() {
        WaterProperties water = WaterProperties.atCelsius(20);
        assertEquals(998.2, water.densityKgM3(), 0.1);
        assertEquals(1.002e-3, water.viscosityPaS(), 1e-6);
        assertEquals(0.598, water.conductivityWmk(), 0.002);
        assertEquals(4182, water.specificHeatJkgK(), 2);
    }

    @Test
    void temperatureRiseMatchesEnergyBalance() {
        CoolingRequest request = sampleRequest(0.008, 0.4, 1.5e-5);
        CoolingResult result = calculator.evaluate(request, 0.008, 0.4, 1.5e-5);
        WaterProperties water = WaterProperties.atCelsius(request.inletTempC() + result.waterRiseK() / 2.0);
        double expectedRise = request.heatLoadW()
                / (water.densityKgM3() * 1.5e-5 * water.specificHeatJkgK());
        assertEquals(expectedRise, result.waterRiseK(), 0.05);
    }

    @Test
    void higherFlowLowersWaterTemperatureRise() {
        CoolingRequest request = sampleRequest(0.008, 0.4, null);
        CoolingResult slow = calculator.evaluate(request, 0.008, 0.4, 1.0e-5);
        CoolingResult fast = calculator.evaluate(request, 0.008, 0.4, 2.0e-5);
        assertTrue(fast.waterRiseK() < slow.waterRiseK());
        assertTrue(fast.heatTransferCoeffWm2K() > slow.heatTransferCoeffWm2K());
    }

    @Test
    void smallerDiameterRaisesPressureDropAtFixedFlow() {
        CoolingRequest request = sampleRequest(0.006, 0.4, 1.5e-5);
        CoolingResult narrow = calculator.evaluate(request, 0.006, 0.4, 1.5e-5);
        CoolingResult wide = calculator.evaluate(request, 0.012, 0.4, 1.5e-5);
        assertTrue(narrow.pressureDropPa() > wide.pressureDropPa());
    }

    @Test
    void gnielinskiNusseltIsInExpectedRange() {
        double nu = CoolingCalculator.gnielinski(10_000, 7.0);
        assertEquals(80.0, nu, 8.0);
    }

    @Test
    void autoDesignFor500WStaysWithinLimits() {
        CoolingRequest request = new CoolingRequest(
                500, 0.5, 0.93, 20, 45, 8, TubeMaterial.COPPER, 0.001,
                null, null, null, 2e5, 0, 0.04
        );
        CoolingResult result = calculator.calculate(request);
        assertTrue(result.wallLimitOk(), result.recommendation());
        assertTrue(result.pressureLimitOk(), result.recommendation());
        assertTrue(result.noBoiling(), result.recommendation());
        assertTrue(result.innerDiameterM() > 0);
        assertTrue(result.lengthM() > 0);
        assertTrue(result.volumeFlowM3s() > 0);
    }

    @Test
    void rejectsNonPositiveHeatLoad() {
        CoolingRequest request = new CoolingRequest(
                0, 0.5, 0.93, 20, 45, 8, TubeMaterial.COPPER, 0.001,
                0.008, 0.4, 1.5e-5, 2e5, 0, 0.04
        );
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(request));
    }

    @Test
    void optimizerWithOneIterationReturnsStartPoint() {
        CoolingRequest request = sampleRequest(0.008, 0.4, 1.5e-5);
        OptimizerSettings settings = new OptimizerSettings(
                0.008, 0.012, false,
                0.4, 0.6, false,
                1.5e-5, 3e-5, false,
                8
        );
        OptimizerOutcome outcome = calculator.optimize(request, settings);
        assertEquals(1, outcome.evaluated());
        assertEquals(0.008, outcome.best().innerDiameterM(), 1e-9);
        assertEquals(0.4, outcome.best().lengthM(), 1e-9);
        assertEquals(1.5e-5, outcome.best().volumeFlowM3s(), 1e-12);
    }

    @Test
    void optimizerGridSizeIsProductOfIterationCounts() {
        CoolingRequest request = sampleRequest(0.006, 0.3, 1.2e-5);
        OptimizerSettings settings = new OptimizerSettings(
                0.006, 0.009, true,
                0.3, 0.45, true,
                1.2e-5, 1.8e-5, true,
                4
        );
        OptimizerOutcome outcome = calculator.optimize(request, settings);
        assertEquals(64, outcome.evaluated());
        assertTrue(outcome.feasible() >= 0);
        assertTrue(outcome.best().innerDiameterM() >= 0.006);
        assertTrue(outcome.best().lengthM() >= 0.3);
    }

    @Test
    void moreIterationsCanFindLowerOrEqualWallTemperature() {
        CoolingRequest request = new CoolingRequest(
                500, 0.5, 0.93, 20, 45, 8, TubeMaterial.COPPER, 0.001,
                null, null, null, 2e5, 0, 0.04
        );
        OptimizerSettings coarse = new OptimizerSettings(
                0.004, 0.010, true,
                0.2, 0.5, true,
                8e-6, 2e-5, true,
                2
        );
        OptimizerSettings fine = new OptimizerSettings(
                0.004, 0.010, true,
                0.2, 0.5, true,
                8e-6, 2e-5, true,
                4
        );
        OptimizerOutcome coarseOutcome = calculator.optimize(request, coarse);
        OptimizerOutcome fineOutcome = calculator.optimize(request, fine);
        assertEquals(8, coarseOutcome.evaluated());
        assertEquals(64, fineOutcome.evaluated());
        assertTrue(fineOutcome.feasible() >= coarseOutcome.feasible());
    }

    @Test
    void optimizerSelectsHighestCoolingEfficiencyAmongFeasible() {
        CoolingRequest request = sampleRequest(0.008, 0.4, 1.0e-5);
        CoolingResult lowFlow = calculator.evaluate(request, 0.008, 0.4, 1.0e-5);
        CoolingResult highFlow = calculator.evaluate(request, 0.008, 0.4, 2.0e-5);
        assertTrue(highFlow.coolingConductanceWPerK() > lowFlow.coolingConductanceWPerK());

        OptimizerSettings settings = new OptimizerSettings(
                0.008, 0.008, false,
                0.4, 0.4, false,
                1.0e-5, 2.0e-5, true,
                3
        );
        CoolingResult best = calculator.optimize(request, settings).best();
        assertEquals(2.0e-5, best.volumeFlowM3s(), 1e-12);
        assertEquals(highFlow.coolingConductanceWPerK(), best.coolingConductanceWPerK(), 1e-6);
    }

    @Test
    void optimizerDoesNotClampLengthToFiveMeters() {
        CoolingRequest request = sampleRequest(0.016, 12.0, 8.3e-5);
        OptimizerSettings settings = new OptimizerSettings(
                0.016, 0.016, false,
                12.0, 16.0, true,
                8.3e-5, 8.3e-5, false,
                5
        );
        OptimizerOutcome outcome = calculator.optimize(request, settings);
        assertEquals(5, outcome.evaluated());
        assertTrue(outcome.best().lengthM() >= 12.0 - 1e-9, outcome.best().recommendation());
        assertTrue(outcome.best().lengthM() <= 16.0 + 1e-9);
        assertTrue(outcome.best().coolingConductanceWPerK() > 0);
    }

    @Test
    void serpentineUBendsRaisePressureDropAtFixedFlow() {
        CoolingRequest straight = sampleRequest(0.0109, 3.6, 8.3e-5);
        CoolingRequest plate = new CoolingRequest(
                500, 0.5, 0.93, 20, 45, 8, TubeMaterial.STAINLESS_STEEL, 0.0009,
                0.0109, 3.6, 8.3e-5, 2e5, 7, 0.028
        );
        CoolingResult straightResult = calculator.evaluate(straight, 0.0109, 3.6, 8.3e-5);
        CoolingResult plateResult = calculator.evaluate(plate, 0.0109, 3.6, 8.3e-5);
        assertEquals(0, straightResult.uBends());
        assertEquals(7, plateResult.uBends());
        assertTrue(plateResult.localLossK() > straightResult.localLossK());
        assertTrue(plateResult.pressureDropPa() > straightResult.pressureDropPa());
        assertTrue(plateResult.heatTransferCoeffWm2K() >= straightResult.heatTransferCoeffWm2K() - 1e-6);
    }

    @Test
    void itoUBendLossFallsAsBendGetsGentler() {
        assertTrue(CoolingCalculator.uBendLossK(0.0109, 0.020) > CoolingCalculator.uBendLossK(0.0109, 0.028));
    }

    @Test
    void ml770At10LminMatchesSpecVelocity() {
        CoolingRequest request = new CoolingRequest(
                8000, 0.38, 0.93, 20, 45, 8, TubeMaterial.STAINLESS_STEEL, 0.0009,
                0.0109, 3.6, 10.0 / 60_000.0, 2e5, 7, 0.028
        );
        CoolingResult result = calculator.evaluate(request, 0.0109, 3.6, 10.0 / 60_000.0);
        assertEquals(1.79, result.velocityMps(), 0.02);
        assertTrue(result.reynolds() > 15_000, "expected turbulent Re, got " + result.reynolds());
        assertEquals(8_000 / 0.38, result.maxPowerConsumptionW(), 1.0);
        assertEquals(8_000 / 0.38 - 8_000, result.chillerCapacityW(), 1.0);
        assertEquals(40.0, result.recommendedFlowM3s() * 60_000.0, 0.01);
        assertEquals(7, result.uBends());
    }

    @Test
    void designCurvesFlowSweepRaisesHtcAndPressureDrop() {
        CoolingRequest request = new CoolingRequest(
                8000, 0.38, 0.93, 20, 45, 8, TubeMaterial.STAINLESS_STEEL, 0.0009,
                0.0109, 3.6, 10.0 / 60_000.0, 2e5, 7, 0.028
        );
        var samples = DesignCurves.sweep(
                calculator, request, DesignCurves.Axis.FLOW,
                10.0 / 60_000.0, 30.0 / 60_000.0, 9
        );
        assertEquals(9, samples.size());
        CoolingResult slow = samples.getFirst().result();
        CoolingResult fast = samples.getLast().result();
        assertTrue(fast.heatTransferCoeffWm2K() > slow.heatTransferCoeffWm2K());
        assertTrue(fast.pressureDropPa() > slow.pressureDropPa());
        assertTrue(fast.waterRiseK() < slow.waterRiseK());
        assertTrue(fast.outerWallTempC() < slow.outerWallTempC());
    }

    private static CoolingRequest sampleRequest(double diameterM, double lengthM, Double flowM3s) {
        return new CoolingRequest(
                500, 0.5, 0.93, 20, 45, 8, TubeMaterial.COPPER, 0.001,
                diameterM, lengthM, flowM3s, 2e5, 0, 0.04
        );
    }
}
