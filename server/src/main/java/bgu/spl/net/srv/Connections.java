package bgu.spl.net.srv;

import java.io.IOException;

public interface Connections<T> {

    void connect(int connectionId, ConnectionHandler<T> handler);

    boolean send(int connectionId, byte[] msg);

    void disconnect(int connectionId);
}
