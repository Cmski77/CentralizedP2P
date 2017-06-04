import java.util.*;
// We will have to code beforehand to dierct it to use correct method in the MAIN code
// if(query == "INFORM_AND_UPDATE")
//        P2PDirectory.ADDc(String host, String IPa, String fileName, String fileSize);
// if(query == "QUERY")
//        P2PDirectory.REQUESTc(tempFN);
// if(query == "EXIT")
//        P2PDirectory.EXITc(host);
public class  P2PDirectory
{
    ArrayList<String> hostList;
    ArrayList<String> IPaList;
    ArrayList<String>fileNameList;
    ArrayList<String>fileSizeList;
    public P2PDirectory()
    {
        hostList = new ArrayList<String>();
        IPaList = new ArrayList<String>();
        fileNameList = new ArrayList<String>();
        fileSizeList = new ArrayList<String>();
    }

    public String REQUESTc(String fileName)
    {
        String tempFN="", tempH="", tempIPa="", tempFS="",temp="";
        boolean winner=false;
        int n = hostList.size()-1;
        for (int count =0; n>= 0; n--)
        {
            tempFN = fileNameList.get(n);
            if (tempFN.equals(fileName))
            {
                
                tempH = hostList.get(n);
                tempIPa = IPaList.get(n);
                tempFS = fileSizeList.get(n);
                //System.out.println("Host: " + tempH + " IP: " + tempIPa + " File Name: " + tempFN+ " Size: " + tempFS);
                temp="Host: " + tempH + " IP: " + tempIPa + " File Name: " + tempFN+ " Size: " + tempFS;
                winner=true;
            }
        }
        if(winner)
        return temp;
        else
        return "File Not Found";
    }

    public String RequestALL()
    {
        String tempFN="", tempH="", tempIPa="", tempFS="",table="";
        int n = hostList.size();
        for (int count = 0; n>=0; n--)
        {
            tempFN = fileNameList.get(count);
            tempH = hostList.get(count);
            tempIPa = IPaList.get(count);
            tempFS = fileSizeList.get(count);
            //System.out.println("Host: " + tempH + " IP: " + tempIPa + " File Name: " + tempFN + " Size: " + tempFS);
            System.out.println("Host: " + tempH + " IP: " + tempIPa + " Files:\r\n " + tempFN);//""+tempFS);
            
            table+=("Host: " + tempH + " IP: " + tempIPa + " Files:\r\n " + tempFN+"\r\n");
            
        }
        return table;
    }

    public void ADDc(String host, String IPa, String fileName, String fileSize)
    {
        hostList.add(host);
        IPaList.add(IPa);
        fileNameList.add(fileName);
        fileSizeList.add(fileSize);
    }

    public void EXITc(String host)
    {
        String tempFN="", tempH="", tempIPa="", tempFS="";
        boolean loop = false;
        int loopcount = 0;
        while(loop == false)
        {
            int n = hostList.size()-1;
            for (int count =0; n>= 0; n--)
            {
                tempH = hostList.get(n);
                if (tempH.equals(host))
                {
                    hostList.remove(n);
                    IPaList.remove(n);
                    fileNameList.remove(n);
                    fileSizeList.remove(n);
                    loopcount++;
                }
                
            }
            if (loopcount >0)
            {
                loopcount =0;
                loop = true;
            }
        }
    }
}
