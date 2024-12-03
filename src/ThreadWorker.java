import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

// Classe che gestisce la connessione con un client
public class ThreadWorker implements Runnable {

    // Variabili per gestire la connessione TCP col client
    private final Socket clientSocket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    // Variabili per gestire la connessione multicast
    private DatagramSocket multicastSocket;
    private InetAddress group;
    private int multicastPort;

    // Variabili per gestire le partite di gioco
    private int numTentativi; // Numero di tentativi per la parola giocata
    private String secretWord; // Parola da indovinare durante la partita
    private AtomicReference<String> newWord; // Parola aggiornata ogni tot tempo dal threadWordManager
    private ArrayList<ArrayList<String>> currentWordAttemptsList = new ArrayList<>(); // Lista dei tentativi acccumulati per la secretWord della partita in atto

    // Variabili per gestire l'utente
    private JsonUserList jsonUserList; // Struttura dati che gestisce la lista di utenti presenti nel file users.json
    private Vector<String> connectedUser; // Lista di Utenti connessi al server (lista condivisa fra i vari threadWorker)
    private User currentUser;
    private boolean connesso = false;
    private boolean logout = false;

    // Gestione del database di parole del gioco wordle
    private Vector<String> wordListExtracted; // Parole estratte e proposte dal database del file words.txt
    private ArrayList<String> totalWordList; // Lista di tutte le parole del file words.txt

    // costruttore
    public ThreadWorker(Socket clientSocket, AtomicReference<String> newWord, JsonUserList jsonUserList, DatagramSocket multicastSocket, InetAddress group, int multicastPort, Vector<String> wordListExtracted, ArrayList<String> totalWordList, Vector<String> connectedUser) {
        this.clientSocket = clientSocket;
        this.newWord = newWord;
        this.jsonUserList = jsonUserList;
        this.multicastSocket = multicastSocket;
        this.group = group;
        this.multicastPort = multicastPort;
        this.wordListExtracted = wordListExtracted;
        this.totalWordList = totalWordList;
        this.connectedUser = connectedUser;
    }

    public void run() {
        try {
            // Inizializzazione connessione col client
            objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            objectInputStream = new ObjectInputStream(clientSocket.getInputStream());

            // Quando logout = true, il thread worker si chiude
            while (!logout) {
                System.out.println("Waiting message...");
                SmsSocket smsSocket = (SmsSocket) objectInputStream.readObject();
                System.out.println(smsSocket.getInstruction());
                // Indirizza la richiesta del client a seconda dell'instruzione ricevuta nell'smsSocket
                switch (smsSocket.getInstruction()) {
                    case LOGIN:
                        login(smsSocket);
                        break;
                    case REGISTER:
                        register(smsSocket);
                        break;
                    case PLAY:
                        play();
                        break;
                    case SHARE:
                        share();
                        break;
                    case SEND_WORD:
                        sendWork(smsSocket);
                        break;
                    case SEND_ME_STAT:
                        sendMeStatistics();
                        break;
                    case LOGOUT:
                        logout();
                        logout = true;
                        break;
                    case LOGOUT_PLAY:
                        logoutPlay();
                        logout = true;
                        break;
                    default: // Non verra' mai eseguito, perche' l'errore viene anticipato nel lato client
                        SmsSocket errorSms = new SmsSocket(null, null, null, null, null, null, 404);
                        objectOutputStream.writeObject(errorSms);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Metodo che gestisce la richiesta di login del'utente dato dal client
    private void login(SmsSocket log) throws IOException {
        int codice;
        if (jsonUserList.checkUserExistance(log.getUsername(), log.getPassword())) { // controlla l'esistenza dell'utente
            synchronized (connectedUser) {
                if (connectedUser.contains(log.getUsername())) {
                    codice = 2; // esiste, ma gia connesso, ERRORE
                } else {
                    codice = 7; // esiste puo' accedere, OKAY
                    currentUser = jsonUserList.getUser(log.getUsername()); // salva l'utente connesso in una var. per gestioni future
                    connectedUser.add(log.getUsername()); // cosi non potra' esserci due connessioni contemporaneamente con lo stesso utente
                    if (currentUser == null) {
                        System.out.println("Errore : takeUser()");
                        System.exit(-1);
                    }
                    connesso = true;
                    System.out.println("User [" + currentUser.getUsername() + "] connesso.");
                }
            }
        } else codice = 1; // password o username errati
        // Invio risposta al client con esito
        SmsSocket smsSocket = new SmsSocket(null, null, null, null, null, null, codice);
        objectOutputStream.writeObject(smsSocket);
    }

    // Metodo che gestisce la richiesta di registrazione dell'utente dato dal client
    private void register(SmsSocket log) throws IOException {
        if (!connesso) {
            int codice;
            // Controlla se e' possibile registrare l'utente nel database degli utenti
            synchronized (jsonUserList) {
                if (!jsonUserList.isEmptyJsonUserList() && jsonUserList.checkUsername(log.getUsername()))
                    codice = 1;// username esiste gia'
                else if (log.getPassword().equals("")) codice = 2; // password vuota
                else { // Registrazione puo essere effetuata
                    codice = 7;
                    User newUser = new User(log.getUsername(), log.getPassword());
                    jsonUserList.insertUser(newUser); // aggiunge utente nel database degli utenti
                    System.out.println("User [" + newUser.getUsername() + "] registrato.");
                }
            }
            // Invia risposta con esito
            SmsSocket smsSocket = new SmsSocket(null, null, null, null, null, null, codice);
            objectOutputStream.writeObject(smsSocket);
        }
    }

    // Metodo che gestisce la richiesta di inizio partita
    private void play() throws IOException {
        secretWord = newWord.get(); // sara' la parola della partita
        System.out.println("Secretword giocata: " + secretWord + " | Da utente : " + currentUser.getUsername());
        int codice;
        // Controlla se la parola e' gia stata giocata dall'utente
        if (currentUser.isWordAlreadyPlayed(secretWord)) codice = 1; // non puo' giocare
        else { // Nel caso puo' giocare la partita, aggiorna le variabili coinvolte
            currentUser.getStatistics().incNumPartiteGiocate();
            currentUser.addToWordPlayedList(secretWord);
            codice = 7;
            // Resetta le variabili per la nuova partita
            numTentativi = 0;
            currentWordAttemptsList.clear();
        }
        // Invia esito al client
        SmsSocket sms_play = new SmsSocket(null, null, null, null, null, null, codice);
        objectOutputStream.writeObject(sms_play);
    }

    // Metodo che gestisce la parola proposta dall'utente nel client
    private void sendWork(SmsSocket log) throws IOException {
        String receivedWord = log.getWord(); // Parola proposta dall'utente
        int codice = 0;
        boolean parolaIndovinata = false;
        ArrayList<String> tmpStatusWordList = new ArrayList<>(); // Lista tmp contenente gli stati dei char della parola
        if (secretWord.equals(receivedWord)) { // Parola indovinata
            codice = 7;
            numTentativi++;
            parolaIndovinata = true;
            System.out.println("Parola indovinata : " + secretWord + " | Da user : " + currentUser.getUsername());

            jsonUserList.updateUser(currentUser); // Aggiorna utente nella lista con statistiche aggiornate

            // Riempie la lista tmpStatusWordList con gli stati dei char della parola indovinata
            for (int i = 0; i < 10; i++)
                tmpStatusWordList.add("+");
            // Aggiunge la lista creata tmpStatusWordList alla lista dei tentativi per la secretWord attuale
            currentWordAttemptsList.add(tmpStatusWordList);
            // Aggiorna le statistiche
            currentUser.getStatistics().incNumPartiteVinte();
            currentUser.getStatistics().updateStreak(true);
            currentUser.getStatistics().addGuessDistribution(currentWordAttemptsList.size());
        } else if (!totalWordList.contains(receivedWord)) { // Controlla se esiste la parola
            codice = 1;
            System.out.println("Parola non esistente.");
        } else { // Parola esistente, ma non indovinata
            for (int i = 0; i < secretWord.length(); i++) { // Controlla gli stati di ogni char della parola
                char uncertainChar = receivedWord.charAt(i); // char 'i' della parola proposta
                char goodChar = secretWord.charAt(i); // char 'i' della secretWord
                System.out.print(uncertainChar + " = " + goodChar + " : ");
                // Controllo lo stato del char in questione
                if (goodChar == uncertainChar) {
                    tmpStatusWordList.add("+");
                    System.out.println("identici");
                } else if (secretWord.contains(String.valueOf(uncertainChar))) {
                    tmpStatusWordList.add("?");
                    System.out.println("e' da qualche parte nella parola");
                } else if (!secretWord.contains(String.valueOf(uncertainChar))) {
                    tmpStatusWordList.add("X");
                    System.out.println("Non e' presente");
                }
            }
            // Aggiunge la lista creata tmpStatusWordList alla lista dei tentativi per la secretWord attuale
            currentWordAttemptsList.add(tmpStatusWordList);
            numTentativi++;
            codice = 2;
        }
        if (numTentativi == 12 && !parolaIndovinata) { // Parola non indovinata
            // Aggiornamento variabili
            currentUser.getStatistics().updateStreak(false);
            jsonUserList.updateUser(currentUser);
        }
        // Invio esito
        SmsSocket sms_send_word = new SmsSocket(null, null, null, tmpStatusWordList, null, null, codice);
        objectOutputStream.writeObject(sms_send_word);
    }

    // Metodo che gestisce la richiesta di condivizione dei risultati della partita giocata
    private void share() throws IOException {
        // Creazione stringa che contiene l'esito dei tentativi della secretWord della partita giocata e invio
        StringBuilder dataSend = new StringBuilder(currentUser.getUsername() + ",");
        dataSend.append("     Wordle ").append(wordListExtracted.indexOf(secretWord)).append(" : ").append(currentWordAttemptsList.size()).append("/12\n");
        for (ArrayList<String> wordAttemps : currentWordAttemptsList) {
            dataSend.append("    ");
            for (String charAttempt : wordAttemps)
                dataSend.append(charAttempt).append(" ");
            dataSend.append("\n");
        }
        // Invio risultato della partita al gruppo multicast
        synchronized (multicastSocket) {
            byte[] buffer = dataSend.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastPort);
            multicastSocket.send(packet);
        }

        SmsSocket shareSms = new SmsSocket(null, null, null, null, null, null, 7);
        objectOutputStream.writeObject(shareSms);
    }

    private StringBuilder createStat() {
        Statistics stat = currentUser.getStatistics();

        StringBuilder send_stat = new StringBuilder("------- Ecco le statistiche personali aggiornate -------\n");
        send_stat.append(" Numero partite giocate : ").append(stat.getNumPartiteGiocate());
        send_stat.append("\n Numero partite vinte : ").append(stat.getNumPartiteVinte());
        send_stat.append("\n Percentuale partite vinte : ").append(stat.getPercentVinte()).append(" %");
        send_stat.append("\n Ultima sequenza continua di vincite : ").append(stat.getCurrentStreak());
        send_stat.append("\n Massima sequenza continua di vincite : ").append(stat.getMaxStreak());
        send_stat.append("\n Guess Distribution : ").append(stat.getGuessDistribution());
        send_stat.append("\n--------------------------------------------------------");
        return send_stat;
    }

    // Metodo che gestisce la richiesta di invio statistiche aggiornate dell'utente connesso
    private void sendMeStatistics() throws IOException {
        StringBuilder send_stat = createStat();

        SmsSocket sms_send_stat = new SmsSocket(null, null, null, null, null, send_stat, 7);
        objectOutputStream.writeObject(sms_send_stat);
    }

    // Metodo che gestisce la richiesta del logout dell'utente connesso
    private void logout() throws IOException {
        jsonUserList.updateUser(currentUser);
        int codice = 7;
        SmsSocket sms_logout = new SmsSocket(null, null, null, null, null, null, codice);
        objectOutputStream.writeObject(sms_logout);
        connectedUser.remove(currentUser.getUsername()); // Toglie utente dalla lista dei connessi
        System.out.println("Logout di [" + currentUser.getUsername() + "]");
        // Chiude connessione TCP
        objectOutputStream.close();
        objectInputStream.close();
        logout = true;
        connesso = false;
    }

    // Metodo che gestisce la richiesta del logout dell'utente connesso durante una partita in corso
    private void logoutPlay() throws IOException {
        currentUser.getStatistics().updateStreak(false);
        jsonUserList.updateUser(currentUser);
        int codice = 7;
        StringBuilder send_stat = createStat();
        SmsSocket sms_logout = new SmsSocket(null, null, null, null, null, send_stat, codice);
        objectOutputStream.writeObject(sms_logout);
        connectedUser.remove(currentUser.getUsername()); // Toglie utente dalla lista dei connessi
        System.out.println("Logout di [" + currentUser.getUsername() + "]");
        // Chiude connessione TCP
        objectOutputStream.close();
        objectInputStream.close();
        logout = true;
        connesso = false;
    }

}
