package ipg.cooling.calc;

public enum FlowRegime {
    LAMINAR("Laminar"),
    TRANSITIONAL("Transitional"),
    TURBULENT("Turbulent");

    private final String displayName;

    FlowRegime(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

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
