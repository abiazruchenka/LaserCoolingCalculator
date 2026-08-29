package ipg.cooling;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ResultController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onCalculateButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}
