package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.ConnectionsImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

class holder {
    static ConcurrentHashMap<Integer, Boolean> ids_login = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Boolean> username_login = new ConcurrentHashMap<>();

}

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    private int connectionId;
    private boolean shouldTerminate;
    private ConnectionsImpl<byte[]> connections;
    private boolean isLoggedIn;
    private String username;
    private String currentFile;
    // private int currentBlock = 0;
    private boolean ack; //
    private Queue<byte[]> dataACK;
    private int blockNum;
    private String fileNameCur;

    @Override
    public void start(int connectionId, ConnectionsImpl<byte[]> connections) {
        // TODO Auto-generated method stub
        this.connectionId = connectionId;
        this.connections = connections;
        this.shouldTerminate = false;
        this.isLoggedIn = false;
        holder.ids_login.put(connectionId, false);
        this.dataACK = new LinkedList<>();
        this.blockNum = 0;
        this.ack = false;
        this.username = "";
        this.currentFile = "";
        this.fileNameCur = "";

    }

    @Override
    public void process(byte[] message) {
        // TODO Auto-generated method stub
        // shouldTerminate = "bye".equals(message);

        // the first two bytes indicate the opcode
        short opcode = (short) ((message[0] << 8) | (message[1] & 0xff));
        //System.out.println("protocol : opcode is " + opcode);
        // check command
        // is loged in?
        if (!isLoggedIn && opcode != 7) {
            error(6, "");
        } else {
            boolean error = true;

            switch (opcode) {
                case 1:
                    // RRQ - Read request //server sends the file to the client
                    //isCheck
                    RRQ(message);
                    error = false;
                    break;
                case 2:
                    // WRQ - Write request
                     
                    WRQ(message);
                    error = false;
                    break;
                case 3:
                    save_data(message);
                    error = false;
                    break;
                case 4:
                    gotACK(message);
                     //isCheck
                    error = false;
                    break;
                case 6:
                    Dirq(message);
                    error = false;
                    break;
                case 7:
                   
                    LOGRQ(message);
                    error = false;
                     //isCheck
                    break;
                case 8:
                    DELRQ(message);
                    error = false;
                    break;
                case 10:
                    Disc(message);
                    error = false;
                    //isCheck
                    break;
            }
            if (error) {
                error(4, "");
            }

        }
    }

    private void gotACK(byte[] message) {
        // TODO Auto-generated method stub
        //System.out.println("the block num in gotAck func is this.blockNum ");
        if (this.ack) {
            if (this.dataACK.isEmpty()) {
                this.ack = false;
            } else {
                connections.send(connectionId, dataACK.remove());
                this.blockNum--;
            }
        } else// error
        {
            error(0, "not expecting ack packet");
        }

    }

    

        
    

    private FileOutputStream fileToWriteOS;
    public boolean writeToFile(String path, byte[] content) {

       // try (FileOutputStream outputStream = new FileOutputStream(path)) {
        try{

        
            fileToWriteOS.write(content);

            return true;
        } catch (IOException e) {
            // exception handling ...
            //System.out.println("error in write to file " +e.getMessage());
            error(2,"");
            return false;
        }
    }

    private void save_data(byte[] message) {
        //System.out.println(currentFile);
        if (currentFile=="") {
            // didn't receive WRQ request first
            // error
            //System.out.println("the curr file is null");
            error((short) 0, "not expecting data packet");
        } else {
           // String currDir = System.getProperty("user.dir");
            //String folderPath = currDir + "\\server\\Flies";
             String fullPath = getFullPath(currentFile) ; // Use backslashes for Windows

            short blockNum = (short) (((short) message[4]) << 8 | (short) (message[5]));
            short length = (short) (((short) message[2]) << 8 | (short) (message[3]));
            byte[] bytes = Arrays.copyOfRange(message, 6, length+6);
            if (writeToFile(fullPath, bytes)) {
                connections.send(connectionId, ACK(message, (short) blockNum));
                // bcast
                // BCAST();
                if(length!=512){
                    BCAST(1, currentFile);
                    currentFile = "";
                    try {
                        fileToWriteOS.close();
                    } catch (IOException e) {}
                }

            } else {
                // error, couldn't save bytes
                error((short) 2, "");
            }

        }
    }

    @Override
    public boolean shouldTerminate() {
        // TODO Auto-generated method stub
        // this.connections.disconnect(this.connectionId);
        // holder.ids_login.remove(this.connectionId);
        // return shouldTerminate;
        return shouldTerminate;
    }

    public void RRQ(byte[] message) {

        String fileName = new String(message, 2, message.length - 2, StandardCharsets.UTF_8);
        // file exist? no return ack yes return error packet
        //System.out.println(fileName);
        if(fileName!=null){
        boolean exist = fileExist(fileName);
        
        if (exist) // send data
        {
            fileName=this.fileNameCur;
            this.fileNameCur="";
            //String currDir = System.getProperty("user.dir");

            //String folderPath = currDir + "\\server\\Flies";
            //String folderPath = ".\\Skeleton\\server\\Flies"; // Folder path where the file is stored
             String fullPath =  getFullPath(fileName); // Use backslashes for Windows
            // Create a Path object representing the full path to the file
             Path pathObj = Paths.get(fullPath);
           
            //System.out.println("we enterd loop , path : "+pathObj.toString());
            try {
                byte[] fileContent = Files.readAllBytes(pathObj);
                
                int blockNumber=1;
                int left=fileContent.length;
                boolean hasToAddPacketZero =true;
                for (int i=0; i<fileContent.length;i=i+512)
                {   int from=i;
                    int to=i+512;
                    if (left<512)
                    {     to=fileContent.length; }  
                    byte[] dataPacket = data(blockNumber, Arrays.copyOfRange(fileContent, from, to));
                    if(dataPacket.length!=518) hasToAddPacketZero = false;
                    dataACK.add(dataPacket);
                    //System.out.println("block is added to queue "+blockNumber);
                     left=fileContent.length-to;
                     blockNumber++;
                }
                if (hasToAddPacketZero)
                {
                    byte[] byteArray = new byte[1];
                    byteArray[0]=(byte)0;
                    //System.out.println("we have addded  0s ize packet");
                    byte[] dataPacket = data(blockNumber,byteArray);
                    dataACK.add(dataPacket);
                    //send empty packet
                }

                //System.out.println("block num is "+ String.valueOf(blockNumber));
                this.ack = true;
                this.blockNum = blockNumber;
                connections.send(connectionId, dataACK.remove());

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                error(2, "");
            }

        } else // send error
        {
            // file not found
            error((short) 1, "");
        }
    }
    }

    public void WRQ(byte[] message)// 2
    {
        String fileName = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
        // file exist? no return ack yes return error packet
        boolean exist = fileExist(fileName);
        //System.out.println(fileName+" his length is "+fileName.length());
        if (!exist) {
           // System.out.println(" init currFile ");
            this.currentFile = fileName;
            
            //String currDir = System.getProperty("user.dir");
           // String folderPath = currDir + "\\server\\Flies";
             //String fullPath = folderPath + "\\" + this.currentFile; // Use backslashes for Windows
             String fullPath=getFullPath(this.currentFile);
             //File file = new File(".\\Skeleton\\server\\Flies\\" + this.currentFile);
             File file = new File(fullPath);
            try {
                file.createNewFile();
                fileToWriteOS = new FileOutputStream(file);
                
            } catch (IOException e) {
            }
            
            connections.send(connectionId, ACK(message, (short) 0));
        } else {
            error((short) 5, "");
        }

    }

    public byte[] data(int blockNumber, byte[] data) {
        byte[] bytes = new byte[data.length + 6];
        bytes[0] = 0;
        bytes[1] = 3;
        bytes[2] = (byte) (data.length >> 8);
        bytes[3] = (byte) (data.length & 0xff);
        bytes[4] = (byte) (blockNumber >> 8);
        bytes[5] = (byte) (blockNumber & 0xff);
        for (int i = 0; i < data.length; i++) {
            bytes[i + 6] = data[i];
        }
        return bytes;
    }

    public byte[] ACK(byte[] message, short opcode)// 0-new , 1-date ,2-the rest comands 4
    {
        byte[] bytes = new byte[4];
        bytes[0] = 0;
        bytes[1] = 4;
        bytes[2] = (byte) (opcode >> 8);
        bytes[3] = (byte) (opcode & 0xff);
        //System.out.println("we must return ACK");
        return bytes;
    }

    public void Dirq(byte[] message)// 6
    {
        
        String currDir = System.getProperty("user.dir");
        //System.out.println(currDir);
        String pathToFlies = currDir + "/server/Flies";
        //C:\Skeleton\Skeleton\server\Flies
        
        //System.out.println(pathToFlies);
        File files = new File(pathToFlies);
        if (!files.isDirectory()) {
            //System.out.println("Error: Not a valid directory path.");
            error(0,"Error: Not a valid directory path.");
            return;
        }

        //File[] listings = files.list();
        File[] listings = files.listFiles();
        String res = "";
        for (int i = 0; i < listings.length; i++) {
           // System.out.println(listings[i].getName());
            res = res + listings[i].getName() + '\0';
        }
        if (res.length() == 0) {
            error(1, "");

        }
        byte[] ListBytes = res.getBytes();

        double numOfPackets = Math.ceil(((double) ListBytes.length) / 512);
        this.blockNum = 0;
        int i = 0;
        byte[] data;
        for (int j = 0; j < numOfPackets; j++) {
            if (j < numOfPackets - 1) {
                byte[] currBlock = Arrays.copyOfRange(ListBytes, i, i + 512);
                // create data
                data = data(j+1, currBlock);
            } else {// last block
                byte[] currBlock = Arrays.copyOfRange(ListBytes, i, ListBytes.length);
                data = data(j+1, currBlock);
                // data
            }
            i += 512;
            this.blockNum++;
            dataACK.add(data);
        }
        // send(Id, data);
        this.ack = true;
        connections.send(connectionId, dataACK.remove());
    }

    public void DELRQ(byte[] message)// 8
    {
       
        //String fullPath = folderPath + "\\" + fileName;
        String fileName = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
        boolean exist = fileExist(fileName);
        if (exist) {
            fileName=this.fileNameCur;
            this.fileNameCur="";
            //String currDir = System.getProperty("user.dir");
           // String folderPath = currDir + "\\server\\Flies";
            //String fullPath = folderPath + "\\" + fileName; 
            String fullPath=getFullPath(fileName);
           // File fileDel = new File(".\\Skeleton\\server\\Flies\\" + fileName); 
            File fileDel = new File(fullPath);
            if(!fileDel.exists())
            {
                error((short) 1, "");
            }
            else {
                 //System.out.println("file is not found");
                fileDel.delete();
                 connections.send(connectionId,ACK(message, (short) 0));
                // should call bcast
                BCAST(0, fileName); }
        } else {
            error((short) 1, "");
            // error
        }
    }

    public void LOGRQ(byte[] message) {
        if (isLoggedIn) {
            // error, already logged in!
            error(0, "User logging in twice");
        } else {
            String username = new String(message, 2, message.length - 2, StandardCharsets.UTF_8);
            if (!holder.username_login.containsKey(username)) {
                // if( holder.username_login.get(username) == true)
                // // error, user already logged from a different connection
                // error(7,"");

                this.username = username;
                holder.username_login.putIfAbsent(username, true);
                isLoggedIn = true;
                //System.out.println("we have donr the log req");
                // message ACK 7
                connections.send(connectionId, ACK(message, (short) 0));
            }
        }

    }

    public void BCAST(int n, String str) {

        // connections.broadcast(message);
        byte[] strB = str.getBytes();
        byte[] bytes = new byte[strB.length + 3];
        bytes[0] = 0;
        bytes[1] = 9;
        bytes[2] = 0;
        if (n == 1) {
            bytes[2] = 1;
        }
        for (int j = 0; j < strB.length; j++) {
            bytes[j + 3] = strB[j];
        }

        for (int i = 0; i < holder.ids_login.size(); i++) {
            connections.send(i, bytes);
        }
    }

    public void Disc(byte[] message) {// 10
       // System.out.println("protocol : we joined the disc fun ");
        if (holder.username_login.containsKey(username)) {
            // remove id
            // holder.ids_login.remove(connectionId);
            holder.username_login.remove(username);
            // holder.username_login.put(username, false);
            // send ack packet
            // connections.send(clientId, ack);
        }
        shouldTerminate = true;
        // code is not arrive hear !!
        connections.send(connectionId, ACK(message, (short) 0));
        isLoggedIn = false;
        connections.disconnect(connectionId);

    }

    public void error(int i, String msg) {

        String str = "";
        if (i == 0) {
            str = msg;
        }
        if (i == 1) {
            str = "File not found";
        }
        if (i == 2) {
            str = "Access violation ";
        }
        if (i == 3) {
            str = "Disk full or allocation exceeded";
        }
        if (i == 4) {
            str = "Illegal TFTP operation";
        }
        if (i == 5) {
            str = "File already exists ";
        }
        if (i == 6) {
            str = "User not logged in ";
        }
        if (i == 7) {
            str = "User already logged in ";
        }
        byte[] bytesStr = str.getBytes();
        byte[] bytes = new byte[4 + bytesStr.length];
        bytes[0] = 0;
        bytes[1] = 5;
        bytes[2] = (byte) (i >> 8);
        bytes[3] = (byte) (i & 0xff);
        for (int j = 0; j < bytesStr.length; j++) {
            bytes[j + 4] = bytesStr[j];
        }
        connections.send(connectionId, bytes);
    }

    public String getFullPath(String name)
    {
        String currDir = System.getProperty("user.dir");
        String folderPath = currDir + "/server/Flies";
         String fullPath = folderPath + "/" + name; // Use backslashes for Windows
         return fullPath;
    }

    public boolean fileExist(String file) {
        
        String currDir = System.getProperty("user.dir");
        String pathToFlies = currDir + "/server/Flies";
        File files = new File(pathToFlies);
        String [] fileNames=files.list();
        //"C:\\Skeleton\\Skeleton\\server\\Flies"
        //File folder = new File (".\\Skeleton\\Skeleton\\server\\Flies\\");
		//String [] fileNames=folder.list(); //get a list of the filenames from the folder

        if (fileNames!=null)
        { 
            for (int i=0; i<fileNames.length; i++){ //check if the file already exist
                if ((fileNames[i].trim()).equals(file.trim())){
                     this.fileNameCur=fileNames[i];
                        return true;
                            }
                    }
        }
     return false; 
    }

}