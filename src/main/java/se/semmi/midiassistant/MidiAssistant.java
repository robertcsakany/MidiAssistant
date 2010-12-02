/*
 * DesktopApplication1.java
 */
package se.semmi.midiassistant;

import java.util.ArrayList;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

/**
 * The main class of the application.
 */
public class MidiAssistant { //extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    /*
    @Override
    protected void startup() {
        show(new MidiAssistantView(this));
    }
*/
    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
  /*
    @Override
    protected void configureWindow(java.awt.Window root) {
    }
*/
    /**
     * A convenient static getter for the application instance.
     * @return the instance of DesktopApplication1
     */
  /*
    public static MidiAssistant getApplication() {
        return Application.getInstance(MidiAssistant.class);
    }
   * 
   */
    static ArrayList<OSCOutConnection> outConnections = new ArrayList<OSCOutConnection>();
    static ArrayList<OSCInConnection> inConnections = new ArrayList<OSCInConnection>();
    static ArrayList<MIDIInputConnection> midiIns = new ArrayList<MIDIInputConnection>();
    static ArrayList<MIDIOutputConnection> midiOuts = new ArrayList<MIDIOutputConnection>();
    static String serverName = "";

    static Object sync = new Object();

    public static void close() {
        synchronized(sync) {
            for (OSCOutConnection osc : outConnections) {
                osc.close();
            }
            outConnections.clear();

            for (OSCInConnection osc : inConnections) {
                osc.close();
            }
            inConnections.clear();

            for (MIDIInputConnection midi : midiIns) {
                midi.removeMidiListener();
            }
            midiIns.clear();
            System.exit(0);
        }
    }

    public static void main(String[] args)
            throws Exception {
        //launch(MidiAssistant.class, args);



        if (args.length < 1) {
            System.out.println("Usage: MidiAssistant [command [param1] [param2]...] [command [param1 [param2]]]\n"
                    + "  Commands: \n"
                    + "       list                       List all MIDI port\n"
                    + "       name                       Set the name of this host\n"
                    + "       remote <host> <port>       The remote host and port where the MIDI messages\n"
                    + "                                  is transfered\n"
                    + "       ping <milisec>             Ping the remotehost host periodicaly\n"
                    + "       midiin <midi port index>   The MIDI port listen in for messages \n"
                    + "       listen <port>              Set a server where the remote clients sends MIDI messages\n"
                    + "       midiout <midi port index>  MIDI port where the messages from remote host\n"
                    + "                                  is transfered\n");

        }


        OSCOutConnection actOSCout = null;
        OSCInConnection actOSCin = null;
        int argidx = 0;
        while (argidx < args.length) {
            String paramName = args[argidx];

            if ("list".equalsIgnoreCase(paramName)) {
                System.out.println("MIDI ports: ");
                MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
                for (int i = 0; i < aInfos.length; i++) {
                    try {
                        MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                        boolean bAllowsInput = (device.getMaxTransmitters() != 0);
                        boolean bAllowsOutput = (device.getMaxReceivers() != 0);
                        if ((bAllowsInput)
                                || (bAllowsOutput)) {
                            System.out.println("" + i + "  "
                                    + (bAllowsInput ? "IN " : "   ")
                                    + (bAllowsOutput ? "OUT " : "    ")
                                    + aInfos[i].getName() + ", "
                                    + aInfos[i].getVendor() + ", "
                                    + aInfos[i].getVersion() + ", "
                                    + aInfos[i].getDescription());
                        }

                    } catch (MidiUnavailableException e) {
                        // device is obviously not available...
                        System.out.println("[ERROR] MIDI subsystem is unavailable");
                    }
                }
                if (aInfos.length == 0) {
                    System.out.println("[ERROR] [No MIDI devices available]");
                }

            } else if ("name".equalsIgnoreCase(paramName)) {
                serverName = args[argidx + 1];
                for (OSCOutConnection osc : outConnections) {
                    osc.setServerName(serverName);
                }
                for (OSCInConnection osc : inConnections) {
                    osc.setServerName(serverName);
                }

                argidx++;

            } else if ("remote".equalsIgnoreCase(paramName)) {
                int portnum = 0;
                try {
                    portnum = Integer.parseInt(args[argidx + 2]);
                } catch (Throwable th) {
                    System.out.println("[ERROR] Invalid port number: " + args[argidx + 2]);
                    close();
                }
                if (portnum < 1 || portnum > 65534) {
                    System.out.println("[ERROR] Invalid port number, must be 0-65534: " + args[argidx + 2]);
                    close();
                }

                actOSCout = new OSCOutConnection();
                outConnections.add(actOSCout);
                actOSCout.setServerName(serverName);
                actOSCout.setHost(args[argidx + 1]);
                actOSCout.setPort(portnum);
                String ret = actOSCout.addTransmitter();
                if (!ret.startsWith(actOSCin.STATUS_OK)) {
                    System.out.println("[ERROR] Cannot add transmitter: "+ret);
                }

                argidx += 2;
            } else if ("midiin".equalsIgnoreCase(paramName)) {
                if (actOSCout == null) {
                    System.out.println("[ERROR] No remote host defined, define it first");
                    close();
                }
                int portnum = 0;
                try {
                    portnum = Integer.parseInt(args[argidx + 1]);
                } catch (Throwable th) {
                    System.out.println("[ERROR] Invalid channel index: " + args[argidx + 1]);
                    close();
                }
                MIDIInputConnection midiIn = new MIDIInputConnection();
                midiIn.addMidiListener(portnum, actOSCout);
                midiIns.add(midiIn);
                argidx++;
            } else if ("ping".equalsIgnoreCase(paramName)) {
                if (actOSCout == null) {
                    System.out.println("[ERROR] No remote host defined, define it first");
                    close();
                }
                int interval = 0;
                try {
                    interval = Integer.parseInt(args[argidx + 1]);
                } catch (Throwable th) {
                    System.out.println("[ERROR] Invalid interval " + args[argidx + 1]);
                    close();
                }
                actOSCout.addRemoteChecker(interval);
                argidx++;
            } else if ("listen".equalsIgnoreCase(paramName)) {
                int portnum = 0;
                try {
                    portnum = Integer.parseInt(args[argidx + 1]);
                } catch (Throwable th) {
                    System.out.println("[ERROR] Invalid port number: " + args[argidx + 1]);
                    close();
                }
                if (portnum < 1 || portnum > 65534) {
                    System.out.println("[ERROR] Invalid port number, must be 0-65534: " + args[argidx + 1]);
                    close();
                }

                actOSCin = new OSCInConnection();
                inConnections.add(actOSCin);
                actOSCin.setServerName(serverName);
                actOSCin.setListenPort(portnum);
                String ret = actOSCin.addReceiver();
                if (!ret.startsWith(actOSCin.STATUS_OK)) {
                    System.out.println("[ERROR] Cannot add listener: "+ret);
                }
                argidx ++;
            } else if ("midiout".equalsIgnoreCase(paramName)) {
                if (actOSCin == null) {
                    System.out.println("[ERROR] Listen port is defined, define it first");
                    close();
                }
                int portnum = 0;
                try {
                    portnum = Integer.parseInt(args[argidx + 1]);
                } catch (Throwable th) {
                    System.out.println("[ERROR] Invalid channel index: " + args[argidx + 1]);
                    close();
                }
                MIDIOutputConnection midiOut = new MIDIOutputConnection();
                midiOut.addMidiSender(portnum);
                actOSCin.addOutputMidiConnection(midiOut);
                midiOuts.add(midiOut);
                argidx++;
            } else {
                System.out.println("[ERROR] Unknown command: " + paramName);
                close();
            }
            argidx++;
        }


        System.out.println("To terminate program press ENTER");
        boolean exit=false;
        while (!exit) {
            char input = (char) System.in.read();
            System.out.print(input);
//            if (input == 27) {
//                System.out.println("[SHUTDOWN] Stopping services and connections");
                close();
//            }
            Thread.sleep(100);
         }
    

            /*
            OSCInConnection peer1 = new OSCInConnection();
            OSCInConnection peer2 = new OSCInConnection();

            peer1.setHost("127.0.0.1");
            peer1.setPort(5001);
            peer1.setListenPort(5002);
            peer1.setServerName("peer1");
            System.out.println(peer1.addReceiver(null));
            System.out.println(peer1.addTransmitter());
            MIDIInputConnection midipeer1 = new MIDIInputConnection();
            midipeer1.addMidiListener(2, peer1);
            peer1.addRemoteChecker();


            peer2.setHost("127.0.0.1");
            peer2.setPort(5002);
            peer2.setListenPort(5001);
            peer2.setServerName("peer2");
            MIDIOutputConnection midipeer2 = new MIDIOutputConnection();
            midipeer2.addMidiSender(12);
            ArrayList<MIDIOutputConnection> midiOutPorts = new ArrayList<MIDIOutputConnection>();
            midiOutPorts.add(midipeer2);

            System.out.println(peer2.addReceiver(midiOutPorts));
            System.out.println(peer2.addTransmitter());
            peer2.addRemoteChecker();




            // System.out.println(peer1.ping());
            // System.out.println(peer2.ping());
            //peer1.addRemoteChecker();
            //peer2.addRemoteChecker();

            while (1 == 1) {
            }
            /*
            peer1.removeRemoteChecker();
            peer2.removeRemoteChecker();
            peer1.removeReceiver();
            peer2.removeReceiver();
             */
    }
}
