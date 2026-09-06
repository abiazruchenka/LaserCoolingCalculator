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
        CoolingRequest request = sampleRequest(0.008, 0.4);
        CoolingResult result = calculator.evaluate(request, 0.008, 0.4, 1.5e-5);
        WaterProperties water = WaterProperties.atCelsius(request.inletTempC() + result.waterRiseK() / 2.0);
        double expectedRise = request.heatLoadW()
                / (water.densityKgM3() * 1.5e-5 * water.specificHeatJkgK());
        assertEquals(expectedRise, result.waterRiseK(), 0.05);
    }

    @Test
    void higherFlowLowersWaterTemperatureRise() {
        CoolingRequest request = sampleRequest(0.008, 0.4);
        CoolingResult slow = calculator.evaluate(request, 0.008, 0.4, 1.0e-5);
        CoolingResult fast = calculator.evaluate(request, 0.008, 0.4, 2.0e-5);
        assertTrue(fast.waterRiseK() < slow.waterRiseK());
        assertTrue(fast.heatTransferCoeffWm2K() > slow.heatTransferCoeffWm2K());
    }

    @Test
    void smallerDiameterRaisesPressureDropAtFixedFlow() {
        CoolingRequest request = sampleRequest(0.006, 0.4);
        CoolingResult narrow = calculator.evaluate(request, 0.006, 0.4, 1.5e-5);
        CoolingResult wide = calculator.evaluate(request, 0.012, 0.4, 1.5e-5);
        assertTrue(narrow.pressureDropPa() > wide.pressureDropPa());
    }

    @Test
    void smallerDiameterLowersFlowAtFixedChillerPressure() {
        CoolingRequest request = sampleRequest(0.006, 0.4);
        CoolingResult narrow = calculator.evaluateAtPressure(request, 0.006, 0.4, 2e5);
        CoolingResult wide = calculator.evaluateAtPressure(request, 0.012, 0.4, 2e5);
        assertTrue(narrow.volumeFlowM3s() < wide.volumeFlowM3s());
        assertEquals(2e5, narrow.inletPressurePa(), 5e3);
        assertEquals(2e5, wide.inletPressurePa(), 5e3);
    }

    @Test
    void gnielinskiNusseltIsInExpectedRange() {
        double nu = CoolingCalculator.gnielinski(10_000, 7.0);
        assertEquals(80.0, nu, 8.0);
    }

    @Test
    void calculateUsesPlateGeometryAndChillerPressure() {
        CoolingRequest request = sampleRequest(0.008, 0.4);
        CoolingResult result = calculator.calculate(request);
        assertEquals(0.008, result.innerDiameterM(), 1e-9);
        assertEquals(0.4, result.lengthM(), 1e-9);
        assertEquals(2e5, result.inletPressurePa(), 5e3);
        assertTrue(result.volumeFlowM3s() > 0);
    }

    @Test
    void rejectsNonPositiveHeatLoad() {
        CoolingRequest request = new CoolingRequest(
                0, 0.5, 0.93, 20, 45, 8.0, TubeMaterial.COPPER, 0.001,
                0.008, 0.4, 0, 2e5, 0, 0.04, null
        );
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(request));
    }

    @Test
    void serpentineUBendsRaisePressureDropAtFixedFlow() {
        CoolingRequest straight = sampleRequest(0.0109, 3.6);
        CoolingRequest plate = new CoolingRequest(
                500, 0.5, 0.93, 20, 45, 8.0, TubeMaterial.STAINLESS_STEEL, 0.0009,
                0.0109, 3.6, 0, 2e5, 7, 0.028, null
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
                8000, 0.38, 0.93, 20, 45, 8.0, TubeMaterial.STAINLESS_STEEL, 0.0009,
                0.0109, 3.6, 0, 2e5, 7, 0.028, null
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
    void designCurvesPressureSweepRaisesFlowAndLowersWaterRise() {
        CoolingRequest request = new CoolingRequest(
                8000, 0.38, 0.93, 20, 45, 8.0, TubeMaterial.STAINLESS_STEEL, 0.0009,
                0.0109, 3.6, 0, 2.5e5, 7, 0.028, null
        );
        var samples = DesignCurves.sweep(calculator, request, 2.5e5, 5.0e5, 9);
        assertEquals(9, samples.size());
        CoolingResult low = samples.getFirst().result();
        CoolingResult high = samples.getLast().result();
        assertTrue(high.volumeFlowM3s() > low.volumeFlowM3s());
        assertTrue(high.heatTransferCoeffWm2K() > low.heatTransferCoeffWm2K());
        assertTrue(high.waterRiseK() < low.waterRiseK());
        assertTrue(high.outerWallTempC() < low.outerWallTempC());
    }

    @Test
    void plateSerpentineFitsAndAddsAluminumResistance() {
        CoolingPlate plate = CoolingPlate.typicalCastPlate();
        SerpentineLayout layout = SerpentineLayout.tryCreate(
                plate, TubeOrientation.ALONG_WIDTH, 8, 0.028, 0.0127);
        assertTrue(layout != null);
        assertEquals(7, layout.uBends());
        assertTrue(layout.developedLengthM() > 3.0 && layout.developedLengthM() < 5.0);
        CoolingRequest request = new CoolingRequest(
                8000, 0.38, 0.93, 20, 45, 8.0, TubeMaterial.STAINLESS_STEEL, 0.0009,
                0.0109, layout.developedLengthM(), 0, 2.5e5, 7, 0.028, plate
        );
        CoolingResult withPlate = calculator.evaluateAtPressure(request, 0.0109, layout.developedLengthM(), 2.5e5);
        CoolingResult tubeOnly = calculator.evaluateAtPressure(
                request.withPlate(null), 0.0109, layout.developedLengthM(), 2.5e5);
        assertTrue(withPlate.plateTempC() > withPlate.outerWallTempC());
        assertTrue(withPlate.timeConstantS() > 0);
        assertTrue(withPlate.plateTempC() > tubeOnly.outerWallTempC() - 1e-6);
    }

    private static CoolingRequest sampleRequest(double diameterM, double lengthM) {
        return new CoolingRequest(
                500, 0.5, 0.93, 20, 45, 8.0, TubeMaterial.COPPER, 0.001,
                diameterM, lengthM, 0, 2e5, 0, 0.04, null
        );
    }
}
