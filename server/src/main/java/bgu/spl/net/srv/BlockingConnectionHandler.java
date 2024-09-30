package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.impl.tftp.TftpEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;


public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final TftpProtocol protocol;
    private final TftpEncoderDecoder encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    public BlockingConnectionHandler(Socket sock, TftpEncoderDecoder reader, TftpProtocol protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    @Override
    public void run() {
       // System.out.println("connectionHandler:we joind");
        try (Socket sock = this.sock) { //just for automatic closing
            int read;
            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

           //"C:\Tftpclient.exe.exe" C:\
           
           
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                
                byte[] nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    protocol.process(nextMessage);
                }
            }
            
            //new 
            // if(protocol.shouldTerminate()){
            //     close();
            //  }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(byte[] msg) {
        //my implement
        try {
            //System.out.println("\nmessage to send to client:\n"+msg);
           byte[] response=encdec.encode(msg); 
           for(int i = 0; i<response.length;i++) 
                { 
                if (i==6) i=response.length;
        }
           if (response != null) {
             //System.out.println("response sent");
                out.write(response);
                out.flush();
           
           }
       }
           catch(IOException ex){
             // System.out.println("client is disconnected");
           }
    }

    public void startProtocol(int currId, ConnectionsImpl<byte[]> connections) {
        protocol.start(currId,connections);
    }
}
