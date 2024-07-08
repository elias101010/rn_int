public class RoutingEintrag {
    String zielPeer;
    String gateway;
    int metrik;

    public RoutingEintrag(String zielPeer,String gateway, int metrik){
        this.zielPeer = zielPeer;
        this.gateway = gateway;
        this.metrik = metrik;
    }

    public RoutingEintrag(String zielPeer, int metrik){
        this.zielPeer = zielPeer;
        this.metrik = metrik;
    }

    public static void main(String[] args) {

        RoutingEintrag eintrag = new RoutingEintrag("ziel1", "hop2", 0);
    }
    public void setZielPeer(String zielPier){
        this.zielPeer = zielPier;
    }
    public void setGateway(String gateway){
        this.gateway = gateway;
    }
    public void setMetrik(int metrik){
        this.metrik = metrik;
    }
    public String getZielPeer(){
        return zielPeer;
    }
    public String getGateway(){
        return gateway;
    }
    public int getMetrik(){
        return metrik;
    }
}