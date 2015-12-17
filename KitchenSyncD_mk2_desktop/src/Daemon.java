import com.moosd.kitchensyncd.networking.Networking;
import com.moosd.kitchensyncd.networking.PacketHandler;
import com.moosd.kitchensyncd.sync.OneWaySync;
import com.moosd.kitchensyncd.sync.SyncScheduler;

import javax.swing.*;


public class Daemon {
    static String oldtext = "";

    public static boolean rsyncing = false;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// Init and test networking layer
			try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
		}
            rsyncing = false;
			
			final Networking net = new Networking("TESTEST123", 10000);
			System.out.println("IsConnected: "+net.isConnected());

            // Broadcast popup
            net.hooks.addDirectHook(3, new PacketHandler() {
                @Override
                public void handle(String senderUid, String senderIp,
                                   int senderPort, byte[] data) {
                    String dat = new String(data).trim();
                    JTextArea ta = new JTextArea(10, 30);
                    ta.setText(dat);
                    ta.setWrapStyleWord(true);
                    ta.setLineWrap(true);
                    ta.setCaretPosition(0);
                    ta.setEditable(false);

                    JOptionPane.showMessageDialog(null, new JScrollPane(ta), "KitchenSync Broadcast", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            // Notification
            net.hooks.addDirectHook(4, new PacketHandler() {
                @Override
                public void handle(String senderUid, String senderIp,
                                   int senderPort, byte[] data) {
                    String dat = new String(data).trim();
                    if(!oldtext.equals(dat)) {
                        oldtext = dat;
                        try {
                            String[] d = dat.split("\\n");
                            if (d.length == 1)
                                (new ProcessBuilder(new String[]{"/usr/bin/notify-send", d[0]})).start();
                            else {
                                if(!d[1].equals("Incoming call"))
                                    (new ProcessBuilder(new String[]{"/usr/bin/notify-send", d[0], dat.substring(d[0].length())})).start();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            // Sync /sdcard/DCIM/Camera here
            net.hooks.addDirectHook(5, new PacketHandler() {
                @Override
                public void handle(String senderUid, String senderIp,
                                   int senderPort, byte[] data) {
                    String dat = new String(data).trim();
                    if(rsyncing) return;
                    rsyncing = true;
                    long testTime = Long.parseLong(dat);
                    if (testTime == (System.currentTimeMillis()/(1000*60))) {
                        try {
                            (new ProcessBuilder(new String[]{"/usr/bin/rsync", "-e", "ssh -p 5120 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no", "-azv", "--no-perms", "--no-times", "--size-only", "shell@"+senderIp + ":/sdcard/DCIM/Camera/", "/home/souradip/Pictures/Camera/"})).start();
                            (new ProcessBuilder(new String[]{"/usr/bin/python3", "/home/souradip/Projects/KitchenSync/KitchenSyncD_mk2_desktop/sync_pim.py", senderIp})).start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else System.out.println(" Tamper detection - posisble playback attack?");
                    rsyncing = false;
                }
            });

            // start sms program server

           /* new Thread() {
                @Override
                public void run() {
                    super.run();
                    net.directSend.dump(10000, 999, ("ping").getBytes());


                    try {
                        Thread.sleep(1000 * 20);
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }.start();*/
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
