package ipg.cooling;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class LaserCoolingApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(LaserCoolingApplication.class.getResource("laser-cooling-calculate-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1180, 720);
        stage.setTitle(I18n.t("window.title"));
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }
}
