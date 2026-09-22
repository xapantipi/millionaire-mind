package millionairemind.gui.screens;

import millionairemind.gui.GameUI;

import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class PlayUI {

    private final GameUI gameUI;

    public PlayUI(GameUI gameUI) {
        this.gameUI = gameUI;
    }

    public Node build() {

        Label heading =
                new Label("PLAY");

        heading
                .getStyleClass()
                .add("screen-heading");

        Button quitGame =
                new Button("QUIT GAME");

        quitGame
                .getStyleClass()
                .add("menu-button");

        quitGame.setFocusTraversable(false);

        quitGame.setOnAction(
                event -> attemptQuitGame()
        );

        VBox content =
                new VBox(
                        18,
                        heading,
                        quitGame
                );

        content.setAlignment(Pos.CENTER);

        content.getStyleClass()
               .add("screen-root");

        var scene =
                gameUI.getScene();

        quitGame
                .prefWidthProperty()
                .bind(
                        Bindings.createDoubleBinding(

                                () -> clamp(
                                        scene.getWidth() * 0.18,
                                        190,
                                        390
                                ),

                                scene.widthProperty()
                        )
                );

        heading
                .styleProperty()
                .bind(
                        Bindings.createStringBinding(

                                () ->
                                        "-fx-font-size: "
                                        + clamp(
                                                scene.getHeight()
                                                        * 0.055,
                                                24,
                                                58
                                        )
                                        + "px;",

                                scene.heightProperty()
                        )
                );

        quitGame
                .styleProperty()
                .bind(
                        Bindings.createStringBinding(

                                () ->
                                        "-fx-font-size: "
                                        + clamp(
                                                scene.getHeight()
                                                        * 0.022,
                                                16,
                                                28
                                        )
                                        + "px;",

                                scene.heightProperty()
                        )
                );

        return content;
    }

    private void attemptQuitGame() {

        Alert confirm =
                new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Are you sure you want to leave the game?",
                        ButtonType.YES,
                        ButtonType.NO
                );

        confirm.setTitle("Quit Game");

        confirm.setHeaderText("Quit Game");

        confirm.initOwner(
                gameUI.getStage()
        );

        Optional<ButtonType> result =
                confirm.showAndWait();

        if (result.isPresent()
                && result.get() == ButtonType.YES) {

            /*
             * There is no GUI game state yet.
             * Simply return to the title screen.
             */
            gameUI.showTitle();
        }
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(max, value)
        );
    }
}