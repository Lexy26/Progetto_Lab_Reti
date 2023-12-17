import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;

// Classe per la gestione notifiche delle partite condivise attraverso la connessione multicast
public class ThreadShareManager implements Runnable {

    private MulticastSocket multicastSocket;
    private int multicastPort;
    private String multicastIP;
    private ArrayList<String> sharedGameList; // Lista partite condivise dai giocatori
    private boolean logout = false;
    private String clientUsername; // Variabile utile er evitare di vedere la propria partita condivisa


    public ThreadShareManager(int multicastPort, String multicastIP, String clientUsername) {
        this.multicastPort = multicastPort;
        this.multicastIP = multicastIP;
        this.clientUsername = clientUsername;
    }

    @Override
    public void run() {
        try {
            multicastSocket = new MulticastSocket(multicastPort);
            InetAddress group = InetAddress.getByName(multicastIP);
            multicastSocket.joinGroup(group);
            sharedGameList = new ArrayList<>();
            while (!logout) {
                byte[] buffer = new byte[2048];
                DatagramPacket sharedGame = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(sharedGame);
                sharedGameList.add(new String(sharedGame.getData(), 0, sharedGame.getLength()));
            }
        } catch (SocketException e) {
            if (!logout)
                e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        logout = true;
        multicastSocket.close();
    }

    // Stampa tutte le partite condivise
    public void printSharedGameList() {

        // Viene controllato se tutte le partite condivise sono dell'utente corrente.
        int tot_partite_condivise = 0;
        int tot_partite_username_corrente = 0;
        for (String game : sharedGameList) {
            String[] dataRecieved = game.split(",");
            String username = dataRecieved[0];
            tot_partite_condivise++;
            if (clientUsername.equals(username)) {
                tot_partite_username_corrente++;
            }
        }

        // Se sì, si salta l'if e si procede direttamente con l'else,
        // evitando di scorrere inutilmente la lista per stampare i risultati condivisi.
        if (!sharedGameList.isEmpty() && tot_partite_condivise != tot_partite_username_corrente) {
            System.out.println("[Legenda: grigio->'X' | giallo->'?' | verde->'+']");
            System.out.println();
            for (String game : sharedGameList) {
                String[] dataRecieved = game.split(",");
                String username = dataRecieved[0];
                if (!clientUsername.equals(username)) {
                    System.out.println();
                    System.out.println("----- Username : " + username + " -----");
                    System.out.println(dataRecieved[1]);
                }
            }
            sharedGameList.clear();
        } else System.out.println("Nessun giocatore ha condiviso una partita, per il momento.");
    }
}
