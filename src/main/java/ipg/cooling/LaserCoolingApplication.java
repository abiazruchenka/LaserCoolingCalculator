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
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        stage.setTitle("LaserCooling Calculator");
        stage.setScene(scene);
        stage.show();
    }
}
