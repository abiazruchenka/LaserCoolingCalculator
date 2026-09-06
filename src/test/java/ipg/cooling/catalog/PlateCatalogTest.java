package ipg.cooling.catalog;

import ipg.cooling.calc.CoolingCalculator;
import ipg.cooling.calc.CoolingRequest;
import ipg.cooling.calc.CoolingResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlateCatalogTest {

    @Test
    void loadsMl770AndMl786FromFilesystem() {
        Path file = Path.of("data/plates.json");
        PlateCatalog catalog = PlateCatalog.load(file);
        assertEquals(10, catalog.plates().size());

        PlateDefinition ml770 = catalog.byId("ML-770");
        assertEquals(10.9, ml770.tube().innerDiameterMm(), 1e-9);
        assertEquals(0.9, ml770.tube().wallThicknessMm(), 1e-9);
        assertEquals(8, ml770.layout().passes());
        assertEquals(28.0, ml770.layout().bendRadiusMm(), 1e-9);
        assertEquals(56.0, ml770.layout().pitchMm(), 1e-9);
        assertEquals(10.34, ml770.toCoolingPlate().massKg(), 1e-9);
        assertEquals(7, ml770.toCoolingPlate().fixedLayout().uBends());
        assertTrue(ml770.toCoolingPlate().fixedLayout().developedLengthM() > 3.0);

        PlateDefinition ml786 = catalog.byId("ML-786");
        assertEquals(13.0, ml786.tube().innerDiameterMm(), 1e-9);
        assertEquals(14.0, ml786.tube().outerDiameterMm(), 1e-9);
        assertEquals(0.5, ml786.tube().wallThicknessMm(), 1e-9);
        assertEquals(1.16, ml786.tube().massKg(), 1e-9);
        assertEquals(13, ml786.layout().passes());
        assertEquals(44.7, ml786.layout().pitchMm(), 1e-9);
        assertEquals(22.35, ml786.layout().bendRadiusMm(), 1e-9);
        assertEquals(12, ml786.toCoolingPlate().fixedLayout().uBends());
        assertTrue(ml786.toCoolingPlate().fixedLayout().developedLengthM() > 4.0);
        assertFalse(ml786.hydraulics().leakTestIsOperatingPressure());
        assertEquals(7.5, ml786.hydraulics().leakTestBarMin(), 1e-9);

        PlateDefinition ml785 = catalog.byId("ML-785");
        assertEquals(12.0, ml785.tube().innerDiameterMm(), 1e-9);
        assertEquals(1.0, ml785.tube().wallThicknessMm(), 1e-9);
        assertEquals(2.18, ml785.tube().massKg(), 1e-9);
        assertEquals(13, ml785.layout().passes());
        assertEquals(ml786.layout().pitchMm(), ml785.layout().pitchMm(), 1e-9);
        assertEquals(
                ml786.toCoolingPlate().fixedLayout().developedLengthM(),
                ml785.toCoolingPlate().fixedLayout().developedLengthM(),
                1e-9
        );

        PlateDefinition ml780 = catalog.byId("ML-780");
        assertEquals(3, ml780.layout().passes());
        assertEquals(2.720, ml780.toCoolingPlate().fixedLayout().developedLengthM(), 1e-9);

        PlateDefinition ml789 = catalog.byId("ML-789");
        assertEquals(15.6, ml789.toCoolingPlate().massKg(), 1e-9);
        assertEquals(12.0, ml789.tube().innerDiameterMm(), 1e-9);
        assertEquals(13, ml789.layout().passes());

        PlateDefinition ml790 = catalog.byId("ML-790");
        assertEquals(15, ml790.layout().passes());
        assertEquals(16.64, ml790.toCoolingPlate().massKg(), 1e-9);

        for (PlateDefinition plate : catalog.plates()) {
            assertTrue(
                    plate.toCoolingPlate().fixedLayout().developedLengthM() > 1.0,
                    () -> plate.id() + " developed length"
            );
        }
    }

    @Test
    void catalogPlatesAtSameLaserAndChillerPressure() throws Exception {
        PlateCatalog catalog = PlateCatalog.load(Path.of("data/plates.json"));
        CoolingCalculator calculator = new CoolingCalculator();
        StringBuilder report = new StringBuilder();
        CoolingResult flow785 = null;
        CoolingResult flow786 = null;
        for (PlateDefinition plate : catalog.plates()) {
            CoolingResult result = calculator.calculate(requestFor(plate));
            report.append(String.format(
                    "%s  di=%.1f mm  L=%.0f mm  V=%.2f L/min  v=%.2f m/s  dP=%.3f bar  Tw=%.1f  TAl=%.1f  tau=%.1f s%n",
                    plate.id(),
                    result.innerDiameterM() * 1000.0,
                    result.lengthM() * 1000.0,
                    result.volumeFlowM3s() * 60_000.0,
                    result.velocityMps(),
                    result.pressureDropPa() / 1e5,
                    result.outerWallTempC(),
                    result.plateTempC(),
                    result.timeConstantS()
            ));
            if ("ML-785".equals(plate.id())) {
                flow785 = result;
            }
            if ("ML-786".equals(plate.id())) {
                flow786 = result;
            }
        }
        Path out = Path.of("target/plate-compare.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString());
        assertTrue(flow786.volumeFlowM3s() > flow785.volumeFlowM3s());
        assertTrue(flow786.plateTempC() < flow785.plateTempC());
    }

    private static CoolingRequest requestFor(PlateDefinition plate) {
        var cooling = plate.toCoolingPlate();
        var layout = cooling.fixedLayout();
        return new CoolingRequest(
                8000, 0.38, 0.93, 20, 45, null,
                plate.tube().tubeMaterial(),
                plate.tube().wallThicknessMm() / 1000.0,
                plate.tube().innerDiameterMm() / 1000.0,
                layout.developedLengthM(),
                0, 2.5e5,
                layout.uBends(),
                plate.layout().bendRadiusMm() / 1000.0,
                cooling
        );
    }
}
