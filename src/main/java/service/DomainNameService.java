package service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class DomainNameService {
    private static final Pattern domainNamePattern = Pattern.compile("(?=^.{1,253}$)(^(((?!-)[a-zA-Z0-9-]{1,63}(?<!-))|((?!-)[a-zA-Z0-9-]{1,63}(?<!-)\\.)+[a-zA-Z]{2,63})$)");
    private static final Pattern ipAddressPattern = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private final InetAddress GooglePublicDnsAddress;
    private final int GooglePublicDnsPort;

    public DomainNameService() throws UnknownHostException {
        GooglePublicDnsAddress = InetAddress.getByAddress(new byte[]{8, 8, 8, 8});
        GooglePublicDnsPort = 53;
    }

    public static boolean validateDomainName(final String domainName) {
        return domainNamePattern.matcher(domainName).find();
    }

    public static boolean validationIPAddress(final String ipAddress) {
        return ipAddressPattern.matcher(ipAddress).matches();
    }

    public String getIpAddress(String domainName) throws IOException {
        byte[] bytes;
        DatagramSocket socket;
        DatagramPacket packet;

        // Build and Send DNS Request
        bytes = buildRequest(domainName);
        socket = new DatagramSocket();
        packet = new DatagramPacket(bytes, bytes.length, GooglePublicDnsAddress, GooglePublicDnsPort);
        socket.send(packet);

        // Receive and Process DNS Response
        bytes = new byte[1024];
        packet = new DatagramPacket(bytes, bytes.length);
        socket.receive(packet);
        return processResponse(bytes);
    }

    private byte[] buildRequest(String domainName) throws IOException {
        byte[] bytes;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream)) {
            // Identifier
            outputStream.writeShort(0x1234);

            // Write Query Flags
            outputStream.writeShort(0x0100);

            // Question Count
            outputStream.writeShort(0x0001);

            // Answer Record Count
            outputStream.writeShort(0x0000);

            // Authority Record Count
            outputStream.writeShort(0x0000);

            // Additional Record Count
            outputStream.writeShort(0x0000);

            // Write domain name part
            for (String domainPart : domainName.split("\\.")) {
                bytes = domainPart.getBytes(StandardCharsets.UTF_8);

                outputStream.writeByte(bytes.length);
                outputStream.write(bytes);
            }

            // No more parts
            outputStream.writeByte(0x00);

            // Type 0x01 = A (Host Request)
            outputStream.writeShort(0x0001);

            // Class 0x01 = IN
            outputStream.writeShort(0x0001);

            return byteArrayOutputStream.toByteArray();
        }
    }

    private String processResponse(byte[] bytes) throws IOException {
        byte[] record;
        int size;
        short addressSize;
        StringBuilder address;

        try (DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(bytes))) {
            // Transaction ID
            inputStream.readShort();

            // Flags
            inputStream.readShort();

            // Questions
            inputStream.readShort();

            // Answers RRs
            inputStream.readShort();

            // Authority RRs
            inputStream.readShort();

            // Additional RRs
            inputStream.readShort();

            // Record
            while ((size = inputStream.readByte()) > 0) {
                record = new byte[size];
                for (int i = 0; i < size; i++) {
                    record[i] = inputStream.readByte();
                }
            }

            // Record Type
            inputStream.readShort();

            // Class
            inputStream.readShort();

            // Field
            inputStream.readShort();

            // Type
            inputStream.readShort();

            // Class
            inputStream.readShort();

            // TTL
            inputStream.readInt();

            addressSize = inputStream.readShort();
            address = new StringBuilder();
            for (int i = 0; i < addressSize; i++) {
                address.append(String.format("%d", (inputStream.readByte() & 0xFF)));

                if (i + 1 < addressSize) {
                    address.append(".");
                }
            }

            return address.toString();
        }
    }
}
