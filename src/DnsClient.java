import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class DnsClient {

    private static int timeout = 5; //OPTIONAL
    private static int max_retries = 3; //OPTIONAL
    private static int port = 53; //OPTIONAL
    private static String queryType; //OPTIONAL
    private static String server; //REQUIRED
    private static String name; //REQUIRED

    public static void main(String[] args) throws Exception {

        // Parsing all arguments from user input to variables above
        ArrayList<String> inputArgs = new ArrayList<>(Arrays.asList(args));

        for (int i = 0; i < inputArgs.size(); i++) {
            String arg = inputArgs.get(i);
            if (arg.equals("-t")) {
                try { i++; timeout = Integer.parseInt(inputArgs.get(i)); }
                catch (NumberFormatException exception) {
                    System.out.println("ERROR   Incorrect input syntax: Timeout value must be an integer.");
                    return;
                }
            }
            else if (arg.equals("-r")) {
                try { i++; max_retries = Integer.parseInt(inputArgs.get(i)); }
                catch (NumberFormatException exception) {
                    System.out.println("ERROR   Incorrect input syntax: Max retries value must be an integer.");
                    return;
                }
            }
            else if (arg.equals("-p")) {
                try { i++; port = Integer.parseInt(inputArgs.get(i)); }
                catch (NumberFormatException exception) {
                    System.out.println("ERROR   Incorrect input syntax: Port number must be an integer.");
                    return;
                }
            }
            else if (arg.equals("-mx")) {
                queryType = "mx";
            }
            else if (arg.equals("-ns")) {
                queryType = "ns";
            }
            else if (arg.contains("@")) {
                server = arg.replace("@", "");
                i++;
                name = inputArgs.get(i);
            }
        }

        System.out.println(port);

//        //CREATE CLIENT SOCKET
//        DatagramSocket clientSocket = new DatagramSocket();
//
//        //TRANSLATE HOSTNAME TO IP ADDRESS USING DNS
//        InetAddress IPAddress = InetAddress.getByName(dc.server);
//
//        byte[] sendData = new byte[1024];
//        byte[] receiveData = new byte[1024];
//
//        String sentence = inFromUser.readLine();
//        sendData = sentence.getBytes();
//
//        //CREATE DATAGRAM WITH DATA-TO-SEND, LENGTH, IP ADDR, PORT
//        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
//
//        //SEND DATAGRAM TO SERVER
//        clientSocket.send(sendPacket);
//
//        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//
//        //READ DATAGRAM FROM SERVER
//        clientSocket.receive(receivePacket);
//
//        String modifiedSentence = new String(receivePacket.getData());
//
//        System.out.println("FROM SERVER:" + modifiedSentence);
//        clientSocket.close();
    }
}
