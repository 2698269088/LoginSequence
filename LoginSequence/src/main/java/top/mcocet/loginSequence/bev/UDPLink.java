package top.mcocet.loginSequence.bev;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPLink {
    public void send(String ip, int port, String message) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(
                buffer,
                buffer.length,
                InetAddress.getByName(ip),
                port
        );
        socket.send(packet);
        socket.close();
    }
}
