import java.io.*;
import java.net.Socket;
import java.util.*;

// Classe per avviare la parte Client e permettere agli utenti di interfacciarsi col server
public class WordleClientMain {

    private static String ip_adress;
    private static int port;
    private static int multicastPort;
    private static String multicastIP;

    private static ThreadShareManager threadShareManager;
    private static Thread thread;
    private static Socket socket;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;
    private static Scanner scanner = new Scanner(System.in);


    private static boolean connesso = false; // Per definire se il processo client e' connesso al server
    private static boolean logout; // Nel caso l'utente si scconnette, serve per uscire dai vari loop
    private static User user; // Utente loggato
    private static int loginFlag; // Flag per attivare/disattivare il titolo "Login Room"
    private static int serviceFlag; // Flag per attivare/disattivare il titolo "Service room"

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        System.out.println("\t╔════════════════════════════════════╗");
        System.out.println("\t║       Welcome to WORDLE Game!      ║");
        System.out.println("\t╚════════════════════════════════════╝");
        System.out.println();

        // Inizializzazione variabili dal file di configurazione client.properties
        readConfig();
        loginFlag = 0;

        // Main while : Gestisce la schermata iniziale di login,
        // in cui e' possibile :
        // 1) registrare un utente
        // 2) login con un utente gia' registrato
        // 3) help -> per capire meglio come sono strutturate le stanze/schermate e
        //            cosa uno puo fare una volta al suo interno
        while (true) {
            if (loginFlag == 0) {
                System.out.println();
                System.out.println("┌──────────────────────────────┐");
                System.out.println("│     LOGIN/REGISTER ROOM      │");
                System.out.println("└──────────────────────────────┘");
                System.out.println();
                System.out.println("Inserire 'help' per il manuale d'istruzione generale. \nE per il manuale d'istruzione locale inserire qualsiasi carattere alfanumerico.");
            }
            System.out.println();
            loginFlag = 1;

            System.out.print(">> ");
            System.out.flush();
            String instructions = scanner.nextLine();
            String[] instructionList = instructions.split(" ");
            switch (instructionList[0]) {
                case "help": {
                    printIstructions();
                    break;
                }
                case "register": {
                    register(instructionList);
                    break;
                }
                case "login": {
                    login(instructionList);
                    break;
                }
                default: {
                    // Nel caso di comando non trovato, espone le possibili azioni che possono essere eseguite nella sezione 'login room'
                    System.out.println("Comando non trovato.");
                    System.out.println("    ┌──────────────────────┐");
                    System.out.println("┌───┤ Usage of Login Room: ├─────────────────────────────────────────────────────┐");
                    System.out.println("│   └──────────────────────┘                                                     │");
                    System.out.println(
                            "│ - 'help'                           -> il manuale di istruzione generale        │\n" +
                                    "│ - 'register <username> <password>' -> registrazione dell'utente                │\n" +
                                    "│ - 'login <username> <password>'    -> utente accede al servizio (SERVICE ROOM) │");
                    System.out.println("└────────────────────────────────────────────────────────────────────────────────┘");
                    System.out.flush();
                    break;
                }
            }

        }
    }

    // Metodo che stampa le instruzioni di come navigare attraverso le stanze/schermate del gioco
    public static void printIstructions() {
        System.out.println("┌────────────────────────────────────────────┐");
        System.out.println("│      MANUALE DI ISTRUZIONE GENERALE        │");
        System.out.println("├────────────────────────────────────────────┴──────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ LOGIN ROOM :                                                                                                          │\n" +
                "│       - 'help'                           -> il manuale di istruzione generale                                         │\n" +
                "│       - 'register <username> <password>' -> registrazione dell'utente                                                 │\n" +
                "│       - 'login <username> <password>'    -> utente accede al servizio (SERVICE ROOM)                                  │");
        System.out.println("│                                                                                                                       │");
        System.out.println("│ SERVICE ROOM :                                                                                                        │\n" +
                "│       - 'help'                           -> il manuale di istruzione generale                                         │\n" +
                "│       - 'logout'                         -> effettua il logout dell'utente e torna nella pagina di login (LOGIN ROOM) │\n" +
                "│       - 'play'                           -> gioca una partita                                                         │\n" +
                "│       - 'sendMeStat'                     -> riceve le statistiche generali aggiornate dopo l'ultima partita           │\n" +
                "│       - 'showMeSharing'                  -> mostra la lista degli share degli altri utenti                            │");
        System.out.println("│                                                                                                                       │");
        System.out.println("│ PLAYING ROOM :                                                                                                        │\n" +
                "│       - 'logout'                         -> effettua il logout dell'utente e torna nella pagina di login (LOGIN ROOM) │\n" +
                "│       - 'help'                           -> il manuale di istruzione generale                                         │\n" +
                "│       - 'send <word>'                    -> richiesta se la parola da indovinare e' quella giusta                     │");
        System.out.println("│                                                                                                                       │");
        System.out.println("│ Dopo aver giocato una partita:                                                                                        │");
        System.out.println("│       - 's' o 'n'                        -> scelta di condivisione dei risultati della partita                        │");
        System.out.println("|                                                                                                                       |");
        System.out.println("| Legenda per lo status delle lettere della parola proposta:                                                            |\n" +
                "|       - 'X'                              -> grigio: la lettera non e' presente nella parola segreta                   |\n" +
                "|       - '?'                              -> giallo: la lettera non e' corretta, ma e' presenta nella parola segreta   |\n" +
                "|       - '+'                              -> verde : la lettera e' corretta                                            |");
        System.out.println("└───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘");

    }

    /**
     * Metodo per leggere il file di configurazione del client.
     *
     * @throws FileNotFoundException se il file non e' presente
     * @throws IOException           se qualcosa non va in fase di lettura
     */
    public static void readConfig() throws FileNotFoundException, IOException {
        String path = "client.properties";
        InputStream input = new FileInputStream(path);
        Properties prop = new Properties();
        prop.load(input);
        port = Integer.parseInt(prop.getProperty("port"));
        ip_adress = prop.getProperty("ip_adress");
        multicastIP = prop.getProperty("multicastIP");
        multicastPort = Integer.parseInt(prop.getProperty("multicastPort"));
        input.close();
    }

    // Inizializza e avvia la connessione con il server nel caso non c'e' ancora un utente connesso
    private static void socketConnection() throws IOException {
        if (!connesso) {
            socket = new Socket(ip_adress, port);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            connesso = true;
        }
    }

    // Metodo per registrare un nuovo utente
    private static void register(String[] instruction) throws IOException, ClassNotFoundException {
        if (instruction.length != 3) {
            System.out.println("Usage: register <username> <password>");
            return;
        }
        socketConnection(); // conessione al server

        // Invio richiesta di registrazione utente dato
        SmsSocket sms_register = new SmsSocket(Instruction.REGISTER, instruction[1], instruction[2], null, null, null, 0);
        objectOutputStream.writeObject(sms_register);
        sms_register = (SmsSocket) objectInputStream.readObject();

        // Esito risposta
        if (sms_register.getCodice() == 1) System.out.println("Username gia' in uso");
        else if (sms_register.getCodice() == 2) System.out.println("Password vuota");
        else if (sms_register.getCodice() == 7) System.out.println("Registrato con successo!");
    }

    // Metodo per connettere utente gia esistente al server
    private static void login(String[] instruction) throws IOException, ClassNotFoundException {
        if (instruction.length != 3) {
            System.out.println("Usage: login <username> <password>");
            return;
        }
        socketConnection(); // Conessione al server

        // Invio richiesta di login con l'utente dato
        SmsSocket sms_login = new SmsSocket(Instruction.LOGIN, instruction[1], instruction[2], null, null, null, 0);
        objectOutputStream.writeObject(sms_login);
        sms_login = (SmsSocket) objectInputStream.readObject();

        if (sms_login.getCodice() == 1) {
            System.out.println("Username o password errati");
            return;
        } else if (sms_login.getCodice() == 2) {
            System.out.println("Utente gia' connesso");
            return;
        } else if (sms_login.getCodice() == 7) {
            System.out.println("Login con successo!");
            user = new User(instruction[1], null);
            // Avvio thread multicast per ricezione delle notifiche di game di altri giocatori
            threadShareManager = new ThreadShareManager(multicastPort, multicastIP, user.getUsername());
            thread = new Thread(threadShareManager);
            thread.start();
        }
        // Se login va a buon fine, accesso alla stanza/schermata "service room"
        // Inizializzazione flag
        logout = false;
        loginFlag = 0;
        serviceFlag = 0;

        // Main "service room" in cui e' possibile :
        // 1) logout : fare logout
        // 2) play : avviare una partita wordle
        // 3) sendMeStat : ricevere statistiche di gioco aggiornate
        // 4) showMeSharing : stampare partite condivise dagli altri utenti
        // 5) help : stampare il manuale di instruzioni generale del gioco Wordle
        while (!logout) {
            // Titolo per far capire che si trova nella stanza/schermata dei servizi offerti dal server del gioco wordle
            if (serviceFlag == 0) {
                System.out.println();
                System.out.println("┌──────────────────────────────┐");
                System.out.println("│         SERVICE ROOM         │");
                System.out.println("└──────────────────────────────┘");
                System.out.println();
                System.out.println("Inserire 'help' per il manuale d'istruzione generale. \nE per il manuale d'istruzione locale inserire qualsiasi carattere alfanumerico.");
            }
            System.out.println();
            System.out.print(">> ");
            String instructions = scanner.nextLine();
            String[] instructionList = instructions.split(" ");
            serviceFlag = 1;
            switch (instructionList[0]) {
                case "logout": {
                    logout(Instruction.LOGOUT);
                    break;
                }
                case "play": {
                    playWORDLE();
                    break;
                }
                case "sendMeStat": {
                    sendMeStatistics();
                    break;
                }
                case "showMeSharing": {
                    showMeSharing();
                    break;
                }
                case "help": {
                    printIstructions();
                    break;
                }
                default: {
                    // Nel caso di comando non trovato, espone le possibili azioni che possono essere eseguite nella stanza 'service room'
                    System.out.println("Comando non trovato.");
                    System.out.println("    ┌────────────────────────┐");
                    System.out.println("┌───┤ Usage of Service Room: ├──────────────────────────────────────────────────────────────────┐");
                    System.out.println("│   └────────────────────────┘                                                                  │");
                    System.out.println(
                            "│ - 'logout'        -> effettua il logout dell'utente e torna nella pagina di login (LOGIN ROOM)│\n" +
                                    "│ - 'play'          -> gioca una partita                                                        │\n" +
                                    "│ - 'sendMeStat'    -> riceve le statistiche generali aggiornate dopo l'ultima partita          │\n" +
                                    "│ - 'help'          -> il manuale di istruzione                                                 │\n" +
                                    "│ - 'showMeSharing' -> mostra la lista degli share degli altri utenti                           │");
                    System.out.println("└───────────────────────────────────────────────────────────────────────────────────────────────┘");
                    System.out.flush();
                    break;
                }
            }

        }
    }

    // Metodo per disconnettere l'utente connesso
    private static void logout(Instruction instruction) throws IOException, ClassNotFoundException {
        if (instruction == Instruction.LOGOUT) {
            // Invio richiesta di logout dell'utente al server
            SmsSocket sms_logout = new SmsSocket(Instruction.LOGOUT, null, null, null, null, null, 0);
            objectOutputStream.writeObject(sms_logout);
            sms_logout = (SmsSocket) objectInputStream.readObject();
        } else if (instruction == Instruction.LOGOUT_PLAY) {
            // Invio richiesta di logout dell'utente al server durante la partita
            SmsSocket sms_logout = new SmsSocket(Instruction.LOGOUT_PLAY, null, null, null, null, null, 0);
            objectOutputStream.writeObject(sms_logout);
            sms_logout = (SmsSocket) objectInputStream.readObject();
            StringBuilder stat = sms_logout.getStatistics(); // stampa le statistiche aggiornate
            System.out.println(stat);
        }
        System.out.println("Logout done.");
        // Disconnette la connessione TCP e multicast
        objectOutputStream.close();
        objectInputStream.close();
        threadShareManager.logout();
        connesso = false; // Avverte che non c'e' piu connessione TCP attiva
        logout = true; // Serve per uscire dai loop while (sia della 'playing room' che della 'service room')
    }

    // Metodo per avviare la scchemata di gioco di una partita del gioco Wordle
    private static void playWORDLE() throws IOException, ClassNotFoundException {
        serviceFlag = 0; // cosi che la sezione "Service Room" riappaia una volta finita la partita
        // Richiesta al server se l'utente puo' giocare
        SmsSocket smsregister = new SmsSocket(Instruction.PLAY, null, null, null, null, null, 0);
        objectOutputStream.writeObject(smsregister);
        smsregister = (SmsSocket) objectInputStream.readObject();

        // Esito risposta
        if (smsregister.getCodice() == 1) {
            System.out.println("Parola gia' giocata. Riprova piu' tardi.");
            return;
        } else if (smsregister.getCodice() == 7) { // Parola non ancora giocata
            System.out.println();
            System.out.println("┌──────────────────────────────┐");
            System.out.println("│          PLAYING ROOM        │");
            System.out.println("└──────────────────────────────┘");
            System.out.println();
            System.out.println("[Legenda: grigio->'X' | giallo->'?' | verde->'+']");
            System.out.println();
            System.out.println("Inserire 'help' per il manuale d'istruzione generale. \nE per il manuale d'istruzione locale inserire qualsiasi carattere alfanumerico.");
            System.out.println();
            boolean parolaIndovinata = false;
            int numTentativi = 0; // numero tentativi fatti
            int codice;
            boolean logout_locale = false;
            // Loop in funzione finche non finisce i tentativi o fa logout
            while (!parolaIndovinata && numTentativi < 12 && !logout_locale) {
                System.out.printf("[Numero tentativi rimasti: %s]\n", 12 - numTentativi);
                System.out.print(">> ");
                String instructions = scanner.nextLine();
                String[] instructionList = instructions.split(" ");
                // Azioni possibili nella schermata di gioco 'playing room' :
                // 1) send : inviare la parola scelta dall'utente
                // 2) logout : Disconnetere l'utente
                // 3) help : per accedere al manuale d'instruzione generale del gioco wordle/
                switch (instructionList[0]) {
                    case "send": {
                        codice = sendWord(instructionList);
                        if (codice == 1) System.out.println("Parola non esistente. Riprova");
                        else if (codice == 2)
                            numTentativi++;// parola non indovinata, la stampa e' fatta in sendWord()
                        else if (codice == 7) parolaIndovinata = true;
                        break;
                    }
                    case "help": {
                        printIstructions();
                        break;
                    }
                    case "logout": { // logout definitivo viene fatto fuori dal while
                        logout_locale = true;
                        System.out.println("Partita persa.");
                        break;
                    }
                    default: {
                        // Nel caso di comando non trovato, espone le possibili azioni che possono essere eseguite nella stanza 'playing room'
                        System.out.println("Comando non trovato.");
                        System.out.println("    ┌────────────────────────┐");
                        System.out.println("┌───┤ Usage of Playing Room: ├─────────────────────────────────────────────────────────────────┐");
                        System.out.println("│   └────────────────────────┘                                                                 │");
                        System.out.println(
                                "│ - 'logout'      -> effettua il logout dell'utente e torna nella pagina di login (LOGIN ROOM) │\n" +
                                        "│ - 'send <word>' -> richiesta se la parola da indovinare e' quella giusta                     │\n" +
                                        "│ - 'help'        -> il manuale di istruzione                                                  │");
                        System.out.println("└──────────────────────────────────────────────────────────────────────────────────────────────┘");
                        break;
                    }
                }
                System.out.println();
            }
            // Esito finale della partita
            if (parolaIndovinata) {
                System.out.println("Parola indovinata!");
                System.out.println();
            } else if (!logout_locale) {
                System.out.println("Tentativi finiti. Parola non indovinata.");
                System.out.println();
            }

            // Invio stat alla fine della partita
            if (!logout_locale) {
                sendMeStatistics();
                System.out.println();
            }

            // Richiesta all'utente di condividere la partita giocata
            String answer = "";
            while (!answer.equals("s") && !answer.equals("n")) {
                System.out.print("Vuoi condividere la tua partita: si [s] o no [n] -> ");
                answer = scanner.nextLine();
                if (!answer.equals("s") && !answer.equals("n"))
                    System.out.println("Warning : devi inserire 's' per si o 'n' per no. Riprova.");
            }
            System.out.println();
            if (answer.equals("s")) share();

            // Nel caso e' stato fatto logout nella schermata play
            if (logout_locale) logout(Instruction.LOGOUT_PLAY);

        }
    }

    // Metodo per inviare la parola proposta dall'utente e ricezione dell'esito
    private static int sendWord(String[] instruction) throws IOException, ClassNotFoundException {
        if (instruction.length != 2) {
            System.out.println("Usage: send <word>");
            return -1;
        }
        if (instruction[1].length() != 10) {
            System.out.println("La parola deve essere lunga 10 caratteri.");
            return -1;
        }
        // Invio parola proposta dall'utente
        String attemptWord = instruction[1];
        SmsSocket sms_guess_word = new SmsSocket(Instruction.SEND_WORD, null, null, null, attemptWord, null, 0);
        objectOutputStream.writeObject(sms_guess_word);
        sms_guess_word = (SmsSocket) objectInputStream.readObject();

        // Esito risposta del server
        if (sms_guess_word.getCodice() == 1) return 1; // parola non esistente
        else if (sms_guess_word.getCodice() == 2) { // parola non indovinata
            System.out.println(attemptWord);
            for (String status : sms_guess_word.getStatusAttemptWordList())
                System.out.print(status);
            System.out.println();
            return 2;
        } else return 7; // parola indovinata
    }

    // Metodo per stampare le statistiche (richieste al server) dell'utente attualmente connesso
    private static void sendMeStatistics() throws IOException, ClassNotFoundException {
        SmsSocket sms_send_stat = new SmsSocket(Instruction.SEND_ME_STAT, null, null, null, null, null, 0);
        objectOutputStream.writeObject(sms_send_stat);
        sms_send_stat = (SmsSocket) objectInputStream.readObject();

        StringBuilder stat = sms_send_stat.getStatistics();
        System.out.println(stat);
    }

    // Metodo per stampare cio' che gli altri utenti hanno condiviso
    private static void showMeSharing() {
        threadShareManager.printSharedGameList();
    }

    // Metodo per condividere i tentativi fatti sulla parola giocata
    private static void share() throws IOException, ClassNotFoundException {
        SmsSocket sms_share = new SmsSocket(Instruction.SHARE, null, null, null, null, null, 0);
        objectOutputStream.writeObject(sms_share);
        sms_share = (SmsSocket) objectInputStream.readObject();

        if (sms_share.getCodice() == 7)
            System.out.println("Condivisione risultato partita riuscita!");
    }
}
