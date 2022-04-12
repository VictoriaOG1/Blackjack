package proyecto3.proyecto3;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;


public class BlackjackServer extends Application
{

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(BlackjackServer.class.getResource("BlackjackServerFXML.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 500, 500);
        stage.setTitle("BlackjackServer");
        stage.setScene(scene);
        stage.show();

        //En un thread se inicializa el juego
        BlackjackServerController controller = fxmlLoader.getController();
        new Thread(() ->
        {
            controller.initializeGame();
        }).start();

        //Si se cierra la ventana
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                controller.setTermination();
            }
        });
    }
    public static void main(String[] args)
    {
        launch(args);
    }
}
