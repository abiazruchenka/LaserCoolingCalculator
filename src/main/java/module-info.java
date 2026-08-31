module ipg.cooling {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    opens ipg.cooling to javafx.fxml;
    exports ipg.cooling;
}
