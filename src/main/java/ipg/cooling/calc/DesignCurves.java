package ipg.cooling.calc;

import java.util.ArrayList;
import java.util.List;

/**
 * Sweeps chiller inlet pressure and evaluates the thermal-hydraulic model
 * at each point. Flow is solved from the pressure; plate geometry stays fixed.
 */
public final class DesignCurves {
    public static final int DEFAULT_POINTS = 40;

    public record Sample(CoolingResult result) {
    }

    private DesignCurves() {
    }

    public static List<Sample> sweep(
            CoolingCalculator calculator,
            CoolingRequest request,
            double minPa,
            double maxPa,
            int points
    ) {
        if (calculator == null || request == null || points < 2) {
            return List.of();
        }
        double lo = Math.min(minPa, maxPa);
        double hi = Math.max(minPa, maxPa);
        if (!(lo > 0) || !(hi > 0) || !Double.isFinite(lo) || !Double.isFinite(hi)) {
            return List.of();
        }
        List<Sample> samples = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            double pressurePa = lo + t * (hi - lo);
            try {
                samples.add(new Sample(calculator.evaluateAtPressure(request, pressurePa)));
            } catch (RuntimeException ignored) {
                // skip points that violate geometry or cannot produce flow
            }
        }
        return samples;
    }
}
