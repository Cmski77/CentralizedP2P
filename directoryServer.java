
public class directoryServer
{
   public static void main(String[] args){
        RDTReceiver receiverThread = null;
        try{
            // Start receiver
            receiverThread = new RDTReceiver("Receiver", 2003);
            receiverThread.start();
            while (true){   
                Thread.sleep(10000);
                
            }
        }catch(Exception e){
            e.printStackTrace();}}       
}