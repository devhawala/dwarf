/*
Copyright (c) 2019, Dr. Hans-Walter Latz
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import dev.hawala.dmachine.dwarf.DebuggerSubstituteMpHandler;
import dev.hawala.dmachine.dwarf.KeyHandler;
import dev.hawala.dmachine.dwarf.KeyboardMapper;
import dev.hawala.dmachine.dwarf.MainUI;
import dev.hawala.dmachine.dwarf.MainUI.RunningState;
import dev.hawala.dmachine.dwarf.MouseHandler;
import dev.hawala.dmachine.dwarf.PropertiesExt;
import dev.hawala.dmachine.dwarf.UiRefresher;
import dev.hawala.dmachine.dwarf.WindowStateListener;
import dev.hawala.dmachine.dwarf.eKeyEventCode;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.InitialMesaMicrocode;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Opcodes;
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.Xfer;
import dev.hawala.dmachine.engine.iUiDataConsumer;
import dev.hawala.dmachine.engine.agents.NetworkInternalTimeService;
import dev.hawala.dmachine.engine.iop6085.HDisk;
import dev.hawala.dmachine.engine.iop6085.HDisk.VerifyLabelOp;
import dev.hawala.dmachine.engine.iop6085.HEthernet;
import dev.hawala.dmachine.engine.iop6085.HProcessor;
import dev.hawala.dmachine.engine.iop6085.IOP;

/**
 * Draco application main program for the 6085/daybreak architecture
 * of the Dwarf Mesa emulator, loading the configuration file and setting
 * up the mesa engine, building the main UI and running the mesa engine
 * in a separate thread driving the Dwarf UI. 
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class Draco {
	
	// the defaults for optional engine configuration
	private static final boolean DEFAULT_LARGE_DISPLAY = true;
	private static final String DEFAULT_SWITCHES = "8Wy{|}\\346\\347\\350\\377";
	private static final String DEFAULT_MAC = "00-1D-AB-EA-F4-C4";
	
	/*
	 * ui control structures
	 */
	
	// the (Java-side) intended refresh rate for the UI
	private static final int UiRefreshInterval = 20; // 20 ms => 50 wakeups => 25 display refeshes in average
	
	// the interface between the Java UI and the mesa engine
	private static UiRefresher uiRefresher;
	
	// the Swing timer allowing to run asynchronous ui updates in the swing ui thread   
	private static Timer uiTimer;
	
	// the file chooser for selecting virtual floppy disks
	private static JFileChooser fileChooser = new JFileChooser();
	
	// the main frame/window
	private static MainUI window;
	
	/*
	 * configuration
	 */
	
	private static String configFilename;
	
	private static String title = "Draco UI";
	
	private static boolean largeScreen = false;
	
	private static String diskFile = null;
	private static int oldDeltasToKeep = 5;
	private static String germFile = null;
	private static String bootSwitches = DEFAULT_SWITCHES;
	
	private static VerifyLabelOp labelOpOnRead = VerifyLabelOp.updateDisk;
	private static VerifyLabelOp labelOpOnWrite = VerifyLabelOp.updateDisk;
	private static VerifyLabelOp labelOpOnVerify = VerifyLabelOp.verify;
	private static boolean logLabelProblems = false;
	
	private static boolean stopOnNetDebug = true;
	
	private static String netbootGerm = null;
	
	private static int[] macBytes = new int[6];
	private static int[] macWords = new int[3];
	private static String recognizedMacId = "";
	
	private static String initialFloppy = null;
	private static String floppyDirectory = null;
	
	private static String netHubHost = "";
	private static int netHubPort = 3333;
	private static int localTimeOffsetMinutes = 0;
	
	private static String keyboardMapFile = null;
	private static int xeroxControlKeyCode = eKeyEventCode.VK_CONTROL.getCode();
	private static boolean resetKeysOnFocusLost = true;
	
	private static int daysBackInTime = 0;
	
	// control flags for the mesa engine
	private static boolean doStartEngine = true;
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
	
	private static VerifyLabelOp scanVerifyLabelOp(String opText, String what) {
		if (opText == null || opText.isEmpty()) {
			return VerifyLabelOp.verify;
		}
		String lcOpText = opText.toLowerCase();
		if ("verify".startsWith(lcOpText)) { return VerifyLabelOp.verify; }
		if ("updatedisk".startsWith(lcOpText)) { return VerifyLabelOp.updateDisk; }
		if ("noverify".startsWith(lcOpText)) { return VerifyLabelOp.noVerify; }
		System.out.printf("Warning: invalid value '%s' for %s, using 'verify'\n", opText, what);
		return VerifyLabelOp.verify;
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
		
		diskFile = props.getString("boot", null);
		if (!Utils.isFileOk("boot", diskFile)) { return false; }
		oldDeltasToKeep = props.getInt("oldDeltasToKeep", oldDeltasToKeep);
		
		germFile = props.getString("fallbackGerm", null);
		if (germFile != null && !Utils.isFileOk("germ", germFile)) { return false; }
		bootSwitches = props.getString("switches", DEFAULT_SWITCHES);
		doStartEngine = props.getBoolean("autostart", doStartEngine);
		
		labelOpOnRead = scanVerifyLabelOp(props.getString("labelOpOnRead"), "option labelOpOnRead");
		labelOpOnWrite = scanVerifyLabelOp(props.getString("labelOpOnWrite"), "option labelOpOnWrite");
		labelOpOnVerify = scanVerifyLabelOp(props.getString("labelOpOnVerify"), "option labelOpOnVerify");
		logLabelProblems = props.getBoolean("logLabelProblems", logLabelProblems);
		
		stopOnNetDebug = props.getBoolean("stopOnNetDebug", stopOnNetDebug);
		
		netbootGerm = props.getString("netbootGerm", netbootGerm);
		
		title = props.getString("title", diskFile);
		largeScreen = props.getBoolean("largeScreen", DEFAULT_LARGE_DISPLAY);
		
		initialFloppy = props.getString("initialFloppy", initialFloppy);
		floppyDirectory = props.getString("floppyDirectory", floppyDirectory);
		
		netHubHost = props.getString("netHubHost", netHubHost);
		netHubPort = props.getInt("netHubPort", netHubPort);
		localTimeOffsetMinutes = props.getInt("localTimeOffsetMinutes", localTimeOffsetMinutes);
		
		daysBackInTime = props.getInt("daysBackInTime", daysBackInTime);
		
		keyboardMapFile = props.getString("keyboardMapFile", keyboardMapFile);
		String ctrlKeyCode = props.getString("xeroxControlKeyCode", null);
		if (ctrlKeyCode != null && ctrlKeyCode.length() > 0) {
			xeroxControlKeyCode = Utils.parseKeycode(ctrlKeyCode);
		}
		resetKeysOnFocusLost = props.getBoolean("resetKeysOnFocusLost", resetKeysOnFocusLost);
		
		String mac = props.getString("processorId", null);
		if (mac == null) {
			System.out.printf("Warning: no processor id specified, using default\n");
			mac = DEFAULT_MAC;
		}
		if ((recognizedMacId = Utils.parseMac(mac, macBytes, macWords)).isEmpty()) {
			System.out.printf("Warning: invalid processor id specified, using default\n");
			recognizedMacId = Utils.parseMac(DEFAULT_MAC, macBytes, macWords);
		}
		
		String xdeNoBlinkWorkAround = props.getString("xdeNoBlinkWorkAround", null);
		if (xdeNoBlinkWorkAround != null && !xdeNoBlinkWorkAround.isEmpty()) {
			String[] parts = xdeNoBlinkWorkAround.split(":");
			
			String htsndPart = parts[0].trim();
			long xdeNoBlinkInstrCnt = 0;
			try {
				xdeNoBlinkInstrCnt = Integer.parseInt(htsndPart) * 100_000L;
			} catch (NumberFormatException nfe) {
				System.out.printf("Warning: invalid xdeNoBlinkWorkAround/100tsnd-instructions specified, work-around for XDE not activated\n");
			}
			
			if (parts.length > 1 && xdeNoBlinkInstrCnt > 0) {
				String datePart = parts[1].trim();
				try {
					LocalDate xdeTargetDate = LocalDate.parse(datePart);
					HProcessor.installXdeNoBlinkWorkAround(xdeTargetDate, xdeNoBlinkInstrCnt);
				} catch (DateTimeParseException dtpe) {
					System.out.printf("Warning: invalid xdeNoBlinkWorkAround/target-date, work-around for XDE not activated\n");
				}
			}
		}
		
		return true;
	}
	
	// load/mount a virtual floppy into the mesa engine (the corresponding device handler)
	// and adjust the ui accordingly
	private static void insertFloppy(MainUI window, File floppyFile) throws IOException {
		boolean writeProtected = window.writeProtectFloppy();
		boolean isReadonly = IOP.insertFloppy(floppyFile, writeProtected);
		
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
		System.out.printf(" fallbackGerm: %s\n", germFile);
		System.out.printf(" switches    : %s\n", bootSwitches);
		System.out.printf(" boot file   : %s\n", diskFile);
		System.out.printf(" deltas limit: %d\n", oldDeltasToKeep);
		System.out.printf(" display     : %s\n", largeScreen ? "large - 1152 x 861 (19\")" : "small - 832 x 633 (15\")");
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
		System.out.printf(" daysBack    : %d\n", daysBackInTime);
	}
	
	// load the germ file from the rigid disk
	// (simple version, able to load only a germ stored as contiguous sequence of pages)
	
	private static final short PVSeal = (short)0xA28A;
	
	private static boolean scanDiskForGerm(int diskIdx, List<short[]> germSectors) {
		short[] label = new short[10];
		short[] data = new short[256];
		
		// get physical volume root page
		HDisk.rawRead(diskIdx, 0, label, data);
		if (data[0] != PVSeal) {
			System.out.printf("** invalid PVSeal 0x%04X on Pilot disk\n", data[0]);
			return false;
		}
		
		// get germ start location
		int cyl = data[0x21] & 0xFFFF;
		int head = data[0x22] >>> 8;
		int sector = data[0x22] & 0x00FF;
		// System.out.printf("++ germ at: cyl = 0x%04X , head = 0x%02X , sector = 0x%02X\n", cyl, head, sector);
		if (cyl == 0 && head == 0 && sector == 0) {
			System.out.println("** no germ file found on Pilot disk");
			return false;
		}
		
		// get germ sectors
		int absSector = HDisk.getAbsSectNo(diskIdx, cyl, head, sector);
		int reqGermPageNo = (data[0x20] << 16) | (data[0x1F] & 0xFFFF);
		HDisk.rawRead(diskIdx, absSector++, label, data);
		while (reqGermPageNo <= 96) { // a germ can have at most 96 pages (95 code + 1 GFT page)
				
			short[] germSector = new short[256];
			System.arraycopy(data, 0, germSector, 0, germSector.length);
			germSectors.add(germSector);
			
			if (label[8] == (short)0xFFFF || label[9] == (short)0xFFFF) {
				break; // last sector of pilot boot file
			}
			
			reqGermPageNo++;
			HDisk.rawRead(diskIdx, absSector++, label, data);
			// System.out.printf(
			// 	"++ candidate germ page %2d :: label: 0x %04X %04X %04X %04X %04X %04X %04X %04X %04X %04X\n",
			//	reqGermPageNo, label[0], label[1], label[2], label[3], label[4], label[5], label[6], label[7], label[8], label[9]);
		}
		if (label[8] != (short)0xFFFF || label[9] != (short)0xFFFF) {
			// System.out.println("** not yet supported feature: discontiguous germ file on disk");
			System.out.println("** did not find plausible germ file on disk (max. 96 pages)");
			return false;
		}
		
		// done
		return true;
	}
	
	// checks if the given 16 words (must be the first words of the first sector) are a plausible GFT
	// (Global Frame Table) for a germ (i.e. all global frames must be in first memory bank)
	private static boolean isPostPrincOps4dot0Germ(short[] first16words) {
		return first16words[0] == 0 && first16words[1] == 0 // first entry is reserved and GF *must* be 0
			&& first16words[3] == 0     // CB is in first memory bank
			&& first16words[5] == 0     // GF is in first memory bank
			&& first16words[7] == 0     // CB is in first memory bank
			&& first16words[9] == 0     // GF is in first memory bank
			&& first16words[0x0B] == 0  // CB is in first memory bank
			&& first16words[0x0D] == 0  // GF is in first memory bank
			&& first16words[0x0F] == 0; // CB is in first memory bank
	}
	
	private static boolean isPostPrincOps4dot0Germ(String germFilename) {
		if (germFilename == null || germFilename.isEmpty()) { return false; }
		try (FileInputStream fis = new FileInputStream(germFilename)) {
			short[] first16words = new short[16];
			for (int i = 0; i < first16words.length; i++) {
				int b1 = fis.read();
				if (b1 < 0) { return false; }
				int b2 = fis.read();
				if (b2 < 0) { return false; }
				first16words[i] = (short)( (b1 << 8) | (b2 & 0x00FF) );
			}
			return isPostPrincOps4dot0Germ(first16words);
		} catch (IOException e) {
			return false;
		}
	}

	public static void main(String[] args) throws IOException {

		boolean logKeyPressed = false;
		boolean doMerge = false;
		boolean doNetboot = false;
		long bootFileNumber = 0;
		String cfgFile = null;
		
		// command line parameters pass 1: check for test only OR run configuration
		for (String arg : args) {
			if (!arg.startsWith("-")) {
				if (cfgFile == null) {
					cfgFile = arg;
				} else {
					System.out.printf("Warning: ignoring unknown argument: %s\n", arg);
				}
			}
		}

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
				} else if ("-netexec".equalsIgnoreCase(arg)) {
					doNetboot = true;
					bootFileNumber = InitialMesaMicrocode.BFN_Daybreak_SimpleNetExec;
				} else if ("-netinstall".equalsIgnoreCase(arg)) {
					doNetboot = true;
					bootFileNumber = InitialMesaMicrocode.BFN_Daybreak_Installer;
				} else {
					System.out.printf("Warning: ignoring unknown command line argument: %s\n", arg);
				}
			}
		}
		if (dumpConfig) {
			dumpConfiguration();
		}
		
		// merge disks if requested, doing nothing else afterwards
		if (doMerge) {
			StringBuilder sb = new StringBuilder();
			if (!HDisk.addFile(diskFile, false, 32, sb)) {
				System.out.printf("## error loading harddisk: %s\n", sb.toString());
				return;
			}
			PrintStream ps = System.out;
			HDisk.mergeDisks(ps);
			return;
		}
		
		// adjust absolute date
		long timeShiftSeconds = -86400L * daysBackInTime;
		HProcessor.setTimeShiftSeconds(timeShiftSeconds);
		NetworkInternalTimeService.setTimeShiftSeconds(timeShiftSeconds);

		// setup the mesa machine to finally get the callbacks for the ui to the mesa machine
		iUiDataConsumer uiDataConsumer;

		// setup the mesa machine
		{
			// set processor id (aka MAC address) and network access data
			Cpu.setPID(macWords[0], macWords[1], macWords[2]);
			HEthernet.setHubParameters(netHubHost, netHubPort, localTimeOffsetMinutes);
			
			// initialize the memory subsystem with the display-size as configured (large: 1152x861, small: 832x633)
			Mem.initializeMemoryDaybreak(largeScreen);
			
			// initialize the harddisk
			StringBuilder sb = new StringBuilder();
			if (!HDisk.addFile(diskFile, false, oldDeltasToKeep, sb)) {
				System.out.printf("## error initializing harddisk: %s\n", sb.toString());
				return;
			}
			
			// get the germ to use
			List<short[]> germContent = new ArrayList<>();
			boolean havingGerm = !doNetboot && scanDiskForGerm(0, germContent);
			
			if (doNetboot) {
				havingGerm = HEthernet.loadFileFromBootService(
											macWords[0], macWords[1], macWords[2],
											InitialMesaMicrocode.BFN_Daybreak_Germ,
											germContent);
				if (!havingGerm) {
					germFile = netbootGerm;
				}
			}
			if (!havingGerm && germFile == null) {
				System.out.println("** ERROR: unable to boot: no (usable) germ found on disk / bootservice and no fallback germ file specified");
				return;
			} else if (havingGerm) {
				if (dumpConfig) {
					System.out.printf("Booting with germ from %s\n", doNetboot ? "bootservice" : "pilot disk");
				}
			} else if ((new File(germFile)).canRead()) {
				if (dumpConfig) {
					System.out.printf("Booting with specified %s germ file: %s\n",
						doNetboot ? "netbootGerm" : "fallbackGerm",
						germFile);
				}
			} else {
				System.out.println("## ERROR: germ file not readable");
				return;
			}
			
			// check which PrincOps flavor we have this time
			boolean post40PrincOps 
						= (havingGerm)
						? isPostPrincOps4dot0Germ(germContent.get(0))
						: isPostPrincOps4dot0Germ(germFile);
			if (dumpConfig) {
				System.out.printf("Germ has %sPrincOps 4.0 flavor\n", post40PrincOps ? "post-" : "");
			}
			
			// initialize the opcodes dispatch engine
			if (post40PrincOps) {
				Opcodes.initializeInstructionsPrincOpsPost40();
				Xfer.switchToNewPrincOps();
			} else {
				Opcodes.initializeInstructionsPrincOps40();
			}
			
			// initialize the 6085 IOP, allocating the static device handler structures in the IORegion
			IOP.initialize(labelOpOnRead, labelOpOnWrite, labelOpOnVerify, logLabelProblems);
			// (debug) IORegion.dumpIORegionStructure(Mem.IORegion_Virtual_StartPage * 256);
			
			// prepare booting the machine (setup germ, boot source, boot switches)
			if (havingGerm) {
				InitialMesaMicrocode.loadGerm(germContent, post40PrincOps);
			} else if (post40PrincOps) {
				InitialMesaMicrocode.loadGerm(germFile, true);
			} else {
				InitialMesaMicrocode.loadGerm(germFile, false);
			}
			if (doNetboot) {
				InitialMesaMicrocode.setBootRequestEthernet((short)0, bootFileNumber);
			} else {
				InitialMesaMicrocode.setBootRequestDisk((short)0);
			}
			InitialMesaMicrocode.setBootSwitches(bootSwitches);
			
			// setup BWS debugger substitute handler
			Cpu.setMPHandler(new DebuggerSubstituteMpHandler(stopOnNetDebug));
			
			// retrieve the mesa machine callbacks for the ui
			uiDataConsumer = IOP.getUiCallbacks();
		}
		
		// create and start the ui
		boolean logKeys = logKeyPressed;
		EventQueue.invokeLater(() -> {	
			try {	
				// setup the ui main window
				int displayWidth = Mem.displayPixelWidth;
				int displayHeight = Mem.displayPixelHeight;
				window = new MainUI("Dwarf / Draco 6085", title, displayWidth, displayHeight, true, false); // TODO: make resizable a program/configuration parameter?
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
				uiRefresher = new UiRefresher(window, uiDataConsumer, !largeScreen);
				window.getFrame().addWindowListener(new WindowStateListener(window, uiRefresher, Draco::isMesaEngineRunning, Draco::terminateOnEngineStopped));
				
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
						IOP.ejectFloppy();
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
		new Thread( () -> {
			try {
				// wait for the ui to get up and the request to start the mesa engine
				synchronized(lock) {
					while(!doStartEngine || window == null) {
						lock.wait();
					}
					engineIsRunning = true;
					if (uiRefresher != null) { uiRefresher.engineStarted(); }
					window.setRunningState(RunningState.running);
				}
				
				// run the mesa engine until it halts by itself or by external request
				String finalMessage = Cpu.processor();
				
				// inform the user about why the mesa engine halted
				System.out.printf("\n***\n*** processor exited: %s\n***\n", finalMessage);
				uiRefresher.setEngineEndedMessage(finalMessage);
				window.setRunningState(RunningState.stopped);
				
				// shutdown the devices, mainly saving changes to the harddisk and a possibly mounted virtual floppy
				StringBuilder errMsgTarget = new StringBuilder();
				IOP.shutdown(errMsgTarget);
				if (errMsgTarget.length() > 0) {
					String errMsg = errMsgTarget.toString();
					System.out.printf("\n***\n*** Error(s) shutting down mesa engine devices: %s\n***\n", errMsg);
					JOptionPane.showMessageDialog(
							window.getFrame(),
							errMsg,
							"Error(s) shutting down mesa engine devices",
							JOptionPane.OK_OPTION);
				}
				
				// terminate Draco if requested so
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