package millionairemind.gui;

import millionairemind.gui.screens.InstructionsUI;
import millionairemind.gui.screens.PlayUI;
import millionairemind.gui.screens.TitleUI;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class GameUI extends Application {

    private static final String STYLESHEET =
            "/millionairemind/gui/game.css";

    private final StackPane root = new StackPane();

    private Stage stage;
    private Scene scene;

    @Override
    public void start(Stage primaryStage) {

        stage = primaryStage;

        scene = new Scene(
                root,
                1280,
                720
        );

        var css =
                getClass().getResource(STYLESHEET);

        if (css == null) {
            throw new IllegalStateException(
                    "Missing stylesheet: " + STYLESHEET
            );
        }

        scene.getStylesheets()
             .add(css.toExternalForm());

        stage.setTitle("Millionaire Mind");

        stage.setScene(scene);

        stage.setMinWidth(640);
        stage.setMinHeight(480);

        stage.setOnCloseRequest(
                event -> Platform.exit()
        );

        /*
         * Disable JavaFX's normal fullscreen
         * exit hint.
         */
        stage.setFullScreenExitHint("");

        stage.setFullScreenExitKeyCombination(
                KeyCombination.NO_MATCH
        );

        /*
         * F11 = toggle fullscreen
         * ESC = leave fullscreen
         */
        scene.setOnKeyPressed(event -> {

            switch (event.getCode()) {

                case F11 ->
                        stage.setFullScreen(
                                !stage.isFullScreen()
                        );

                case ESCAPE -> {

                    if (stage.isFullScreen()) {
                        stage.setFullScreen(false);
                    }
                }

                default -> {
                }
            }
        });

        /*
         * Initial screen.
         */
        showTitle();

        stage.show();

        /*
         * Open in fullscreen.
         */
        stage.setFullScreen(true);
    }

    public void showTitle() {

        root.getChildren().setAll(
                new TitleUI(this).build()
        );
    }

    public void showPlay() {

        root.getChildren().setAll(
                new PlayUI(this).build()
        );
    }

    public void showInstructions() {

        root.getChildren().setAll(
                new InstructionsUI(this).build()
        );
    }

    public void quitApplication() {

        Platform.exit();
    }

    public Scene getScene() {

        return scene;
    }

    public Stage getStage() {

        return stage;
    }
}