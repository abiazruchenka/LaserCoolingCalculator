package ipg.cooling;

import ipg.cooling.calc.CoolingCalculator;
import ipg.cooling.calc.CoolingRequest;
import ipg.cooling.calc.CoolingResult;
import ipg.cooling.catalog.PlateCatalog;
import ipg.cooling.catalog.PlateDefinition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.text.NumberFormat;

public class ResultController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private MenuButton languageButton;
    @FXML private MenuButton helpButton;
    @FXML private MenuItem helpTopicMenuItem;
    @FXML private MenuItem aboutMenuItem;
    @FXML private Button calcTabButton;
    @FXML private Button chartsTabButton;
    @FXML private Button diagramTabButton;
    @FXML private GridPane symbolsPane;
    @FXML private TabPane mainTabs;
    @FXML private Tab calcTab;
    @FXML private Tab chartsTab;
    @FXML private Tab diagramTab;
    @FXML private ScrollPane diagramScroll;
    @FXML private DiagramView diagramView;
    @FXML private Label diagramTitleLabel;
    @FXML private Label diagramHintLabel;
    @FXML private ChartsController chartsController;
    @FXML private Label inputsTitleLabel;
    @FXML private Label laserPowerLabel;
    @FXML private TextField laserPowerField;
    @FXML private Label efficiencyLabel;
    @FXML private TextField efficiencyField;
    @FXML private Label powerFactorLabel;
    @FXML private TextField powerFactorField;
    @FXML private Label inletTempLabel;
    @FXML private TextField inletTempField;
    @FXML private Label maxWallTempLabel;
    @FXML private TextField maxWallTempField;
    @FXML private Label maxWaterRiseLabel;
    @FXML private TextField maxWaterRiseField;
    @FXML private Label plateTitleLabel;
    @FXML private Label plateSelectLabel;
    @FXML private ComboBox<PlateDefinition> plateBox;
    @FXML private TextArea plateSummaryArea;
    @FXML private Label circuitLabel;
    @FXML private ComboBox<CircuitLoop> circuitBox;
    @FXML private Label outletPressureLabel;
    @FXML private TextField outletPressureField;
    @FXML private Label chillerPressureLabel;
    @FXML private TextField chillerPressureField;
    @FXML private Button calculateButton;
    @FXML private Label statusLabel;
    @FXML private Label resultsTitleLabel;
    @FXML private Label maxPowerResultLabel;
    @FXML private Label maxPowerResult;
    @FXML private Label apparentPowerResultLabel;
    @FXML private Label apparentPowerResult;
    @FXML private Label chillerResultLabel;
    @FXML private Label chillerResult;
    @FXML private Label recommendedFlowResultLabel;
    @FXML private Label recommendedFlowResult;
    @FXML private Label channelResultsTitleLabel;
    @FXML private Label lengthResultLabel;
    @FXML private Label lengthResult;
    @FXML private Label flowResultLabel;
    @FXML private Label flowResult;
    @FXML private Label velocityResultLabel;
    @FXML private Label velocityResult;
    @FXML private Label reynoldsResultLabel;
    @FXML private Label reynoldsResult;
    @FXML private Label regimeResultLabel;
    @FXML private Label regimeResult;
    @FXML private Label htcResultLabel;
    @FXML private Label htcResult;
    @FXML private Label waterRiseResultLabel;
    @FXML private Label waterRiseResult;
    @FXML private Label outletTempResultLabel;
    @FXML private Label outletTempResult;
    @FXML private Label wallTempResultLabel;
    @FXML private Label wallTempResult;
    @FXML private Label plateTempResultLabel;
    @FXML private Label plateTempResult;
    @FXML private Label timeConstantResultLabel;
    @FXML private Label timeConstantResult;
    @FXML private Label pressureResultLabel;
    @FXML private Label pressureResult;
    @FXML private Label localLossResultLabel;
    @FXML private Label localLossResult;

    private final CoolingCalculator calculator = new CoolingCalculator();
    private PlateCatalog plateCatalog;
    private CoolingRequest lastRequest;

    @FXML
    public void initialize() {
        circuitBox.setConverter(circuitConverter());
        circuitBox.getItems().setAll(CircuitLoop.values());
        circuitBox.getSelectionModel().select(CircuitLoop.CLOSED_CHILLER);
        circuitBox.valueProperty().addListener((obs, oldLoop, loop) -> updateCircuitFields());
        try {
            loadPlateCatalog();
        } catch (RuntimeException ex) {
            setStatus(ex.getMessage() != null ? ex.getMessage() : I18n.t("error.catalog"), true);
        }
        plateBox.valueProperty().addListener((obs, oldPlate, plate) -> updatePlateSummary(plate));
        for (AppLanguage language : AppLanguage.values()) {
            MenuItem item = new MenuItem(language.displayName());
            item.setOnAction(event -> switchLanguage(language));
            languageButton.getItems().add(item);
        }
        if (chartsController != null) {
            chartsController.setInputSource(this::lastCalculatedRequest);
        }
        mainTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, tab) -> {
            updateTabSwitcher();
            if (tab == chartsTab && chartsController != null) {
                chartsController.plotIfPossible();
            }
            if (tab == diagramTab) {
                Platform.runLater(this::fitDiagram);
            }
        });
        if (diagramScroll != null && diagramView != null) {
            diagramScroll.viewportBoundsProperty().addListener((obs, oldBounds, bounds) ->
                    Platform.runLater(this::fitDiagram));
        }
        applyI18n();
        Platform.runLater(() -> {
            hideNativeTabHeader();
            updateWindowTitle();
        });
    }

    @FXML
    private void showCalculatorTab() {
        mainTabs.getSelectionModel().select(calcTab);
        updateTabSwitcher();
    }

    @FXML
    private void showChartsTab() {
        mainTabs.getSelectionModel().select(chartsTab);
        updateTabSwitcher();
    }

    @FXML
    private void showDiagramTab() {
        mainTabs.getSelectionModel().select(diagramTab);
        updateTabSwitcher();
        Platform.runLater(this::fitDiagram);
    }

    private void fitDiagram() {
        if (diagramScroll == null || diagramView == null) {
            return;
        }
        var bounds = diagramScroll.getViewportBounds();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return;
        }
        double used = 28;
        if (diagramScroll.getContent() instanceof VBox box) {
            used = box.getPadding().getTop() + box.getPadding().getBottom();
            int others = 0;
            for (var child : box.getChildren()) {
                if (child == diagramView) {
                    continue;
                }
                used += Math.max(child.getLayoutBounds().getHeight(), child.prefHeight(bounds.getWidth()));
                others++;
            }
            used += box.getSpacing() * others;
        }
        double width = Math.max(1, bounds.getWidth() - 4);
        double height = Math.max(220, bounds.getHeight() - used);
        diagramView.setMinSize(0, 0);
        diagramView.setPrefSize(width, height);
        diagramView.setMaxSize(width, height);
    }

    @FXML
    private void onHelp() {
        Window owner = titleLabel.getScene() != null ? titleLabel.getScene().getWindow() : null;
        HelpWindow.show(owner);
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
        alert.setTitle(I18n.t("about.title"));
        alert.setHeaderText(I18n.t("about.header"));
        alert.initOwner(titleLabel.getScene() != null ? titleLabel.getScene().getWindow() : null);
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(560);
        alert.getDialogPane().setPrefWidth(560);
        if (titleLabel.getScene() != null) {
            alert.getDialogPane().getStylesheets().addAll(titleLabel.getScene().getStylesheets());
        }
        alert.getDialogPane().setContent(aboutBody(I18n.t(
                "about.body", AppVersion.display(), AppVersion.author())));
        alert.setOnShown(event -> alert.getDialogPane().getScene().getWindow().sizeToScene());
        alert.showAndWait();
    }

    private static VBox aboutBody(String body) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(8, 4, 12, 4));
        box.setMaxWidth(508);
        for (String line : body.split("\n", -1)) {
            if (line.isEmpty()) {
                Label spacer = new Label();
                spacer.setMinHeight(6);
                box.getChildren().add(spacer);
                continue;
            }
            TextFlow flow = Formula.flow(line);
            flow.getStyleClass().add("about-body");
            flow.setMaxWidth(500);
            box.getChildren().add(flow);
        }
        return box;
    }

    @FXML
    protected void onCalculateButtonClick() {
        try {
            lastRequest = readRequest();
            CoolingResult result = calculator.calculate(lastRequest);
            showResult(result);
            if (chartsController != null) {
                chartsController.plotIfPossible();
            }
            setStatus(I18n.t("status.done"), false);
        } catch (IllegalArgumentException | NullPointerException ex) {
            lastRequest = null;
            setStatus(ex.getMessage() != null ? ex.getMessage() : I18n.t("status.checkInputs"), true);
        }
    }

    private void switchLanguage(AppLanguage language) {
        I18n.setLanguage(language);
        applyI18n();
        if (lastRequest != null) {
            showResult(calculator.calculate(lastRequest));
            if (chartsController != null) {
                chartsController.plotIfPossible();
            }
            setStatus(I18n.t("status.done"), false);
        }
    }

    private void applyI18n() {
        languageButton.setText(I18n.language().code());
        helpButton.setText(I18n.t("menu.help"));
        helpTopicMenuItem.setText(I18n.t("menu.methodology"));
        aboutMenuItem.setText(I18n.t("menu.about"));
        HelpWindow.applyI18n();
        calcTab.setText(I18n.t("tab.calculator"));
        chartsTab.setText(I18n.t("tab.charts"));
        diagramTab.setText(I18n.t("tab.diagram"));
        calcTabButton.setText(I18n.t("tab.calculator"));
        chartsTabButton.setText(I18n.t("tab.charts"));
        diagramTabButton.setText(I18n.t("tab.diagram"));
        diagramTitleLabel.setText(I18n.t("diagram.title"));
        diagramHintLabel.setText(I18n.t("diagram.hint"));
        if (diagramView != null) {
            diagramView.redraw();
            Platform.runLater(this::fitDiagram);
        }
        refreshSymbols();
        updateTabSwitcher();
        if (chartsController != null) {
            chartsController.applyI18n();
            if (mainTabs.getSelectionModel().getSelectedItem() == chartsTab) {
                chartsController.plotIfPossible();
            }
        }
        titleLabel.setText(I18n.t("title"));
        subtitleLabel.setText(I18n.t("subtitle"));
        inputsTitleLabel.setText(I18n.t("section.inputs"));
        laserPowerLabel.setText(I18n.t("label.laserPower"));
        efficiencyLabel.setText(I18n.t("label.efficiency"));
        powerFactorLabel.setText(I18n.t("label.powerFactor"));
        inletTempLabel.setText(I18n.t("label.inletTemp"));
        maxWallTempLabel.setText(I18n.t("label.maxWallTemp"));
        maxWaterRiseLabel.setText(I18n.t("label.maxWaterRise"));
        maxWaterRiseField.setPromptText(I18n.t("prompt.optional"));
        plateTitleLabel.setText(I18n.t("section.plate"));
        plateSelectLabel.setText(I18n.t("label.plate"));
        circuitLabel.setText(I18n.t("label.circuit"));
        outletPressureLabel.setText(I18n.t("label.outletPressure"));
        chillerPressureLabel.setText(I18n.t("label.chillerPressure"));
        calculateButton.setText(I18n.t("button.calculate"));
        resultsTitleLabel.setText(I18n.t("section.results"));
        named(maxPowerResultLabel, "P_max", "result.maxPower");
        named(apparentPowerResultLabel, "S_max", "result.apparentPower");
        named(chillerResultLabel, "Q", "result.chiller");
        named(recommendedFlowResultLabel, null, "result.recommendedFlow");
        channelResultsTitleLabel.setText(I18n.t("section.heatSink"));
        named(lengthResultLabel, "L", "result.length");
        named(flowResultLabel, "V̇", "result.flow");
        named(velocityResultLabel, "v", "result.velocity");
        named(reynoldsResultLabel, "Re", "result.reynolds");
        named(regimeResultLabel, null, "result.regime");
        named(htcResultLabel, "α", "result.htc");
        named(waterRiseResultLabel, "ΔT", "result.waterRise");
        named(outletTempResultLabel, "T_out", "result.outletTemp");
        named(wallTempResultLabel, "T_w", "result.wallTemp");
        named(plateTempResultLabel, "T_Al", "result.plateTemp");
        named(timeConstantResultLabel, "τ", "result.timeConstant");
        named(pressureResultLabel, "ΔP", "result.pressure");
        named(localLossResultLabel, "K", "result.localLoss");
        refreshPlateBox();
        refreshCircuitBox();
        updateCircuitFields();
        updateWindowTitle();
    }

    private void hideNativeTabHeader() {
        var header = mainTabs.lookup(".tab-header-area");
        if (header != null) {
            header.setVisible(false);
            header.setManaged(false);
        }
    }

    private void updateTabSwitcher() {
        Tab selected = mainTabs.getSelectionModel().getSelectedItem();
        boolean charts = selected == chartsTab;
        calcTabButton.getStyleClass().remove("tab-switch-selected");
        chartsTabButton.getStyleClass().remove("tab-switch-selected");
        diagramTabButton.getStyleClass().remove("tab-switch-selected");
        if (charts) {
            chartsTabButton.getStyleClass().add("tab-switch-selected");
        } else if (selected == diagramTab) {
            diagramTabButton.getStyleClass().add("tab-switch-selected");
        } else {
            calcTabButton.getStyleClass().add("tab-switch-selected");
        }
    }

    private void refreshSymbols() {
        HBox[] items = {
                symbol("T_in", "legend.Tin"),
                symbol("T_out", "legend.Tout"),
                symbol("T_b", "legend.Tb"),
                symbol("T_w", "legend.Tw"),
                symbol("T_Al", "legend.TAl"),
                symbol("ΔT", "legend.dT"),
                symbol("τ", "legend.tau"),
                symbol("Q", "legend.Q"),
                symbol("α", "legend.alpha"),
                symbol("R_wall", "legend.Rwall"),
                symbol("P_max", "legend.Pmax"),
                symbol("S_max", "legend.Smax"),
                symbol("η", "legend.eta"),
                symbol("V̇", "legend.Vdot"),
                symbol("v", "legend.v"),
                symbol("ṁ", "legend.mdot"),
                symbol("P_in", "legend.Pin"),
                symbol("P_out", "legend.Pout"),
                symbol("ΔP", "legend.dP"),
                symbol("K", "legend.K"),
                symbol("f", "legend.f"),
                symbol("L", "legend.L"),
                symbol("d", "legend.d"),
                symbol("S", "legend.S"),
                symbol("Re", "legend.Re"),
                symbol("Nu", "legend.Nu"),
                symbol("λ", "legend.lambda"),
                symbol("ρ", "legend.rho"),
                symbol("c_p", "legend.cp")
        };
        symbolsPane.getChildren().clear();
        symbolsPane.getColumnConstraints().setAll(
                percentColumn(), percentColumn(), percentColumn(), percentColumn());
        int columns = 4;
        int rows = (items.length + columns - 1) / columns;
        for (int i = 0; i < items.length; i++) {
            int column = i / rows;
            int row = i % rows;
            GridPane.setHgrow(items[i], Priority.ALWAYS);
            GridPane.setFillWidth(items[i], true);
            symbolsPane.add(items[i], column, row);
        }
    }

    private static ColumnConstraints percentColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(25);
        column.setHgrow(Priority.ALWAYS);
        return column;
    }

    private static HBox symbol(String name, String meaningKey) {
        TextFlow symbol = Formula.flow(name);
        symbol.getStyleClass().add("formula-chip");
        Label meaning = new Label(" — " + I18n.t(meaningKey));
        meaning.getStyleClass().add("symbol-item");
        meaning.setWrapText(true);
        meaning.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(meaning, Priority.ALWAYS);
        HBox box = new HBox(0, symbol, meaning);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static void named(Label label, String symbol, String key) {
        label.setText(I18n.t(key));
        if (symbol == null || symbol.isBlank()) {
            label.setGraphic(null);
            return;
        }
        TextFlow graphic = Formula.flow(symbol);
        graphic.getStyleClass().add("formula-chip");
        label.setGraphic(graphic);
        label.setContentDisplay(ContentDisplay.LEFT);
        label.setGraphicTextGap(8);
    }

    private void refreshCircuitBox() {
        CircuitLoop selected = circuitBox.getValue();
        circuitBox.setConverter(circuitConverter());
        circuitBox.getItems().setAll(CircuitLoop.values());
        circuitBox.setValue(selected != null ? selected : CircuitLoop.CLOSED_CHILLER);
    }

    private void loadPlateCatalog() {
        plateCatalog = PlateCatalog.load();
        plateBox.setConverter(plateConverter());
        plateBox.getItems().setAll(plateCatalog.plates());
        if (!plateCatalog.plates().isEmpty()) {
            plateBox.getSelectionModel().select(plateCatalog.plates().getFirst());
        }
    }

    private void refreshPlateBox() {
        PlateDefinition selected = plateBox.getValue();
        plateBox.setConverter(plateConverter());
        if (plateCatalog != null) {
            plateBox.getItems().setAll(plateCatalog.plates());
            if (selected != null) {
                plateBox.setValue(plateCatalog.byId(selected.id()));
            } else if (!plateCatalog.plates().isEmpty()) {
                plateBox.setValue(plateCatalog.plates().getFirst());
            }
        }
        updatePlateSummary(plateBox.getValue());
    }

    private void updatePlateSummary(PlateDefinition plate) {
        if (plate == null) {
            plateSummaryArea.setText("");
            return;
        }
        plateSummaryArea.setText(plateSummary(plate));
    }

    private CoolingRequest lastCalculatedRequest() {
        return lastRequest;
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String plateSummary(PlateDefinition plate) {
        boolean massFromDrawing = plate.plate().massKg() != null && !plate.plate().massEstimated();
        return I18n.t(
                "plate.summary",
                plate.displayName(),
                plate.plate().widthMm(),
                plate.plate().heightMm(),
                plate.plate().thicknessMm(),
                plate.plate().thicknessEstimated() ? I18n.t("plate.estimated") : "",
                plate.plate().resolvedMassKg(),
                massFromDrawing ? "" : I18n.t("plate.estimated"),
                plate.tube().innerDiameterMm(),
                plate.tube().outerDiameterMm(),
                plate.tube().wallThicknessMm(),
                dash(plate.tube().standard()),
                plate.layout().passes(),
                plate.layout().pitchMm(),
                plate.layout().bendRadiusMm()
        );
    }

    private void updateCircuitFields() {
        boolean closed = circuitBox.getValue() == CircuitLoop.CLOSED_CHILLER;
        outletPressureLabel.setVisible(closed);
        outletPressureLabel.setManaged(closed);
        outletPressureField.setVisible(closed);
        outletPressureField.setManaged(closed);
        if (!closed) {
            outletPressureField.setText("0");
        }
    }

    private void updateWindowTitle() {
        if (titleLabel.getScene() != null && titleLabel.getScene().getWindow() instanceof Stage stage) {
            stage.setTitle(I18n.t("window.title"));
        }
    }

    private CoolingRequest readRequest() {
        PlateDefinition selected = plateBox.getValue();
        if (selected == null) {
            throw new IllegalArgumentException(I18n.t("error.noPlate"));
        }
        var cooling = selected.toCoolingPlate();
        var layout = cooling.fixedLayout();
        int bends = layout != null ? layout.uBends() : Math.max(0, selected.layout().passes() - 1);
        double lengthM = layout != null && layout.developedLengthM() > 0 ? layout.developedLengthM() : 1.0;
        return new CoolingRequest(
                required("label.laserPower", laserPowerField),
                required("label.efficiency", efficiencyField) / 100.0,
                required("label.powerFactor", powerFactorField),
                required("label.inletTemp", inletTempField),
                required("label.maxWallTemp", maxWallTempField),
                optional("label.maxWaterRise", maxWaterRiseField),
                selected.tube().tubeMaterial(),
                selected.tube().wallThicknessMm() / 1000.0,
                selected.tube().innerDiameterMm() / 1000.0,
                lengthM,
                outletPressurePa(),
                required("label.chillerPressure", chillerPressureField) * 1e5,
                bends,
                selected.layout().bendRadiusMm() / 1000.0,
                cooling
        );
    }

    private void showResult(CoolingResult result) {
        maxPowerResult.setText(format(result.maxPowerConsumptionW() / 1000.0, 1, "unit.kw"));
        apparentPowerResult.setText(format(result.apparentPowerVa() / 1000.0, 1, "unit.kva"));
        chillerResult.setText(format(result.chillerCapacityW() / 1000.0, 1, "unit.kw"));
        recommendedFlowResult.setText(format(result.recommendedFlowM3s() * 60_000.0, 0, "unit.lmin"));
        lengthResult.setText(format(result.lengthM() * 1000.0, 0, "unit.mm"));
        flowResult.setText(format(result.volumeFlowM3s() * 60_000.0, 2, "unit.lmin"));
        velocityResult.setText(format(result.velocityMps(), 2, "unit.mps"));
        reynoldsResult.setText(format(result.reynolds(), 0, null));
        regimeResult.setText(I18n.t("regime." + result.regime().name()));
        htcResult.setText(format(result.heatTransferCoeffWm2K(), 0, "unit.wm2k"));
        waterRiseResult.setText(format(result.waterRiseK(), 2, "unit.celsius"));
        outletTempResult.setText(format(result.outletTempC(), 1, "unit.celsius"));
        wallTempResult.setText(format(result.outerWallTempC(), 1, "unit.celsius"));
        plateTempResult.setText(format(result.plateTempC(), 1, "unit.celsius"));
        timeConstantResult.setText(format(result.timeConstantS(), 1, "unit.s"));
        pressureResult.setText(format(result.pressureDropPa() / 1e5, 3, "unit.bar"));
        localLossResult.setText(format(result.localLossK(), 2, null));
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-ok", "status-error");
        statusLabel.getStyleClass().add(error ? "status-error" : "status-ok");
    }

    private static Double optional(String labelKey, TextField field) {
        String raw = field.getText();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parse(labelKey, raw);
    }

    private static double required(String labelKey, TextField field) {
        String raw = field.getText();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(I18n.t("error.required", I18n.t(labelKey)));
        }
        return parse(labelKey, raw);
    }

    private static double parse(String labelKey, String raw) {
        try {
            double value = Double.parseDouble(raw.trim().replace(',', '.').replace(" ", ""));
            if (!Double.isFinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(I18n.t("error.badNumber", I18n.t(labelKey)));
        }
    }

    private static String format(double value, int fractionDigits, String unitKey) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(I18n.locale());
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(fractionDigits);
        String number = numberFormat.format(value);
        return unitKey == null ? number : number + " " + I18n.t(unitKey);
    }

    private double outletPressurePa() {
        if (circuitBox.getValue() != CircuitLoop.CLOSED_CHILLER) {
            return 0.0;
        }
        return required("label.outletPressure", outletPressureField) * 1e5;
    }

    private static StringConverter<PlateDefinition> plateConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(PlateDefinition plate) {
                return plate == null ? "" : plate.displayName();
            }

            @Override
            public PlateDefinition fromString(String string) {
                return null;
            }
        };
    }

    private static StringConverter<CircuitLoop> circuitConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(CircuitLoop loop) {
                return loop == null ? "" : I18n.t("circuit." + loop.name());
            }

            @Override
            public CircuitLoop fromString(String string) {
                return null;
            }
        };
    }

}
