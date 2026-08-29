package ipg.cooling.calc;

import ipg.cooling.I18n;

/**
 * Thermophysical properties of liquid water, interpolated from standard tables.
 */
public record WaterProperties(
        double densityKgM3,
        double viscosityPaS,
        double conductivityWmk,
        double specificHeatJkgK
) {
    private static final double[] TEMP_C = {0, 10, 20, 30, 40, 50, 60, 80, 100};
    private static final double[] DENSITY = {999.8, 999.7, 998.2, 995.7, 992.2, 988.1, 983.2, 971.8, 958.4};
    private static final double[] VISCOSITY = {1.787e-3, 1.307e-3, 1.002e-3, 7.978e-4, 6.530e-4, 5.471e-4, 4.666e-4, 3.544e-4, 2.818e-4};
    private static final double[] CONDUCTIVITY = {0.561, 0.580, 0.598, 0.615, 0.631, 0.643, 0.654, 0.670, 0.679};
    private static final double[] SPECIFIC_HEAT = {4217, 4192, 4182, 4178, 4178, 4180, 4184, 4196, 4216};

    public double prandtl() {
        return viscosityPaS * specificHeatJkgK / conductivityWmk;
    }

    public static WaterProperties atCelsius(double temperatureC) {
        if (temperatureC < 0 || temperatureC > 100) {
            throw new IllegalArgumentException(I18n.t("error.waterRange"));
        }
        return new WaterProperties(
                lerp(TEMP_C, DENSITY, temperatureC),
                lerp(TEMP_C, VISCOSITY, temperatureC),
                lerp(TEMP_C, CONDUCTIVITY, temperatureC),
                lerp(TEMP_C, SPECIFIC_HEAT, temperatureC)
        );
    }

    private static double lerp(double[] xs, double[] ys, double x) {
        if (x <= xs[0]) {
            return ys[0];
        }
        for (int i = 1; i < xs.length; i++) {
            if (x <= xs[i]) {
                double t = (x - xs[i - 1]) / (xs[i] - xs[i - 1]);
                return ys[i - 1] + t * (ys[i] - ys[i - 1]);
            }
        }
        return ys[ys.length - 1];
    }
}
