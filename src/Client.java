import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class Client {

    private String localHost;
    private int localPort;
    private ExecutorService threadPool;
    private ConcurrentHashMap<String, Socket> peerConnections;
    private Server server;
    private RoutingTabelle routingTabelle;
    public Client(String localHost, int localPort, ExecutorService threadPool, ConcurrentHashMap<String, Socket> peerConnections, Server server,RoutingTabelle routingTabelle) {
        this.localHost = localHost;
        this.localPort = localPort;
        this.threadPool = threadPool;
        this.peerConnections = peerConnections;
        this.server = server;
        this.routingTabelle = routingTabelle;
    }

    public void connectToPeer(String host, int port) {
        try {
            Socket initialSocket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(initialSocket.getInputStream()));
            int dynamicPort = Integer.parseInt(in.readLine());
            System.out.println("Dynamischer Port erhalten: " + dynamicPort);
            try {
                PrintWriter out = new PrintWriter(initialSocket.getOutputStream(), true);
                out.println(routingTabelle.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Socket dynamicSocket = new Socket(host, dynamicPort);
            System.out.println("Mit Peer verbunden über dynamischen Port: " + dynamicSocket.getPort());

            String peerKey = host + ":" + port;
            peerConnections.put(peerKey, dynamicSocket);
            threadPool.submit(() -> server.handlePeerConnection(dynamicSocket));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToPeers(String key, String message) {
        Socket socket = peerConnections.get(key);
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printConnectedPeers() {
        System.out.println("Verbundene Peers:");
        for (Map.Entry<String, Socket> entry : peerConnections.entrySet()) {
            String peerKey = entry.getKey();
            Socket socket = entry.getValue();
            System.out.println("Peer: " + peerKey + " - IP: " + socket.getInetAddress().getHostAddress() + ", Port: " + socket.getPort());
        }
    }
}