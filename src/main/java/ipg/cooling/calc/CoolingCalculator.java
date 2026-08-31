package ipg.cooling.calc;

import ipg.cooling.I18n;

/**
 * Designs or evaluates a water-cooled tube for laser heat removal.
 *
 * <p>Heat is taken as uniformly distributed along the inner wall (typical cooling channel).
 * Convection uses Gnielinski (turbulent) / Hausen-style (laminar) Nusselt numbers,
 * with a length-weighted coil correction on U-bends. Wall conduction is a cylindrical shell.
 * Pressure drop is Darcy friction along the developed centreline plus fitting K and Ito U-bend K.
 * Water properties are taken at the mean bulk temperature.
 */
public final class CoolingCalculator {

    static final double[] STANDARD_INNER_M = {
            0.003, 0.004, 0.005, 0.006, 0.008, 0.010, 0.012, 0.014, 0.016, 0.020, 0.025
    };
    private static final double MIN_LENGTH_M = 0.02;
    private static final double MAX_LENGTH_M = 50.0;
    /** Inlet + outlet fittings (NPT etc.), in velocity heads. */
    static final double FITTING_LOSS_K = 1.5;
    private static final double BOILING_C = 99.0;

    public CoolingResult calculate(CoolingRequest request) {
        validate(request);

        if (request.innerDiameterM() != null && request.lengthM() != null && request.volumeFlowM3s() != null) {
            return evaluate(request, request.innerDiameterM(), request.lengthM(), request.volumeFlowM3s());
        }

        Candidate best = null;
        for (double diameter : candidateDiameters(request)) {
            for (double flow : candidateFlows(request, diameter)) {
                double length = request.lengthM() != null
                        ? request.lengthM()
                        : minLength(request, diameter, flow);
                Candidate candidate = new Candidate(diameter, length, flow, evaluate(request, diameter, length, flow));
                if (isBetter(candidate, best, request, false)) {
                    best = candidate;
                }
            }
        }

        if (best == null) {
            throw new IllegalArgumentException(I18n.t("error.noTube"));
        }
        return best.result;
    }

    public OptimizerOutcome optimize(CoolingRequest request, OptimizerSettings settings) {
        validate(request);
        settings.validate();

        Candidate best = null;
        int evaluated = 0;
        int feasible = 0;
        for (double diameter : settings.diameters()) {
            for (double length : settings.lengths()) {
                for (double flow : settings.flows()) {
                    CoolingResult result = evaluate(request, diameter, length, flow);
                    evaluated++;
                    if (isViable(result, request, true)) {
                        feasible++;
                    }
                    Candidate candidate = new Candidate(diameter, length, flow, result);
                    if (isBetter(candidate, best, request, true)) {
                        best = candidate;
                    }
                }
            }
        }

        if (best == null) {
            throw new IllegalArgumentException(I18n.t("error.noTube"));
        }
        return new OptimizerOutcome(best.result, evaluated, feasible);
    }

    public CoolingResult evaluate(CoolingRequest request, double innerDiameterM, double lengthM, double volumeFlowM3s) {
        if (innerDiameterM <= 0 || lengthM <= 0 || volumeFlowM3s <= 0) {
            throw new IllegalArgumentException(I18n.t("error.positiveGeometry"));
        }
        if (request.uBendCount() > 0 && request.bendRadiusM() < innerDiameterM / 2.0) {
            throw new IllegalArgumentException(I18n.t("error.bendRadius"));
        }

        double outerDiameterM = innerDiameterM + 2.0 * request.wallThicknessM();
        double heatLoadW = request.heatLoadW();
        WaterProperties water = propertiesAtMeanTemp(heatLoadW, request.inletTempC(), volumeFlowM3s);

        double massFlow = water.densityKgM3() * volumeFlowM3s;
        double waterRiseK = heatLoadW / (massFlow * water.specificHeatJkgK());
        double outletTempC = request.inletTempC() + waterRiseK;
        double meanBulkC = request.inletTempC() + waterRiseK / 2.0;

        double area = Math.PI * innerDiameterM * innerDiameterM / 4.0;
        double velocity = volumeFlowM3s / area;
        double reynolds = water.densityKgM3() * velocity * innerDiameterM / water.viscosityPaS();
        double prandtl = water.prandtl();
        int uBends = request.uBendCount();
        double nusseltStraight = nusseltNumber(reynolds, prandtl, innerDiameterM, lengthM);
        double nusselt = nusseltWithBends(
                nusseltStraight, uBends, innerDiameterM, request.bendRadiusM(), lengthM);
        double h = nusselt * water.conductivityWmk() / innerDiameterM;

        double convectionResistance = 1.0 / (h * Math.PI * innerDiameterM * lengthM);
        double wallResistance = wallResistance(innerDiameterM, outerDiameterM, lengthM, request.material());
        double thermalResistance = convectionResistance + wallResistance;

        double innerWallTempC = meanBulkC + heatLoadW * convectionResistance;
        double outerWallTempC = meanBulkC + heatLoadW * thermalResistance;

        double friction = frictionFactor(reynolds);
        double dynamicPressure = 0.5 * water.densityKgM3() * velocity * velocity;
        double localLossK = FITTING_LOSS_K + uBends * uBendLossK(innerDiameterM, request.bendRadiusM());
        double pressureDropPa = (friction * (lengthM / innerDiameterM) + localLossK) * dynamicPressure;

        boolean wallLimitOk = outerWallTempC <= request.maxWallTempC() + 1e-6;
        boolean pressureLimitOk = pressureDropPa <= request.maxPressureDropPa() + 1e-6;
        boolean noBoiling = outletTempC < BOILING_C && innerWallTempC < BOILING_C;
        double coolingConductanceWPerK = heatLoadW
                / Math.max(outerWallTempC - request.inletTempC(), 1e-6);

        String recommendation = buildRecommendation(
                request, innerDiameterM, lengthM, volumeFlowM3s, velocity, reynolds,
                outerWallTempC, pressureDropPa, wallLimitOk, pressureLimitOk, noBoiling, outletTempC,
                coolingConductanceWPerK, uBends, localLossK
        );

        return new CoolingResult(
                innerDiameterM,
                outerDiameterM,
                lengthM,
                volumeFlowM3s,
                velocity,
                reynolds,
                FlowRegime.of(reynolds),
                nusselt,
                h,
                waterRiseK,
                outletTempC,
                innerWallTempC,
                outerWallTempC,
                pressureDropPa,
                thermalResistance,
                coolingConductanceWPerK,
                request.maxPowerConsumptionW(),
                request.apparentPowerVa(),
                heatLoadW,
                request.recommendedFlowM3s(),
                request.bendCount(),
                uBends,
                request.bendRadiusM(),
                localLossK,
                wallLimitOk,
                pressureLimitOk,
                noBoiling,
                recommendation
        );
    }

    double nusseltNumber(double reynolds, double prandtl, double diameterM, double lengthM) {
        if (reynolds < 2300) {
            return laminarNusselt(reynolds, prandtl, diameterM, lengthM);
        }
        if (reynolds >= 4000) {
            return Math.max(laminarNusselt(2300, prandtl, diameterM, lengthM), gnielinski(reynolds, prandtl));
        }
        double nuLam = laminarNusselt(2300, prandtl, diameterM, lengthM);
        double nuTurb = gnielinski(4000, prandtl);
        double t = (reynolds - 2300) / (4000 - 2300);
        return nuLam + t * (nuTurb - nuLam);
    }

    static double gnielinski(double reynolds, double prandtl) {
        double f = petukhovFriction(reynolds);
        double numerator = (f / 8.0) * (reynolds - 1000.0) * prandtl;
        double denominator = 1.0 + 12.7 * Math.sqrt(f / 8.0) * (Math.pow(prandtl, 2.0 / 3.0) - 1.0);
        return numerator / denominator;
    }

    static double petukhovFriction(double reynolds) {
        double inner = 0.79 * Math.log(reynolds) - 1.64;
        return 1.0 / (inner * inner);
    }

    static double laminarNusselt(double reynolds, double prandtl, double diameterM, double lengthM) {
        double gz = reynolds * prandtl * diameterM / lengthM;
        double developing = 1.86 * Math.pow(Math.max(gz, 1e-9), 1.0 / 3.0);
        return Math.max(4.36, developing);
    }

    /**
     * Extra loss coefficient of one 180° circular U-bend (Ito 90° × 1.8).
     * {@code r/d} is clamped to ≥ 0.5.
     */
    static double uBendLossK(double diameterM, double bendRadiusM) {
        double radiusOverD = Math.max(bendRadiusM / Math.max(diameterM, 1e-9), 0.5);
        double k90 = 0.131 + 0.163 * Math.pow(1.0 / radiusOverD, 3.5);
        return 1.8 * k90;
    }

    static double nusseltWithBends(
            double nuStraight, int uBends, double diameterM, double bendRadiusM, double lengthM
    ) {
        if (uBends <= 0 || lengthM <= 0) {
            return nuStraight;
        }
        double bendFraction = Math.min(1.0, uBends * Math.PI * bendRadiusM / lengthM);
        return nuStraight * ((1.0 - bendFraction) + bendFraction * coiledNusseltFactor(diameterM, bendRadiusM));
    }

    /** Gnielinski / VDI helical-coil factor; coil diameter = 2R. */
    static double coiledNusseltFactor(double diameterM, double bendRadiusM) {
        double coilDiameter = Math.max(2.0 * bendRadiusM, diameterM + 1e-9);
        double dOverCoil = Math.min(diameterM / coilDiameter, 0.4);
        double factor = 1.0 + 3.6 * (1.0 - dOverCoil) * Math.pow(dOverCoil, 0.8);
        return clamp(factor, 1.0, 1.8);
    }

    static double frictionFactor(double reynolds) {
        if (reynolds < 2300) {
            return 64.0 / Math.max(reynolds, 1.0);
        }
        if (reynolds < 4000) {
            double fLam = 64.0 / 2300.0;
            double fTurb = 0.3164 / Math.pow(4000, 0.25);
            double t = (reynolds - 2300) / (4000 - 2300);
            return fLam + t * (fTurb - fLam);
        }
        return 0.3164 / Math.pow(reynolds, 0.25);
    }

    static double wallResistance(double innerDiameterM, double outerDiameterM, double lengthM, TubeMaterial material) {
        if (outerDiameterM <= innerDiameterM) {
            return 0.0;
        }
        return Math.log(outerDiameterM / innerDiameterM) / (2.0 * Math.PI * material.conductivityWmk() * lengthM);
    }

    private WaterProperties propertiesAtMeanTemp(double heatPowerW, double inletTempC, double volumeFlowM3s) {
        WaterProperties water = WaterProperties.atCelsius(clamp(inletTempC, 0, 100));
        for (int i = 0; i < 4; i++) {
            double massFlow = water.densityKgM3() * volumeFlowM3s;
            double rise = heatPowerW / (massFlow * water.specificHeatJkgK());
            double mean = clamp(inletTempC + rise / 2.0, 0, 100);
            water = WaterProperties.atCelsius(mean);
        }
        return water;
    }

    private double[] candidateDiameters(CoolingRequest request) {
        if (request.innerDiameterM() != null) {
            return new double[]{request.innerDiameterM()};
        }
        return STANDARD_INNER_M;
    }

    private double[] candidateFlows(CoolingRequest request, double diameterM) {
        if (request.volumeFlowM3s() != null) {
            return new double[]{request.volumeFlowM3s()};
        }
        WaterProperties inlet = WaterProperties.atCelsius(clamp(request.inletTempC(), 0, 100));
        double thermalFlow = request.heatLoadW()
                / (inlet.densityKgM3() * inlet.specificHeatJkgK() * request.maxWaterRiseK());
        double area = Math.PI * diameterM * diameterM / 4.0;
        double targetFlow = area * CoolingRequest.TARGET_VELOCITY_MPS;
        double minFlow = area * CoolingRequest.MIN_VELOCITY_MPS;
        double maxFlow = area * CoolingRequest.MAX_VELOCITY_MPS;
        double preferred = clamp(Math.max(thermalFlow, targetFlow * 0.5), minFlow, maxFlow);
        return uniquePositive(thermalFlow, preferred, targetFlow, minFlow, maxFlow);
    }

    private double minLength(CoolingRequest request, double diameterM, double volumeFlowM3s) {
        WaterProperties water = propertiesAtMeanTemp(request.heatLoadW(), request.inletTempC(), volumeFlowM3s);
        double massFlow = water.densityKgM3() * volumeFlowM3s;
        double waterRiseK = request.heatLoadW() / (massFlow * water.specificHeatJkgK());
        double meanBulkC = request.inletTempC() + waterRiseK / 2.0;
        double margin = request.maxWallTempC() - meanBulkC;
        if (margin <= 0.2) {
            return MAX_LENGTH_M;
        }

        double length = 0.2;
        for (int i = 0; i < 8; i++) {
            double reynolds = reynolds(water, diameterM, volumeFlowM3s);
            double nusselt = nusseltNumber(reynolds, water.prandtl(), diameterM, length);
            double h = nusselt * water.conductivityWmk() / diameterM;
            double outer = diameterM + 2.0 * request.wallThicknessM();
            double perMeter = 1.0 / (h * Math.PI * diameterM)
                    + wallResistance(diameterM, outer, 1.0, request.material());
            length = clamp(request.heatLoadW() * perMeter / margin, MIN_LENGTH_M, MAX_LENGTH_M);
        }
        return length;
    }

    private static double reynolds(WaterProperties water, double diameterM, double volumeFlowM3s) {
        double velocity = volumeFlowM3s / (Math.PI * diameterM * diameterM / 4.0);
        return water.densityKgM3() * velocity * diameterM / water.viscosityPaS();
    }

    private boolean isBetter(Candidate candidate, Candidate best, CoolingRequest request, boolean searchMode) {
        if (!isViable(candidate.result, request, searchMode) && (best != null && isViable(best.result, request, searchMode))) {
            return false;
        }
        if (best == null) {
            return true;
        }
        boolean candidateOk = isViable(candidate.result, request, searchMode);
        boolean bestOk = isViable(best.result, request, searchMode);
        if (candidateOk != bestOk) {
            return candidateOk;
        }
        int efficiencyCmp = Double.compare(
                candidate.result.coolingConductanceWPerK(),
                best.result.coolingConductanceWPerK()
        );
        if (efficiencyCmp != 0) {
            return efficiencyCmp > 0;
        }
        if (Double.compare(candidate.result.outerWallTempC(), best.result.outerWallTempC()) != 0) {
            return candidate.result.outerWallTempC() < best.result.outerWallTempC();
        }
        return candidate.result.thermalResistanceKw() < best.result.thermalResistanceKw();
    }

    private boolean isViable(CoolingResult result, CoolingRequest request, boolean searchMode) {
        boolean lengthOk = searchMode
                || request.lengthM() != null
                || result.lengthM() < MAX_LENGTH_M - 1e-9;
        return result.wallLimitOk() && result.pressureLimitOk() && result.noBoiling() && lengthOk
                && result.velocityMps() <= CoolingRequest.MAX_VELOCITY_MPS + 1e-6;
    }

    private String buildRecommendation(
            CoolingRequest request,
            double innerDiameterM,
            double lengthM,
            double volumeFlowM3s,
            double velocity,
            double reynolds,
            double outerWallTempC,
            double pressureDropPa,
            boolean wallLimitOk,
            boolean pressureLimitOk,
            boolean noBoiling,
            double outletTempC,
            double coolingConductanceWPerK,
            int uBends,
            double localLossK
    ) {
        StringBuilder text = new StringBuilder();
        text.append(I18n.t(
                "rec.summary",
                I18n.t("material." + request.material().name()),
                innerDiameterM * 1000.0,
                lengthM * 1000.0,
                volumeFlowM3s * 60_000.0
        ));
        if (request.bendCount() > 0) {
            text.append(I18n.t(
                    "rec.serpentine",
                    uBends,
                    request.bendRadiusM() * 1000.0,
                    localLossK
            ));
        }
        text.append(I18n.t(
                "rec.flow",
                I18n.t("regime." + FlowRegime.of(reynolds).name()),
                reynolds
        ));
        text.append(I18n.t("rec.efficiency", coolingConductanceWPerK));

        if (wallLimitOk && pressureLimitOk && noBoiling) {
            text.append(I18n.t(
                    "rec.ok",
                    outerWallTempC,
                    request.maxWallTempC(),
                    request.maxWallTempC() - outerWallTempC
            ));
        }
        if (!wallLimitOk) {
            text.append(I18n.t("rec.wallHot"));
        }
        if (!pressureLimitOk) {
            text.append(I18n.t("rec.pressureHigh", pressureDropPa / 1e5));
        }
        if (!noBoiling) {
            text.append(I18n.t("rec.boiling", outletTempC));
        }
        if (FlowRegime.of(reynolds) == FlowRegime.LAMINAR) {
            text.append(I18n.t("rec.laminar"));
        }
        if (velocity > CoolingRequest.MAX_VELOCITY_MPS) {
            text.append(I18n.t("rec.velocityHigh"));
        }
        return text.toString();
    }

    private void validate(CoolingRequest request) {
        if (request.laserPowerW() <= 0) {
            throw new IllegalArgumentException(I18n.t("error.laserPower"));
        }
        if (request.efficiency() <= 0.05 || request.efficiency() >= 1.0) {
            throw new IllegalArgumentException(I18n.t("error.efficiency"));
        }
        if (request.powerFactor() <= 0.5 || request.powerFactor() > 1.0) {
            throw new IllegalArgumentException(I18n.t("error.powerFactor"));
        }
        if (request.heatLoadW() <= 0) {
            throw new IllegalArgumentException(I18n.t("error.heatPower"));
        }
        if (request.inletTempC() < 0 || request.inletTempC() >= BOILING_C) {
            throw new IllegalArgumentException(I18n.t("error.inletTemp"));
        }
        if (request.maxWallTempC() <= request.inletTempC()) {
            throw new IllegalArgumentException(I18n.t("error.wallVsWater"));
        }
        if (request.maxWaterRiseK() <= 0) {
            throw new IllegalArgumentException(I18n.t("error.waterRise"));
        }
        if (request.wallThicknessM() < 0) {
            throw new IllegalArgumentException(I18n.t("error.wallThickness"));
        }
        if (request.material() == null) {
            throw new IllegalArgumentException(I18n.t("error.material"));
        }
        if (request.maxPressureDropPa() <= 0) {
            throw new IllegalArgumentException(I18n.t("error.pressure"));
        }
        if (request.inletTempC() + request.maxWaterRiseK() >= BOILING_C) {
            throw new IllegalArgumentException(I18n.t("error.boiling"));
        }
        if (request.bendCount() < 0) {
            throw new IllegalArgumentException(I18n.t("error.bends"));
        }
        if (request.bendCount() > 0 && request.bendRadiusM() <= 0) {
            throw new IllegalArgumentException(I18n.t("error.bendRadius"));
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    private static double[] uniquePositive(double... values) {
        return java.util.Arrays.stream(values)
                .filter(v -> v > 0 && Double.isFinite(v))
                .distinct()
                .toArray();
    }

    private record Candidate(double diameter, double length, double flow, CoolingResult result) {
    }
}
