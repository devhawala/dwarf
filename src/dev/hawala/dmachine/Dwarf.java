/*
Copyright (c) 2017, Dr. Hans-Walter Latz
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * The name of the author may not be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package dev.hawala.dmachine;

import java.awt.EventQueue;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import dev.hawala.dmachine.dwarf.DebuggerSubstituteMpHandler;
import dev.hawala.dmachine.dwarf.KeyHandler;
import dev.hawala.dmachine.dwarf.MainUI;
import dev.hawala.dmachine.dwarf.MouseHandler;
import dev.hawala.dmachine.dwarf.PropertiesExt;
import dev.hawala.dmachine.dwarf.UiRefresher;
import dev.hawala.dmachine.dwarf.WindowStateListener;
import dev.hawala.dmachine.dwarf.eKeyEventCode;
import dev.hawala.dmachine.dwarf.KeyboardMapper;
import dev.hawala.dmachine.dwarf.TestUiDataConsumer;
import dev.hawala.dmachine.dwarf.MainUI.RunningState;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.InitialMesaMicrocode;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Opcodes;
import dev.hawala.dmachine.engine.PilotDefs;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.Xfer;
import dev.hawala.dmachine.engine.iUiDataConsumer;
import dev.hawala.dmachine.engine.agents.Agents;
import dev.hawala.dmachine.engine.agents.DiskAgent;
import dev.hawala.dmachine.engine.agents.DiskState;
import dev.hawala.dmachine.engine.agents.NetworkAgent;

/**
 * Dwarf application main program, loading the configuration for the
 * mesa engine, building the main UI and running the mesa engine
 * in a separate thread driving the Dwarf UI. 
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Dwarf {
	
	// the defaults for optional engine cnfiguration
	private static final int DEFAULT_DISPLAY_WIDTH = 1024;
	private static final int DEFAULT_DISPLAY_HEIGHT = 640;
	private static final String DEFAULT_SWITCHES = "8Wy{|}\\346\\347\\350\\377";
	private static final String DEFAULT_MAC = "00-1D-BA-AE-04-C3";
	
	// the (Java-side) intended refresh rate for the UI
	private static final int UiRefreshInterval = 20; // 20 ms => 50 wakeups => 25 display refeshes in average
	
	// the interface between the Java UI and the mea engine
	private static UiRefresher uiRefresher;
	
	// the Swing timer allowing to run asynchronous ui updates in the swing ui thread   
	private static Timer uiTimer;
	
	// the file chooser for selecting virtual floppy disks
	private static JFileChooser fileChooser = new JFileChooser();
	
	// the main Dwarf frame/window
	private static MainUI window;
	
	// the loaded configuration of the mesa engine to run including some defaults.
	private static String configFilename;
	private static String title = "unknown disk/system";
	private static String bootFile = null;
	private static String germFile = null;
	private static int addressBitsVirtual = 23;
	private static int addressBitsReal = 22;
	private static int displayWidth = DEFAULT_DISPLAY_WIDTH;
	private static int displayHeight = DEFAULT_DISPLAY_HEIGHT;
	private static boolean displayTypeColor = false;
	private static String switches = DEFAULT_SWITCHES;
	private static int[] macBytes = new int[6];
	private static int[] macWords = new int[3];
	private static String recognizedMacId = "";
	private static int oldDeltasToKeep = 5;
	private static String initialFloppy = null;
	private static String floppyDirectory = null;
	private static String keyboardMapFile = null;
	private static int xeroxControlKeyCode = eKeyEventCode.VK_CONTROL.getCode();
	private static boolean resetKeysOnFocusLost = true;
	private static String netHubHost = "";
	private static int netHubPort = 3333;
	private static int localTimeOffsetMinutes = 0;
	
	// control flags for the mesa engine
	private static boolean doStartEngine = false;
	private static boolean engineIsRunning = false;
	private static boolean doTerminate = false;
	
	// synchonization object for the 2 relevant thread (mesa engine thread and Swing ui thread)
	private static final Object lock = new Object();
	
	// set the flag for starting the engine
	private static void startEngine() {
		synchronized(lock) {
			doStartEngine = true;
			lock.notifyAll();
		}
	}
	
	/**
	 * @return the running status of the mesa engine
	 */
	public static boolean isMesaEngineRunning() {
		synchronized(lock) {
			return engineIsRunning;
		}
	}
	
	/**
	 * Request to close the main window (and the application) when
	 * the mesa engine has come to halt.
	 */
	public static void terminateOnEngineStopped() {
		synchronized(lock) {
			doTerminate = true;
		}
	}
	
	// check if the filename identifies an readably file
	private static boolean isFileOk(String kind, String filename) {
		if (filename == null) {
			System.err.printf("Error: no filename given for %s\n", kind);
			return false;
		}
		File f = new File(filename);
		if (!f.canRead()) {
			System.err.printf("Error: file '%s' given for %s does not exist or is not readable\n", filename, kind);
			return false;
		}
		return true;
	}
	
	// parse the mac (machine address, processor id) from the given string
	private static boolean parseMac(String mac) {
		String[] submacs = mac.split("-");
		if (submacs.length != macBytes.length) {
			System.err.printf("Error: invalid processor id format (not XX-XX-XX-XX-XX-XX): %s\n", mac);
			return false;
		}
		
		for (int i = 0; i < macBytes.length; i++) {
			try {
				macBytes[i] = Integer.parseInt(submacs[i], 16) & 0xFF;
			} catch (Exception e) {
				System.err.printf("Error: invalid processor id format (not XX-XX-XX-XX-XX-XX): %s\n", mac); 
				return false;
			}
		}
		
		recognizedMacId = String.format(
				"%02X-%02X-%02X-%02X-%02X-%02X", 
				macBytes[0], macBytes[1], macBytes[2], macBytes[3], macBytes[4], macBytes[5]);
		macWords[0] = (macBytes[0] << 8) | macBytes[1];
		macWords[1] = (macBytes[2] << 8) | macBytes[3];
		macWords[2] = (macBytes[4] << 8) | macBytes[5];
		return true;
	}
	
	// parse the keycode either as 0x-hexcode or as VK_-name of the key
	private static int parseKeycode(String keycode) {
		if (keycode.startsWith("x")) {
			try {
				return Integer.parseInt(keycode.substring(1), 16);
			} catch (NumberFormatException nfe) {
				System.out.printf("Invalid hex-code for keycode '%s', using Ctrl-Key instead\n", keycode);
				return eKeyEventCode.VK_CONTROL.getCode();
			}
		} else {
			eKeyEventCode javaKey = null;
			try {
				javaKey = eKeyEventCode.valueOf(keycode);
			} catch (Exception e) {
				// ignored
			}
			if (javaKey == null) {
				System.out.printf("Invalid key-name '%s' for keycode, using Ctrl-Key instead\n", keycode);
				return eKeyEventCode.VK_CONTROL.getCode();
			}
			return javaKey.getCode();
		}
	}
	
	// load the mesa engine configuration from the given file
	private static boolean initializeConfiguration(String filename) {
		if (!filename.endsWith(".properties")) { filename += ".properties"; }
		File cfgFile = new File(filename);
		if (!cfgFile.canRead()) {
			System.err.printf("Error: unable to read configuration properties file: %s\n", filename);
			return false;
		}
		configFilename = filename;
		
		PropertiesExt props = new PropertiesExt();
		try {
			FileInputStream fis = new FileInputStream(cfgFile);
			props.load(fis);
			fis.close();
		} catch (Exception e) {
			System.err.printf("Error: unable to load configuration from properties file: %s\n", filename);
			System.err.printf("=> %s\n", e.getMessage());
			return false;
		}
		
		bootFile = props.getString("boot", null);
		if (!isFileOk("boot", bootFile)) { return false; }
		germFile = props.getString("germ", null);
		if (!isFileOk("germ", germFile)) { return false; }
		addressBitsVirtual = props.getInt("addressBitsVirtual", addressBitsVirtual);
		addressBitsReal = props.getInt("addressBitsReal", addressBitsReal);
		displayWidth = props.getInt("displayWidth", displayWidth);
		displayHeight = props.getInt("displayHeight", displayHeight);
		displayTypeColor = props.getBoolean("displayTypeColor", displayTypeColor);
		switches = props.getString("switches", switches);
		title = props.getString("title", bootFile);
		oldDeltasToKeep = props.getInt("oldDeltasToKeep", oldDeltasToKeep);
		initialFloppy = props.getString("initialFloppy", initialFloppy);
		floppyDirectory = props.getString("floppyDirectory", floppyDirectory);
		keyboardMapFile = props.getString("keyboardMapFile", keyboardMapFile);
		doStartEngine = props.getBoolean("autostart", doStartEngine);
		netHubHost = props.getString("netHubHost", netHubHost);
		netHubPort = props.getInt("netHubPort", netHubPort);
		localTimeOffsetMinutes = props.getInt("localTimeOffsetMinutes", localTimeOffsetMinutes);
		
		String ctrlKeyCode = props.getString("xeroxControlKeyCode", null);
		if (ctrlKeyCode != null && ctrlKeyCode.length() > 0) {
			xeroxControlKeyCode = parseKeycode(ctrlKeyCode);
		}
		resetKeysOnFocusLost = props.getBoolean("resetKeysOnFocusLost", resetKeysOnFocusLost);
		
		addressBitsReal = Math.max(PrincOpsDefs.MIN_REAL_ADDRESSBITS, Math.min(PrincOpsDefs.MAX_REAL_ADDRESSBITS, addressBitsReal));
		addressBitsVirtual = Math.max(addressBitsReal, Math.min(PrincOpsDefs.MAX_VIRTUAL_ADDRESSBITS, addressBitsVirtual));
		
		String mac = props.getString("processorId", null);
		if (mac == null) {
			System.out.printf("Warning: no processor id specified, using default\n");
			mac = DEFAULT_MAC;
		}
		if (!parseMac(mac)) {
			System.out.printf("Warning: invalid processor id specified, using default\n");
			parseMac(DEFAULT_MAC);
		}
		
		return true;
	}
	
	// load/mount a virtual floppy into the mesa engine (the corresponding agent)
	// and adjust the ui accordingly
	private static void insertFloppy(MainUI window, File floppyFile) throws IOException {
		boolean writeProtected = window.writeProtectFloppy();
		boolean isReadonly = Agents.insertFloppy(floppyFile, writeProtected);
		
		String floppyPrefix = "";
		if (!writeProtected && isReadonly) {
			floppyPrefix = "[forced R/O] ";
		} else if (writeProtected) {
			floppyPrefix = "[R/O] ";
		}
		window.setFloppyName(floppyPrefix + floppyFile.getName());
	}
	
	private static void dumpConfiguration() {
		System.out.printf("Configuration from %s\n", configFilename);
		System.out.printf(" germ file   : %s\n", germFile);
		System.out.printf(" switches    : %s\n", switches);
		System.out.printf(" boot file   : %s\n", bootFile);
		System.out.printf(" deltas limit: %d\n", oldDeltasToKeep);
		System.out.printf(" bits virtual: %d\n", addressBitsVirtual);
		System.out.printf(" bits real   : %d\n", addressBitsReal);
		System.out.printf(" display     : w( %d ) x h( %d ) - %s display\n", displayWidth, displayHeight, displayTypeColor ? "color" : "b/w");
		System.out.printf(" keyboardMap : %s\n", (keyboardMapFile != null) ? keyboardMapFile : "");
		System.out.printf(" xeroxCtrlKey: 0x%08X\n", xeroxControlKeyCode);
		System.out.printf(" resetKeysOnF: %s\n", (resetKeysOnFocusLost)  ? "yes" : "no");
		System.out.printf(" mac words   : %04X - %04X - %04X (%s)\n", macWords[0], macWords[1], macWords[2], recognizedMacId);
		System.out.printf(" autostart   : %s\n", (doStartEngine) ? "yes" : "no");
		System.out.printf(" floppy      : %s\n", (initialFloppy != null) ? initialFloppy : "");
		System.out.printf(" floppy dir  : %s\n", (floppyDirectory != null) ? floppyDirectory : "");
		System.out.printf(" netHubHost  : %s\n", netHubHost);
		System.out.printf(" netHubPort  : %d\n", netHubPort);
		System.out.printf(" localTimeOff: %d\n", localTimeOffsetMinutes);
	}
	
	// the main program
	public static void main(String[] args) throws IOException {
		
		boolean testOnly = false;
		boolean logKeyPressed = false;
		boolean doMerge = false;
		String cfgFile = null;
		
		// command line parameters pass 1: check for test only OR run configuration
		for (String arg : args) {
			if ("-test".equalsIgnoreCase(arg)) {
				testOnly = true;
				break;
			} else if (!arg.startsWith("-")) {
				if (cfgFile == null) {
					cfgFile = arg;
				} else {
					System.out.printf("Warning: ignoring unknown argument: %s\n", arg);
				}
			}
		}
		
		// load the configuration file
		if (!testOnly) {
			if (cfgFile == null) {
				System.err.println("Error: no configuration specified.");
				return;
			}
			if (!initializeConfiguration(cfgFile)) {
				return;
			}
			
			// command line parameters pass 2: check for overrides to configuration
			boolean dumpConfig = false;
			for (String arg : args) {
				if (arg.startsWith("-") && !"-test".equalsIgnoreCase(arg)) {
					if ("-run".equalsIgnoreCase(arg)) {
						doStartEngine = true;
					} else if ("-v".equalsIgnoreCase(arg)) {
						dumpConfig = true;
					} else if ("-logkeypressed".equalsIgnoreCase(arg)) {
						logKeyPressed = true;
					} else if ("-merge".equalsIgnoreCase(arg)) {
						doMerge = true;
					} else {
						System.out.printf("Warning: ignoring unknown command line argument: %s\n", arg);
					}
				}
			}
			if (dumpConfig) { 
				dumpConfiguration();
			}
		}
		
		// merge disks of requested, doing nothing else afterwards
		if (doMerge) {
			PrintStream ps = System.out;
			DiskState diskState = DiskAgent.addFile(bootFile, false, oldDeltasToKeep);
			if (diskState == DiskState.ReadOnly) {
				ps.printf("Bootdisk '%s' is readonly and cannot be merged, aborting!\n", bootFile);
			} else if (diskState == DiskState.Corrupted) {
				ps.printf("Bootdisk '%s' has corrupted delta and cannot be merged, aborting!\n", bootFile);
			} else {
				DiskAgent.mergeDisks(ps);
			}
			return;
		}
		
		// setup the mesa machine to get the callbacks for the ui to the mesa machine
		// (or to a test pseudo-machine)
		iUiDataConsumer uiDataConsumer;
		if (testOnly) {
			// test mode
			uiDataConsumer = new TestUiDataConsumer(DEFAULT_DISPLAY_WIDTH, DEFAULT_DISPLAY_HEIGHT);
		} else {
			// setup the mesa machine
			
			// set processor id (aka MAC address)
			Cpu.setPID(macWords[0], macWords[1], macWords[2]);
			
			// initialize the memory subsystem with the configured display configuration
			Mem.initializeMemory(
					addressBitsVirtual, addressBitsReal,
					(displayTypeColor) ? PilotDefs.DisplayType.byteColor : PilotDefs.DisplayType.monochrome,
					displayWidth, displayHeight);
			
			// initialize the opcodes dispatch engine for the new princops (mds relieved),
			// as used by available germ and boot disks
			Opcodes.initializeInstructionsPrincOpsPost40();
			Xfer.switchToNewPrincOps();
			
			// initialize the device-interface agents
			DiskState diskState = DiskAgent.addFile(bootFile, false, oldDeltasToKeep);
			if (diskState == DiskState.ReadOnly) {
				title += " [read-only]";
			} else if (diskState == DiskState.Corrupted) {
				title += " [ CORRUPTED ; don't use this disk delta ]";
			}
			NetworkAgent.setHubParameters(netHubHost, netHubPort, localTimeOffsetMinutes);
			Agents.initialize();
			
			// perform the initial microcode pre-boot actions (simulating the IOP on a 8000/6085)
			InitialMesaMicrocode.loadGerm(germFile);
			InitialMesaMicrocode.setBootRequestDisk((short)0); // boot the (first) disk
			InitialMesaMicrocode.setBootSwitches(switches);
			
			// setup BWS debugger substitute handler
			Cpu.setMPHandler(new DebuggerSubstituteMpHandler()); // TODO: make 0915 handling configurable (when network is available AND there is a chance for a remote debugger)
			
			// retrieve the mesa machine callbacks for the ui
			uiDataConsumer = Agents.getUiCallbacks();
		}
		
		// create and start the ui
		boolean logKeys = logKeyPressed;
		EventQueue.invokeLater(() -> {	
			try {	
				// setup the ui main window
				window = new MainUI(title, displayWidth, displayHeight, true, displayTypeColor); // TODO: make resizable a program/configuration parameter?
				window.getFrame().setVisible(true);
				
				// attach the mouse and keyboard handlers (java-ui => mesa engine) 
				MouseHandler mouseHandler = new MouseHandler(window, uiDataConsumer, displayWidth, displayHeight);
				window.getDisplayPane().addMouseMotionListener(mouseHandler);
				window.getDisplayPane().addMouseListener(mouseHandler);
				
				KeyboardMapper kMapper = new KeyboardMapper(uiDataConsumer, xeroxControlKeyCode, logKeys);
				if (keyboardMapFile != null) {
					kMapper.loadConfigFile(keyboardMapFile);
				} else {
					kMapper.mapDefaults_de_DE();
				}
				window.getDisplayPane().addKeyListener(new KeyHandler(kMapper));
				
				if (resetKeysOnFocusLost) {
					FocusListener focusHandler = new FocusListener() {
						@Override public void focusGained(FocusEvent e) { uiDataConsumer.resetKeys(); }
						@Override public void focusLost(FocusEvent e) { uiDataConsumer.resetKeys(); }
					};
					window.getDisplayPane().addFocusListener(focusHandler);
				}
				
				// install the ui refresher (mesa engine => java-ui)
				uiRefresher = new UiRefresher(window, uiDataConsumer);
				window.getFrame().addWindowListener(new WindowStateListener(window, uiRefresher));
				
				// start regular invocations of the ui refresher by the Swing ui thread
				uiTimer = new Timer(UiRefreshInterval, uiRefresher);
				uiTimer.start();
				
				// setup the actions for the toolbar buttons
				window.addStartAction((e) -> {
					startEngine();
				});
				
				window.addStopAction((e) -> { 
					if (isMesaEngineRunning()) { Processes.requestMesaEngineStop(); }
				});
				
				window.addInsertFloppyAction((e) -> {
					fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					if (floppyDirectory != null) {
						File floppyDir = new File(floppyDirectory);
						if (floppyDir.exists() && floppyDir.isDirectory()) {
							fileChooser.setCurrentDirectory(floppyDir);
						}
						floppyDirectory = null;
					}
					int result = fileChooser.showOpenDialog(window.getFrame());
					if (result == JFileChooser.APPROVE_OPTION) {
						try {
							File floppyFile = fileChooser.getSelectedFile();
							insertFloppy(window, floppyFile);
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(
									window.getFrame(),
									ex.getMessage(),
									"Error while inserting floppy",
									JOptionPane.OK_OPTION);
							}
					}
				});
				
				window.addEjectFloppyAction((e) -> {
					try {
						Agents.ejectFloppy();
						window.setFloppyName(null);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(
							window.getFrame(),
							ex.getMessage(),
							"Error while ejecting floppy",
							JOptionPane.OK_OPTION);
					}
				});
				
				// try to load the initially inserted floppy if configured so
				if (initialFloppy != null && initialFloppy.length() > 0) {
					try {
						insertFloppy(window, new File(initialFloppy));
					} catch (Exception ex) {
						doStartEngine = false; // prevent autostart if the initial floppy is misconfigured
						window.setStatusLine("invalid initial floppy: " + ex.getMessage());
					}
				}
				
				// notify the possibly already waiting mesa engine thread that the ui is now present
				synchronized(lock) {
					lock.notifyAll();
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		});
		
		// run the mesa engine thread
		if (!testOnly) {
			new Thread( () -> {
				try {
					// wait for the ui to get up and the request to start the mesa engine
					synchronized(lock) {
						while(!doStartEngine || window == null) {
							lock.wait();
						}
						engineIsRunning = true;
						window.setRunningState(RunningState.running);
					}
					
					// run the mesa engine until it halts by itself or by external request
					String finalMessage = Cpu.processor();
					
					// inform the user about why the mesa engine halted
					System.out.printf("\n***\n*** processor exited: %s\n***\n", finalMessage);
					uiRefresher.setEngineEndedMessage(finalMessage);
					window.setRunningState(RunningState.stopped);
					
					// shutdown the agents to save changes to the harddisk and a possibly mounted virtual floppy
					StringBuilder errMsgTarget = new StringBuilder();
					Agents.shutdown(errMsgTarget);
					if (errMsgTarget.length() > 0) {
						String errMsg = errMsgTarget.toString();
						System.out.printf("\n***\n*** Error(s) shutting down mesa engine devices: %s\n***\n", errMsg);
						JOptionPane.showMessageDialog(
								window.getFrame(),
								errMsg,
								"Error(s) shutting down mesa engine devices",
								JOptionPane.OK_OPTION);
					}
					
					// terminate Dwarf if requested so
					synchronized(lock) {
						engineIsRunning = false;
						if (doTerminate) {
							System.exit(0);
						}
					}
				} catch (InterruptedException e) {}
			}).start();
		}
	}

}