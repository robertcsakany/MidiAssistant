package se.semmi.midiassistant;

import de.sciss.net.OSCChannel;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCTransmitter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

class PollingThread extends Thread {

    OSCTransmitter transmitter;

    public PollingThread(OSCTransmitter transmitter) {
        this.transmitter = transmitter;
    }

    @Override
    public void run() {
        boolean exit = false;
        while (!exit) {
            if (transmitter != null && (!transmitter.isConnected())) {
                try {
                    transmitter.connect();
                } catch (IOException ex) {
                    try {
                        System.out.println("[ERROR] Could not connect to host: " + transmitter.getLocalAddress().getHostName() + ":" + transmitter.getLocalAddress().getPort());
                        Thread.sleep(1000);
                    } catch (InterruptedException ex1) {
                        System.out.println("[STOP] Connection PollingThread for: "+transmitter.getLocalAddress().getHostName() + ":" + transmitter.getLocalAddress().getPort());
                        exit = true;
                    }
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                System.out.println("[STOP] Connection PollingThread for: "+transmitter.getLocalAddress().getHostName() + ":" + transmitter.getLocalAddress().getPort());
                exit = true;
            }
            System.out.println("Polling");
        }

    }
}

/**
 *
 * @author robson
 */
public class OSCOutConnection {

    public static final String STATUS_OK = "OK";
    public static final String STATUS_NO_SERVER_DEFINED = "NO_SERVER_DEFINED";
    public static final String STATUS_NO_TRANSMITTER = "NO_TRANSMITTER";
    private OSCTransmitter transmitter;
    InetSocketAddress targetAddr;
    private Object sync = new Object();
    private String host;
    private int port;
    private boolean connected = false;
    private Timer checkerTimer;
    private String serverName;
    PollingThread pollingThread;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void close() {
        removeTransmitter();
    }

    public String addTransmitter() {

        try {
            transmitter = OSCTransmitter.newUsing(OSCChannel.UDP, 0, false);
            targetAddr = new InetSocketAddress(host, port);
            transmitter.connect();
            transmitter.setTarget(targetAddr);

            //transmitter.send(new OSCMessage("/ping", new Object[]{"hello"}));
            //transmitter.dispose();
            return STATUS_OK;
        } catch (IOException ex) {
            return "I/O error: " + ex.getMessage();
        } finally {
            System.out.println("[START] Connection PollingThread for: "+transmitter.getLocalAddress().getHostName() + ":" + transmitter.getLocalAddress().getPort());
            pollingThread = new PollingThread(transmitter);
        }

    }

    public void removeTransmitter() {
        if (transmitter != null) {
            transmitter.dispose();
            transmitter = null;
        }
    }

    public String ping() {
        if (transmitter == null) {
            return STATUS_NO_TRANSMITTER;
        }
        try {
            transmitter.send(new OSCMessage("/ping", new Object[]{serverName}));
            return STATUS_OK;
        } catch (IOException ex) {
            return "I/O error: " + ex.getMessage();
        }
    }

    public void addRemoteChecker(int interval) {
        removeRemoteChecker();
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                System.out.println(serverName + " PING");
                String ret = ping();
                if (STATUS_OK.equals(ret)) {
                    setConnected(true);
                } else {
                    System.out.println("[ERROR] " + ret);
                    setConnected(false);
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, 0, interval);
    }

    public void removeRemoteChecker() {
        if (checkerTimer != null) {
            checkerTimer.purge();
            checkerTimer = null;
        }

    }

    public String hexify(byte[] data) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            if ((((int) data[i]) & 0xFF) < 16) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(((int) data[i]) & 0xFF) + " ");
        }
        return sb.toString();
    }

    public synchronized String sendMidiMessage(byte[] msg, long timestamp) {
        if (transmitter == null) {
            return STATUS_NO_TRANSMITTER;
        }
        try {
            System.out.println(serverName + " SEND MIDI TO REMOTE: " + hexify(msg));
            transmitter.send(new OSCMessage("/midimessage", new Object[]{serverName, Base64.encode(msg), Base64.encode(String.valueOf(timestamp).getBytes())}));
            return STATUS_OK;
        } catch (IOException ex) {
            return "I/O error: " + ex.getMessage();
        }
    }
}
