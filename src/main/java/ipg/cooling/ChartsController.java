package ipg.cooling;

import ipg.cooling.calc.CoolingCalculator;
import ipg.cooling.calc.CoolingRequest;
import ipg.cooling.calc.CoolingResult;
import ipg.cooling.calc.DesignCurves;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.ToDoubleFunction;

public class ChartsController {
    @FunctionalInterface
    public interface InputSource {
        Input read();
    }

    public record Input(
            CoolingRequest request,
            double diameterMinM,
            double diameterMaxM,
            double lengthMinM,
            double lengthMaxM,
            double flowMinM3s,
            double flowMaxM3s
    ) {
    }

    @FXML private Label sweepLabel;
    @FXML private ComboBox<DesignCurves.Axis> sweepBox;
    @FXML private Button plotButton;
    @FXML private Label chartsStatusLabel;
    @FXML private GridPane chartsGrid;

    private final CoolingCalculator calculator = new CoolingCalculator();
    private InputSource inputSource;

    private LineChart<Number, Number> htcChart;
    private LineChart<Number, Number> pressureChart;
    private LineChart<Number, Number> wallChart;
    private LineChart<Number, Number> riseChart;
    private LineChart<Number, Number> efficiencyChart;
    private LineChart<Number, Number> tradeoffChart;

    @FXML
    public void initialize() {
        htcChart = addChart(0, 0);
        pressureChart = addChart(1, 0);
        wallChart = addChart(2, 0);
        riseChart = addChart(0, 1);
        efficiencyChart = addChart(1, 1);
        tradeoffChart = addChart(2, 1);
        sweepBox.setConverter(axisConverter());
        sweepBox.getItems().setAll(DesignCurves.Axis.values());
        sweepBox.getSelectionModel().select(DesignCurves.Axis.FLOW);
        sweepBox.getSelectionModel().selectedItemProperty().addListener((obs, oldAxis, axis) -> {
            if (oldAxis != null && axis != null && inputSource != null) {
                plotIfPossible();
            }
        });
        applyI18n();
    }

    public void setInputSource(InputSource inputSource) {
        this.inputSource = inputSource;
    }

    public void applyI18n() {
        sweepLabel.setText(I18n.t("charts.sweep"));
        plotButton.setText(I18n.t("charts.plot"));
        DesignCurves.Axis selected = sweepBox.getValue();
        sweepBox.setConverter(axisConverter());
        sweepBox.getItems().setAll(DesignCurves.Axis.values());
        sweepBox.setValue(selected != null ? selected : DesignCurves.Axis.FLOW);
        labelAxes();
    }

    public void plotIfPossible() {
        if (inputSource == null) {
            return;
        }
        try {
            plot(inputSource.read());
        } catch (IllegalArgumentException | NullPointerException ex) {
            setStatus(ex.getMessage() != null ? ex.getMessage() : I18n.t("status.checkInputs"), true);
        }
    }

    @FXML
    private void onPlot() {
        plotIfPossible();
    }

    void plot(Input input) {
        DesignCurves.Axis axis = sweepBox.getValue() != null ? sweepBox.getValue() : DesignCurves.Axis.FLOW;
        double[] range = sweepRange(axis, input);
        List<DesignCurves.Sample> samples = DesignCurves.sweep(
                calculator, input.request(), axis, range[0], range[1], DesignCurves.DEFAULT_POINTS);
        if (samples.isEmpty()) {
            setStatus(I18n.t("charts.status.empty"), true);
            return;
        }
        CoolingResult current = calculator.evaluate(
                input.request(),
                input.request().innerDiameterM(),
                input.request().lengthM(),
                input.request().volumeFlowM3s()
        );
        double currentX = switch (axis) {
            case FLOW -> current.volumeFlowM3s() * 60_000.0;
            case DIAMETER -> current.innerDiameterM() * 1000.0;
            case LENGTH -> current.lengthM() * 1000.0;
        };

        labelAxes();
        fill(htcChart, samples, currentX, current.heatTransferCoeffWm2K(), CoolingResult::heatTransferCoeffWm2K);
        fill(pressureChart, samples, currentX, current.pressureDropPa() / 1e5, r -> r.pressureDropPa() / 1e5);
        fill(wallChart, samples, currentX, current.outerWallTempC(), CoolingResult::outerWallTempC);
        fill(riseChart, samples, currentX, current.waterRiseK(), CoolingResult::waterRiseK);
        fill(efficiencyChart, samples, currentX, current.coolingConductanceWPerK(), CoolingResult::coolingConductanceWPerK);
        fillTradeoff(tradeoffChart, samples, current);
        setStatus("", false);
    }

    private void labelAxes() {
        if (htcChart == null) {
            return;
        }
        DesignCurves.Axis axis = sweepBox.getValue() != null ? sweepBox.getValue() : DesignCurves.Axis.FLOW;
        String xLabel = I18n.t("axis." + axis.name());
        setChart(htcChart, "chart.htc", xLabel, "axis.htc");
        setChart(pressureChart, "chart.pressure", xLabel, "axis.pressure");
        setChart(wallChart, "chart.wall", xLabel, "axis.wall");
        setChart(riseChart, "chart.rise", xLabel, "axis.rise");
        setChart(efficiencyChart, "chart.efficiency", xLabel, "axis.efficiency");
        setChart(tradeoffChart, "chart.tradeoff", I18n.t("axis.pressure"), "axis.htc");
    }

    private static void setChart(LineChart<Number, Number> chart, String titleKey, String xLabel, String yKey) {
        chart.setTitle(I18n.t(titleKey));
        chart.getXAxis().setLabel(xLabel);
        chart.getYAxis().setLabel(I18n.t(yKey));
    }

    private LineChart<Number, Number> addChart(int column, int row) {
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
        GridPane.setHgrow(chart, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setVgrow(chart, javafx.scene.layout.Priority.ALWAYS);
        chartsGrid.add(chart, column, row);
        return chart;
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
        XYChart.Series<Number, Number> curve = new XYChart.Series<>();
        for (DesignCurves.Sample sample : samples) {
            curve.getData().add(new XYChart.Data<>(sample.x(), yFn.applyAsDouble(sample.result())));
        }
        chart.getData().setAll(curve, mark(currentX, currentY));
    }

    private static void fillTradeoff(
            LineChart<Number, Number> chart,
            List<DesignCurves.Sample> samples,
            CoolingResult current
    ) {
        XYChart.Series<Number, Number> curve = new XYChart.Series<>();
        for (DesignCurves.Sample sample : samples) {
            curve.getData().add(new XYChart.Data<>(
                    sample.result().pressureDropPa() / 1e5,
                    sample.result().heatTransferCoeffWm2K()
            ));
        }
        curve.getData().sort((a, b) -> Double.compare(a.getXValue().doubleValue(), b.getXValue().doubleValue()));
        chart.getData().setAll(curve, mark(current.pressureDropPa() / 1e5, current.heatTransferCoeffWm2K()));
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

    private static double[] sweepRange(DesignCurves.Axis axis, Input input) {
        double min;
        double max;
        double collapseLow;
        double collapseHigh;
        switch (axis) {
            case FLOW -> {
                min = input.flowMinM3s();
                max = input.flowMaxM3s();
                collapseLow = 0.5;
                collapseHigh = 2.0;
            }
            case DIAMETER -> {
                min = input.diameterMinM();
                max = input.diameterMaxM();
                collapseLow = 0.7;
                collapseHigh = 1.3;
            }
            case LENGTH -> {
                min = input.lengthMinM();
                max = input.lengthMaxM();
                collapseLow = 0.6;
                collapseHigh = 1.5;
            }
            default -> throw new IllegalStateException(axis.name());
        }
        double lo = Math.min(min, max);
        double hi = Math.max(min, max);
        if (hi <= lo * 1.02) {
            return new double[]{lo * collapseLow, hi * collapseHigh};
        }
        return new double[]{lo, hi};
    }

    private void setStatus(String message, boolean error) {
        chartsStatusLabel.setText(message);
        chartsStatusLabel.getStyleClass().removeAll("status-ok", "status-error");
        chartsStatusLabel.getStyleClass().add(error ? "status-error" : "status-ok");
    }

    private static StringConverter<DesignCurves.Axis> axisConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(DesignCurves.Axis axis) {
                return axis == null ? "" : I18n.t("charts.sweep." + axis.name());
            }

            @Override
            public DesignCurves.Axis fromString(String string) {
                return null;
            }
        };
    }
}
