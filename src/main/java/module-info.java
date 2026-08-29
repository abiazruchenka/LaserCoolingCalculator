module ipg.coolingcalculator.lasercoolingcalculator {
    requires javafx.controls;
    requires javafx.fxml;


    opens ipg.coolingcalculator.lasercoolingcalculator to javafx.fxml;
    exports ipg.coolingcalculator.lasercoolingcalculator;
}