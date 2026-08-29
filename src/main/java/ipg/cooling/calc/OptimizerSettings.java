package ipg.cooling.calc;

import ipg.cooling.I18n;

/**
 * Grid search over min…max. The step is {@code (max - min) / (iterations - 1)} when a parameter is varied.
 */
public record OptimizerSettings(
        double minDiameterM,
        double maxDiameterM,
        boolean varyDiameter,
        double minLengthM,
        double maxLengthM,
        boolean varyLength,
        double minFlowM3s,
        double maxFlowM3s,
        boolean varyFlow,
        int iterations
) {
    public static final double MIN_DIAMETER_M = 0.001;
    public static final double MAX_DIAMETER_M = 0.200;
    public static final double MIN_LENGTH_M = 0.020;
    public static final double MAX_LENGTH_M = 50.0;
    public static final double MIN_FLOW_M3S = 1.0e-7;
    public static final double MAX_FLOW_M3S = 0.02;
    public static final int MAX_ITERATIONS = 40;

    public void validate() {
        if (iterations < 1 || iterations > MAX_ITERATIONS) {
            throw new IllegalArgumentException(I18n.t("error.iterations", MAX_ITERATIONS));
        }
        validateRange(minDiameterM, maxDiameterM, varyDiameter, "label.innerDiameter",
                MIN_DIAMETER_M, MAX_DIAMETER_M);
        validateRange(minLengthM, maxLengthM, varyLength, "label.length", MIN_LENGTH_M, MAX_LENGTH_M);
        validateRange(minFlowM3s, maxFlowM3s, varyFlow, "label.flow", MIN_FLOW_M3S, MAX_FLOW_M3S);
    }

    public double[] diameters() {
        return series(minDiameterM, maxDiameterM, varyDiameter, MIN_DIAMETER_M, MAX_DIAMETER_M);
    }

    public double[] lengths() {
        return series(minLengthM, maxLengthM, varyLength, MIN_LENGTH_M, MAX_LENGTH_M);
    }

    public double[] flows() {
        return series(minFlowM3s, maxFlowM3s, varyFlow, MIN_FLOW_M3S, MAX_FLOW_M3S);
    }

    private double[] series(double rangeMin, double rangeMax, boolean vary, double absMin, double absMax) {
        double from = clamp(rangeMin, absMin, absMax);
        if (!vary) {
            return new double[]{from};
        }
        double to = clamp(rangeMax, absMin, absMax);
        if (iterations == 1 || to <= from) {
            return new double[]{from};
        }
        double[] values = new double[iterations];
        double step = (to - from) / (iterations - 1);
        for (int i = 0; i < iterations; i++) {
            values[i] = from + i * step;
        }
        values[iterations - 1] = to;
        return values;
    }

    private static void validateRange(
            double min, double max, boolean vary, String labelKey, double absMin, double absMax
    ) {
        if (min <= 0 || (vary && max <= 0)) {
            throw new IllegalArgumentException(I18n.t("error.positiveGeometry"));
        }
        if (vary && max < min) {
            throw new IllegalArgumentException(I18n.t("error.rangeOrder", I18n.t(labelKey)));
        }
        if (min < absMin || min > absMax || (vary && (max < absMin || max > absMax))) {
            throw new IllegalArgumentException(I18n.t("error.optimizerRange", I18n.t(labelKey)));
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }
}
