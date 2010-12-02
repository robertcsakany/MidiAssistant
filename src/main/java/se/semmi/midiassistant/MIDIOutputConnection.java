/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.semmi.midiassistant;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

/**
 *
 * @author robson
 */
public class MIDIOutputConnection {
    
    public static final String STATUS_MIDI_OK = "MIDI_OK";
    public static final String STATUS_MIDI_UNAVAILABLE = "MIDI_UNAVAILABLE";
    public static final String STATUS_MIDI_ERROR_RETREIVING_DEVICE = "MIDI_ERROR_RETREIVING_DEVICE";


    MidiDevice outputDevice = null;
    private Transmitter sender;

    public String addMidiSender(int deviceNum) {
        removeMidiSender();

        Transmitter transmitter;

        MidiDevice.Info info;
        info = MidiCommon.getMidiDeviceInfo(deviceNum);

        try {
            outputDevice = MidiSystem.getMidiDevice(info);
            outputDevice.open();
        } catch (MidiUnavailableException e) {
            return STATUS_MIDI_UNAVAILABLE;
        }
        if (outputDevice == null) {
            return STATUS_MIDI_ERROR_RETREIVING_DEVICE;
        }

        Receiver r;
        try {
            r = outputDevice.getReceiver();
        } catch (MidiUnavailableException ex) {
            return STATUS_MIDI_UNAVAILABLE;
        }


        /*
        try {
            transmitter = outputDevice.getTransmitter();
            transmitter.setReceiver(r);
        } catch (MidiUnavailableException e) {
            return STATUS_MIDI_UNAVAILABLE;
        }
        */
        return STATUS_MIDI_OK;

    }

    public void removeMidiSender() {
        if (outputDevice != null) {
            outputDevice.close();
            outputDevice = null;
        }
    }

    public String send(byte[] msg, long timeStamp) {
        Receiver r;
        try {
            r = outputDevice.getReceiver();
            MidiMessage midi = new MidiMessage(msg) {

                @Override
                public Object clone() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
            r.send(midi, timeStamp);
        } catch (MidiUnavailableException ex) {
            return STATUS_MIDI_UNAVAILABLE;
        }
        return STATUS_MIDI_OK;
    }

    public void close() {
        removeMidiSender();
    }

}
