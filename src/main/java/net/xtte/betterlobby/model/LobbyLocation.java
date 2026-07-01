package net.xtte.betterlobby.model;

public class LobbyLocation {

    private final String name;
    private final String server;
    private final TeleportPoint point;

    public LobbyLocation(String name, String server, TeleportPoint point) {
        this.name = name;
        this.server = server;
        this.point = point;
    }

    public String getName() {
        return name;
    }

    public String getServer() {
        return server;
    }

    public TeleportPoint getPoint() {
        return point;
    }
}
