import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

/**
 * Created by souradip on 14/12/15.
 */
public class Client {

    public static int PORT = 8989;

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {}
        return null;
    }

    public static InetAddress getBroadcast() throws SocketException {
        for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
            NetworkInterface ni = niEnum.nextElement();
            if (!ni.isLoopback()) {
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    if(interfaceAddress.getAddress() instanceof Inet4Address) {
                        System.out.println(interfaceAddress.getAddress());
                        return interfaceAddress.getBroadcast();
                    }
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(PORT+1);
            socket.setBroadcast(true);
            String data = "hello";

            DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
                    getBroadcast(), PORT);
            socket.send(packet);

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
