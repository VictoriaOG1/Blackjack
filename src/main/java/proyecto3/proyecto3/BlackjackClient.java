package proyecto3.proyecto3;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;

public class BlackjackClient extends Application
{
    public void start(Stage stage) throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader(BlackjackClient.class.getResource("BlackjackClientFXML.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 500, 500);
        stage.setTitle("BlackjackClient");
        stage.setScene(scene);
        stage.show();

        //Si se cierra la ventana
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                BlackjackClientController controller = fxmlLoader.getController();
                controller.setTermination("Connection closed");
            }
        });
    }
    public static void main(String[] args)
    {
        launch(args);
    }
}
