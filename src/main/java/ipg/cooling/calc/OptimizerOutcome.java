package ipg.cooling.calc;

public record OptimizerOutcome(
        CoolingResult best,
        int evaluated,
        int feasible
) {
}
