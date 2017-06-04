/**
 * @author Chris Mazurski, Matt Bradley, Andrew Bob
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.*;
import java.util.zip.*;
import java.net.InetAddress;
import java.io.*;

public class RDTReceiver extends Thread {
    public int sequenceNum=0;
    private int port;
    private DatagramSocket receivingSocket = null;
    private String previousSequence="-1";
    private String Data="";
    private boolean processedUpdate=false,processedQuery=false,processedExit=false;
    private byte[] archive;
    private P2PDirectory dir = new P2PDirectory();

    String tempHost,tempIP,tempFileName,tempFileSize;
    public RDTReceiver(String name, int port) {
        super(name);
        this.port = port;
    }

    public void stopListening() {
        if (receivingSocket != null) {
            receivingSocket.close();
        }
    }

    public int getAck(byte[] data)
    {
        String message= new String(data);
        String[] ACK= message.split(" ");
        return Integer.valueOf(ACK[0]);
    }

    public int getSeq(byte[] data)
    {
        String message= new String(data);
        String[] Seq= message.split(" ");
        return Integer.valueOf(Seq[1]);
    }

    public String getData(byte[] data)
    {
        //Should sufficiently handle parsing for data of String type in a UDP_Packet. 
        String message= new String(data);
        String[] Data= message.split(" ");  
        String output="";
        if (Data[0].equals("1") || Data[0].equals("0"))
        {
            for (int i=2; i<Data.length; i++)
            {
                if((i+1) == Data.length)
                    output+=Data[i];
                else
                    output+=Data[i]+" ";
            }
            return output;
        }
        else
        {
            for (int i=0; i<Data.length; i++)
            {
                if((i+1) == Data.length)
                    output+=Data[i];
                else
                    output+=Data[i]+" ";
            }
            return output;
        }
    }

    public String getQuery(String data)
    {
        String[] temp = data.split(" ");
        return temp[0];
    }

    public String getHostName(String data)
    {
        String[] temp =data.split(" ");
        return temp[1]; 
    }

    public String getIP(String data)
    {
        String[] temp = data.split(" ");
        return temp[2];
    }

    public String getFileName(String data)
    {
        int i=4;
        String accumulator="";
        //String test ="INFORM_AND_UPDATE HOST_NAME IP_ADDRESS \r\n directoryServer.class 763bytes \r\n directoryServer.ctxt 107bytes";
        String[] temp = data.split(" ");
        //System.out.println("THE FILENAME IS:"+temp[6]); //The very first filename is at index 4, increment by 3 to get the next one
        //System.out.println("THE NEXT FILENAME IS:"+temp[9]);
        while(i<temp.length)
        {
            if(temp[i].contains("."))
                accumulator+=temp[i]+" ";
            i+=1;
        }

        accumulator = accumulator.replaceAll("\\r\\n", "");
        accumulator=accumulator.trim();

        i=0;
        String[] temp2= accumulator.split(" ");
        accumulator="";
        while(i<temp2.length)
        {
            if(temp2[i].contains(".") && !temp2[i].contains("bytes"))
                accumulator+=temp2[i]+" ";
            else
            {
                temp2[i]=temp2[i].replace("bytes",""); //hard fixes for that one file that never concatenates right
                temp2[i]=temp2[i].replace("11","");
                accumulator+=temp2[i]+" ";
            }

            i++;
        }
        return accumulator;
    }

    public String getFileSize(String data)
    { 
        int i=5;
        String accumulator="";
        String[] temp = data.split(" ");
        while(i<temp.length)
        {
            if(temp[i].contains("bytes"))
                accumulator+=temp[i]+" ";
            i+=1;
        }
        accumulator = accumulator.replaceAll("\\r\\n", "");
        accumulator=accumulator.trim();

        i=0;
        String[] temp2= accumulator.split(" ");
        accumulator="";
        while(i<temp2.length)
        {
            if(temp2[i].contains("bytes"))
                accumulator+=temp2[i]+" ";
            i++;
        }
        return accumulator;
    }

    public void processRequest(InetAddress hostDestination, int destinationPortNumber)
    {
        try{
            if(getQuery(Data).equals("INFORM_AND_UPDATE"))
            {
                //Do thing
                Data=Data.trim();
                //System.out.println("This is the pristine untouched Data");
                //System.out.println(Data); 
                //Data= String.format("%-10s",Data);
                //System.out.println(Data);
                //System.out.println("These are the filenames....");
                //System.out.println(getFileName(Data));
                String[] rawFileNames=getFileName(Data).split(" ");
                //System.out.println("These are the filesizes....");
                //System.out.println(getFileSize(Data));
                String[] rawFileSizes=getFileSize(Data).split(" ");

                System.out.println("@@@ PERFORMING INFORM AND UPDATE PROCESS");
                for (int i= 0; i<rawFileNames.length; i++)
                {
                    dir.ADDc(getHostName(Data),getIP(Data),rawFileNames[i],rawFileSizes[i]);
                    System.out.println(getHostName(Data)+" "+getIP(Data)+" "+rawFileNames[i]+" "+rawFileSizes[i]);
                }
                Data="";  //The Data has been entered into the directory database, so this buffer should be emptied now

                UDP_Packet update = new UDP_Packet("200","OK","Listing updated");
                DatagramPacket dirUpdate= new DatagramPacket(update.toString().getBytes(),update.toString().getBytes().length,hostDestination,destinationPortNumber);
                receivingSocket.send(dirUpdate);
                processedUpdate=true;
            }
            else
            if(getQuery(Data).equals("QUERY_FOR_CONTENT"))
            {     
                System.out.println("@@@ RECEIVER THINKS THAT THE USER IS REQUESTING FOR :"+getFileName(Data));
                String desiredFile=getFileName(Data).trim(); //trim to make sure whitespace doesn't mess with the string
                if (dir.REQUESTc(desiredFile).equals("File Not Found"))
                {
                    processedQuery=true; 
                    UDP_Packet Error = new UDP_Packet("400","ERROR","File not Found");
                    DatagramPacket notFound= new DatagramPacket(Error.toString().getBytes(),Error.toString().getBytes().length,hostDestination,destinationPortNumber);
                    System.out.println("@@@Receiver sending error code (File not found)");
                    receivingSocket.send(notFound);
                }

                if (!dir.REQUESTc(desiredFile).equals("File Not Found"))
                {
                    UDP_Packet response= new UDP_Packet("200","OK",dir.REQUESTc(desiredFile));
                    DatagramPacket table= new DatagramPacket(response.toString().getBytes(),response.toString().getBytes().length,hostDestination,destinationPortNumber);
                    System.out.println("@@@Receiver is sending the table");
                    receivingSocket.send(table);
                    processedQuery=true;
                }
                Data="";
            }
            else
            if(getQuery(Data).equals("EXIT"))
            {
                System.out.println("Entering Exit block");
                dir.EXITc(getHostName(Data));
                System.out.println("@@@Removing" +getHostName(Data)+"from the directory...");
                Data="";
                UDP_Packet Exit= new UDP_Packet("200","OK","Client files succesfully removed from directory listing");
                DatagramPacket goodBye= new DatagramPacket(Exit.toString().getBytes(),Exit.toString().getBytes().length,hostDestination,destinationPortNumber);
                receivingSocket.send(goodBye);
                processedExit=true;
            }
            else
            if(!getQuery(Data).equals("EXIT") &&!getQuery(Data).equals("QUERY_FOR_CONTENT") && !getQuery(Data).equals("INFORM_AND_UPDATE"))
            {
                System.out.println("@@@ Failure to process, sending error code 400" );    
                UDP_Packet fail= new UDP_Packet("400","Error","Processing Error");
                DatagramPacket failure= new DatagramPacket(fail.toString().getBytes(),fail.toString().getBytes().length,hostDestination,destinationPortNumber);
                receivingSocket.send(failure);
                Data="";
                //processedExit=true;
            }

        }catch (IOException e) 
        {
            System.out.println("ERROR PROCESSING REQUESTS");
        }
    }

    public void sendAck(String incomingSequenceNumber,InetAddress hostDestination, int destinationPortNumber, byte[] buffer) throws IOException
    {
        UDP_Packet ack= new UDP_Packet("1","0"); //initialization

        if (getData(buffer).equals("TERMINATE"))
        {
            //If the client has sent us a termination packet, then that means receiver is ready to take a look at the request
            sequenceNum=0; //Seq number is set back to its default value
            
            System.out.println("@@@Termination packet recieved, processing request...");
            processRequest(hostDestination,destinationPortNumber);
        }
        else if ((Integer.valueOf(incomingSequenceNumber)==(Integer.valueOf(previousSequence)))) 
        {
            ack = new UDP_Packet("1",previousSequence); //Sender gave us a retransmission, so send an ACK with the old sequence number
            System.out.println("The previousSequence number is "+previousSequence+", and sender sent a duplicate"); 
            if(getData(archive).contains("QUERY_FOR_CONTENT") || getData(archive).contains("EXIT"))
            //Special Case: Program crashes client when they try to do two of the same Queries in a row, since a "duplicate" request doesn't update the buffer before the 
            //termination packet gets processed and sent. Solution is to allow consecutive QUERY FOR CONTENT requests to update the buffer. 
            //Consecutive INFORMS or EXITS will crash client at this time
                Data+=getData(buffer);
            
            
        }
        else
        {
            ack = new UDP_Packet("1",incomingSequenceNumber); //Sender gave us a new packet, ACK it with the new sequence number
            
            //store the old values just in case the ack gets lost and the sender has to resend 
            previousSequence=incomingSequenceNumber;
            archive=buffer;
            
            Data +=getData(buffer);
            System.out.println("@@@ Reciever now contains the following message: "+Data);
        }

        //Send the Ack over to the Sender 

        if (processedUpdate==false && processedQuery==false && processedExit==false) //If there was nothing to be processed, continue ACKing
        {
            DatagramPacket ackPacket = new DatagramPacket(ack.toStringA().getBytes(),ack.toStringA().getBytes().length,hostDestination,destinationPortNumber);
            try{  
                sequenceNum++; //inrecement the sequence number after an ACK gets sent off
                receivingSocket.send(ackPacket);
                System.out.println("@@@ Receiver sending ACK with sequence number "+ack.sequenceNumber);
                
            }catch (IOException e) {
                System.out.println("ERROR WITH SENDING ACKS");
            }
        }
        processedUpdate=false;
        processedQuery=false;
        processedExit=false;
    }

    public void run() {
        try {
            receivingSocket = new DatagramSocket(2003);
            while (true) {
                System.out.println("@@@ Receiver waiting for packet");
                byte[] buf = new byte[UDP_Packet.MTU];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                receivingSocket.receive(packet);

                byte[] packetData = Arrays.copyOf(packet.getData(), packet.getLength());

                //To ensure that the sequence numbers are accurate, a check is implemented here 
                //that will ensure a duplicate packet gets sent out with the right sequence number
                if(Arrays.equals(archive,packetData))
                {
                    sequenceNum--;
                }
                else
                    archive=packetData;

                //From here, we need to increment the sequence number (global variable here), and respond by
                //sending back an ACK packet or response code

                //Start by getting the relevant info of the sender of the packet
                InetAddress senderHostAddress= packet.getAddress();
                String senderHostName= packet.getAddress().toString();
                int senderPortNumber= packet.getPort();

                System.out.println("@@@ Incoming packet from:"+packet.getAddress());

                //Now to send the Ack:
                sendAck(String.valueOf(sequenceNum),senderHostAddress,senderPortNumber,packetData);
            }
        } catch (Exception e) {
            stopListening();
        }
    }
}
