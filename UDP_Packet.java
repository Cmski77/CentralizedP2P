/**
 * @author Chris Mazurski, Matt Bradley, Andrew Bob
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.net.InetAddress;
import java.io.*;

public class UDP_Packet implements Serializable
{
    public String ACK = new String();
    public String sequenceNumber = new String();
    public String packetData = new String();
    public static final int MTU = 128;
    public InetAddress senderHostAddress;
    public int senderPortNumber;
    //PACKET FORMAT: "ACK SEQUENCE_NUMBER PAYLOAD_DATA"
    
    //standard UDP packet
    public UDP_Packet(String ACK, String sequence, String Data)
    {
        this.ACK = ACK;
        sequenceNumber =sequence;
        packetData = Data;
    }
    
    //standard UDP ACK packet
    public UDP_Packet(String ACK,String sequence)
     {
        this.ACK = ACK;
        sequenceNumber =sequence;
    }
    
    //Turns packet contents into a single string for easy parsing later
    public String toString()
    {
        String output= ACK+" "+sequenceNumber+" "+packetData;
        return output;
    }
    
    //toString for Ack packets
      public String toStringA()
    {
        String output= ACK+" "+sequenceNumber;
        return output;
    }
}
