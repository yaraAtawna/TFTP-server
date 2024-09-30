package bgu.spl.net.impl.tftp;

import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    // TODO: Implement here the TFTP encoder and decoder
    private byte[] bytes = new byte[1 << 10]; // start with 1k
    private int len = 0;
    private int opcode = -1;
    private int packetSize = 0;
    //server
    

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        pushByte(nextByte);
        if (len == 2) {
            short result = (short) ((bytes[0] & 0xff) << 8);
            result += (short) (bytes[1] & 0xff);
            opcode = result;
        }
        
        if (len >= 2) {
            if (opcode == 1 || opcode ==2 || opcode ==7 || opcode ==8 || opcode == 9 || opcode ==5 ) {
                if (nextByte == (byte)0) {
                   // System.out.println("we must return the byte[]");
                    return popBytes();
                }
            } else if (opcode == 10 || opcode == 6) {
                //System.out.println("must return two bytes");
                return popBytes();
            } else if(opcode == 3){
                if(len == 4){
                    short result = (short) ((bytes[2] & 0xff) << 8);
                    result += (short) (bytes[3] & 0xff);
                    packetSize = result;
                }
                if(packetSize + 6 == len){
                    return popBytes();
                }
            }
            else if(opcode == 4){
                if(len == 4){
                    //System.out.println("we must return the byte[]");
                    return popBytes(); 
                }}
        }
        // else if (){

        // }

        return null; // not a complete line yet

    }

    @Override
    public byte[] encode(byte[] message) {
        // TODO: implement this
        short opcode = (short) ((message[0] << 8) | (message[1] & 0xff));
        if(opcode == 1 || opcode == 2 || opcode == 5 || opcode == 7 || opcode == 8 || opcode == 9)
        {
            if(message[message.length-1]==(byte)0) return message;
            
            byte [] newBytesToSend = new byte[message.length+1];
            for(int i=0;i<message.length;i++){
                newBytesToSend[i]=message[i];

            }
            newBytesToSend[newBytesToSend.length-1]=(byte) 0;
             return newBytesToSend; // uses utf8 by default
        }
        return message;

    }

    // added
    private byte[] popBytes() {
        // notice that we explicitly requesting that the string will be decoded from
        // UTF-8
        // this is not actually required as it is the default encoding in java.
        byte[] bytes1 = new byte[len];
        bytes1 = Arrays.copyOf(bytes, len);
        len = 0;
        opcode = -1;
        packetSize = 0;
        bytes = new byte[518];
        return bytes1;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len] = nextByte;
        len++;
        /* */
        // new

    }
}