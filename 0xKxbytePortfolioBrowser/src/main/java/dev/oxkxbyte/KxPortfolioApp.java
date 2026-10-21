package dev.oxkxbyte;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;

import java.net.URI;
import java.util.Objects;

/** Native Windows-decorated portfolio window; not a general-purpose browser. */
public class KxPortfolioApp extends Application {
    private static final String GITHUB = "https://github.com/0xKxbyte";
    private static final String LINK_SCRIPT = """
        (function(){
          if (window.__kxLinksInstalled) return;
          window.__kxLinksInstalled = true;
          document.addEventListener('click', function(event){
            const anchor = event.target.closest('a[href]');
            if (!anchor) return;
            const target = new URL(anchor.getAttribute('href'), document.baseURI);
            if (['https:', 'http:', 'mailto:'].includes(target.protocol)) {
              event.preventDefault();
              event.stopImmediatePropagation();
              window.kxBridge.openExternal(target.href);
            }
          }, true);
          window.open = function(url) {
            if (url) window.kxBridge.openExternal(String(url));
            return null;
          };
        })();
        """;

    public static class ExternalLinks {
        private final javafx.application.HostServices host;
        public ExternalLinks(javafx.application.HostServices host) { this.host = host; }
        public void openExternal(String address) {
            try {
                URI uri = URI.create(address);
                String scheme = uri.getScheme();
                if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)
                        || "mailto".equalsIgnoreCase(scheme)) {
                    Platform.runLater(() -> host.showDocument(uri.toString()));
                }
            } catch (IllegalArgumentException ignored) { }
        }
    }

    @Override
    public void start(Stage stage) {
        // Genuine operating-system title bar and window controls.
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("KxPortfolio Desktop — 0xKxbyte");
        stage.getIcons().add(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/portfolio/assets/oxkxbyte-profile.png"))));

        ExternalLinks links = new ExternalLinks(getHostServices());
        WebView view = new WebView();
        view.setContextMenuEnabled(false);
        WebEngine engine = view.getEngine();
        engine.setJavaScriptEnabled(true);
        String home = Objects.requireNonNull(getClass().getResource("/portfolio/index.html")).toExternalForm();
        String localRoot = home.substring(0, home.lastIndexOf('/') + 1);

        engine.getLoadWorker().stateProperty().addListener((obs, before, state) -> {
            if (state == Worker.State.SUCCEEDED && engine.getLocation().startsWith(localRoot)) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("kxBridge", links);
                engine.executeScript(LINK_SCRIPT);
            }
        });
        engine.locationProperty().addListener((obs, previous, next) -> {
            if (next == null || next.isBlank() || next.equals("about:blank") || next.startsWith(localRoot)) return;
            engine.getLoadWorker().cancel();
            links.openExternal(next);
            Platform.runLater(() -> engine.load(home));
        });

        // Application footer, OUTSIDE the HTML page and the native Windows title bar.
        Label footerLabel = new Label("KxPortfolio Desktop  •  0xKxbyte");
        footerLabel.setStyle("-fx-text-fill: #a6a6b4; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Hyperlink github = new Hyperlink("GitHub ↗");
        github.setStyle("-fx-text-fill: #e9e9f5; -fx-font-size: 12px; -fx-border-color: transparent;");
        github.setOnAction(event -> links.openExternal(GITHUB));
        HBox footer = new HBox(12, footerLabel, spacer, github);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(4, 15, 4, 15));
        footer.setStyle("-fx-background-color: #17151e; -fx-border-color: #302d38; -fx-border-width: 1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setCenter(view);
        root.setBottom(footer);
        stage.setScene(new Scene(root, 1180, 760));
        stage.setMinWidth(650);
        stage.setMinHeight(480);
        stage.show();
        engine.load(home);
    }

    public static void main(String[] args) { launch(args); }
}
