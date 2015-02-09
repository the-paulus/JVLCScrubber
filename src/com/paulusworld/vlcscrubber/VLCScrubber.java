package com.paulusworld.vlcscrubber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.phidgets.Manager;
import com.phidgets.PhidgetException;

/**
 * The application class which contains the main function.
 * 
 * @author Paul Lyon (pmlyon@gmail.com)
 * 
 */
public class VLCScrubber {

    /**
     * @param args
     */
    public static void main(String[] args) {

	/**
	 * Default values for connecting to VLC via telnet interface.
	 */
	String host = "localhost";
	String password = "password";
	int port = 4212;
	boolean invalidArguments = false;

	// Parsing arguments.
	try {
	    for (int idx = 0; idx < args.length; idx++) {
		if (args[idx].equals("--host")) {
		    if (!args[idx + 1].contains("--")) {
			host = args[idx + 1];
		    } else {
			invalidArguments = true;
		    }
		}

		if (args[idx].equals("--port")) {
		    if (!args[idx + 1].contains("--")) {
			port = Integer.parseInt(args[idx + 1]);
		    } else {
			invalidArguments = true;
		    }
		}

		if (args[idx].equals("--password")) {
		    if (!args[idx + 1].contains("--")) {
			password = args[idx + 1];
		    } else {
			invalidArguments = true;
		    }
		}
	    }
	} catch (ArrayIndexOutOfBoundsException ex) {
	    printHelp();
	    System.exit(1);
	}

	if (invalidArguments) {
	    printHelp();
	    System.exit(1);
	}

	try {
	    String line;
	    BufferedReader br = new BufferedReader(new InputStreamReader(
		    System.in));

	    LinearTouchPhidgetManager manager = new LinearTouchPhidgetManager(
		    host, port, password);

	    while ((line = br.readLine()) != null && !line.equals("quit")) {
		// Just keep the program running.
	    }

	    manager.addAttachListener(manager);
	    manager.addDetachListener(manager);
	    manager.closeAll();
	    System.exit(0);

	} catch (IOException ioex) {
	    ioex.printStackTrace();
	} catch (PhidgetException ex) {
	    ex.printStackTrace();
	} catch (IllegalArgumentException ex) {
	    System.out.println(ex.getMessage());
	}

    }

    /**
     * Prints the help message.
     */
    private static void printHelp() {
	System.out.println("usage VLCScrubber <options> --videos <videos>");
	System.out.println("Where options are:");
	System.out
		.println("--host <host>\t\t- Hostname, FQDN, or IP address of the computer running the VLC telnet interface. (Default: localhost)");
	System.out
		.println("--port <port>\t\t- Port number for VLC. (Default: 4212)");
	System.out
		.println("--password <password>\t- Telnet password for VLC. This is defined by the VLC commandline argument --telnet-password.");
    }
}
