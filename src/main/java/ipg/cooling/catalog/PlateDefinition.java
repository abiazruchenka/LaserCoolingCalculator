package ipg.cooling.catalog;

import ipg.cooling.calc.CoolingPlate;

public record PlateDefinition(
        String id,
        String name,
        String drawing,
        String ipgNumber,
        String source,
        String notes,
        PlateBody plate,
        TubeBody tube,
        LayoutBody layout,
        HydraulicsBody hydraulics
) {
    public String displayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return id;
    }

    public CoolingPlate toCoolingPlate() {
        PlateBody body = plate;
        LayoutBody routing = layout;
        return new CoolingPlate(
                body.widthMm() / 1000.0,
                body.heightMm() / 1000.0,
                body.thicknessMm() / 1000.0,
                body.resolvedMassKg(),
                body.conductivityWmk() > 0 ? body.conductivityWmk() : CoolingPlate.DEFAULT_CONDUCTIVITY_WMK,
                body.specificHeatJkgK() > 0 ? body.specificHeatJkgK() : CoolingPlate.DEFAULT_SPECIFIC_HEAT,
                body.edgeMarginMm() / 1000.0,
                routing.tubeOrientation(),
                routing.passes(),
                routing.toLayout(body, tube)
        );
    }
}
