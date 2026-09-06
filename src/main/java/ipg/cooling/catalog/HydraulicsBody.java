package ipg.cooling.catalog;

public record HydraulicsBody(
        double leakTestBarMin,
        double leakTestBarMax,
        boolean leakTestIsOperatingPressure
) {
}
