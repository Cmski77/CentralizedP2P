import java.io.*;
import java.net.*;
import java.util.*;
/**
 * Write a description of class P2PServer here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class P2PServer extends Thread
{
    private ServerSocket server;
    private Socket client;
    //private InputStream in;
    //private DataOutputStream out;
    //private BufferedReader httpIN; 
    //private PrintWriter httpOUT;
    private String fileList;
    private String fileDirectory;
    //192.168.1.2
    public P2PServer(String files, String fileLocation)
    {
        fileList=files;
        fileDirectory= fileLocation;
        try{
            int port = 2003;
            server = new ServerSocket(2003);
            System.out.println("%%% P2PServer started and listening....");
            //boolean loop=true;
            while(true)
            {
                client = server.accept(); 
                this.start();
            }
        }catch(IOException e)
        {
            System.out.println(e);
        }
    }

    public void run()
    {
        DataOutputStream outToClient;
        DataInputStream inFromClient;
        String input="";
        String accumulator="";
        boolean fileShareMode=false;
        System.out.println("%%%Waiting for HTTP request");
        //GET /index.htm HTTP/1.1
        //HTTP/1.1 200 OK //400 BAD_REQUEST, 404 FILE_NOT_FOUND, 505 HTTP_VERSION_UNSUPPORTED
        try{
            //InputStream in = client.getInputStream();
            //DataOutputStream out = new DataOutputStream(client.getOutputStream());
            inFromClient = new DataInputStream(new BufferedInputStream(client.getInputStream()));
            //read HTTP GET from client
            
            try{
            while((input=inFromClient.readUTF())!=null)
            {
                accumulator+=input;
                System.out.println(accumulator);
                if (accumulator.contains("HTTP"))
                    break;
            }
        }catch (EOFException e)
        {
           
        }
            System.out.println("%%% The server got a req: "+accumulator);
            //inFromClient.close();
            //Now to process the GET
            String temp[] = accumulator.split(" ");
            temp[1]=temp[1].replace("/","");
            System.out.println(temp[1]);
            outToClient = new DataOutputStream (new BufferedOutputStream(client.getOutputStream()));
            if (temp[2].contains("HTTP/1.1") == false)
            {
                outToClient.writeUTF("HTTP/1.1 505 HTTP_VERSION_UNSUPPORTED");
                //outToClient.println("");
                outToClient.flush();
            }
            else if( temp[0].contains("GET") && fileList.contains(temp[1])==false)
            {
                outToClient.writeUTF("HTTP/1.1 404 FILE_NOT_FOUND");
                //httpOUT.println("");
                outToClient.flush();
            }
            else if (temp[0].contains("GET") && fileList.contains(temp[1])==true)
            {
                outToClient.writeUTF("HTTP/1.1 200 OK");
                //httpOUT.println("");
                outToClient.flush();
                fileShareMode=true;
            }
            else
            {
                outToClient.writeUTF("HTTP/1.1 400 BAD_REQUEST");  
                //httpOUT.println("");
                outToClient.flush();
            }

            //C:\Users\Chris\Desktop\My Stuff\client1Files
            if (fileShareMode ==true)
            {
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                temp[1]=temp[1].replace("/","");
                temp[1].trim();
                //File file = new File ("C:/Users/Chris/Desktop/My Stuff/client1Files/"+temp[1]);
                File file = new File (fileDirectory+temp[1]);
                byte [] mybytearray  = new byte [(int)file.length()];
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                bis.read(mybytearray,0,mybytearray.length);
                outToClient = new DataOutputStream (new BufferedOutputStream(client.getOutputStream()));
                System.out.println("Sending " + file + "(" + mybytearray.length + " bytes)");
                outToClient.write(mybytearray,0,mybytearray.length);
                outToClient.flush();
                System.out.println(Arrays.toString(mybytearray));
                System.out.println("Done.");
                System.out.println("%%% FILE transfer successful");
                fileShareMode=false;

                if (bis != null)
                bis.close();
                if (outToClient != null) 
                outToClient.close();
                if (client!=null) 
                client.close();
                if(server !=null)
                server.close();
            }

        }catch  (IOException e)
        {
            System.out.println("P2PServer socket closed");
        }
    }

}
