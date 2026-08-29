module ipg.cooling {
    requires javafx.controls;
    requires javafx.fxml;

    opens ipg.cooling to javafx.fxml;
    exports ipg.cooling;
}
