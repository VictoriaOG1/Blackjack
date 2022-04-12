package proyecto3.proyecto3;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class BlackjackServerController implements Initializable
{
    @FXML
    private TextArea serverText;

    private HashMap<String,Integer> deckValues; //Baraja y valores correspondientes
    private Stack<String> deck; //Baraja
    private List<BlackjackPlayer> players; //Lista de jugadores
    private ServerSocket server; //Socket servidor
    private ThreadPoolExecutor runPlayers; //ThreadPoolExecutor para los jugadores

    @Override
    public void initialize(URL location, ResourceBundle resources) //Inicializacion del controller
    {
        serverText.setEditable(false); //No modificar la caja de texto

        //Inicializacion de la baraja
        initializeDeckValues();
        initializeDeck();

        //Barajar cartas
        for(int i=0; i<6; i++)
        {
            shuffleDeck();
        }

        //Inicializacion de la lista de jugadores
        players = new ArrayList<>();


        //Mandar mensaje a la caja de texto
        serverText.setText("Server waiting for connections...\n");
    }

    public void setTermination() //Se cierra la ventana se envia mensajes a los jugadores
    {
        players.forEach(player -> {player.sendMessage("Lost connection with server");
            player.close();});
        runPlayers.shutdown();
        try {
            server.close();
            runPlayers.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeGame() //Inicilizar el juego
    {
        //ThreadPoolExecutor
        runPlayers = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>());

        //Crear socket servidor
        try
        {
            server = new ServerSocket(6666);
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
            System.exit(1);
        }

        while (true) //Crear cada un jugador nuevo cada vez que se conecte
        {
            try
            {
                //Anadir el jugador en la lista de jugadores
                players.add(new BlackjackPlayer (server.accept(),players.size()+1));
                //Ejecutar con el ThreadPoolExecutor el jugador
                runPlayers.execute(players.get(players.size()-1));

            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    public void initializeDeckValues() //Valores de las cartas en un hashmap
    {
        deckValues = new HashMap<>();

        char[] suits= {'\u2660','\u2663','\u2665','\u2666'};
        int suitsIndex=0;

        //Loop del numero de cartas
        for(int i=0; i<52; i++)
        {
            String cardName="";
            int cardValue;

            int module = i%13;

            switch (module)
            {
                case 0 -> {
                    cardName += "A";
                    cardValue = 11;
                    if (i != 0) suitsIndex++;
                }
                case 10 -> {
                    cardName += "J";
                    cardValue = 10;
                }
                case 11 -> {
                    cardName += "Q";
                    cardValue = 10;
                }
                case 12 -> {
                    cardName += "K";
                    cardValue = 10;
                }
                default -> {
                    cardName += String.valueOf(module + 1);
                    cardValue = module + 1;
                }
            }
            cardName+=suits[suitsIndex];

            deckValues.put(cardName,cardValue);
        }
    }

    //Inicializar baraja de la que se va obtener
    public void initializeDeck()
    {
        deck = new Stack<>();
        deckValues.keySet().forEach(card -> deck.push(card));
    }

    //Shuffle de baraja
    public void shuffleDeck()
    {
        Collections.shuffle(deck.subList(0,deck.size()));
    }

    //Obtener una carta de la baraja
    public synchronized String getCard()
    {
        //Control de la baraja
        if(deck.size()==26)
        {
            messageToServerText("Deck shuffled");
            shuffleDeck();
        }
        if(deck.empty())
        {
            messageToServerText("New deck");
            initializeDeck();
        }

        return deck.pop();
    }

    //Si se pierde conexion con el jugador
    public synchronized void connectionLost(BlackjackPlayer player)
    {
        serverText.appendText("Lost connection with player " + player.getPlayerNumber() + "\n");
        player.close();

        players.stream().skip(player.getPlayerNumber()).forEach(player1 ->
            {
                String m = String.valueOf(player1.getPlayerNumber());
                int num=player1.getPlayerNumber()-1;
                player1.setPlayerNumber(num);
                player1.sendMessage("Player " + num);
                messageToServerText("Player " +m+" is now player "+player1.getPlayerNumber());
            });
        runPlayers.remove(player);
        players.remove(player);
    }

    //Mandar un mensaje a la caja de texto
    public synchronized void messageToServerText(String message)
    {
        serverText.appendText(message+"\n");
    }

    //CLASE PARA EL JUGADOR
    private class BlackjackPlayer implements Runnable
    {
        private Socket connection; // Socket de conexion
        private Scanner inputServer; // Stream para recibir datos del cliente
        private Formatter outputServer; // Stream para mandar datos al cliente
        private int playerNumber; // Numero de jugador
        private int bet; //Cuanto apostó el jugador
        private boolean stands; //Si el jugador se retira
        private boolean reset; //Si el jugador decide reiniciar el juego
        private String cards; //Cartas del jugador
        private String dealerPlayer; //Un dealer por cada jugador
        private int count;

        public BlackjackPlayer (Socket socket, int number)
        {
            try
            {
                playerNumber=number; //Numero de jugador
                connection=socket; //Socket
                count=0;
                reset=true;
                //Streams
                inputServer = new Scanner(connection.getInputStream());
                outputServer = new Formatter(connection.getOutputStream());
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
                System.exit(1);
            }
        }

        @Override
        public void run()
        {
            try
            {
                while(true)
                {
                    count++;
                    //Reset y stands se inicializan en falso
                    reset=false;
                    stands=false;

                    //Inicializacion de las cartas
                    cards="";

                    //El dealer obtiene una carta
                    dealerPlayer = getCard();

                    //Dos cartas para el jugador
                    for (int i=0; i<2; i++)
                    {
                        cards+=getCard()+" ";
                    }

                    if(count==1)
                        messageToServerText("Player " + playerNumber + " connected");

                    //Se envian mensajes al cliente
                    sendMessage("Player " + playerNumber);
                    sendMessage("Game starts");
                    sendMessage("Bet: 2-500");
                    sendMessage(cards+"#");


                    //Mientras que el cliente no se retira se recibe cualquier información
                    while (!stands)
                    {
                        if(inputServer.hasNext())
                        {
                            String action = inputServer.nextLine();
                            processAction(action);

                        }
                    }

                    //Si ya se retiro acaba el juego
                    blackjackOver();
                    sendMessage("New Game?");

                    //Se espera hasta que el cliente envie que quiere reiniciar el juego
                    while(!reset)
                    {
                        if(inputServer.hasNext())
                        {
                            String action = inputServer.nextLine();
                            processAction(action);
                        }
                    }
                }
            }
            finally //Se cierra la conexion si sale del loop
            {
                try
                {
                    connection.close();
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                    System.exit(1);
                }
            }
        }

        //Procesar la accion que realiza el cliente
        public void processAction(String action)
        {
            if (action.equals("Connection closed")) //Si el cliente se desconecta
            {
                connectionLost(this);
                try
                {
                    connection.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            else //Si no se desconecta
            {
                //Mensaje a la caja de texto
                messageToServerText("Player " + playerNumber+ " " + action.toLowerCase());

                //Si quiere apostar
                if (action.contains("Bet"))
                {
                    bet=Integer.parseInt(action.substring(4));
                    sendMessage("Bet successful");
                    sendMessage("Dealer card: " + dealerPlayer);
                    if(getSumOfCards(cards)==21)
                    {
                        sendMessage("Blackjack");
                        stands=true;
                    }
                    else
                        sendMessage("Next move?");
                }
                //Pedir una carta
                else if (action.equals("Hit"))
                {
                    String newCard = getCard() + " ";
                    cards += newCard;
                    sendMessage(newCard+"#");

                    //Si se pasa de 21
                    if (getSumOfCards(cards) > 21)
                    {
                        sendMessage("No hit");
                        messageToServerText("Player " + playerNumber+ " " + "stand");
                        stands=true;
                    }
                    //Si es igual a 21
                    else if (getSumOfCards(cards) == 21)
                    {
                        sendMessage("21");
                        messageToServerText("Player " + playerNumber+ " " + "stand");
                        stands=true;
                    }
                }
                //Si se retira
                else if (action.equals("Stand"))
                {
                    sendMessage("Stand successful");
                    stands=true;
                }
                //Si reinicia
                else if (action.equals("Reset"))
                {
                    reset=true;
                    stands=false;
                    dealerPlayer="";
                    cards="";
                    sendMessage("Reset");
                }
            }
        }

        //Suma de las cartas
        public int getSumOfCards(String cards)
        {
            String [] cards1 = cards.split(" ");

            List<Integer> cards2 = Arrays.stream(cards1)
                    .map(card -> deckValues.get(card)).toList();

            int countA = (int) cards2.stream()
                    .filter(cardValue -> cardValue == 11).count();

            int sum = cards2.stream()
                    .mapToInt(cardValue-> cardValue.intValue())
                    .sum();


            if(countA==1)
            {
                if(sum>21)
                {
                    sum -= 10;
                }
            }
            else if(countA>=2)
            {
                for(int i=0; i<countA;i++)
                {
                    sum -= 10;
                }
            }
            else
            {
                return sum;
            }

            return sum;
        }

        //Si el juego se termina
        public void blackjackOver()
        {
            //El dealer coge cartas
            dealerPlayer += " " +getCard();

            while (getSumOfCards(dealerPlayer) < 17)
                dealerPlayer += " " + getCard();

            //Si el jugador pierde o gana
            sendMessage("Dealer cards: " + dealerPlayer);
            int sumPlayer = getSumOfCards(cards);
            int sumDealer = getSumOfCards(dealerPlayer);

            if(sumPlayer>21)
            {
                messageToServerText("Player " + playerNumber + " lost");
                sendMessage("You lost " + bet);
            }
            else if(sumPlayer==sumDealer)
            {
                messageToServerText("Player " + playerNumber + " tied with dealer");
                sendMessage("Tie");
                sendMessage("Return: "+bet);
            }
            else if(sumPlayer==21)
            {
                messageToServerText("Player " + playerNumber + " won");

                String [] cards1 = cards.split(" ");
                if(sumPlayer==sumDealer)
                {
                    sendMessage("Tie");
                    sendMessage("Return: "+bet);
                }
                else
                {
                    if (cards1.length == 5)
                        sendMessage("You won " + (2.5 * bet));
                    else
                        sendMessage("You won " + (2 * bet));
                }
            }
            else
            {
                if(sumPlayer>sumDealer || sumDealer>21)
                {
                    messageToServerText("Player " + playerNumber + " won");
                    sendMessage("You won "+(2*bet));
                }
                else
                {
                    sendMessage("You lost "+bet);
                    messageToServerText("Player " + playerNumber + " lost");
                }

            }
        }

        //Mandar mensajes por el stream
        public void sendMessage(String message)
        {
            outputServer.format("%s\n",message);
            outputServer.flush();
        }

        //Obtener el numero del jugador
        public int getPlayerNumber()
        {
            return playerNumber;
        }

        public void setPlayerNumber(int playerNumber)
        {
            this.playerNumber = playerNumber;
        }
        public void close()
        {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputServer.close();
            inputServer.close();
        }
    }
}
