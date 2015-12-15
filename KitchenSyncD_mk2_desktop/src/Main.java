import com.moosd.kitchensyncd.networking.Networking;
import com.moosd.kitchensyncd.networking.SubnetUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class Main {
/*
    public static long lastscan = 0;
    public static String[] ips;

    public static void dump(Networking net, int type, byte[] data) {
        String range = net.directSend.getIpRange();
        // portscan
        //if (System.currentTimeMillis() - lastscan > 1000 * 60) {
            Runtime r = Runtime.getRuntime();
            Process p;
            String line;
            List<String> out = new LinkedList<String>();

            try {
            ProcessBuilder pb = new ProcessBuilder(new String[]{"/usr/bin/nmap", "-sP", "-n", "-PS", "-oG", "-", "--send-ip", range});
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            while ((line = br.readLine()) != null) {
                // Outputs your process execution
                System.out.println(line);
                if(line.contains("Host:"))
                    out.add(line.split(" ")[1]);
            }

            } catch (Exception e) {
                e.printStackTrace();
            }
            ips = out.toArray(new String[out.size()]);
            lastscan = System.currentTimeMillis();
        //}
        // dump
        for (String i : ips) {
            System.out.println("Dumping to " + i);
        }
        /*try {
            SubnetUtils utils = new SubnetUtils(range);
            ips = utils.getInfo().getAllAddresses();
        } catch(Exception e) {
            e.printStackTrace();
        }
        net.directSend.dump(ips, 10000, type, data);
    }
*/
    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            if(args.length != 2) {
				System.err.println("Usage:\n\tkitchensend type message");
				System.exit(1);
			}

            final Networking net = new Networking("TESTEST123");
            //net.directSend.dump(10000, 2, "test".getBytes());

			net.directSend.dump(10000, Integer.parseInt(args[0]), (args[1]).getBytes());

 //           dump(net, 2, ("Hello world").getBytes());

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
