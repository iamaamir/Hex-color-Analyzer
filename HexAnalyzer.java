import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.BufferedReader;
import java.net.URLConnection;
import javafx.geometry.Insets;
import javafx.concurrent.Task;
import javafx.scene.text.Font;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.event.ActionEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.io.InputStreamReader;
import javafx.concurrent.Service;
import javafx.scene.control.Alert;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.Rectangle;
import javafx.application.Application;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert.AlertType;

/**
 *
 * @author Aamir khan
 */
public class HexAnalyzer extends Application {

    final String HEX_PATTERN = "#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})";
    final String CSS_URL = "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css";
    Pattern pattern;
    TilePane colorsPane;
    TextField urlField;
    TextField colorVal;
    Button fetchBtn;

    @Override
    public void init() {
        pattern = Pattern.compile(HEX_PATTERN);
        urlField = new TextField(CSS_URL);
        fetchBtn = new Button("Fetch");
        colorVal = new TextField();
        colorsPane = new TilePane();
    }

    @Override
    public void start(Stage window) {

        fetchBtn.setPrefWidth(80.0);
        fetchBtn.setDefaultButton(true);

        colorVal.setFont(Font.font(14.0));
        colorVal.setVisible(false);

        HBox bar = new HBox(urlField, fetchBtn);
        bar.setSpacing(1.5);
        bar.setPadding(new Insets(5));
        
        HBox.setHgrow(urlField, Priority.ALWAYS);

        ScrollPane colorPane = new ScrollPane(colorsPane);
        colorPane.setFitToWidth(true);

        VBox root = new VBox(bar, colorPane, colorVal);

        window.setTitle("#HEX Color Analyzer");
        window.setScene(new Scene(root, 610, 400));
        window.show();

        fetchBtn.setOnAction(this::runTask);
    }

    boolean wasTaskStarted = false;

    private void runTask(ActionEvent e) {
        colorsPane.getChildren().clear();
        Service task = prepareTask();
        bindProperties(task);

        if (wasTaskStarted) {
            task.restart();
        } else {
            task.start();
            wasTaskStarted = true;
        }

    }

    private Service prepareTask() {
        return new Service() {
            @Override
            protected Task createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {

                        try {

                            final String css = fetchDataFrom(urlField.getText());
                            final Set<String> hexColors = extractHexColors(css);

                            hexColors.forEach(color -> {
                                Rectangle colorBox = new Rectangle(40, 40, Color.web(color));
                                colorBox.setEffect(new DropShadow(3, Color.rgb(0, 0, 0, 0.5)));
                                colorBox.setUserData(color);

                                colorBox.setOnMouseClicked(ae -> {
                                    final String val = colorBox.getUserData().toString().toUpperCase();
                                    colorVal.setText(val);
                                });
                                Platform.runLater(() -> {
                                    colorsPane.getChildren().add(colorBox);
                                });
                            });
                        } catch (IOException ex) {
                            Alert alert = new Alert(AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText("Oops, Something went wrong");
                            alert.setContentText(ex.toString());
                            alert.showAndWait();
                        }
                        return null;
                    }

                    //Util method to fetch data from the given url
                    private String fetchDataFrom(String url) throws IOException {
                        updateMessage("Fetching");
                        StringBuilder result = new StringBuilder();
                        URL cssurl = new URL(url);
                        URLConnection data = cssurl.openConnection();
                        try (BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                        data.getInputStream())
                        )) {
                            in.lines().forEach(result::append);
                        }
                        updateMessage("Done");

                        return result.toString();

                    }

                    //Util method to extract hex colors from the given String
                    private Set<String> extractHexColors(final String css) {
                        updateMessage("extracting hex");
                        Set<String> colorSet = new HashSet();
                        Matcher matcher = pattern.matcher(css);
                        while (matcher.find()) {
                            colorSet.add(matcher.group(0));
                        }
                        return colorSet;
                    }

                    @Override
                    public void succeeded() {
                        super.succeeded();
                        updateMessage("Fetch");//restrore the button
                        colorVal.setText("");//remove any previous val
                    }
                };
            }

        };
    }

    private void bindProperties(Service task) {
        fetchBtn.textProperty().bind(task.messageProperty());
        fetchBtn.disableProperty().bind(task.runningProperty());
        colorVal.visibleProperty().bind(task.runningProperty().not());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
