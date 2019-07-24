package elevator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        URL location = getClass().getResource("elevator_ui.fxml");

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(location);
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
        StackPane root = fxmlLoader.load();

        //获取Controller实例对象
        Controller controller = fxmlLoader.getController();
        primaryStage.setTitle("Elevator");
        Scene scene = new Scene(root, 600, 700);
        primaryStage.setScene(scene);
        //加载css样式
        scene.getStylesheets().add(getClass().getResource("MainStyle.css").toExternalForm());
        primaryStage.show();
        //初始化controller
        controller.init();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
