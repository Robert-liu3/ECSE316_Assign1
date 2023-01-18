import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class DnsClient {

    private int timeout; //OPTIONAL
    private int max_retries; //OPTIONAL
    private int port; //OPTIONAL
    private String queryType; //OPTIONAL
    private String server; //REQUIRED
    private String name; //REQUIRED

    public static void main(String[] args) throws Exception {

        //GETTING USER INPUT
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        //PARSING ALL ARGUMENTS FROM USER INPUT INTO THE GLOBAL VARIABLES
        ArrayList<String> inFromUserString = new ArrayList<String>();
        while (inFromUser.readLine() != null) {
            inFromUserString.add(inFromUser.readLine());
        }
        DnsClient dc = new DnsClient();
        dc.getArgs(inFromUserString);

        //CREATE CLIENT SOCKET
        DatagramSocket clientSocket = new DatagramSocket();

        //TRANSLATE HOSTNAME TO IP ADDRESS USING DNS
        InetAddress IPAddress = InetAddress.getByName(dc.server);

        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        String sentence = inFromUser.readLine();
        sendData = sentence.getBytes();

        //CREATE DATAGRAM WITH DATA-TO-SEND, LENGTH, IP ADDR, PORT
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);

        //SEND DATAGRAM TO SERVER
        clientSocket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        //READ DATAGRAM FROM SERVER
        clientSocket.receive(receivePacket);

        String modifiedSentence = new String(receivePacket.getData());

        System.out.println("FROM SERVER:" + modifiedSentence);
        clientSocket.close();
    }
    public void getArgs(ArrayList<String> args) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) == "-t") {
                timeout = Integer.parseInt(args.get(i ++));
                break;
            }
            else if (args.get(i) == "-r") {
                max_retries = Integer.parseInt(args.get(i++));
                break;
            }
            else if (args.get(i) == "-p") {
                port = Integer.parseInt(args.get(i++));
                break;
            }
            else if (args.get(i) == "-mx") {
                queryType = "mx";
            }
            else if (args.get(i) == "-ns") {
                queryType = "ns";
            }
            else if (args.get(i).contains("@")) {
                String serverNameWithAt = args.get(i);
                String serverName = serverNameWithAt.replace("@", "");
                server = serverName;
                name = args.get(i++);
                break;
            }
        }
    }

}
