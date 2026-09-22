package millionairemind.gui.screens;

import millionairemind.gui.GameUI;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class InstructionsUI {

    private final GameUI gameUI;

    public InstructionsUI(GameUI gameUI) {
        this.gameUI = gameUI;
    }

    public Node build() {

        Label heading =
                new Label("INSTRUCTIONS");

        heading
                .getStyleClass()
                .add("screen-heading");

        Button back =
                new Button("BACK TO MENU");

        back
                .getStyleClass()
                .add("menu-button");

        back.setFocusTraversable(false);

        back.setOnAction(
                event -> gameUI.showTitle()
        );

        VBox content =
                new VBox(
                        18,
                        heading,
                        back
                );

        content.setAlignment(Pos.CENTER);

        content.getStyleClass()
               .add("screen-root");

        var scene =
                gameUI.getScene();

        back
                .prefWidthProperty()
                .bind(
                        Bindings.createDoubleBinding(

                                () -> clamp(
                                        scene.getWidth() * 0.20,
                                        210,
                                        420
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

        back
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