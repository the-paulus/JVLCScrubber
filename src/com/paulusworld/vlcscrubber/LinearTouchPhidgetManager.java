/**
 * 
 */
package com.paulusworld.vlcscrubber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.phidgets.InterfaceKitPhidget;
import com.phidgets.Manager;
import com.phidgets.Phidget;
import com.phidgets.PhidgetException;
import com.phidgets.event.AttachEvent;
import com.phidgets.event.AttachListener;
import com.phidgets.event.DetachEvent;
import com.phidgets.event.DetachListener;
import com.phidgets.event.InputChangeEvent;
import com.phidgets.event.InputChangeListener;
import com.phidgets.event.SensorChangeEvent;
import com.phidgets.event.SensorChangeListener;

/**
 * The LinearTouchPhidgetManager only manages one LinearTouch Phidget. This is
 * because we are using a LinearTouch Phidget as a video scrubber and there is
 * only one video scrubber.
 * 
 * @author Paul Lyon (pmlyon@gmail.com)
 * 
 */
public class LinearTouchPhidgetManager extends Manager implements
	AttachListener, DetachListener, SensorChangeListener,
	InputChangeListener {

    /**
     * Logger for this class.
     */
    private final Logger Log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * Number of phidgets managed.
     */
    private int mNumPhidgets = 0;

    /**
     * The phidget we're working with.
     */
    private InterfaceKitPhidget mPhidget;

    /**
     * The socket in which we connect to VLC through.
     */
    private static Socket mVlcSocket;

    /**
     * The PrintWriter that is used send commands to VLC via its Telnet
     * interface.
     */
    private PrintWriter mWriter;

    /**
     * A BufferedReader that reads VLC's response.
     */
    private BufferedReader mReader;

    /**
     * The position, in percentage, we want to seek to.
     */
    private double mSeekTo = 0;

    /**
     * Creates a new LinearTouchPhidgetManager and connects to VLC's telnet interface.
     * Once connected to VLC, attach and detach listeners are added which monitor when 
     * a phidiget is plugged in or removed from the computer.
     * 
     * @throws PhidgetException
     */
    public LinearTouchPhidgetManager(String host, int port, String password)
	    throws PhidgetException, IllegalArgumentException {

	try {

	    Log.setLevel(Level.INFO);
	    FileHandler logFileHandler = new FileHandler("vlcscrubber.log");
	    logFileHandler.setFormatter(new SimpleFormatter());
	    Log.addHandler(logFileHandler);

	    mVlcSocket = new Socket();
	    mVlcSocket.connect(new InetSocketAddress(host, port));

	    Log.log(Level.INFO, "Connected to " + host + " on " + port);

	    mWriter = new PrintWriter(mVlcSocket.getOutputStream(), true);
	    mReader = new BufferedReader(new InputStreamReader(
		    mVlcSocket.getInputStream()));

	    Log.log(Level.INFO, "Sending password...");

	    mWriter.println(password);

	    System.out.println("Password sent");

	    String line;
	    while ((line = mReader.readLine()) != null) {
		Log.log(Level.INFO, line);
		if (line.equals("Wrong password")) {
		    Log.log(Level.SEVERE,
			    "Unable to connect to "
				    + host
				    + " on port "
				    + port
				    + ". Please check your password and VLC's command line arguments.");
		    mVlcSocket.close();
		    mWriter.close();
		    mReader.close();
		    throw new IllegalArgumentException("Invalid password.");
		}
	    }

	    this.addAttachListener(this);
	    this.addDetachListener(this);
	    this.open();

	} catch (UnknownHostException e) {
	    Log.log(Level.SEVERE, "Unable to connect to host " + host + ":"
		    + port);
	    System.err
		    .println("Unable to connect to host " + host + ":" + port);
	} catch (IOException e) {
	    Log.log(Level.SEVERE, "Couldn't get I/O for host.");
	    System.err.println("Couldn't get I/O for host.");
	}
    }

    /**
     * Closes the connection, iostreams, and manager.
     * 
     * @throws PhidgetException
     */
    public void closeAll() throws PhidgetException {
	try {
	    mVlcSocket.close();
	    mWriter.close();
	    mReader.close();
	    close();
	} catch (IOException e) {
	    e.printStackTrace();
	} 
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.phidgets.event.DetachListener#detached(com.phidgets.event.DetachEvent
     * )
     */
    @Override
    public void detached(DetachEvent event) {
	try {
	    String message = "Removing " + event.getSource().getDeviceName() + " (" + event.getSource().getSerialNumber() + ")";
	    Log.log(Level.INFO, message);
	} catch (PhidgetException ex) {
	    Log.log(Level.SEVERE, ex.getMessage());
	}
	
	mNumPhidgets--;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.phidgets.event.AttachListener#attached(com.phidgets.event.AttachEvent
     * )
     */
    @Override
    public void attached(AttachEvent event) {
	mNumPhidgets++;
	try {
	    String message = "Attached new Phidget\n" + event.getSource().getDeviceName() + " " + event.getSource().getDeviceLabel() + " " + event.getSource().getDeviceType() + " " + event.getSource().getSerialNumber();
	    Log.log(Level.INFO, message);
	    if (event.getSource().getDeviceID() == Phidget.PHIDID_LINEAR_TOUCH) {
		mPhidget = new InterfaceKitPhidget();
		mPhidget.open(event.getSource().getSerialNumber());
		mPhidget.addInputChangeListener(this);
		mPhidget.addSensorChangeListener(this);

	    } else {
		throw new PhidgetException(PhidgetException.EPHIDGET_INVALID,
			"Invalid phidget.");
	    }

	    if (mNumPhidgets > 1) {
		throw new PhidgetException(
			PhidgetException.EPHIDGET_UNSUPPORTED,
			"Only one Phidget is supported.");
	    }
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }

    @Override
    public void inputChanged(InputChangeEvent event) {
	int input = event.getIndex();
	boolean state = event.getState();

	if (input == 0 && state) {
	    mWriter.println("pause");
	    System.out.println("Pausing...");
	}

	if (input == 0 && !state) {
	    try {
		Thread.sleep(5000);
		mWriter.println("play");
		System.out.println("Playing...");
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

    @Override
    public void sensorChanged(SensorChangeEvent event) {
	mSeekTo = Math.round(event.getValue() / 10);
	mWriter.println("seek " + mSeekTo + "%");
	System.out.println("Seeking to " + mSeekTo + "%");
    }

}
