import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.Socket;

public class Main {

    private static ExecutorService threadPool;
    private static String localHost;
    private static int localPort;
    private static Client client;
    private static Server server;
    private static ConcurrentHashMap<String, Socket> peerConnections = new ConcurrentHashMap<>();
    private static RoutingTabelle routingTabelle = new RoutingTabelle();
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        threadPool = Executors.newFixedThreadPool(10);

        System.out.println("Geben Sie die IP-Adresse für diesen Peer ein:");
        localHost = scanner.nextLine();

        System.out.println("Geben Sie den Port für diesen Peer ein:");
        localPort = Integer.parseInt(scanner.nextLine());

        RoutingEintrag eintrag = new RoutingEintrag(localHost + ":" + localPort, localHost +":"+ localPort,0);
        routingTabelle.addEintrag(eintrag);

        server = new Server(localHost, localPort, threadPool, peerConnections,routingTabelle);
        client = new Client(localHost, localPort, threadPool, peerConnections, server, routingTabelle);

        threadPool.submit(() -> server.startServer());

        // Starte einen separaten Thread für die Benutzerinteraktion
        threadPool.submit(() -> {
            while (true) {
                System.out.println("1. Mit einem Peer verbinden");
                System.out.println("2. Nachricht senden");
                System.out.println("3. Verbundene Peers anzeigen");
                System.out.println("4. Erreichbare Teilnehmer anzeigen");
                System.out.println("5. Routingtabelle anzeigen");
                System.out.println("Wählen Sie eine Option:");
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        System.out.println("Geben Sie die IP-Adresse des Ziel-Peers ein:");
                        String targetHost = scanner.nextLine();
                        System.out.println("Geben Sie den Port des Ziel-Peers ein:");
                        int targetPort = Integer.parseInt(scanner.nextLine());
                        client.connectToPeer(targetHost, targetPort);
                        break;
                    case 2:
                        System.out.println("Geben Sie die IP-Adresse des Ziel-Peers ein:");
                        String messageHost = scanner.nextLine();
                        System.out.println("Geben Sie den Port des Ziel-Peers ein:");
                        int messagePort = Integer.parseInt(scanner.nextLine());
                        System.out.println("Geben Sie die Nachricht ein:");
                        String message = scanner.nextLine();
                        client.sendMessageToPeers(messageHost + ":" + messagePort, message);
                        break;
                    case 3:
                        client.printConnectedPeers();
                        break;
                    case 4:
                        server.printPeerConnections();
                        break;
                    case 5:
                        routingTabelle.ausgebenListe();
                        break;
                    default:
                        System.out.println("Ungültige Wahl. Bitte versuchen Sie es erneut.");
                }
            }
        });

        // Hauptthread wartet, um die Benutzerinteraktion und Nachrichtenempfang zu koordinieren
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}