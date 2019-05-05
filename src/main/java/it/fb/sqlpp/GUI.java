package it.fb.sqlpp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class GUI extends Application {

    @SuppressWarnings("FieldCanBeLocal")
    private Button formatButton;
    private TextArea textArea;
    private TextField columnsField;
    private TextField spacingField;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hello World!");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.setGridLinesVisible(true);
        {
            ColumnConstraints column1 = new ColumnConstraints(100, 100, Double.MAX_VALUE);
            column1.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().addAll(column1);
        }
        {
            RowConstraints row1 = new RowConstraints(100, 100, Double.MAX_VALUE);
            row1.setVgrow(Priority.ALWAYS);
            RowConstraints row2 = new RowConstraints();
            grid.getRowConstraints().addAll(row1, row2);
        }

        textArea = new TextArea();
        textArea.setFont(Font.font("monospaced", 14.));
        grid.add(textArea, 0, 0);

        formatButton = new Button("Format");
        formatButton.setOnAction(ignored -> doFormat());

        columnsField = new TextField("80");
        columnsField.setPrefColumnCount(4);
        spacingField = new TextField("4");
        spacingField.setPrefColumnCount(4);

        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(columnsField);
        hbBtn.getChildren().add(spacingField);
        hbBtn.getChildren().add(formatButton);
        grid.add(hbBtn, 0, 1);

        Scene scene = new Scene(grid, 300, 275);
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    private void doFormat() {
        String formatted;
        try {
            formatted = StatementLayout2.format(Integer.parseInt(columnsField.getText()), Integer.parseInt(spacingField.getText()), textArea.getText());
        } catch (RuntimeException ex) {
            new Alert(Alert.AlertType.WARNING, "Error parsing statement: " + ex.getMessage(), ButtonType.OK)
                    .showAndWait();
            return;
        }
        textArea.setText(formatted);
    }
}
