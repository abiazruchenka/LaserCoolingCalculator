package ipg.cooling.catalog;

import ipg.cooling.calc.CoolingPlate;
import ipg.cooling.calc.SerpentineLayout;
import ipg.cooling.calc.TubeOrientation;

public record LayoutBody(
        String orientation,
        int passes,
        double pitchMm,
        double bendRadiusMm,
        Double developedLengthMm
) {
    public TubeOrientation tubeOrientation() {
        if (orientation == null || orientation.isBlank()) {
            return TubeOrientation.ALONG_WIDTH;
        }
        try {
            return TubeOrientation.valueOf(orientation.trim());
        } catch (IllegalArgumentException ex) {
            return TubeOrientation.ALONG_WIDTH;
        }
    }

    public SerpentineLayout toLayout(PlateBody plate, TubeBody tube) {
        CoolingPlate geometry = new CoolingPlate(
                plate.widthMm() / 1000.0,
                plate.heightMm() / 1000.0,
                plate.thicknessMm() / 1000.0,
                plate.resolvedMassKg(),
                plate.conductivityWmk() > 0 ? plate.conductivityWmk() : CoolingPlate.DEFAULT_CONDUCTIVITY_WMK,
                plate.specificHeatJkgK() > 0 ? plate.specificHeatJkgK() : CoolingPlate.DEFAULT_SPECIFIC_HEAT,
                plate.edgeMarginMm() / 1000.0,
                tubeOrientation(),
                Math.max(1, passes),
                null
        );
        double outerMm = tube != null && tube.outerDiameterMm() > 0
                ? tube.outerDiameterMm()
                : tube.innerDiameterMm() + 2.0 * tube.wallThicknessMm();
        SerpentineLayout layout = SerpentineLayout.tryCreate(
                geometry,
                tubeOrientation(),
                Math.max(1, passes),
                bendRadiusMm / 1000.0,
                outerMm / 1000.0,
                pitchMm / 1000.0
        );
        double drawn = developedLengthMm != null && developedLengthMm > 0
                ? developedLengthMm / 1000.0
                : 0.0;
        if (layout != null) {
            if (drawn <= 0) {
                return layout;
            }
            return new SerpentineLayout(
                    layout.orientation(),
                    layout.passes(),
                    layout.uBends(),
                    layout.pitchM(),
                    layout.bendRadiusM(),
                    layout.straightLengthM(),
                    drawn
            );
        }
        int bends = Math.max(0, passes - 1);
        return new SerpentineLayout(
                tubeOrientation(),
                Math.max(1, passes),
                bends,
                pitchMm / 1000.0,
                bendRadiusMm / 1000.0,
                0,
                drawn
        );
    }
}
