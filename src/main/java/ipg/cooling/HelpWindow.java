package ipg.cooling;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class HelpWindow {
    private static Stage stage;
    private static WebView webView;

    private HelpWindow() {
    }

    static void show(Window owner) {
        if (stage == null) {
            webView = new WebView();
            Scene scene = new Scene(new BorderPane(webView), 860, 720);
            stage = new Stage();
            stage.setMinWidth(640);
            stage.setMinHeight(480);
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setScene(scene);
        }
        webView.getEngine().loadContent(html(), "text/html");
        stage.setTitle(I18n.t("help.title"));
        stage.show();
        stage.toFront();
    }

    static void applyI18n() {
        if (stage != null && stage.isShowing()) {
            stage.setTitle(I18n.t("help.title"));
        }
    }

    private static String html() {
        try (InputStream in = HelpWindow.class.getResourceAsStream("help.html")) {
            if (in == null) {
                return fallbackHtml();
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{VERSION}}", AppVersion.display())
                    .replace("{{AUTHOR}}", AppVersion.author());
        } catch (IOException ex) {
            return fallbackHtml();
        }
    }

    private static String fallbackHtml() {
        return "<html><body><p>" + I18n.t("help.missing") + "</p></body></html>";
    }
}
