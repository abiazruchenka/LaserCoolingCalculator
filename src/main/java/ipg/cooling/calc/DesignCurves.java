package ipg.cooling.calc;

import java.util.ArrayList;
import java.util.List;

/**
 * Sweeps one geometry/flow parameter and evaluates the thermal-hydraulic model
 * at each point. Used for design charts (α, ΔP, wall temperature, …).
 */
public final class DesignCurves {
    public static final int DEFAULT_POINTS = 40;

    public enum Axis {
        FLOW,
        DIAMETER,
        LENGTH
    }

    public record Sample(double x, CoolingResult result) {
    }

    private DesignCurves() {
    }

    public static List<Sample> sweep(
            CoolingCalculator calculator,
            CoolingRequest request,
            Axis axis,
            double minSi,
            double maxSi,
            int points
    ) {
        if (calculator == null || request == null || axis == null || points < 2) {
            return List.of();
        }
        double lo = Math.min(minSi, maxSi);
        double hi = Math.max(minSi, maxSi);
        if (!(lo > 0) || !(hi > 0) || !Double.isFinite(lo) || !Double.isFinite(hi)) {
            return List.of();
        }
        double diameter = required(request.innerDiameterM());
        double length = required(request.lengthM());
        double flow = required(request.volumeFlowM3s());

        List<Sample> samples = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) (points - 1);
            double value = lo + t * (hi - lo);
            double d = diameter;
            double l = length;
            double v = flow;
            double x;
            switch (axis) {
                case FLOW -> {
                    v = value;
                    x = v * 60_000.0;
                }
                case DIAMETER -> {
                    d = value;
                    x = d * 1000.0;
                }
                case LENGTH -> {
                    l = value;
                    x = l * 1000.0;
                }
                default -> throw new IllegalStateException(axis.name());
            }
            try {
                samples.add(new Sample(x, calculator.evaluate(request, d, l, v)));
            } catch (RuntimeException ignored) {
                // skip points that violate geometry (e.g. bend radius vs diameter)
            }
        }
        return samples;
    }

    private static double required(Double value) {
        if (value == null || !(value > 0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException();
        }
        return value;
    }
}
