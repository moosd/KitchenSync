import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by souradip on 14/12/15.
 */
public class Networking {
    public static int PORT = 8989;

    public Networking(String key) {

    }

    public void send(int type, String message) {

    }

    public void receive() {

    }

    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(PORT);
            socket.setBroadcast(true);
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            System.out.println(new String(buf).trim());
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
