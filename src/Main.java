import UI.MainForm;
import ex.Debug;
import ex.Util;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application
{

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        primaryStage.setWidth(600);
        primaryStage.setHeight(400);

        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("UI/MainForm.fxml").openStream());
        primaryStage.setTitle("exDownloader");
        Scene s = new Scene(root, 600, 400);
        primaryStage.setScene(s);
        primaryStage.setOnCloseRequest(we ->
        {
            Debug.InsertBlank();
            Debug.Log("Closing...");
            MainForm m = fxmlLoader.getController();
            m.Terminate();
            Debug.close();
        });
        primaryStage.show();
    }


    public static void main(String[] args)
    {
        Util.makeDir(Util.fileOutput);
        Util.parseConfig();
        Debug.create();
        launch(args);
    }
}
