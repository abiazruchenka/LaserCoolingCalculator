package ipg.cooling.catalog;

import ipg.cooling.calc.TubeMaterial;

public record TubeBody(
        String material,
        String standard,
        double innerDiameterMm,
        double outerDiameterMm,
        double wallThicknessMm,
        Double massKg
) {
    public TubeMaterial tubeMaterial() {
        if (material == null || material.isBlank()) {
            return TubeMaterial.STAINLESS_STEEL;
        }
        try {
            return TubeMaterial.valueOf(material.trim());
        } catch (IllegalArgumentException ex) {
            return TubeMaterial.STAINLESS_STEEL;
        }
    }
}
