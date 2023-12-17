import java.io.*;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Random;

// Per ogni tot tempo (delayWordUpdate) viene aggiornata la variabile newWord e eaggiornata la lista delle parole estratte
public class ThreadWordManager implements Runnable {

    private Vector<String> wordListExtracted; // Lista parole estratte dal file words.txt
    private final String wordFilename; // words.txt
    private AtomicReference<String> newWord; // Nuova parola estratta

    public ThreadWordManager(String wordFilename, AtomicReference<String> newWord, Vector<String> wordListExtracted) {

        this.wordFilename = wordFilename;
        this.wordListExtracted = wordListExtracted;
        this.newWord = newWord;
    }

    public void run() {
        try {
            RandomAccessFile fileReader = new RandomAccessFile(wordFilename, "r");
            int countWords = (int) (fileReader.length() / 11); // num parole nel file  (11 = len(parola) + '\n')

            Random random = new Random();
            int randomWord = random.nextInt(countWords);

            fileReader.seek(randomWord * 11); // Posiziona puntatore del file alla posizione randomWord
            String wordExtracted = fileReader.readLine();
            newWord.set(wordExtracted); // Modifica la variabile con la nuova parola estratta
            wordListExtracted.add(wordExtracted); // Aggiungi alle parole estratte

            System.out.println("WORD estratta : " + wordExtracted + " | Parola numero : " + wordListExtracted.size());
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
