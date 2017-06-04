
import java.util.Arrays;
import java.io.*;
import java.net.*;
import java.util.zip.*;

/**
 * @author Chris Mazurski, Matt Bradley, Andrew Bob
 */
public class RDTSender {
    private int receiverPortNumber = 0;
    private DatagramSocket socket = null;
    private InetAddress internetAddress = null;
    private String previousSequence="";
    private int seqNumber;
    private int recievedSeq= -1;
    private boolean resendMode=true;
    private static int x = 15000;
    private String Data="";
    public RDTSender() {
    }

    public void startSender(byte[] targetAddress, int receiverPortNumber) throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        internetAddress = InetAddress.getByAddress(targetAddress);
        this.receiverPortNumber = receiverPortNumber;
    }

    public void stopSender() 
    {
        if (socket!=null){
            socket.close();
        }
    }

    public String getAck(byte[] data)
    {
        String message= new String(data);
        String[] ACK= message.split(" ");
        return ACK[0];
    }

    public String getSeq(byte[] data)
    {
        String message= new String(data);
        String[] Seq= message.split(" ");
        return Seq[1];
    }

    public String getData(byte[] data)
    {
        String message= new String(data);
        String[] Data= message.split(" ");  
        String output="";
        for (int i=2; i<Data.length; i++)
        {
            output+=Data[i]+" ";
        }
        return output;
    }

    public UDP_Packet waitForAck() throws IOException,ClassNotFoundException
    {
        byte[] buf = new byte[UDP_Packet.MTU];
        UDP_Packet receivedAck;
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        UDP_Packet failure = new UDP_Packet("0","0");
        //A timeout will occur after 5000 milliseconds of waiting
        socket.setSoTimeout(x);

        try{
            socket.receive(packet);
        }catch (SocketTimeoutException e)
        {              
            return failure; 
        }

        byte[] packetData = Arrays.copyOf(packet.getData(), packet.getLength());
        receivedAck =new UDP_Packet(getAck(packetData),getSeq(packetData),getData(packetData));  

        //System.out.println(getData(packetData));
        return receivedAck;

    }

    public void rdtSend(byte[] data) throws SocketException, IOException, InterruptedException,ClassNotFoundException {

        //MTU is 128
        //
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);

        int packetNumber = 0;
        int totalPackets;
        while (byteStream.available()>0){

            //First few blocks sets up the packet, packet length, etc,
            byte[] packetData = new byte[UDP_Packet.MTU];
            int bytesRead = byteStream.read(packetData); 
            totalPackets=  bytesRead / 128;

            if (bytesRead<packetData.length)
            {
                packetData = Arrays.copyOf(packetData, bytesRead);
            }

            System.out.println("### Sender sending packet("+new String((packetNumber++)+")")+"of "+totalPackets+": '" +new String(packetData)+"'");
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, internetAddress, receiverPortNumber);

            //At the moment the starting sequence number is hardcoded to 0 in the initial packet and receiver, subject to change
            if (recievedSeq == -1) //If this is the first packet, get that packet's seq number and store it
            {
                seqNumber=Integer.valueOf(getSeq(packetData));
                recievedSeq=0;
            }

            System.out.println("### Sender is expecting an ACK with sequence number :"+seqNumber+" or a response code");

            socket.send(packet);
            //Now that the packet was sent, we wanna know if it got there ok
            UDP_Packet receiverResponse = waitForAck();

            if (receiverResponse.ACK.equals("1") && Integer.valueOf(receiverResponse.sequenceNumber) == seqNumber)
            {
                //System.out.println("**********************************************************");
                System.out.println("### Sender got an ACK from the reciever for the last packet");
                //System.out.println("**********************************************************");
                seqNumber++;
            }
            else if(receiverResponse.ACK.equals("200") && receiverResponse.sequenceNumber.equals("OK"))
            {
                System.out.println("**********************************************************");
                System.out.println("### 200 OK: Directory Server has responded to the request");
                System.out.println("**********************************************************");
                System.out.println();
                System.out.println(receiverResponse.packetData);
            }

            else if(receiverResponse.ACK.equals("400") && receiverResponse.sequenceNumber.equals("ERROR"))
            {
                System.out.println("**********************************************************");
                System.out.println("### 400 ERROR: directory failed to process request");
                System.out.println("**********************************************************");
                System.out.println();
                System.out.println(receiverResponse.packetData);

            }
            else if(receiverResponse.ACK.equals("0") || Integer.valueOf(receiverResponse.sequenceNumber) != seqNumber)
            {
                System.out.println("### Sender failed to recieve an ACK (or recieved a Duplicate ACK), going into resend cycle...");
                resendMode = false;
                while(resendMode == false)
                {
                    System.out.println("## Resending");
                    socket.send(packet);
                    receiverResponse = waitForAck();
                    if (receiverResponse.ACK.equals("1") && Integer.valueOf(receiverResponse.sequenceNumber) == seqNumber)
                    {
                        System.out.println("### Sender got an ACK from the reciever for the last packet");
                        seqNumber++;
                        resendMode = true;
                    }
                    //Thread.sleep(1200);
                }
            }

            //System.out.println("### The sequence number of the packet getting ACK'd is: "+getSeq(receiverResponse.toStringA().getBytes()));
            System.out.println("### The sequence number of the packet getting ACK'd is: "+(receiverResponse.sequenceNumber));
            //Thread.sleep(1200);

        }

        System.out.println("### Sender done sending");

        recievedSeq=-1;
    }
}
