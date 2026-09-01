package ipg.cooling;

import ipg.cooling.calc.CoolingCalculator;
import ipg.cooling.calc.CoolingRequest;
import ipg.cooling.calc.CoolingResult;
import ipg.cooling.calc.OptimizerOutcome;
import ipg.cooling.calc.OptimizerSettings;
import ipg.cooling.calc.TubeMaterial;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.text.NumberFormat;

public class ResultController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private MenuButton languageButton;
    @FXML private Menu helpMenu;
    @FXML private MenuItem helpTopicMenuItem;
    @FXML private MenuItem aboutMenuItem;
    @FXML private TabPane mainTabs;
    @FXML private Tab calcTab;
    @FXML private Tab chartsTab;
    @FXML private ChartsController chartsController;
    @FXML private Label inputsTitleLabel;
    @FXML private Label autoHintLabel;
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
    @FXML private Label materialLabel;
    @FXML private ComboBox<TubeMaterial> materialBox;
    @FXML private Label wallThicknessLabel;
    @FXML private TextField wallThicknessField;
    @FXML private Label maxPressureLabel;
    @FXML private TextField maxPressureField;
    @FXML private Label bendsLabel;
    @FXML private TextField bendsField;
    @FXML private Label bendRadiusLabel;
    @FXML private TextField bendRadiusField;
    @FXML private Label optimizerTitleLabel;
    @FXML private Label optimizerHintLabel;
    @FXML private Label minHeaderLabel;
    @FXML private Label maxHeaderLabel;
    @FXML private Label searchHeaderLabel;
    @FXML private Label innerDiameterLabel;
    @FXML private TextField innerDiameterField;
    @FXML private TextField diameterMaxField;
    @FXML private CheckBox varyDiameterBox;
    @FXML private Label lengthLabel;
    @FXML private TextField lengthField;
    @FXML private TextField lengthMaxField;
    @FXML private CheckBox varyLengthBox;
    @FXML private Label flowLabel;
    @FXML private TextField flowField;
    @FXML private TextField flowMaxField;
    @FXML private CheckBox varyFlowBox;
    @FXML private Label iterationsLabel;
    @FXML private TextField iterationsField;
    @FXML private Button calculateButton;
    @FXML private Button optimizeButton;
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
    @FXML private Label innerDiameterResultLabel;
    @FXML private Label innerDiameterResult;
    @FXML private Label outerDiameterResultLabel;
    @FXML private Label outerDiameterResult;
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
    @FXML private Label efficiencyResultLabel;
    @FXML private Label efficiencyResult;
    @FXML private Label pressureResultLabel;
    @FXML private Label pressureResult;
    @FXML private Label serpentineResultLabel;
    @FXML private Label serpentineResult;
    @FXML private Label localLossResultLabel;
    @FXML private Label localLossResult;
    @FXML private Label resistanceResultLabel;
    @FXML private Label resistanceResult;
    @FXML private Label evaluatedResultLabel;
    @FXML private Label evaluatedResult;
    @FXML private Label feasibleResultLabel;
    @FXML private Label feasibleResult;
    @FXML private Label recommendationTitleLabel;
    @FXML private TextArea recommendationArea;

    private final CoolingCalculator calculator = new CoolingCalculator();
    private CoolingRequest lastRequest;
    private OptimizerSettings lastOptimizerSettings;

    @FXML
    public void initialize() {
        materialBox.setConverter(materialConverter());
        materialBox.getItems().setAll(TubeMaterial.values());
        materialBox.getSelectionModel().select(TubeMaterial.STAINLESS_STEEL);
        for (AppLanguage language : AppLanguage.values()) {
            MenuItem item = new MenuItem(language.displayName());
            item.setOnAction(event -> switchLanguage(language));
            languageButton.getItems().add(item);
        }
        applyI18n();
        if (chartsController != null) {
            chartsController.setInputSource(this::readChartInput);
            mainTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, tab) -> {
                if (tab == chartsTab) {
                    chartsController.plotIfPossible();
                }
            });
        }
        Platform.runLater(this::updateWindowTitle);
    }

    @FXML
    private void onHelp() {
        Window owner = titleLabel.getScene() != null ? titleLabel.getScene().getWindow() : null;
        HelpWindow.show(owner);
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, I18n.t("about.body", AppVersion.display(), AppVersion.author()), ButtonType.OK);
        alert.setTitle(I18n.t("about.title"));
        alert.setHeaderText(I18n.t("about.header"));
        alert.initOwner(titleLabel.getScene() != null ? titleLabel.getScene().getWindow() : null);
        alert.setResizable(true);
        alert.getDialogPane().setMinWidth(560);
        alert.getDialogPane().setPrefWidth(560);
        alert.getDialogPane().setMinHeight(320);
        alert.getDialogPane().setPrefHeight(360);
        if (alert.getDialogPane().lookup(".content.label") instanceof Label content) {
            content.setWrapText(true);
            content.setPrefWidth(500);
            content.setMinHeight(160);
        }
        alert.showAndWait();
    }

    @FXML
    protected void onCalculateButtonClick() {
        try {
            lastRequest = readRequest();
            lastOptimizerSettings = null;
            CoolingResult result = calculator.calculate(lastRequest);
            showResult(result);
            evaluatedResult.setText("");
            feasibleResult.setText("");
            setStatus(I18n.t("status.done"), false);
        } catch (IllegalArgumentException | NullPointerException ex) {
            lastRequest = null;
            lastOptimizerSettings = null;
            setStatus(ex.getMessage() != null ? ex.getMessage() : I18n.t("status.checkInputs"), true);
        }
    }

    @FXML
    protected void onOptimizeButtonClick() {
        try {
            lastRequest = readRequest();
            lastOptimizerSettings = readOptimizerSettings();
            OptimizerOutcome outcome = calculator.optimize(lastRequest, lastOptimizerSettings);
            showOutcome(outcome);
            setStatus(I18n.t("status.optimized", outcome.evaluated(), outcome.feasible()), false);
        } catch (IllegalArgumentException | NullPointerException ex) {
            lastRequest = null;
            lastOptimizerSettings = null;
            setStatus(ex.getMessage() != null ? ex.getMessage() : I18n.t("status.checkInputs"), true);
        }
    }

    private void switchLanguage(AppLanguage language) {
        I18n.setLanguage(language);
        applyI18n();
        if (lastOptimizerSettings != null && lastRequest != null) {
            OptimizerOutcome outcome = calculator.optimize(lastRequest, lastOptimizerSettings);
            showOutcome(outcome);
            setStatus(I18n.t("status.optimized", outcome.evaluated(), outcome.feasible()), false);
        } else if (lastRequest != null) {
            showResult(calculator.calculate(lastRequest));
            evaluatedResult.setText("");
            feasibleResult.setText("");
            setStatus(I18n.t("status.done"), false);
        }
    }

    private void applyI18n() {
        languageButton.setText(I18n.language().code());
        helpMenu.setText(I18n.t("menu.help"));
        helpTopicMenuItem.setText(I18n.t("menu.methodology"));
        aboutMenuItem.setText(I18n.t("menu.about"));
        HelpWindow.applyI18n();
        calcTab.setText(I18n.t("tab.calculator"));
        chartsTab.setText(I18n.t("tab.charts"));
        if (chartsController != null) {
            chartsController.applyI18n();
            if (mainTabs.getSelectionModel().getSelectedItem() == chartsTab) {
                chartsController.plotIfPossible();
            }
        }
        titleLabel.setText(I18n.t("title"));
        subtitleLabel.setText(I18n.t("subtitle"));
        inputsTitleLabel.setText(I18n.t("section.inputs"));
        autoHintLabel.setText(I18n.t("hint.auto"));
        laserPowerLabel.setText(I18n.t("label.laserPower"));
        efficiencyLabel.setText(I18n.t("label.efficiency"));
        powerFactorLabel.setText(I18n.t("label.powerFactor"));
        inletTempLabel.setText(I18n.t("label.inletTemp"));
        maxWallTempLabel.setText(I18n.t("label.maxWallTemp"));
        maxWaterRiseLabel.setText(I18n.t("label.maxWaterRise"));
        materialLabel.setText(I18n.t("label.material"));
        wallThicknessLabel.setText(I18n.t("label.wallThickness"));
        maxPressureLabel.setText(I18n.t("label.maxPressure"));
        bendsLabel.setText(I18n.t("label.bends"));
        bendRadiusLabel.setText(I18n.t("label.bendRadius"));
        optimizerTitleLabel.setText(I18n.t("section.optimizer"));
        optimizerHintLabel.setText(I18n.t("hint.optimizer"));
        minHeaderLabel.setText(I18n.t("opt.min"));
        maxHeaderLabel.setText(I18n.t("opt.max"));
        searchHeaderLabel.setText(I18n.t("opt.search"));
        innerDiameterLabel.setText(I18n.t("label.innerDiameter"));
        lengthLabel.setText(I18n.t("label.length"));
        flowLabel.setText(I18n.t("label.flow"));
        iterationsLabel.setText(I18n.t("opt.iterations"));
        calculateButton.setText(I18n.t("button.calculate"));
        optimizeButton.setText(I18n.t("button.optimize"));
        resultsTitleLabel.setText(I18n.t("section.results"));
        maxPowerResultLabel.setText(I18n.t("result.maxPower"));
        apparentPowerResultLabel.setText(I18n.t("result.apparentPower"));
        chillerResultLabel.setText(I18n.t("result.chiller"));
        recommendedFlowResultLabel.setText(I18n.t("result.recommendedFlow"));
        channelResultsTitleLabel.setText(I18n.t("section.heatSink"));
        innerDiameterResultLabel.setText(I18n.t("result.innerDiameter"));
        outerDiameterResultLabel.setText(I18n.t("result.outerDiameter"));
        lengthResultLabel.setText(I18n.t("result.length"));
        flowResultLabel.setText(I18n.t("result.flow"));
        velocityResultLabel.setText(I18n.t("result.velocity"));
        reynoldsResultLabel.setText(I18n.t("result.reynolds"));
        regimeResultLabel.setText(I18n.t("result.regime"));
        htcResultLabel.setText(I18n.t("result.htc"));
        waterRiseResultLabel.setText(I18n.t("result.waterRise"));
        outletTempResultLabel.setText(I18n.t("result.outletTemp"));
        wallTempResultLabel.setText(I18n.t("result.wallTemp"));
        efficiencyResultLabel.setText(I18n.t("result.efficiency"));
        pressureResultLabel.setText(I18n.t("result.pressure"));
        serpentineResultLabel.setText(I18n.t("result.serpentine"));
        localLossResultLabel.setText(I18n.t("result.localLoss"));
        resistanceResultLabel.setText(I18n.t("result.resistance"));
        evaluatedResultLabel.setText(I18n.t("result.evaluated"));
        feasibleResultLabel.setText(I18n.t("result.feasible"));
        recommendationTitleLabel.setText(I18n.t("section.recommendation"));
        refreshMaterialBox();
        updateWindowTitle();
    }

    private void refreshMaterialBox() {
        TubeMaterial selected = materialBox.getValue();
        materialBox.setConverter(materialConverter());
        materialBox.getItems().setAll(TubeMaterial.values());
        materialBox.setValue(selected);
    }

    private void updateWindowTitle() {
        if (titleLabel.getScene() != null && titleLabel.getScene().getWindow() instanceof Stage stage) {
            stage.setTitle(I18n.t("window.title"));
        }
    }

    private CoolingRequest readRequest() {
        return new CoolingRequest(
                required("label.laserPower", laserPowerField),
                required("label.efficiency", efficiencyField) / 100.0,
                required("label.powerFactor", powerFactorField),
                required("label.inletTemp", inletTempField),
                required("label.maxWallTemp", maxWallTempField),
                required("label.maxWaterRise", maxWaterRiseField),
                materialBox.getValue(),
                required("label.wallThickness", wallThicknessField) / 1000.0,
                required("label.innerDiameter", innerDiameterField) / 1000.0,
                required("label.length", lengthField) / 1000.0,
                required("label.flow", flowField) / 60_000.0,
                required("label.maxPressure", maxPressureField) * 1e5,
                (int) Math.round(required("label.bends", bendsField)),
                required("label.bendRadius", bendRadiusField) / 1000.0
        );
    }

    private ChartsController.Input readChartInput() {
        CoolingRequest request = readRequest();
        return new ChartsController.Input(
                request,
                required("opt.min", innerDiameterField) / 1000.0,
                required("opt.max", diameterMaxField) / 1000.0,
                required("opt.min", lengthField) / 1000.0,
                required("opt.max", lengthMaxField) / 1000.0,
                required("opt.min", flowField) / 60_000.0,
                required("opt.max", flowMaxField) / 60_000.0
        );
    }

    private OptimizerSettings readOptimizerSettings() {
        return new OptimizerSettings(
                required("opt.min", innerDiameterField) / 1000.0,
                required("opt.max", diameterMaxField) / 1000.0,
                varyDiameterBox.isSelected(),
                required("opt.min", lengthField) / 1000.0,
                required("opt.max", lengthMaxField) / 1000.0,
                varyLengthBox.isSelected(),
                required("opt.min", flowField) / 60_000.0,
                required("opt.max", flowMaxField) / 60_000.0,
                varyFlowBox.isSelected(),
                (int) Math.round(required("opt.iterations", iterationsField))
        );
    }

    private void showOutcome(OptimizerOutcome outcome) {
        showResult(outcome.best());
        evaluatedResult.setText(format(outcome.evaluated(), 0, null));
        feasibleResult.setText(format(outcome.feasible(), 0, null));
    }

    private void showResult(CoolingResult result) {
        maxPowerResult.setText(format(result.maxPowerConsumptionW() / 1000.0, 1, "unit.kw"));
        apparentPowerResult.setText(format(result.apparentPowerVa() / 1000.0, 1, "unit.kva"));
        chillerResult.setText(format(result.chillerCapacityW() / 1000.0, 1, "unit.kw"));
        recommendedFlowResult.setText(format(result.recommendedFlowM3s() * 60_000.0, 0, "unit.lmin"));
        innerDiameterResult.setText(format(result.innerDiameterM() * 1000.0, 1, "unit.mm"));
        outerDiameterResult.setText(format(result.outerDiameterM() * 1000.0, 1, "unit.mm"));
        lengthResult.setText(format(result.lengthM() * 1000.0, 0, "unit.mm"));
        flowResult.setText(format(result.volumeFlowM3s() * 60_000.0, 2, "unit.lmin"));
        velocityResult.setText(format(result.velocityMps(), 2, "unit.mps"));
        reynoldsResult.setText(format(result.reynolds(), 0, null));
        regimeResult.setText(I18n.t("regime." + result.regime().name()));
        htcResult.setText(format(result.heatTransferCoeffWm2K(), 0, "unit.wm2k"));
        waterRiseResult.setText(format(result.waterRiseK(), 2, "unit.celsius"));
        outletTempResult.setText(format(result.outletTempC(), 1, "unit.celsius"));
        wallTempResult.setText(format(result.outerWallTempC(), 1, "unit.celsius"));
        efficiencyResult.setText(format(result.coolingConductanceWPerK(), 2, "unit.wPerK"));
        pressureResult.setText(format(result.pressureDropPa() / 1e5, 3, "unit.bar"));
        if (result.uBends() <= 0) {
            serpentineResult.setText(I18n.t("result.straightChannel"));
        } else {
            serpentineResult.setText(I18n.t(
                    "result.serpentineValue",
                    result.uBends(),
                    result.bendRadiusM() * 1000.0
            ));
        }
        localLossResult.setText(format(result.localLossK(), 2, null));
        resistanceResult.setText(format(result.thermalResistanceKw() * 1000.0, 2, "unit.kPerKw"));
        recommendationArea.setText(result.recommendation());
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-ok", "status-error");
        statusLabel.getStyleClass().add(error ? "status-error" : "status-ok");
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

    private static StringConverter<TubeMaterial> materialConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(TubeMaterial material) {
                return material == null ? "" : I18n.t("material." + material.name());
            }

            @Override
            public TubeMaterial fromString(String string) {
                return null;
            }
        };
    }
}
