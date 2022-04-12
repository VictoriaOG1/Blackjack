package proyecto3.proyecto3;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Formatter;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BlackjackClientController implements Initializable, Runnable
{
    @FXML
    private TextArea betText;
    @FXML
    private Label messageText;
    @FXML
    private Text playerLabel;
    @FXML
    private Button betButton;
    @FXML
    private Button hitButton;
    @FXML
    private Button standButton;
    @FXML
    private Button resetButton;
    @FXML
    private TextArea dealerText;
    @FXML
    private TextArea playerText;

    private Socket connection=null; //socket de cliente
    ExecutorService executorService; //
    private Scanner inputClient; // input del servidor
    private Formatter outputClient; // output del servidor
    private boolean available; // determina el turno

    @FXML
    protected void onBetButtonClick() //Si quiere apostar
    {
        String bet = betText.getText();

        //Solo si esta dentro de 2 y 500
        if(available && Integer.parseInt(bet)>2  && Integer.parseInt(bet)<=500)
        {
            outputClient.format("%s\n", "Bet "+Integer.parseInt(bet));
            outputClient.flush();

            available = false;
        }
    }
    @FXML
    protected void onHitButtonClick() //Si quiere pedir otra carta
    {
            outputClient.format("%s\n", "Hit");
            outputClient.flush();

    }
    @FXML
    protected void onStandButtonClick() //Si quiere retirarse
    {
        if(available)
        {
            outputClient.format("%s\n", "Stand");
            outputClient.flush();

            available = false;
        }
    }
    @FXML
    protected void onResetButtonClick() //Si quiere reiniciar el juego
    {
        if(available)
        {
            outputClient.format("%s\n", "Reset");
            outputClient.flush();

            available = false;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) //Inicializacion del GUI
    {
        betButton.setDisable(true);
        hitButton.setDisable(true);
        standButton.setDisable(true);
        resetButton.setDisable(true);
        dealerText.setEditable(false);
        playerText.setEditable(false);
        betText.setEditable(true);

        available=false;

        startClient();
    }

    @Override
    public void run() //Siempre buscar si el servidor envio algo
    {
        while (true)
        {
            if (inputClient.hasNextLine())
                processMessage(inputClient.nextLine());
        }

    }

    public void startClient() //Iniciar la conexion con el socket e inicializar los streams
    {
        try
        {
            connection = new Socket(InetAddress.getByName("localhost"), 6666);
            inputClient = new Scanner(connection.getInputStream());
            outputClient = new Formatter(connection.getOutputStream());
        }
        catch (Exception ioe)
        {
                connection=null;
                messageText.setText("No connection to server");

        }

        executorService = Executors.newFixedThreadPool(1);
        executorService.execute(this);
    }

    public void processMessage(String message) //Procesar el mensaje
    {
        if(message.contains("#"))
        {
            String message1 = message.replace("#","");
            playerText.appendText(message1);
        }
        else if(message.contains("Player "))
        {
            playerLabel.setText(message);
        }
        else if(message.equals("Game starts"))
        {
            available=true;
            dealerText.appendText(message+"\n");
            resetButton.setDisable(true);
            betText.setEditable(true);
            betButton.setDisable(false);
        }
        else if(message.equals("Next move?"))
        {
            available=true;
            standButton.setDisable(false);
            hitButton.setDisable(false);
        }
        else if(message.equals("Bet successful"))
        {
            betText.clear();
            betText.setEditable(false);
            betButton.setDisable(true);
        }
        else if(message.equals("Stand successful"))
        {
            hitButton.setDisable(true);
            standButton.setDisable(true);
        }
        else if(message.equals("No hit"))
        {
            dealerText.appendText("Over 21\n");
            hitButton.setDisable(true);
            standButton.setDisable(true);
        }
        else if(message.equals("21"))
        {
            dealerText.appendText("21!\n");
            hitButton.setDisable(true);
            standButton.setDisable(true);
        }
        else if(message.equals("Blackjack"))
        {
            dealerText.appendText("Blackjack!\n");
            hitButton.setDisable(true);
            standButton.setDisable(true);
        }
        else if(message.equals("New Game?"))
        {
            available=true;
            betButton.setDisable(true);
            hitButton.setDisable(true);
            standButton.setDisable(true);
            resetButton.setDisable(false);
            betText.setEditable(true);
            dealerText.appendText("New Game?\n");
        }
        else if(message.equals("Reset"))
        {
            playerText.clear();
        }
        else if(message.equals("Lost connection with server"))
        {
            dealerText.appendText(message + "\n");
            playerText.clear();
            dealerText.clear();
            executorService.shutdown();
            try {
                executorService.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else if(message.equals("Your turn"))
        {
            hitButton.setDisable(false);
            standButton.setDisable(false);
        }
        else
        {
            dealerText.appendText(message + "\n");
        }
    }

    //Si se cierra la ventana
    public void setTermination(String message)
    {
        outputClient.format("%s\n",message);
        outputClient.flush();

        executorService.shutdown();
        try {
            connection.close();
            outputClient.close();
            inputClient.close();
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
