/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.semmi.midiassistant;

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
public class MIDIInputConnection {
    
    public static final String STATUS_MIDI_OK = "MIDI_OK";
    public static final String STATUS_MIDI_UNAVAILABLE = "MIDI_UNAVAILABLE";
    public static final String STATUS_MIDI_ERROR_RETREIVING_DEVICE = "MIDI_ERROR_RETREIVING_DEVICE";


    MidiDevice inputDevice = null;
    private Transmitter sender;

    public String addMidiListener(int deviceNum, final OSCOutConnection osc) {
        removeMidiListener();

        Transmitter transmitter;

        MidiDevice.Info info;
        info = MidiCommon.getMidiDeviceInfo(deviceNum);

        try {
            inputDevice = MidiSystem.getMidiDevice(info);
            inputDevice.open();
        } catch (MidiUnavailableException e) {
            return STATUS_MIDI_UNAVAILABLE;
        }
        if (inputDevice == null) {
            return STATUS_MIDI_ERROR_RETREIVING_DEVICE;
        }
        Receiver r = new Receiver() {

            @Override
            public void send(MidiMessage message, long timeStamp) {
                osc.sendMidiMessage(message.getMessage(), timeStamp);
            }

            @Override
            public void close() {
            }
            
        };

        try {
            transmitter = inputDevice.getTransmitter();
            transmitter.setReceiver(r);
        } catch (MidiUnavailableException e) {
            return STATUS_MIDI_UNAVAILABLE;
        }

        return STATUS_MIDI_OK;

    }

    public void removeMidiListener() {
        if (inputDevice != null) {
            inputDevice.close();
            inputDevice = null;
        }
    }

    public void close() {
        removeMidiListener();
    }

}
