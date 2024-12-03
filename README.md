# Progetto Wordle Game

**Laboratorio Reti 22-23 - UniPi**

## Instruzioni per l'installazione e l'esecuzione del gioco Wordle

Per eseguire il gioco, ci sono due metodi disponibili:

### 1. Esecuzione Da Riga di Comando

#### Passi per Compilare ed Eseguire il Gioco:

##### A. Server
1. Entrare nella cartella `src`.
2. Aprire un terminale dedicato all'avvio del **server**.
3. Sul terminale del server:
   - **Per compilare**: 
     - Su Linux: `javac -d ./../out -cp ".:gson-2.8.2.jar" WordleServerMain.java`
     - Su Windows: `javac -d ./../out -cp ".;gson-2.8.2.jar" WordleServerMain.java`
   - **Per eseguire**: 
     - Su Linux: `java -cp "./../out/:gson-2.8.2.jar" WordleServerMain`
     - Su Windows: `java -cp "./../out/;gson-2.8.2.jar" WordleServerMain`

##### B. Client
1. Aprire due (o più) terminali dedicati al **client** per testare concorrenza e condivisione del risultato di una partita.
2. Sul terminale del client:
   - **Per compilare**: 
     - Su Windows: `javac -encoding UTF-8 -d ./../out WordleClientMain.java`
     - Su Linux: `javac -d ./../out WordleClientMain.java`
   - **Per eseguire** (su Linux/Windows): `java -cp ./../out WordleClientMain`

### 2. Utilizzo dei File Eseguibili JAR

#### Passi per Eseguire il Gioco:

1. **Server**: Sul terminale, eseguire `java -jar server.jar`.
2. **Client**: Sul terminale, eseguire `java -jar client.jar`.
