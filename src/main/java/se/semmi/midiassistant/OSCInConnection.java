package se.semmi.midiassistant;

import de.sciss.net.OSCChannel;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCReceiver;
import de.sciss.net.OSCTransmitter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author robson
 */
public class OSCInConnection {

    public static final String STATUS_OK = "OK";
    public static final String STATUS_NO_SERVER_DEFINED = "NO_SERVER_DEFINED";
    public static final String STATUS_NO_TRANSMITTER = "NO_TRANSMITTER";
    private OSCReceiver receiver;
    private Object sync = new Object();
    private int listenPort;
    private String serverName;
    private ArrayList<MIDIOutputConnection> outMidiPorts = new ArrayList<MIDIOutputConnection>() ;


    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }


    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }


    public void close() {
        synchronized (outMidiPorts) {
            for (MIDIOutputConnection con : outMidiPorts) {
                con.close();
            }
            outMidiPorts.clear();
        }
        removeReceiver();
    }

    public void addOutputMidiConnection(MIDIOutputConnection out) {
        synchronized (outMidiPorts) {
            outMidiPorts.add(out);
        }
    }

    public String addReceiver() {
        removeReceiver();
        if (listenPort == 0) {
            return STATUS_NO_SERVER_DEFINED;
        }

        try {
            receiver = OSCReceiver.newUsing(OSCChannel.UDP, listenPort, false);
            System.out.println(serverName+" OSC listener is running on: "+receiver.getLocalAddress().getPort());
            receiver.startListening();
            receiver.addOSCListener(new OSCListener() {

                @Override
                public void messageReceived(OSCMessage msg, SocketAddress addr, long when) {
                    synchronized (sync) {
                        if ("/ping".equalsIgnoreCase(msg.getName())) {
                            System.out.println(serverName+" PING REQUEST from "+((String)msg.getArg(0)));
                            //String stat = pong();
                            //System.out.println(serverName+" PONG: "+stat);
                        } else
                        if ("/pong".equalsIgnoreCase(msg.getName())) {
                            System.out.println(serverName+" PONG from "+((String)msg.getArg(0)));
                        } else
                        if ("/midimessage".equalsIgnoreCase(msg.getName())) {
                            byte[] msgdec = Base64.decode((String) msg.getArg(1));
                            long timestamp = Long.parseLong(new String(Base64.decode((String) msg.getArg(2))));
 //                                   (Base64.decode((String) msg.getArg(2)));

                            System.out.println(serverName+" RECEIVE MIDI FROM REMOTE: " +((String)msg.getArg(0))+" "+ hexify(msgdec)+" "+timestamp);
                            // Send message to the selected MIDI channels
                            if (outMidiPorts != null) {
                                for (MIDIOutputConnection out : outMidiPorts) {
                                    out.send(msgdec, timestamp);
                                }
                            }
                        }
                        sync.notifyAll();
                    }
                }
            });
            return STATUS_OK;

        } catch (IOException ex) {
            return "I/O error: " + ex.getMessage();
        }
    }

    public void removeReceiver() {
        if (receiver != null) {
            receiver.dispose();
            receiver = null;
        }
    }


    public String hexify(byte[] data) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            if ((((int) data[i]) & 0xFF) < 16) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(((int) data[i]) & 0xFF)+" ");
        }
        return sb.toString();
    }

}
