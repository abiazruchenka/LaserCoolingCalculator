package ipg.cooling.calc;

public enum FlowRegime {
    LAMINAR,
    TRANSITIONAL,
    TURBULENT;

    public static FlowRegime of(double reynolds) {
        if (reynolds < 2300) {
            return LAMINAR;
        }
        if (reynolds < 4000) {
            return TRANSITIONAL;
        }
        return TURBULENT;
    }
}
