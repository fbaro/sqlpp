package it.fb.sqlpp;

import it.fb.sqlpp.it.fb.sqlpp.mybatis.MybatisFormatter;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class GUI extends Application {

    @SuppressWarnings("FieldCanBeLocal")
    private Button formatButton;
    private Button formatDirectory;
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
        grid.setGridLinesVisible(false);
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

        formatDirectory = new Button("Mybatis");
        formatDirectory.setOnAction(ignored -> doMybatis(primaryStage));

        columnsField = new TextField("80");
        columnsField.setPrefColumnCount(4);
        spacingField = new TextField("4");
        spacingField.setPrefColumnCount(4);

        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(columnsField);
        hbBtn.getChildren().add(spacingField);
        hbBtn.getChildren().add(formatButton);
        hbBtn.getChildren().add(formatDirectory);
        grid.add(hbBtn, 0, 1);

        Scene scene = new Scene(grid, 600, 275);
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

    private void doMybatis(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose the mybatis resources directory");
        File result = directoryChooser.showDialog(primaryStage);
        if (result == null) {
            return;
        }
        try {
            int modified = Files.walk(result.toPath(), Integer.MAX_VALUE)
                    .filter(MybatisFormatter::isMapperFile)
                    .mapToInt(this::formatMybatis)
                    .sum();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Completed");
            alert.setHeaderText("Mybatis scanning completed");
            alert.setContentText(String.format("Formatted %d files", modified));
            alert.showAndWait();
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error scanning directory");
            alert.setContentText(ex.toString());
            alert.showAndWait();
        }
    }

    private int formatMybatis(Path mapperPath) {
        try (InputStream in = Files.newInputStream(mapperPath)) {
            Path tmpOutputPath = Files.createTempFile("tempMapper", "xml");
            try (OutputStream out = Files.newOutputStream(tmpOutputPath)) {
                MybatisFormatter.format(in, out, Integer.parseInt(columnsField.getText()),
                        Integer.parseInt(spacingField.getText()));
            } catch (ParserConfigurationException | XMLStreamException | SAXException e) {
                Files.delete(tmpOutputPath);
                return 0;
            }
            Files.move(tmpOutputPath, mapperPath, StandardCopyOption.REPLACE_EXISTING);
            return 1;
        } catch (IOException ignored) {
            return 0;
        }
    }
}
