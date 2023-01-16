import java.io.*;
import java.net.*;

public class DnsClient {

    private int timeout; //OPTIONAL
    private int max_retries; //OPTIONAL
    private int port; //OPTIONAL
    private String queryType; //OPTIONAL
    private String server; //REQUIRED
    private String name; //REQUIRED

    public static void main(String[] args) throws Exception {

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        DatagramSocket clientSocket = new DatagramSocket();

        InetAddress IPAddress = InetAddress.getByName("hostname");

        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        String sentence = inFromUser.readLine();
        sendData = sentence.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);

        clientSocket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        clientSocket.receive(receivePacket);

        String modifiedSentence = new String(receivePacket.getData());

        System.out.println("FROM SERVER:" + modifiedSentence);
        clientSocket.close();
    }
    public void getArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == "-t") {
                timeout = Integer.parseInt(args[i ++]);
                break;
            }
            else if (args[i] == "-r") {
                max_retries = Integer.parseInt(args[i++]);
                break;
            }
            else if (args[i] == "-p") {
                port = Integer.parseInt(args[i++]);
                break;
            }
            else if (args[i] == "-mx") {
                queryType = "mx";
            }
            else if (args[i] == "-ns") {
                queryType = "ns";
            }
            else if (args[i].contains("@")) {
                String serverNameWithAt = args[i];
                String serverName = serverNameWithAt.replace("@", "");
                server = serverName;
                name = args[i ++];
                break;
            }
        }
    }

}
