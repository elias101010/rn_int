import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.net.InetSocketAddress;

public class Server {

    private String localHost;
    private int localPort;
    private ExecutorService threadPool;
    private ConcurrentHashMap<String, Socket> peerConnections;
    private RoutingTabelle routingTabelle;

    public Server(String localHost, int localPort, ExecutorService threadPool, ConcurrentHashMap<String, Socket> peerConnections, RoutingTabelle routingTabelle) {
        this.localHost = localHost;
        this.localPort = localPort;
        this.threadPool = threadPool;
        this.peerConnections = peerConnections;
        this.routingTabelle = routingTabelle;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(localPort, 50, InetAddress.getByName(localHost))) {
            System.out.println("Server läuft auf " + localHost + ":" + localPort + " und wartet auf Verbindungen...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Peer verbunden: " + clientSocket.getInetAddress().getHostAddress());

                // Handle initial connection
                handleInitialConnection(clientSocket);

                // Bearbeiten der Verbindung
                threadPool.submit(() -> {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                        // Lese die Routing-Tabelle vom Client
                        String routingTableStr = in.readLine();
                        System.out.println(routingTableStr);
                        RoutingTabelle receivedRoutingTabelle = RoutingTabelle.fromString(routingTableStr);

                        // Aktualisiere die lokale Routing-Tabelle
                        synchronized (routingTabelle) {
                            routingTabelle.update(receivedRoutingTabelle);
                        }

                        System.out.println("RoutingTabelle empfangen: " + routingTabelle.getTabelle());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            clientSocket.close(); // Verbindung schließen
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleInitialConnection(Socket clientSocket) {
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            ServerSocket dynamicServerSocket = new ServerSocket(0);
            int dynamicPort = dynamicServerSocket.getLocalPort();
            out.println(dynamicPort);

            Socket dynamicClientSocket = dynamicServerSocket.accept();
            System.out.println("Peer verbunden über dynamischen Port: " + dynamicPort);

            String peerKey = ((InetSocketAddress) dynamicClientSocket.getRemoteSocketAddress()).getAddress().getHostAddress() + ":" + ((InetSocketAddress) dynamicClientSocket.getRemoteSocketAddress()).getPort();
            peerConnections.put(peerKey, dynamicClientSocket);

            threadPool.submit(() -> handlePeerConnection(dynamicClientSocket));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handlePeerConnection(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Sende die Routing-Tabelle jede Sekunde
            threadPool.submit(() -> {
                while (!socket.isClosed()) {
                    out.println(routingTabelle.toString());
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            // Wrapper-Klasse für die letzte Empfangszeit
            class LastReceivedTime {
                private long value;

                public LastReceivedTime(long value) {
                    this.value = value;
                }

                public long getValue() {
                    return value;
                }

                public void setValue(long value) {
                    this.value = value;
                }
            }

            LastReceivedTime lastReceivedTime = new LastReceivedTime(System.currentTimeMillis());

            // Empfange und aktualisiere die Routing-Tabelle
            threadPool.submit(() -> {
                while (!socket.isClosed()) {
                    if (System.currentTimeMillis() - lastReceivedTime.getValue() > 1000) {
                        // Setze die Metrik auf 16 (Verbindung abgebrochen)
                        synchronized (routingTabelle) {
                            for (RoutingEintrag eintrag : routingTabelle.getTabelle()) {
                                if (eintrag.getGateway().equals(socket.getInetAddress().getHostAddress())) {
                                    eintrag.setMetrik(16);
                                }
                            }
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            String message;
            boolean methodCalled = false;
            while ((message = in.readLine()) != null) {
                //System.out.println("Empfangen von " + socket.getInetAddress().getHostAddress() + ": " + message);
                if (message.contains(",")) { // Routing-Tabelle erhalten
                    RoutingTabelle receivedRoutingTabelle = RoutingTabelle.fromString(message);
                    synchronized (routingTabelle) {
                        routingTabelle.update(receivedRoutingTabelle);
                    }
                    lastReceivedTime.setValue(System.currentTimeMillis()); // Aktualisieren
                    if(!methodCalled){
                        System.out.println(socket.getInetAddress().getHostAddress()+":"+ socket.getPort());
                        System.out.println(receivedRoutingTabelle.getFirstEntry().zielPeer);



                        threadPool.submit(() -> {
                            updateKey(((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress() + ":" + ((InetSocketAddress) socket.getRemoteSocketAddress()).getPort(), receivedRoutingTabelle.getFirstEntry().zielPeer);});
                        methodCalled = true;

                    }
                } else { // Nachricht erhalten
                    System.out.println("Nachricht: " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            String peerKey = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            peerConnections.remove(peerKey);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Verbindung zu " + socket.getInetAddress().getHostAddress() + " geschlossen.");
        }
    }

    // Methode zum Ausgeben der HashMap
    public void printPeerConnections() {
        System.out.println("Aktuelle Peer-Verbindungen:");
        for (Map.Entry<String, Socket> entry : peerConnections.entrySet()) {
            String key = entry.getKey();
            Socket socket = entry.getValue();
            System.out.println("Key: " + key + ", IP: " + socket.getInetAddress().getHostAddress() + ", Port: " + socket.getPort());
        }
    }

    public void updateKey(String oldKey, String newKey) {
        // Schritt 1: Überprüfen, ob der alte Schlüssel vorhanden ist
        if (peerConnections.containsKey(oldKey)) {
            // Schritt 2: Wert des alten Schlüssels erhalten
            Socket value = peerConnections.get(oldKey);

            // Schritt 3: Eintrag mit neuem Schlüssel hinzufügen
            peerConnections.put(newKey, value);

            // Schritt 4: Alten Eintrag entfernen
            peerConnections.remove(oldKey);

            System.out.println("Key updated successfully.");
        } else if (peerConnections.containsKey(newKey)) {
            System.out.println("Schlüssel vorhanden.");
        } else {
            System.out.println("Key '" + oldKey + "' not found in HashMap.");
        }
    }
}
