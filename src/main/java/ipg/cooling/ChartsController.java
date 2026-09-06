package ipg.cooling;

import ipg.cooling.calc.CoolingCalculator;
import ipg.cooling.calc.CoolingRequest;
import ipg.cooling.calc.CoolingResult;
import ipg.cooling.calc.DesignCurves;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.ToDoubleFunction;

public class ChartsController {
    @FunctionalInterface
    public interface InputSource {
        CoolingRequest read();
    }

    @FXML private GridPane chartsGrid;

    private final CoolingCalculator calculator = new CoolingCalculator();
    private InputSource inputSource;

    private ChartPane htcChart;
    private ChartPane pressureChart;
    private ChartPane wallChart;
    private ChartPane riseChart;
    private ChartPane tradeoffChart;

    @FXML
    public void initialize() {
        pressureChart = addChart(0, 0);
        htcChart = addChart(1, 0);
        wallChart = addChart(2, 0);
        riseChart = addChart(0, 1);
        tradeoffChart = addChart(1, 1);
    }

    public void setInputSource(InputSource inputSource) {
        this.inputSource = inputSource;
    }

    public void applyI18n() {
        labelAxes();
    }

    public void plotIfPossible() {
        if (inputSource == null) {
            return;
        }
        try {
            CoolingRequest request = inputSource.read();
            if (request == null) {
                return;
            }
            plot(request);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // charts stay empty until Calculate succeeds
        }
    }

    private void plot(CoolingRequest request) {
        double[] range = pressureRange(request);
        List<DesignCurves.Sample> samples = DesignCurves.sweep(
                calculator, request, range[0], range[1], DesignCurves.DEFAULT_POINTS);
        if (samples.isEmpty()) {
            return;
        }
        CoolingResult current = calculator.evaluateAtPressure(request, request.chillerInletPressurePa());
        double currentPressure = current.inletPressurePa() / 1e5;
        double currentFlow = current.volumeFlowM3s() * 60_000.0;

        labelAxes();
        fillXy(
                pressureChart.chart,
                samples,
                currentFlow,
                currentPressure,
                r -> r.volumeFlowM3s() * 60_000.0,
                r -> r.inletPressurePa() / 1e5
        );
        fill(htcChart.chart, samples, currentPressure, current.heatTransferCoeffWm2K(), CoolingResult::heatTransferCoeffWm2K);
        fill(wallChart.chart, samples, currentPressure, current.outerWallTempC(), CoolingResult::outerWallTempC);
        fill(riseChart.chart, samples, currentPressure, current.waterRiseK(), CoolingResult::waterRiseK);
        fillXy(
                tradeoffChart.chart,
                samples,
                currentFlow,
                current.heatTransferCoeffWm2K(),
                r -> r.volumeFlowM3s() * 60_000.0,
                CoolingResult::heatTransferCoeffWm2K
        );
    }

    private void labelAxes() {
        if (htcChart == null) {
            return;
        }
        String pressure = I18n.t("axis.PRESSURE");
        String flow = I18n.t("axis.FLOW");
        setChart(pressureChart, "chart.flow", "chart.formula.flow", flow, "axis.PRESSURE");
        setChart(htcChart, "chart.htc", "chart.formula.htc", pressure, "axis.htc");
        setChart(wallChart, "chart.wall", "chart.formula.wall", pressure, "axis.wall");
        setChart(riseChart, "chart.rise", "chart.formula.rise", pressure, "axis.rise");
        setChart(tradeoffChart, "chart.tradeoff", "chart.formula.tradeoff", flow, "axis.htc");
    }

    private static void setChart(
            ChartPane pane, String titleKey, String formulaKey, String xLabel, String yKey
    ) {
        pane.chart.setTitle(I18n.t(titleKey));
        Formula.set(pane.formula, I18n.t(formulaKey));
        pane.chart.getXAxis().setLabel(Formula.compact(xLabel));
        pane.chart.getYAxis().setLabel(Formula.compact(I18n.t(yKey)));
    }

    private ChartPane addChart(int column, int row) {
        NumberAxis xAxis = numberedAxis();
        NumberAxis yAxis = numberedAxis();
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(true);
        chart.setMinHeight(160);
        chart.setPrefHeight(220);
        chart.setMaxHeight(Double.MAX_VALUE);
        chart.getStyleClass().add("design-chart");

        TextFlow formula = Formula.flow("");
        formula.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(4, formula, chart);
        box.getStyleClass().add("chart-card");
        VBox.setVgrow(chart, Priority.ALWAYS);
        GridPane.setHgrow(box, Priority.ALWAYS);
        GridPane.setVgrow(box, Priority.ALWAYS);
        chartsGrid.add(box, column, row);
        return new ChartPane(chart, formula);
    }

    private record ChartPane(LineChart<Number, Number> chart, TextFlow formula) {
    }

    private static NumberAxis numberedAxis() {
        NumberAxis axis = new NumberAxis();
        axis.setAutoRanging(true);
        axis.setForceZeroInRange(false);
        axis.setAnimated(false);
        axis.setTickMarkVisible(true);
        axis.setMinorTickVisible(true);
        axis.setMinorTickCount(4);
        axis.setTickLabelsVisible(true);
        axis.setTickLabelGap(6);
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                if (value == null) {
                    return "";
                }
                double v = value.doubleValue();
                if (Math.abs(v) >= 100) {
                    return String.format("%.0f", v);
                }
                if (Math.abs(v) >= 10) {
                    return String.format("%.1f", v);
                }
                return String.format("%.2f", v);
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        });
        return axis;
    }

    private static void fill(
            LineChart<Number, Number> chart,
            List<DesignCurves.Sample> samples,
            double currentX,
            double currentY,
            ToDoubleFunction<CoolingResult> yFn
    ) {
        fillXy(chart, samples, currentX, currentY, r -> r.inletPressurePa() / 1e5, yFn);
    }

    private static void fillXy(
            LineChart<Number, Number> chart,
            List<DesignCurves.Sample> samples,
            double currentX,
            double currentY,
            ToDoubleFunction<CoolingResult> xFn,
            ToDoubleFunction<CoolingResult> yFn
    ) {
        XYChart.Series<Number, Number> curve = new XYChart.Series<>();
        for (DesignCurves.Sample sample : samples) {
            curve.getData().add(new XYChart.Data<>(
                    xFn.applyAsDouble(sample.result()),
                    yFn.applyAsDouble(sample.result())
            ));
        }
        curve.getData().sort((a, b) -> Double.compare(a.getXValue().doubleValue(), b.getXValue().doubleValue()));
        chart.getData().setAll(curve, mark(currentX, currentY));
    }

    private static XYChart.Series<Number, Number> mark(double x, double y) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        XYChart.Data<Number, Number> point = new XYChart.Data<>(x, y);
        Circle dot = new Circle(6, Color.web("#067647"));
        dot.setStroke(Color.web("#12324d"));
        dot.setStrokeWidth(1);
        point.setNode(dot);
        series.getData().add(point);
        return series;
    }

    private static double[] pressureRange(CoolingRequest request) {
        double p = request.chillerInletPressurePa();
        double lo = Math.max(0.5e5, p * 0.5);
        double hi = Math.max(p * 1.6, 5e5);
        return new double[]{lo, hi};
    }

}
