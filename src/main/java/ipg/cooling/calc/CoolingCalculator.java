package ipg.cooling.calc;

import ipg.cooling.I18n;

/**
 * Evaluates a water-cooled tube for laser heat removal.
 *
 * <p>Heat is taken as uniformly distributed along the inner wall (typical cooling channel).
 * Convection uses Gnielinski (turbulent) / Hausen-style (laminar) Nusselt numbers,
 * with a length-weighted coil correction on U-bends. Wall conduction is a cylindrical shell.
 * Pressure drop is Darcy friction along the developed centreline plus fitting K and Ito U-bend K.
 * Water properties are taken at the mean bulk temperature.
 */
public final class CoolingCalculator {

    /** Inlet + outlet fittings (NPT etc.), in velocity heads. */
    static final double FITTING_LOSS_K = 1.5;
    private static final double BOILING_C = 99.0;

    public CoolingResult calculate(CoolingRequest request) {
        validate(request);
        return evaluateAtPressure(request, request.chillerInletPressurePa());
    }

    public CoolingResult evaluateAtPressure(CoolingRequest request, double inletPressurePa) {
        return evaluateAtPressure(
                request, request.innerDiameterM(), request.lengthM(), inletPressurePa);
    }

    public CoolingResult evaluateAtPressure(
            CoolingRequest request, double innerDiameterM, double lengthM, double inletPressurePa
    ) {
        SerpentineLayout layout = SerpentineLayout.resolve(request, innerDiameterM, lengthM);
        double length = layout != null ? layout.developedLengthM() : lengthM;
        return evaluate(
                request, innerDiameterM, length,
                flowFromInletPressure(request, innerDiameterM, length, inletPressurePa)
        );
    }

    /**
     * Solves for volume flow such that {@code P_in = P_out + ΔP(V̇)}.
     * Friction factor and water properties are iterated because both depend on velocity.
     */
    double flowFromInletPressure(
            CoolingRequest request, double innerDiameterM, double lengthM, double inletPressurePa
    ) {
        if (innerDiameterM <= 0 || lengthM <= 0) {
            throw new IllegalArgumentException(I18n.t("error.positiveGeometry"));
        }
        double targetDropPa = inletPressurePa - request.outletPressurePa();
        if (!(targetDropPa > 0) || !Double.isFinite(targetDropPa)) {
            throw new IllegalArgumentException(I18n.t("error.chillerPressure"));
        }
        SerpentineLayout layout = SerpentineLayout.resolve(request, innerDiameterM, lengthM);
        double length = layout != null ? layout.developedLengthM() : lengthM;
        int uBends = layout != null ? layout.uBends() : request.uBendCount();
        double bendRadius = layout != null ? layout.bendRadiusM() : request.bendRadiusM();
        double flow = request.recommendedFlowM3s();
        if (!(flow > 0) || !Double.isFinite(flow)) {
            flow = Math.PI * innerDiameterM * innerDiameterM / 4.0 * CoolingRequest.TARGET_VELOCITY_MPS;
        }
        for (int i = 0; i < 8; i++) {
            WaterProperties water = propertiesAtMeanTemp(request.heatLoadW(), request.inletTempC(), flow);
            double velocity = velocityFromPressureDrop(
                    water, innerDiameterM, length, uBends, bendRadius, targetDropPa);
            flow = velocity * Math.PI * innerDiameterM * innerDiameterM / 4.0;
        }
        if (!(flow > 0) || !Double.isFinite(flow)) {
            throw new IllegalArgumentException(I18n.t("error.noFlow"));
        }
        return flow;
    }

    private double velocityFromPressureDrop(
            WaterProperties water,
            double diameterM,
            double lengthM,
            int uBends,
            double bendRadiusM,
            double targetDropPa
    ) {
        double localLossK = FITTING_LOSS_K + uBends * uBendLossK(diameterM, bendRadiusM);
        double density = water.densityKgM3();
        double velocity = Math.sqrt(2.0 * targetDropPa / (density * (0.03 * lengthM / diameterM + localLossK)));
        for (int i = 0; i < 12; i++) {
            double reynolds = density * velocity * diameterM / water.viscosityPaS();
            double friction = frictionFactor(reynolds);
            double heads = friction * (lengthM / diameterM) + localLossK;
            if (!(heads > 0)) {
                break;
            }
            velocity = Math.sqrt(2.0 * targetDropPa / (density * heads));
        }
        return velocity;
    }

    public CoolingResult evaluate(CoolingRequest request, double innerDiameterM, double lengthM, double volumeFlowM3s) {
        if (innerDiameterM <= 0 || lengthM <= 0 || volumeFlowM3s <= 0) {
            throw new IllegalArgumentException(I18n.t("error.positiveGeometry"));
        }
        if (request.uBendCount() > 0 && request.bendRadiusM() < innerDiameterM / 2.0) {
            throw new IllegalArgumentException(I18n.t("error.bendRadius"));
        }

        SerpentineLayout layout = SerpentineLayout.resolve(request, innerDiameterM, lengthM);
        double length = layout != null ? layout.developedLengthM() : lengthM;
        int uBends = layout != null ? layout.uBends() : request.uBendCount();
        double bendRadius = layout != null ? layout.bendRadiusM() : request.bendRadiusM();
        double pitchM = layout != null ? layout.pitchM() : 0.0;

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
        double nusseltStraight = nusseltNumber(reynolds, prandtl, innerDiameterM, length);
        double nusselt = nusseltWithBends(
                nusseltStraight, uBends, innerDiameterM, bendRadius, length);
        double h = nusselt * water.conductivityWmk() / innerDiameterM;

        double convectionResistance = 1.0 / (h * Math.PI * innerDiameterM * length);
        double wallResistance = wallResistance(innerDiameterM, outerDiameterM, length, request.material());
        double aluminumResistance = aluminumResistance(request.plate(), pitchM, outerDiameterM, length);
        double thermalResistance = convectionResistance + wallResistance + aluminumResistance;

        double outerWallTempC = meanBulkC + heatLoadW * (convectionResistance + wallResistance);
        double plateTempC = meanBulkC + heatLoadW * thermalResistance;

        double friction = frictionFactor(reynolds);
        double dynamicPressure = 0.5 * water.densityKgM3() * velocity * velocity;
        double localLossK = FITTING_LOSS_K + uBends * uBendLossK(innerDiameterM, bendRadius);
        double pressureDropPa = (friction * (length / innerDiameterM) + localLossK) * dynamicPressure;
        double inletPressurePa = pressureDropPa + request.outletPressurePa();

        double timeConstantS = request.plate() == null
                ? 0.0
                : thermalResistance * request.plate().heatCapacityJPerK();

        return new CoolingResult(
                innerDiameterM,
                length,
                volumeFlowM3s,
                velocity,
                reynolds,
                FlowRegime.of(reynolds),
                h,
                waterRiseK,
                outletTempC,
                outerWallTempC,
                pressureDropPa,
                inletPressurePa,
                request.maxPowerConsumptionW(),
                request.apparentPowerVa(),
                heatLoadW,
                request.recommendedFlowM3s(),
                uBends,
                localLossK,
                plateTempC,
                timeConstantS
        );
    }

    static double aluminumResistance(
            CoolingPlate plate, double pitchM, double outerDiameterM, double lengthM
    ) {
        if (plate == null || lengthM <= 0 || plate.conductivityWmk() <= 0 || outerDiameterM <= 0) {
            return 0.0;
        }
        double radius = outerDiameterM / 2.0;
        double buried = Math.log(Math.max(plate.thicknessM() / radius, 1.05));
        double spreading = pitchM > Math.PI * radius ? Math.log(pitchM / (Math.PI * radius)) : 0.0;
        return (buried + spreading) / (2.0 * Math.PI * plate.conductivityWmk() * lengthM);
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
        if (request.maxWaterRiseK() != null && request.maxWaterRiseK() <= 0) {
            throw new IllegalArgumentException(I18n.t("error.waterRise"));
        }
        if (request.wallThicknessM() < 0) {
            throw new IllegalArgumentException(I18n.t("error.wallThickness"));
        }
        if (request.material() == null) {
            throw new IllegalArgumentException(I18n.t("error.material"));
        }
        if (request.outletPressurePa() < 0) {
            throw new IllegalArgumentException(I18n.t("error.outletPressure"));
        }
        if (!(request.chillerInletPressurePa() > request.outletPressurePa())) {
            throw new IllegalArgumentException(I18n.t("error.chillerPressure"));
        }
        if (request.maxWaterRiseK() != null
                && request.inletTempC() + request.maxWaterRiseK() >= BOILING_C) {
            throw new IllegalArgumentException(I18n.t("error.boiling"));
        }
        if (request.bendCount() < 0) {
            throw new IllegalArgumentException(I18n.t("error.bends"));
        }
        if (request.bendCount() > 0 && request.bendRadiusM() <= 0) {
            throw new IllegalArgumentException(I18n.t("error.bendRadius"));
        }
        if (!(request.innerDiameterM() > 0) || !(request.lengthM() > 0)) {
            throw new IllegalArgumentException(I18n.t("error.positiveGeometry"));
        }
        if (request.plate() != null) {
            CoolingPlate plate = request.plate();
            if (plate.widthM() <= 0 || plate.heightM() <= 0 || plate.thicknessM() <= 0 || plate.massKg() <= 0) {
                throw new IllegalArgumentException(I18n.t("error.plate"));
            }
            if (plate.edgeMarginM() < 0 || plate.passes() < 1) {
                throw new IllegalArgumentException(I18n.t("error.plate"));
            }
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }
}
