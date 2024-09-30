package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Server;

public class TftpServer {
    //TODO: Implement this
    public static void main(String[] args) {
        // TODO: implement this
        int port=7777 ;
        if(args.length>=2)Integer.parseInt(args[0]) ; 
        Server.threadPerClient(
                port, // port
                () -> new TftpProtocol(), //protocol factory
                TftpEncoderDecoder::new //message encoder decoder factory
            ).serve();
        
    }
    // C:\Tftpclient.exe.exe 127.0.0.1 7777
    // 


//mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.stomp.tftp.TftpClient" -Dexec.args=127.0.0.1 7777

}

