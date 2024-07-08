import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RoutingTabelle {
    // Unsere Tabelle
    ArrayList<RoutingEintrag> listeEintraege;

    public RoutingTabelle(){
        listeEintraege = new ArrayList<RoutingEintrag>();
    }
    // holen uns den ersten entry
    public RoutingEintrag getFirstEntry(){
        return listeEintraege.get(0);
    }
    // Füge Eintrag ans Ende der Liste
    public void addEintrag(RoutingEintrag neuerEintrag){
        listeEintraege.add(neuerEintrag);
    }

    // Füge Eintrag an übergebene Position
    public void addEintrag(int position, RoutingEintrag neuerEintrag){
        listeEintraege.add(position, neuerEintrag);
    }

    // Lösche Eintrag
    public void deleteEintrag(RoutingEintrag geloeschterEintrag){
        listeEintraege.remove(geloeschterEintrag);
    }

    // Lösche Eintrag über Position
    public void deleteEintrag(int geloeschterEintrag){
        listeEintraege.remove(geloeschterEintrag);
    }

    // Methode zum Ausgeben der Liste
    public void ausgebenListe(){
        for (RoutingEintrag eintrag: listeEintraege){
            System.out.println("Zielpeer: " + eintrag.getZielPeer() + " | " +
                    "Gateway: " + eintrag.getGateway() + " | " +
                    "Metrik: " + eintrag.getMetrik());
        }
    }

    public void update(RoutingTabelle neueTabelle) {
        synchronized (listeEintraege) {
            // Aktualisiere oder füge Einträge hinzu
            for (RoutingEintrag neuerEintrag : neueTabelle.getTabelle()) {
                boolean gefunden = false;
                for (int i = 0; i < listeEintraege.size(); i++) {
                    RoutingEintrag eintrag = listeEintraege.get(i);
                    if (eintrag.getZielPeer().equals(neuerEintrag.getZielPeer())) {
                        // Eintrag existiert bereits, aktualisiere ihn
                        listeEintraege.set(i, neuerEintrag);
                        gefunden = true;
                        break;
                    }
                }
                if (!gefunden) {
                    // Eintrag existiert nicht, füge ihn hinzu
                    listeEintraege.add(neuerEintrag);
                }
            }

            // Setze die Metrik auf 16 für Einträge, die nicht in der neuen Tabelle sind
            for (RoutingEintrag eintrag : listeEintraege) {
                boolean gefunden = false;
                for (RoutingEintrag neuerEintrag : neueTabelle.getTabelle()) {
                    if (eintrag.getZielPeer().equals(neuerEintrag.getZielPeer())) {
                        gefunden = true;
                        break;
                    }
                }
                if (!gefunden) {
                    eintrag.setMetrik(16);
                }
            }
        }
    }


    // Methode zum Erstellen einer Tabelle aus einem String
    public static RoutingTabelle fromString(String str) {
        RoutingTabelle tabelle = new RoutingTabelle();
        String[] eintraege = str.split(";");
        for (String eintragStr : eintraege) {
            String[] details = eintragStr.split(",");
            if (details.length == 3) {
                String zielPeer = details[0];
                String gateway = details[1];
                int metrik = Integer.parseInt(details[2]);
                RoutingEintrag eintrag = new RoutingEintrag(zielPeer, gateway, metrik);
                tabelle.addEintrag(eintrag);
            }
        }
        return tabelle;
    }

    // Methode zum Umwandeln der Tabelle in einen String
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (RoutingEintrag eintrag : listeEintraege) {
            sb.append(eintrag.getZielPeer()).append(",")
                    .append(eintrag.getGateway()).append(",")
                    .append(eintrag.getMetrik()).append(";");
        }
        return sb.toString();
    }

    // Methode zum Zurückgeben der Liste der Routing-Einträge
    public ArrayList<RoutingEintrag> getTabelle() {
        return listeEintraege;
    }

    public byte[] routingTableByte() {
        int eintragGroesse = 4 + 2 + 1; // 4 Bytes for ZielPeer, 2 Bytes for Port, 1 Byte for Metrik
        ByteBuffer buffer = ByteBuffer.allocate(listeEintraege.size() * eintragGroesse);

        for (RoutingEintrag eintrag : listeEintraege) {
            // Split ZielPeer into IP address and port
            String zielPeer = eintrag.getZielPeer();
            String[] ipAndPort = zielPeer.split(":");
            String ipAddress = ipAndPort[0];
            int port = Integer.parseInt(ipAndPort[1]);

            // Convert IP address to 4 bytes
            String[] adresseTeile = ipAddress.split("\\.");
            for (String teil : adresseTeile) {
                buffer.put((byte) Integer.parseInt(teil));
            }

            // Convert port to 2 bytes
            buffer.putShort((short) port);

            // Convert Metrik to 1 byte
            buffer.put((byte) eintrag.getMetrik());
        }

        return buffer.array();
    }

    public int getLengthRoutingtabelle(){
        return listeEintraege.size();
    }

    public void erreichbarePeers() {
        System.out.println("Erreichbare Peers:");

        int peerNummer = 1;
        for (RoutingEintrag eintrag : listeEintraege) {
            int metrik = eintrag.getMetrik();
            if (metrik > 0 && metrik < 16) {
                System.out.printf("Peer %d: %s%n", peerNummer, eintrag.getZielPeer());
                peerNummer++;
            }
        }
    }

    public static void main(String[] args) {
        RoutingTabelle tabelle = new RoutingTabelle();
        tabelle.addEintrag(new RoutingEintrag("127.0.0.1:5000", "192.168.0.1", 1));
        tabelle.addEintrag(new RoutingEintrag("192.168.1.1:8080", "192.168.0.1", 2));
        tabelle.addEintrag(new RoutingEintrag("10.0.0.1:65535", "192.168.0.1", 3));

        byte[] routingTableBytes = tabelle.routingTableByte();

        System.out.println("Routing Table Byte Array:");
        for (byte b : routingTableBytes) {
            System.out.printf("%d ", b);
        }

        tabelle.erreichbarePeers();
    }
}
