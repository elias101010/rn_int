import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Nachrichtenpaket {
    private byte[] commonHeader;
    private int info;
    private int sourceAddress;
    private int sourcePort;
    private int destinationAddress;
    private int destinationPort;
    private int ttl;
    private int nachrichtenlaenge;
    private byte[] nachrichteninhalt;
    private ArrayList<RoutingEintrag> einträge = new ArrayList<RoutingEintrag>();
    private RoutingTabelle routingTabelle = new RoutingTabelle();

    // Konstruktur empfangen
    public Nachrichtenpaket(byte[] packet) {
        if (packet.length < 18) {
            throw new IllegalArgumentException("Packet too short to contain all header fields");
        }

        // Common Header
        commonHeader = new byte[13];
        System.arraycopy(packet, 0, commonHeader, 0, 13);
        extractCommonHeaderData(commonHeader);

        // TTL
        ttl = Byte.toUnsignedInt(packet[13]);

        // Nachrichtenlänge
        nachrichtenlaenge = ((packet[14] & 0xFF) << 24) |
                ((packet[15] & 0xFF) << 16) |
                ((packet[16] & 0xFF) << 8) |
                (packet[17] & 0xFF);

        // Nachrichteninhalt
        if (packet.length < 18 + nachrichtenlaenge) {
            throw new IllegalArgumentException("Packet too short for the specified Nachrichtenlaenge");
        }

        nachrichteninhalt = new byte[packet.length -18];
        System.arraycopy(packet, 18, nachrichteninhalt, 0, packet.length -18);

        if (this.info == 1) {
            int numEntries = nachrichteninhalt.length / 7; // Each entry is 7 bytes

            System.out.println("If");
            for (int i = 0; i < numEntries; i++) {
                int offset = i * 7;

                System.out.println("For");
                // Extract ZielPeer
                String address = String.format("%d.%d.%d.%d",
                        Byte.toUnsignedInt(nachrichteninhalt[offset]),
                        Byte.toUnsignedInt(nachrichteninhalt[offset + 1]),
                        Byte.toUnsignedInt(nachrichteninhalt[offset + 2]),
                        Byte.toUnsignedInt(nachrichteninhalt[offset + 3]));

                // Extract Port
                int port = ((nachrichteninhalt[offset + 4] & 0xFF) << 8) |
                        (nachrichteninhalt[offset + 5] & 0xFF);

                // Extract Metrik
                int metrik = Byte.toUnsignedInt(nachrichteninhalt[offset + 6]);

                // Combine address and port
                String zielPeer = address + ":" + port;

                // Create a new RoutingEintrag and add it to the list
                RoutingEintrag eintrag = new RoutingEintrag(zielPeer, metrik);
                einträge.add(eintrag);
                this.routingTabelle.addEintrag(eintrag);
            }
        }
    }

    // Konstruktor Nachricht versenden
    public Nachrichtenpaket(int info, String sourceAddress, int sourcePort, String destinationAddress, int destinationPort, int ttl, String nachrichteninhalt) throws UnknownHostException {
        this.info = info;
        this.sourceAddress = inetAddressToInt(InetAddress.getByName(sourceAddress));
        this.sourcePort = sourcePort;
        this.destinationAddress = inetAddressToInt(InetAddress.getByName(destinationAddress));
        this.destinationPort = destinationPort;
        this.ttl = ttl;
        this.nachrichteninhalt = nachrichteninhalt.getBytes(StandardCharsets.UTF_8);
        this.nachrichtenlaenge = this.nachrichteninhalt.length;

        // Setze den Common Header
        commonHeader = new byte[13];
        ByteBuffer buffer = ByteBuffer.wrap(commonHeader);
        buffer.put((byte) info);
        buffer.putInt(this.sourceAddress);
        buffer.putShort((short) sourcePort);
        buffer.putInt(this.destinationAddress);
        buffer.putShort((short) destinationPort);
    }

    //Konstrukur Routingtabelle verschicken
    public Nachrichtenpaket(String sourceAddress, int sourcePort, String destinationAddress, int destinationPort, RoutingTabelle routingTabelle) throws UnknownHostException {
        this.info = 1;
        this.sourceAddress = inetAddressToInt(InetAddress.getByName(sourceAddress));
        this.sourcePort = sourcePort;
        this.destinationAddress = inetAddressToInt(InetAddress.getByName(destinationAddress));
        this.destinationPort = destinationPort;
        this.ttl = 1;
        this.nachrichteninhalt = routingTabelle.routingTableByte();
        this.nachrichtenlaenge = einträge.size();
        this.routingTabelle = routingTabelle;

        // Setze den Common Header
        commonHeader = new byte[13];
        ByteBuffer buffer = ByteBuffer.wrap(commonHeader);
        buffer.put((byte) info);
        buffer.putInt(this.sourceAddress);
        buffer.putShort((short) sourcePort);
        buffer.putInt(this.destinationAddress);
        buffer.putShort((short) destinationPort);
    }


    // Methode, um das Paket als Byte-Array zu erzeugen
    public byte[] toByteArray() {
        if(this.info == 0) {
            ByteBuffer buffer = ByteBuffer.allocate(18 + nachrichtenlaenge);

            buffer.put(commonHeader);

            buffer.put((byte) ttl);


            buffer.putInt(nachrichtenlaenge);

            buffer.put(nachrichteninhalt);


            return buffer.array();

        } else {
            ByteBuffer buffer = ByteBuffer.allocate(18 + routingTabelle.getLengthRoutingtabelle()*7);

            buffer.put(commonHeader);

            buffer.put((byte) ttl);


            buffer.putInt(nachrichtenlaenge);

            buffer.put( routingTabelle.routingTableByte());


            return buffer.array();

        }
    }



    private void extractCommonHeaderData(byte[] commonHeader) {
        ByteBuffer buffer = ByteBuffer.wrap(commonHeader);
        this.info = Byte.toUnsignedInt(buffer.get());
        this.sourceAddress = buffer.getInt();
        this.sourcePort = Short.toUnsignedInt(buffer.getShort());
        this.destinationAddress = buffer.getInt();
        this.destinationPort = Short.toUnsignedInt(buffer.getShort());
    }

    private int inetAddressToInt(InetAddress inetAddress) {
        byte[] addressBytes = inetAddress.getAddress();
        return ((addressBytes[0] & 0xFF) << 24) |
                ((addressBytes[1] & 0xFF) << 16) |
                ((addressBytes[2] & 0xFF) << 8) |
                (addressBytes[3] & 0xFF);
    }

    private String intToInetAddress(int address) {
        return ((address >> 24) & 0xFF) + "." +
                ((address >> 16) & 0xFF) + "." +
                ((address >> 8) & 0xFF) + "." +
                (address & 0xFF);
    }

    // Getter-Methoden
    public byte[] getCommonHeader() {
        return commonHeader;
    }

    public int getInfo() {
        return info;
    }

    public int getSourceAddress() {
        return sourceAddress;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public int getDestinationAddress() {
        return destinationAddress;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public int getTtl() {
        return ttl;
    }

    public int getNachrichtenlaenge() {
        return nachrichtenlaenge;
    }

    public byte[] getNachrichteninhalt() {
        return nachrichteninhalt;
    }

    public ArrayList<RoutingEintrag> getEinträge() {
        return einträge;
    }

    // Methode zum Drucken der Nachricht
    public void printMessage() {
        if (this.info == 1) {
            System.out.println("Information: " + this.getInfo());
            System.out.println("Source Address: " + intToInetAddress(this.getSourceAddress()));
            System.out.println("Source Port: " + this.getSourcePort());
            System.out.println("Destination Address: " + intToInetAddress(this.getDestinationAddress()));
            System.out.println("Destination Port: " + this.getDestinationPort());
            System.out.println("TTL: " + this.getTtl());
            System.out.println("Anzahl Einträge: "+ einträge.size());
            for (RoutingEintrag entry : einträge) {
                System.out.println("ZielPeer: " + entry.getZielPeer() + ", Metrik: " + entry.getMetrik());
            }
        } else {
            System.out.println("Information: " + this.getInfo());
            System.out.println("Source Address: " + intToInetAddress(this.getSourceAddress()));
            System.out.println("Source Port: " + this.getSourcePort());
            System.out.println("Destination Address: " + intToInetAddress(this.getDestinationAddress()));
            System.out.println("Destination Port: " + this.getDestinationPort());
            System.out.println("TTL: " + this.getTtl());
            System.out.println("Nachrichtenlänge: " + this.getNachrichtenlaenge());
            System.out.println("Nachrichteninhalt: " + new String(this.getNachrichteninhalt(), StandardCharsets.UTF_8));
        }
    }

    public void updateTTl(){
        this.ttl -= 1;
    }


    // Main-Methode für Tests
    public static void main(String[] args) {
        // Testen des Konstruktors für den Empfang eines Pakets
        byte[] messagePacket = new byte[] {
                0, -64, -88, 1, 1, 31, -112, -64, -88, 1, 2, 35, -126, // CommonHeader
                5, // TTL (1 byte)
                0, 0, 0, 4, // Nachrichtenlänge (4 bytes, value 4)
                65, 66, 67, 68 // Nachrichteninhalt (4 bytes, "ABCD")
        };

        Nachrichtenpaket mP = new Nachrichtenpaket(messagePacket);
        mP.printMessage();

        // Testen des Konstruktors zum Senden einer Routingtabelle
        byte[] routingPacket = new byte[] {
                1, -64, -88, 1, 1, 31, -112, -64, -88, 1, 2, 35, -126, // Common Header
                5, // TTL (1 byte)
                0, 0, 0, 6, // Nachrichtenlänge (4 bytes, value 21)
                (byte) 192, (byte) 168, 0, 1, 31, -112, 1, // Entry 1: 192.168.0.1:8080, Metrik 1
                (byte) 192, (byte) 168, 0, 2, 31, -111, 2, // Entry 2: 192.168.0.2:8081, Metrik 2
                (byte) 192, (byte) 168, 0, 3, 31, -110, 3,   // Entry 3: 192.168.0.3:8082, Metrik 3
                (byte) 192, (byte) 168, 0, 1, 31, -112, 1, // Entry 1: 192.168.0.1:8080, Metrik 1
                (byte) 192, (byte) 168, 0, 2, 31, -111, 2, // Entry 2: 192.168.0.2:8081, Metrik 2
                (byte) 192, (byte) 168, 0, 3, 31, -110, 3   // Entry 3: 192.168.0.3:8082, Metrik 3
        };

        Nachrichtenpaket rP = new Nachrichtenpaket(routingPacket);
        rP.printMessage();
        byte[] rpByte = rP.toByteArray();
        System.out.println("Reconstructed Byte Array: " + java.util.Arrays.toString(rpByte));
        // Testen des Konstruktors zum Senden einer Nachricht
        try {
            Nachrichtenpaket np2 = new Nachrichtenpaket(0, "192.168.1.1", 8080, "192.168.1.2", 9090, 5, "0005");
            np2.printMessage();



            byte[] byteArray = np2.toByteArray();
            System.out.println("Reconstructed Byte Array: " + java.util.Arrays.toString(byteArray));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            // Erstelle eine Routingtabelle mit mehreren Einträgen
            RoutingTabelle routingTabelle = new RoutingTabelle();
            routingTabelle.addEintrag(new RoutingEintrag("192.168.0.1:8080", 1));
            routingTabelle.addEintrag(new RoutingEintrag("192.168.0.2:8081", 2));
            routingTabelle.addEintrag(new RoutingEintrag("192.168.0.3:8082", 3));

            // Erstelle das Nachrichtenpaket mit der Routingtabelle
            Nachrichtenpaket np = new Nachrichtenpaket("192.168.1.1", 8080, "192.168.1.2", 9090, routingTabelle);

            // Drucke die Nachricht aus, um zu überprüfen
            np.printMessage();

            // Konvertiere das Nachrichtenpaket in ein Byte-Array und ausgebe
            byte[] byteArray = np.toByteArray();
            System.out.println("Reconstructed Byte Array: " + java.util.Arrays.toString(byteArray));

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}

