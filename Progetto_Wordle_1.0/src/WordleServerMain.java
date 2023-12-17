import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

// Classe Thead Master che avvia il server e gestisce le connessioni con i client
public class WordleServerMain {

    // Variabili da configurare tramite file "server.properties"
    private static String jsonFilename;
    private static String wordFilename;
    private static int delayWordUpdate;
    private static int delayJsonUpdate;
    private static String multicastIP;
    private static int port;
    private static int multicastPort;

    // Socket
    private static ServerSocket serverSocket;
    private static DatagramSocket multicastSocket;

    public static void main(String[] args) throws IOException {
        // Inizializzazione variabili da "server.properties"
        readConfig();

        serverSocket = new ServerSocket(port);

        // Creazione lista con tutte le parole del file "words.txt"
        ArrayList<String> totalWordList= new ArrayList<>();
        BufferedReader wordFile = new BufferedReader(new FileReader(wordFilename));
        String word;
        while ((word = wordFile.readLine()) != null)
            totalWordList.add(word);
        wordFile.close();

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        /* --- Inizializzazione e avvio ThreadJsonManager --- */

        // Creazione lista degli utenti gia' presenti (o no) nel file "users.json"
        JsonUserList jsonUserList = new JsonUserList(jsonFilename); // Lista degli utenti registrati al gioco Wordle
        jsonUserList.createUserListFromJsonFile();
        executorService.scheduleWithFixedDelay(new ThreadJsonManager(jsonUserList),3, delayJsonUpdate, TimeUnit.SECONDS);

        /* --- Inizializzazione e avvio ThreadWordManager */

        Vector<String> wordListExtracted = new Vector<>();// Lista parole estratte dal file words.txt
        AtomicReference<String> newWord = new AtomicReference<>(); // Variabile aggiornata dal thread ThreadWordManager
        executorService.scheduleWithFixedDelay(new ThreadWordManager(wordFilename, newWord, wordListExtracted), 0, delayWordUpdate, TimeUnit.SECONDS);

        // Inizializzazione variabili e avvio di un thread worker per interagire con il client.
        Vector<String> connectedUser = new Vector<>(); // Lista utenti connessi al server dopo login
        multicastSocket = new DatagramSocket(0); // Inizializzazione del socket UDP per inviare i risultati delle partite al gruppo multicast
        InetAddress group = InetAddress.getByName(multicastIP);
        ExecutorService pool = Executors.newCachedThreadPool();
        Socket clientSocket;
        try {
            while(true) {
                System.out.println("Waiting a client...");
                clientSocket = serverSocket.accept();
                pool.execute(new ThreadWorker(clientSocket, newWord, jsonUserList, multicastSocket, group, multicastPort, wordListExtracted, totalWordList, connectedUser));
            }
        }
        catch (SocketException e) {
            jsonUserList.saveUserListInJsonFile();
            e.printStackTrace();
        }
    }

    public static void readConfig() throws IOException {
        String path = "server.properties";
        InputStream input = new FileInputStream(path);
        Properties prop = new Properties();
        prop.load(input);
        port = Integer.parseInt(prop.getProperty("port"));
        jsonFilename = prop.getProperty("jsonFile");
        wordFilename = prop.getProperty("fileTxt");
        delayWordUpdate = Integer.parseInt(prop.getProperty("delayWord"));
        delayJsonUpdate = Integer.parseInt(prop.getProperty("delayJson"));
        multicastIP = prop.getProperty("multicastIP");
        multicastPort = Integer.parseInt(prop.getProperty("multicastPort"));
        input.close();
    }
}
