package bgu.spl.net.srv;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    public ConcurrentHashMap<Integer, ConnectionHandler<T>> clients;

    public ConnectionsImpl() {
        clients = new ConcurrentHashMap<>();

        // messageid=0;
    }

    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler) {
        // clients.putIfAbsent(connectionId, handler)
        if (!clients.containsKey(connectionId)) {
            clients.put(connectionId, handler);
            // id++
            // return true;
        }
        // return false; // ConnectionId already exists
    }

    @Override
    public boolean send(int connectionId, byte[] msg) {
        if (clients.get(connectionId) != null) {
            //System.out.println("we have to send msg to the client "+msg[1]);
            clients.get(connectionId).send(msg);// CH send
            return true;
        }
        return false; // ConnectionId not found
    }

    @Override
    public void disconnect(int connectionId) {
        if (clients.get(connectionId) != null) {
            try {

                clients.get(connectionId).close();
            } catch (IOException e) {}
            clients.remove(connectionId);
        }
    }
    

}
