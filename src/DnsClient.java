import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DnsClient {

    private static int timeout = 5; //OPTIONAL
    private static int max_retries = 3; //OPTIONAL
    private static int port = 53; //OPTIONAL
    private static String queryType = "a"; //OPTIONAL
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
                if (server.isBlank()) {
                    System.out.println("ERROR   Incorrect input syntax: Server or domain name is invalid");
                }
            }
        }

        //Create client socket
        DatagramSocket clientSocket = new DatagramSocket();

        //Translate input DNS server to extract ip address
        String[] stringPts = server.split("\\.");
        byte[] addrPts = new byte[stringPts.length];

        for (int i = 0; i < stringPts.length; i++) {
            try {
                Integer pt = Integer.parseInt(stringPts[i]);
                addrPts[i] = pt.byteValue();
            } catch (NumberFormatException e) {
                System.out.println("ERROR   Incorrect input syntax: Server address points are invalid");
                return;
            }
        }

        InetAddress IPAddress = InetAddress.getByAddress(addrPts);

        //Build DNS request
        // 1. Determine length of domain name for allocation
        int QNAME_length = 0;
        String[] labels = name.split("\\."); // Byte per character
        for (String l : labels) {
            QNAME_length += l.length(); // bytes for each character
            QNAME_length ++; // octet representing the length of label
        }

        // 2. Create buffer for request packet
        // 12 bytes for packet header, 4 bytes for QTYPE, QCLASS, 1 byte + variable for domain name + null char
        ByteBuffer requestData = ByteBuffer.allocate(17 + QNAME_length);

        // 3. Allocate and put data for packet header + question
        requestData.put(create_request_header());
        requestData.put(create_request_question(QNAME_length));

        byte[] receiveData = new byte[1024];

        //Create datagram with data-to-send, length, IP addr, port
        DatagramPacket sendPacket = new DatagramPacket(requestData.array(), requestData.array().length, IPAddress, port);

        //Send datagram to server
        clientSocket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        //READ DATAGRAM FROM SERVER
        clientSocket.receive(receivePacket);

        System.out.println(Arrays.toString(receivePacket.getData()));

        String modifiedSentence = new String(receivePacket.getData());

        System.out.println("FROM SERVER:" + modifiedSentence);
        clientSocket.close();
    }

    private static byte[] create_request_question(int domain_length) {
        ByteBuffer question = ByteBuffer.allocate(5 + domain_length);
        String[] labels = name.split("\\.");

        // For each label, put the length, then each ASCII character
        for (String l : labels) {
            question.put((byte) l.length());
            for (char c : l.toCharArray()) {
                question.put((byte) c);
            }
        }
        question.put((byte) 0); // null octet

        // Check query type and put corresponding value
        byte[] qType = new byte[2];
        switch (queryType) {
            case "a" -> qType[1] = 1; // 0x0001
            case "ns" -> qType[1] = 2; // 0x0002
            case "mx" -> qType[1] = 15; // 0x000f
        }

        question.put(qType);

        // Add QCLASS, last byte set to 1 to represent an Internet address
        byte[] qClass = new byte[2];
        qClass[1] = 1;

        question.put(qClass);

        return question.array();
    }

    private static byte[] create_request_header() {
        ByteBuffer header = ByteBuffer.allocate(12);
        byte[] ID = new byte[2]; // 2 bytes (16 bits)
        new Random().nextBytes(ID); // fills ID array with random ID generated

        header.put(ID);

        // For next byte: add QR, Opcode, AA, TC, RD (0 0000 0 0 1)
        header.put((byte) 1);
        header.put((byte) 0); // add RA, Z, RCODE (0 000 0000)

        byte[] QDCOUNT = new byte[2];
        QDCOUNT[1] = 1;
        header.put(QDCOUNT);

        byte[] ANCOUNT = new byte[2];
        header.put(ANCOUNT);

        byte[] NSCOUNT = new byte[2];
        header.put(NSCOUNT);

        byte[] ARCOUNT = new byte[2];
        header.put(ARCOUNT);

        return header.array();
    }
}
