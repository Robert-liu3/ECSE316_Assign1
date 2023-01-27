import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DnsClient {

    /*
     * Request Global variables
     */
    private static int timeout = 5; //OPTIONAL
    private static int max_retries = 3; //OPTIONAL
    private static int port = 53; //OPTIONAL
    private static String queryType = "a"; //OPTIONAL
    private static String server; //REQUIRED
    private static String name; //REQUIRED

    /*
     * Response Global variables
     */

    private static byte[] responseID_G = new byte[2];
    private static byte[] secondRow_G = new byte[2];
    private static byte[] QDCOUNT_G = new byte[2];
    private static int ANCOUNT_INT;
    private static byte[] ARCOUNT_G = new byte[2];


    public static void main(String[] args) {

        // Parsing all arguments from user input to variables above
        ArrayList<String> inputArgs = new ArrayList<>(Arrays.asList(args));

        for (int i = 0; i < inputArgs.size(); i++) {
            String arg = inputArgs.get(i);
            if (arg.equals("-t")) {
                try { i++; timeout = Integer.parseInt(inputArgs.get(i)); }
                catch (NumberFormatException exception) {
                    System.out.println("ERROR   Incorrect input syntax: Timeout value must be an integer."); //TODO CHANGE THIS
                    return;
                }
            }
            else if (arg.equals("-r")) {
                try { i++; max_retries = Integer.parseInt(inputArgs.get(i)); }
                catch (NumberFormatException exception) {
                    System.out.println("ERROR   Incorrect input syntax: Max retries value must be an integer."); //TODO CHANGE THIS
                    return;
                }
            }
            else if (arg.equals("-p")) {
                try { i++; port = Integer.parseInt(inputArgs.get(i)); }
                catch (NumberFormatException exception) {
                    System.out.println("ERROR   Incorrect input syntax: Port number must be an integer."); //TODO CHANGE THIS
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
                    System.out.println("ERROR   Incorrect input syntax: Server or domain name is invalid"); //TODO CHANGE THIS
                }
            }
        }

        int retries = 0; // used to check if max retries is exceeded

        while (retries < max_retries) {
            /*
             * REQUEST
             */

            try {
                //Create client socket
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.setSoTimeout(timeout); // sets the timeout specified before raising exception

                //Translate input DNS server to extract ip address
                String[] stringPts = server.split("\\.");
                byte[] addrPts = new byte[stringPts.length];

                for (int i = 0; i < stringPts.length; i++) {
                    try {
                        int pt = Integer.parseInt(stringPts[i]);
                        addrPts[i] = (byte) pt;
                    } catch (NumberFormatException e) {
                        System.out.println("ERROR   Incorrect input syntax: Server address points are invalid");
                        return;
                    }
                }

                InetAddress IPAddress = InetAddress.getByAddress(addrPts);

                // Summarize query in output
                System.out.println("\nDnsClient sending request for " + name);
                System.out.println("Server: " + IPAddress);
                System.out.println("Request type: " + queryType.toUpperCase() + "\n");

                //Build DNS request
                // 1. Determine length of domain name for allocation
                int QNAME_length = 0;
                String[] labels = name.split("\\."); // Byte per character
                for (String l : labels) {
                    QNAME_length += l.length(); // bytes for each character
                    QNAME_length++; // octet representing the length of label
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
                long startTime = System.currentTimeMillis();
                clientSocket.send(sendPacket);

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                //READ DATAGRAM FROM SERVER
                clientSocket.receive(receivePacket);  //fucks up right here
                long endTime = System.currentTimeMillis();

                long time = endTime - startTime;

                // Valid response received output
                System.out.println("Response received after " + time + " seconds (" + retries + " retries)");

                String modifiedSentence = new String(receivePacket.getData());

                System.out.println("FROM SERVER:" + modifiedSentence);
                clientSocket.close();

                //receiving data
                create_response_header(receivePacket.getData());

                //ONLY WORKS FOR THE DOMAIN NAME
                create_response_answer(ByteBuffer.wrap(receivePacket.getData()), requestData.array().length);

                System.out.println("the name is" + name);
                /*
                 * RESPONSE
                 */

                return; // Successful query!
            } catch (SocketTimeoutException e) {
                System.out.println("ERROR   Socket timed out, will retry request if limit is not exceed.");
                retries++;
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        System.out.println("ERROR   Maximum number of retries " + max_retries + " exceeded.");
    }

    /*
     * REQUEST FUNCTIONS
     */

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

    /*
     * RESPONSE FUNCTIONS
     */

    private static void create_response_header(byte[] receivedData ) {
        //storing id of response
        int counter = 0;
        byte[] responseID = new byte[2];
        byte[] secondRow = new byte[2];
        byte[] QDCOUNT = new byte[2];
        byte[] ANCOUNT = new byte[2];
        byte[] ARCOUNT = new byte[2];

        responseID[0] = receivedData[counter++]; //0
        responseID[1] = receivedData[counter++]; //1

        secondRow[0] = receivedData[counter++]; //2
        secondRow[1] = receivedData[counter++]; //3

        int QR = getBit(secondRow[0], 0);
        if (QR == 0) {
            throw new RuntimeException("ERROR   is not a response.");
        }

        int AA = getBit(secondRow[0], 5); //AUTHORITY???? DO WE NEED TO WORRY ABOUT IT

        int TC = getBit(secondRow[0], 6);

        int RD = getBit(secondRow[0], 7); //not needed cause query

        int RA = getBit(secondRow[1], 8); //SHOULD BE PRINTED IF SERVER PROVIDED AN ANSWER, IF NOT THEN PRINT ERROR

        int RCODE = secondRow[1] & 15;

        QDCOUNT[0] = receivedData[counter++];
        QDCOUNT[1] = receivedData[counter++]; //5

        ANCOUNT[0] = receivedData[counter++];
        ANCOUNT[1] = receivedData[counter++]; //7

        int value = 0;
        for (byte b : ANCOUNT) {
            value = (value << 8) + (b & 0xFF);
        }
        ANCOUNT_INT = value;

        counter = counter + 2;

        ARCOUNT[0] = receivedData[counter++];
        ARCOUNT[1] = receivedData[counter];
    }

    //FUNCTIONS WORKS WITH NAME ONLY
    private static void create_response_answer(ByteBuffer receivedData, int offset) {
        //offset should be the length of the sent data as the sent data ends at question
        receivedData.position(offset);
        int pos = receivedData.position();
        String answerName = "";
        while (true) {
            byte b = receivedData.get();
            if ((b & 0xc0) == 0xc0) {
                int newPOS = (receivedData.get() & 0xff) | ((b & 0x3f) << 8);
                receivedData.position(newPOS);
                continue;
            }
            if (b == 0) {
                break;
            }
            // label
            int length = b & 0xff;
            byte[] label = new byte[length];
            receivedData.get(label);
            answerName = answerName + new String(label);
        }
        receivedData.position(pos);
        name = answerName;
    }

    public static int getBit(byte b, int position)
    {
        return (b >> position) & 1;
    }
}
