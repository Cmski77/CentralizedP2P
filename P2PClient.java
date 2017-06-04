import java.util.*;
import java.io.*;
import java.net.*;
/**
 * @author Chris Mazurski, Matt Bradley, Andrew Bob
 */
public class P2PClient
{
    //TO DO: Test with multiple computers and stuff
    //TO DO: Multithreading fixes
    //TO DO: Implement Andrew's dynamic timeout calc method

    private static UDP_Packet data;
    private static int i=0;
    private static String[] fileNameBuffer;
    private static String Headers="";
    private static String fileName="";
    // QUERY HOST_NAME IP 
    public static void listFilesForFolder(final File folder) 
    {
        fileNameBuffer=new String[folder.list().length];
        for (final File fileEntry : folder.listFiles()) 
        {
            fileNameBuffer[i]=fileEntry.getName()+" "+fileEntry.length()+"bytes\r\n";
            i++;
        }
    }

    public static void main(String[] args){   
        try{
            //Directory Server IP HARDCODED, *******CHANGE HERE********
            byte[] targetAdddress = {(byte)192,(byte)168,(byte)1,(byte)3};
            RDTSender sender = new RDTSender();
            sender.startSender(targetAdddress,2003);

            Scanner menuReader = new Scanner(System.in);

            System.out.println("Enter file location: (ex: C:/Users/Chris/Desktop/My Stuff/client1Files/)");
            String clientFiles = menuReader.nextLine();

            System.out.println("Enter Host Name: ");
            String hostName = menuReader.nextLine();

            System.out.println("Enter IP Address: (ex: 192.168.1.2)");
            String ipAdd = menuReader.nextLine();

            //FILE LOCATION MUST BE SET PRIOR TO DEBUGGING~~~~~~~~~~~~~~~
            //final File folder = new File("C:/Users/Chris/Desktop/My Stuff/client1Files");
            final File folder = new File(clientFiles);
            listFilesForFolder(folder);
            Headers=Arrays.toString(fileNameBuffer);
            Headers=Headers.replace(",","");
            Headers=Headers.replace("[","");
            Headers=Headers.replace("]","");
            Headers.trim();

            //P2PServer listen= new P2PServer(Headers);
            boolean menuLoop = true;
            boolean programLoop = true;

            while (programLoop == true)
            {
                while(menuLoop == true)
                {
                    System.out.println("Menu:");
                    System.out.println("1: QUERY FOR CONTENT");
                    System.out.println("2: INFORM_AND_UPDATE");
                    System.out.println("3: EXIT");
                    System.out.println("4: GET");
                    System.out.println("5: SERVER MODE");

                    int menuChoice = menuReader.nextInt();
                    if(menuChoice == 1)
                    //QUERY FOR CONTENT
                    {
                        menuReader.nextLine();
                        System.out.println("Enter a filename you would like to query for (ex: ImageC1.jpg)");
                        String queryFile = menuReader.nextLine();
                        menuLoop = false;
                        //data= new UDP_Packet("0","0","QUERY_FOR_CONTENT "+hostName+" "+ipAdd+" \r\n ImageC1.jpg");
                        data= new UDP_Packet("0","0","QUERY_FOR_CONTENT "+hostName+" "+ipAdd+" \r\n "+queryFile);
                        for (int x=0;x<10;x++){

                            //Send the data
                            sender.rdtSend(data.toString().getBytes("ISO-8859-1"));

                            //Thread.sleep(10000);  
                            x=10;
                        }    

                        data=new UDP_Packet("0","0","TERMINATE");
                        for (int q=0;q<10;q++){

                            //Send the data
                            sender.rdtSend(data.toString().getBytes("ISO-8859-1"));

                            //Thread.sleep(10000);
                            q=10;
                            //sender.stopSender();
                        }
                    }   

                    if(menuChoice == 2)
                    //INFORM_AND_UPDATE
                    {
                        //send info

                        menuLoop = false;
                        data= new UDP_Packet("0","0",("INFORM_AND_UPDATE "+hostName+" "+ipAdd+" \r\n "+Headers));

                        for (int y=0;y<10;y++){

                            //Send the data
                            sender.rdtSend(data.toString().getBytes("ISO-8859-1"));

                            //Thread.sleep(10000);
                            y=10;
                            //sender.stopSender();
                        }

                        data=new UDP_Packet("0","0","TERMINATE");
                        for (int g=0;g<10;g++){

                            //Send the data
                            sender.rdtSend(data.toString().getBytes("ISO-8859-1"));

                            Thread.sleep(10000);
                            g=10;
                            //sender.stopSender();
                        }
                    }

                    if(menuChoice == 3)
                    //EXIT
                    { 
                        // exit program
                        menuLoop = false;
                        data= new UDP_Packet("0","0","EXIT "+hostName+" "+ipAdd);
                        for (int a=0;a<10;a++){

                            //Send the data
                            sender.rdtSend(data.toString().getBytes("ISO-8859-1"));

                            //Thread.sleep(10000);  
                            a=10;
                        }    

                        data=new UDP_Packet("0","0","TERMINATE");
                        for (int b=0;b<10;b++){

                            //Send the data
                            sender.rdtSend(data.toString().getBytes("ISO-8859-1"));

                            //Thread.sleep(10000);
                            b=10;
                            //sender.stopSender();
                        }

                    }

                    if(menuChoice == 4)
                    //GET
                    {            
                        menuReader.nextLine();
                        menuLoop = false;
                        System.out.println("Enter target IP:");
                        String targetIP =menuReader.nextLine();
                        
                        System.out.println("Enter target filename (ex: ImageC1.jpg)"); 
                        String fileName = menuReader.nextLine();
                        
                        System.out.println("Enter output destination for the file (ex: C:/Users/Chris/Desktop/output/)");
                        String fileDest = menuReader.nextLine();
                        
                        fileDest =fileDest+fileName;
                        

                        DataOutputStream outToServer;
                        DataInputStream inFromServer;
                        
                        Socket clientSock = new Socket(targetIP,2003); //2003 is our designated port for everything
                        outToServer = new DataOutputStream (new BufferedOutputStream(clientSock.getOutputStream()));
                        outToServer.writeUTF("GET /"+fileName+" HTTP/1.1");
                        outToServer.flush();
                        
                        String readResponse="";
                        String accumulator="";
                        inFromServer = new DataInputStream (new BufferedInputStream(clientSock.getInputStream()));
                        try{
                            while((readResponse=inFromServer.readUTF())!=null)
                            {
                                accumulator+=readResponse;
                                System.out.println(accumulator);
                                if(accumulator.contains("HTTP"))
                                    break;
                            }
                        }catch (EOFException e)
                        {
                        }

                        if (readResponse.contains("OK"))
                        {
                            //Prepare for file transfer
                            System.out.println("**********************");
                            System.out.println(accumulator);
                            System.out.println("**********************");
                            int bytesRead;
                            int current = 0;

                            //FileOutputStream fos = new FileOutputStream("C:/Users/Chris/Desktop/output/"+fileName);
                            FileOutputStream fos = null;
                            BufferedOutputStream bos = null;

                            byte [] mybytearray  = new byte [6022386]; //some random number
                            inFromServer = new DataInputStream (new BufferedInputStream(clientSock.getInputStream()));
                            //File destination = new File("C:/Users/Chris/Desktop/output/"+fileName);
                            File destination = new File(fileDest);
                            fos = new FileOutputStream(destination);
                            bos = new BufferedOutputStream(fos);

                            bytesRead = inFromServer.read(mybytearray,0,mybytearray.length);
                            current = bytesRead;

                            do {
                                bytesRead =inFromServer.read(mybytearray, current, (mybytearray.length-current));
                                if(bytesRead >= 0) 
                                    current += bytesRead;
                            } while(bytesRead > -1);

                            bos.write(mybytearray,0,current);
                            bos.flush();
                            System.out.println("File " + destination+ " downloaded (" + current + " bytes read)");

                            if (fos != null) 
                                fos.close();
                            if (bos != null) 
                                bos.close();
                            if (clientSock != null) 
                                clientSock.close();

                            System.out.println("FILE HAS BEEN SAVED");
                        }
                        else
                        {
                            System.out.println("**********************");
                            System.out.println(accumulator);
                            System.out.println("**********************");
                        }
                    }

                    if(menuChoice == 5)
                    {
                        System.out.println("Going into Sender mode...");
                        P2PServer listen= new P2PServer(Headers, clientFiles);
                    }

                    if (menuChoice != 1 && menuChoice != 2  && menuChoice  !=3 && menuChoice !=4)
                    {
                        System.out.println("Incorrect Input");
                    }
                }

                System.out.println("Would you like to access the menu again? Not accesing the menu will close the program.");
                System.out.println("1 = Yes");
                System.out.println("2 = No");
                Scanner Choice = new Scanner(System.in);
                int menu = Choice.nextInt();

                if (menu == 1)
                {
                    menuLoop = true;
                }

                if (menu == 2)
                {
                    menuLoop = false;
                    System.out.println("The connection is closed");
                    break;
                }

                if(menu != 1 && menu != 2)
                {
                    //System.out.println("incorrect input");
                    menuLoop = true;
                }
            }
        }catch(Exception e){
            System.err.println(e);
        }
    }
}

