import java.math.BigInteger;
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
    private static String server = ""; //REQUIRED
    private static String name = ""; //REQUIRED

    /*
     * Response Global variables
     */
    private static int ANCOUNT_INT = 0;
    private static int ARCOUNT_INT = 0;
    private static String auth = null;
    private static ArrayList<AnswerRecord> answerRecords = new ArrayList<>();


    public static void main(String[] args) {

        // Parsing all arguments from user input to variables above
        ArrayList<String> inputArgs = new ArrayList<>(Arrays.asList(args));

        for (int i = 0; i < inputArgs.size(); i++) {
            String arg = inputArgs.get(i);
            if (arg.equals("-t")) {
                try { i++; timeout = Integer.parseInt(inputArgs.get(i)); }
                catch (NumberFormatException exception) {
                    throw new RuntimeException("ERROR   Incorrect input syntax: Timeout value must be an integer.");
                }
            }
            else if (arg.equals("-r")) {
                try { i++; max_retries = Integer.parseInt(inputArgs.get(i)); }
                catch (NumberFormatException exception) {
                    throw new RuntimeException("ERROR   Incorrect input syntax: Max retries value must be an integer.");
                }
            }
            else if (arg.equals("-p")) {
                try { i++; port = Integer.parseInt(inputArgs.get(i)); }
                catch (NumberFormatException exception) {
                    throw new RuntimeException("ERROR   Incorrect input syntax: Port number must be an integer.");
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
                if (server.isBlank() || i == inputArgs.size()) throw new RuntimeException("ERROR   Incorrect input syntax: Server or domain name is invalid");
                name = inputArgs.get(i);
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
                    int pt = Integer.parseInt(stringPts[i]);
                    addrPts[i] = (byte) pt;
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
                long startTime = System.currentTimeMillis(); // used to calculate time between sent and receive
                clientSocket.send(sendPacket);

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                //Read datagram from server
                clientSocket.receive(receivePacket);
                long endTime = System.currentTimeMillis();

                long time = endTime - startTime;

                // Valid response received output
                System.out.println("Response received after " + time + " seconds (" + retries + " retries)\n");

                clientSocket.close();

                System.out.println(Arrays.toString(receivePacket.getData()));
                //Receiving data header
                create_response_header(receivePacket.getData());

                create_response_answer(ByteBuffer.wrap(receivePacket.getData()), requestData.array().length);

                System.out.println("***Answer Section (" + ANCOUNT_INT + " records)***");
                for (AnswerRecord dnsRecord : answerRecords) {
                    if (dnsRecord.qType == 1) {
                        System.out.println("IP   " + dnsRecord.ipAddr + "   " + dnsRecord.ttl + "   " + auth);
                    } else if (dnsRecord.qType == 2) {
                        System.out.println("NS  " + dnsRecord.alias + " " + dnsRecord.ttl + "   " + auth);
                    }
                }

                return; // Successful query!
            } catch (SocketTimeoutException e) {
                System.out.println("ERROR   Socket timed out, will retry request if limit is not exceed.");
                retries++;
            } catch (NumberFormatException e) {
                throw new RuntimeException("ERROR   Incorrect input syntax: Server address points are invalid");
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        throw new RuntimeException("ERROR   Maximum number of retries " + max_retries + " exceeded.");
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

    // Parse the header of the response to get ID, answer record count, etc.
    private static void create_response_header(byte[] receivedData ) {
        int counter = 0; // used for incrementing and moving in byte arr
        byte[] responseID = new byte[2]; //storing id of response
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

        int AA = getBit(secondRow[0], 5); //authority bit
        if (AA == 0) auth = "nonauth";
        else if (AA == 1) auth = "auth";

//        int TC = getBit(secondRow[0], 6);

        int RD = getBit(secondRow[0], 7);

        int RA = getBit(secondRow[1], 8); //SHOULD BE PRINTED IF SERVER PROVIDED AN ANSWER, IF NOT THEN PRINT ERROR

        // Check if server supports recursive queries
        if (RD == 1 && RA == 0) throw new RuntimeException("ERROR   Server does not support recursive queries");

        // Use RCODE to verify records
        int RCODE = secondRow[1] & 15;

        if (RCODE == 1) throw new RuntimeException("ERROR   Name server unable to interpret the query");
        else if (RCODE == 2) throw new RuntimeException("ERROR  Server failure");
        else if (RCODE == 3 && AA == 1) {
            System.out.println("NOTFOUND"); // only thrown if authoritative
            return;
        }
        else if (RCODE == 4) throw new RuntimeException("ERROR  Server does not support kind of query");
        else if (RCODE == 5) throw new RuntimeException("ERROR  Server refuses to perform requested operation");

        QDCOUNT[0] = receivedData[counter++];
        QDCOUNT[1] = receivedData[counter++]; //5

        // Retrieve ANCOUNT to find out how many records there are
        ANCOUNT[0] = receivedData[counter++];
        ANCOUNT[1] = receivedData[counter++]; //7

        int value = 0;
        for (byte b : ANCOUNT) {
            value = (value << 8) + (b & 0xFF);
        }
        ANCOUNT_INT = value;

        value = 0;
        counter = counter + 2;

        // Retrieve ARCOUNT to find out how many *additional* records there are
        ARCOUNT[0] = receivedData[counter++];
        ARCOUNT[1] = receivedData[counter];

        for (byte b : ARCOUNT) {
            value = (value << 8) + (b & 0xFF);
        }
        ARCOUNT_INT = value;
    }

    // Parse DNS records for responses
    private static void create_response_answer(ByteBuffer receivedData, int offset) throws UnknownHostException {
        //offset should be the length of the sent data as the sent data ends at question
        receivedData.position(offset);
        String answerName = getString(receivedData);

        // RDATA values
        //reset buffer position + 2
        offset += 2;
        receivedData.position(offset);

        System.out.println("The name is " + answerName);

        // Fill list of answer records first
        for (int i = 0; i < ANCOUNT_INT; i++) {
            short QTYPE = receivedData.getShort(); // Next 16 bits correspond to QTYPE

            short QCLASS = receivedData.getShort();
            if (QCLASS != 1) throw new RuntimeException("ERROR  QCLASS found to be value other than 1");

            int TTL = receivedData.getInt();

            receivedData.getShort(); // for RDLength, and to increment position

            // A-type, so IP address needs to be created
            if (QTYPE == 1) {
                // Get IP as an integer, convert to bytes then back to the address received
                int IP = receivedData.getInt();
                byte[] bytes = BigInteger.valueOf(IP).toByteArray();

                answerRecords.add(new AnswerRecord(QTYPE, TTL, InetAddress.getByAddress(bytes), null, 0));

            } else if (QTYPE == 2) {
                String alias = getString(receivedData);
                answerRecords.add(new AnswerRecord(QTYPE, TTL, null, alias, 0));
            }
            
        }
    }
    public static String getString(ByteBuffer receivedData) {
        String answerName = "";
        while (true) {
            byte currentByte = receivedData.get();
            //check for 2 significant bits
            if ((currentByte & 0xc0) == 0xc0) {
                //set to unsigned integer (data & 1111 1111)
                int firstByte = receivedData.get() & 0xff;
                //retrieve 6 least significant bits (current byte & 0011 1111)
                int secondByte = currentByte & 0x3f;
                //shift secondByte by 8 bits to the left, and combine firstByte - secondByte
                int newPOS = (firstByte | (secondByte << 8));
                receivedData.position(newPOS);
                continue;
            }
            if (currentByte == 0) {
                break;
            }
            // label
            int length = currentByte & 0xff;
            byte[] label = new byte[length];
            receivedData.get(label);
            answerName = answerName + new String(label) + ".";
        }
        return answerName.substring(0, answerName.length() - 1); // Remove the last period to get proper length
    }

    // Gets the bit at position in byte b
    public static int getBit(byte b, int position)
    {
        return (b >> position) & 1;
    }

    private record AnswerRecord(short qType, int ttl, InetAddress ipAddr, String alias, int pref) {}
}
