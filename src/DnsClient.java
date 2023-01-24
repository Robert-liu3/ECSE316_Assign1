import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
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
            else if (arg.contains("@")) { //CHANGE THIS BACK TO Q
                server = arg.replace("@", "");
                i++;
                name = inputArgs.get(i);
            }
        }

        DnsClient dc = new DnsClient();

        String[] spliited = name.split("\\.");

        System.out.println("server name is " + server);

//        //CREATE CLIENT SOCKET
        DatagramSocket clientSocket = new DatagramSocket();
//
//        //TRANSLATE HOSTNAME TO IP ADDRESS USING DNS
        InetAddress IPAddress = InetAddress.getByName(server);

         //BYTE BUFFER
        ByteBuffer bb = ByteBuffer.allocate(dc.allocateRequest());

        /*
         * CREATE DIFFERENT BYTE BUFFERS THEN ADD THE APPROPRIATE HEX CODES TO THEM
         * such as bb.put(hexcode);
         */
//
//        byte[] sendData = new byte[1024];
//        byte[] receiveData = new byte[1024];
////
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

    public int allocateRequest() {
        int header_byte = 12; //FROM DNS PRIMER
        int question_byte = 5; //FROM DNS QUESTIONs
        int qname_byte = 0;
        String[] domain_parts = server.split(".");
        for(String s: domain_parts) {
            qname_byte += s.length();
        }

        return header_byte+question_byte+qname_byte;
    }
}
