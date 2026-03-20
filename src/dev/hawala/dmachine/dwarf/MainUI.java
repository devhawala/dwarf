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

package dev.hawala.dmachine.dwarf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import dev.hawala.dmachine.Utils;

/**
 * Main UI frame for a Dwarf machine.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class MainUI {

	static {
		// Must be set before any AWT/Swing components are created to take effect on macOS.
		if (Utils.IS_MACOS) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
	}

	/**
	 * possible states of the mesa engine, controlling the 'enabled' states of the
	 * Start/Stop buttons.
	 */
	public enum RunningState { notRunning , running , stopped };
	
	// control if this class can startup as main program (Eclipse UI builder automatically adds an main())
	private static final boolean allowMainStartup = false;

	// the top-level frame for the UI
	private JFrame frmDwarfMesaEngine;
	
	private final String emulatorName;
	private final String title;
	private final int displayWidth;
	private final int displayHeight;
	
	// macOS: menu-bar controls (null on non-macOS)
	private JMenuBar menuBar;
	private JMenuItem miStart;
	private JMenuItem miStop;
	private JMenuItem miInsertFloppy;
	private JCheckBoxMenuItem ckmiReadOnlyFloppy;
	private JMenuItem miEjectFloppy;
	private JMenuItem miFloppyFilename;

	// non-macOS: toolbar controls (null on macOS)
	private JToolBar toolBar;
	private DisplayPane displayPanel;
	private JLabel statusLine;
	private JCheckBox ckReadOnlyFloppy;
	private JButton btnStart;
	private JButton btnStop;
	private JLabel lblSep1;
	private JButton btnInsertFloppy;
	private JButton btnEjectFloppy;
	private JLabel lblFloppyFilename;

	/**
	 * Create the application.
	 * 
	 * @param emulatorName the name of the emulator running in the UI
	 * @param title the title text for the window
	 * @param displayWidth the pixel width of the mesa display
	 * @param displayHeight the pixel height of the mesa display
	 * @param resizable should the top level window be resizable?
	 * @param colorDisplay is this a color (8-bit color lookup table) display machine?
	 * @param runInFullscreen let it be a fullscreen application?
	 */
	public MainUI(String emulatorName, String title, int displayWidth, int displayHeight, boolean resizable, boolean colorDisplay, boolean runInFullscreen) {
		this.emulatorName = emulatorName;
		this.title = title;
		this.displayWidth = displayWidth;
		this.displayHeight = displayHeight;
		initialize(emulatorName, resizable, colorDisplay, runInFullscreen);
	}

	// Initialize the contents of the frame.
	private void initialize(String emulatorName, boolean resizable, boolean colorDisplay, boolean runInFullscreen) {
		this.frmDwarfMesaEngine = new JFrame();
		this.frmDwarfMesaEngine.setTitle(emulatorName + " Mesa Engine - " + this.title);
		this.frmDwarfMesaEngine.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.frmDwarfMesaEngine.getContentPane().setLayout(new BorderLayout(2, 2));
		
		if (runInFullscreen) {
			this.frmDwarfMesaEngine.setUndecorated(true);
			this.frmDwarfMesaEngine.setResizable(false);
			GraphicsEnvironment graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice device = graphics.getDefaultScreenDevice();
			if (device.isFullScreenSupported()) {
				device.setFullScreenWindow(this.frmDwarfMesaEngine);
				resizable = false;
			}
		}
		
		if (Utils.IS_MACOS) {
			// macOS: use a native menu bar instead of a toolbar
			this.menuBar = new JMenuBar();
			this.frmDwarfMesaEngine.setJMenuBar(this.menuBar);
			
			// Engine menu
			JMenu menuEngine = new JMenu("Engine");
			this.menuBar.add(menuEngine);
			
			this.miStart = new JMenuItem("Start");
			this.miStart.setToolTipText("boot the mesa engine");
			menuEngine.add(this.miStart);
			
			this.miStop = new JMenuItem("Stop");
			this.miStop.setToolTipText("stop the running engine and persist disk(s) modifications");
			menuEngine.add(this.miStop);
			
			// Floppy menu
			JMenu menuFloppy = new JMenu("Floppy");
			this.menuBar.add(menuFloppy);
			
			this.miInsertFloppy = new JMenuItem("Insert Floppy...");
			this.miInsertFloppy.setToolTipText("insert a floppy disk image (*.img), possibly in read-only mode if R/O is checked");
			menuFloppy.add(this.miInsertFloppy);
			
			this.ckmiReadOnlyFloppy = new JCheckBoxMenuItem("Read-Only");
			this.ckmiReadOnlyFloppy.setToolTipText("if checked: force the next floppy inserted to be read-only");
			menuFloppy.add(this.ckmiReadOnlyFloppy);
			
			this.miEjectFloppy = new JMenuItem("Eject Floppy");
			this.miEjectFloppy.setToolTipText("eject the floppy currently inserted");
			menuFloppy.add(this.miEjectFloppy);
			
			menuFloppy.addSeparator();
			
			this.miFloppyFilename = new JMenuItem("\u2011"); //macOS doesn't seem to like "-" in menu bar item names
			this.miFloppyFilename.setEnabled(false);
			menuFloppy.add(this.miFloppyFilename);
		} else {
			// Windows / Linux: use the original toolbar
			this.toolBar = new JToolBar();
			this.toolBar.setFloatable(false);
			this.toolBar.setOrientation(SwingConstants.HORIZONTAL);
			this.frmDwarfMesaEngine.getContentPane().add(this.toolBar, BorderLayout.NORTH);
			
			this.btnStart = new JButton("Start");
			this.btnStart.setToolTipText("boot the mesa engine");
			this.toolBar.add(this.btnStart);
			
			this.btnStop = new JButton("Stop");
			this.btnStop.setToolTipText("stop the running engine and persist disk(s) modifications");
			this.toolBar.add(this.btnStop);
			
			this.lblSep1 = new JLabel("   Floppy: ");
			this.toolBar.add(this.lblSep1);
			
			this.btnInsertFloppy = new JButton("Insert");
			this.btnInsertFloppy.setToolTipText("insert a floppy disk image (*.img), possibly in read-only mode\nif the R/O checkbox is checked ");
			this.toolBar.add(this.btnInsertFloppy);
			
			this.ckReadOnlyFloppy = new JCheckBox("R/O");
			this.ckReadOnlyFloppy.setToolTipText("if checked: force the next floppy inserted to be read-only");
			this.toolBar.add(this.ckReadOnlyFloppy);
			
			this.btnEjectFloppy = new JButton("Eject");
			this.btnEjectFloppy.setToolTipText("eject the floppy currently inserted");
			this.toolBar.add(this.btnEjectFloppy);
			
			this.toolBar.add(new JLabel(" "));
			
			this.lblFloppyFilename = new JLabel("...");
			this.lblFloppyFilename.setFont(new Font("Dialog", Font.PLAIN, 12));
			this.toolBar.add(this.lblFloppyFilename);
		}
		
		this.displayPanel = (colorDisplay)
				? new Display8BitColorPane(this.displayWidth, this.displayHeight)
				: new DisplayMonochromePane(this.displayWidth, this.displayHeight);
		this.displayPanel.setBackground(Color.WHITE);
		Dimension dims = new Dimension(this.displayWidth, this.displayHeight);
		this.displayPanel.setMinimumSize(dims);
		this.displayPanel.setMaximumSize(dims);
		this.displayPanel.setPreferredSize(dims);
		this.frmDwarfMesaEngine.getContentPane().add(this.displayPanel, BorderLayout.CENTER);
		
		this.statusLine = new JLabel(" Mesa Engine not running");
		this.statusLine.setFont(new Font("Monospaced", Font.BOLD, 12));
		this.frmDwarfMesaEngine.getContentPane().add(this.statusLine, BorderLayout.SOUTH);

		this.setRunningState(RunningState.notRunning);
		this.setFloppyName(null);
		
		this.frmDwarfMesaEngine.pack();
		this.frmDwarfMesaEngine.setResizable(resizable);
	}
	
	/**
	 * @return the top-level frame of the Dwarf UI
	 */
	public JFrame getFrame() { return this.frmDwarfMesaEngine; }
	
	/**
	 * @return the pane showing the mesa display 
	 */
	public DisplayPane getDisplayPane() { return this.displayPanel; }
	
	/**
	 * Set the text displayed in the status area
	 * 
	 * @param line the new status line content
	 */
	public void setStatusLine(String line) {
		if (this.statusLine == null) { return; }
		this.statusLine.setText(line);
	}
	
	/**
	 * Check if the status line is currently visible
	 * 
	 * @return {@code true} if the status line is currently visible.
	 */
	public boolean isStatusLineVisible() {
		return this.statusLine != null && this.statusLine.isVisible();
	}

	/**
	 * Set the visibility of the status line.
	 *
	 * @param visible {@code true} to show the status line, {@code false} to hide it.
	 */
	public void setStatusLineVisible(boolean visible) {
		if (this.statusLine == null) { return; }
		this.statusLine.setVisible(visible);
		this.frmDwarfMesaEngine.pack();
	}
	
	/**
	 * Update the window title, optionally appending an engine message if available.
	 * Used when the status line is invisible.
	 * When {@code suffix} is non-null the title becomes
	 * {@code "<emulator> Mesa Engine - <title> - <message>"};
	 * when {@code null} it reverts to the base title.
	 *
	 * @param suffix the message to append, or {@code null} to clear it.
	 */
	public void setTitleSuffix(String suffix) {
		String baseTitle = this.emulatorName + " Mesa Engine - " + this.title;
		String trimmed = (suffix != null) ? suffix.trim() : "";
		this.frmDwarfMesaEngine.setTitle(
				!trimmed.isEmpty() ? baseTitle + " - " + trimmed : baseTitle);
	}
	
	/**
	 * Change the running state for setting the 'enabled' states of the
	 * Start/Stop buttons.
	 * 
	 * @param state the new state of the mesa engine
	 */
	public void setRunningState(RunningState state) {
		if (Utils.IS_MACOS) {
			switch(state) {
			case notRunning:
				this.miStart.setEnabled(true);
				this.miStop.setEnabled(false);
				return;
			case running:
				this.miStart.setEnabled(false);
				this.miStop.setEnabled(true);
				return;
			case stopped:
				this.miStart.setEnabled(false);
				this.miStop.setEnabled(false);
				return;
			}
		} else {
			switch(state) {
			case notRunning:
				this.btnStart.setEnabled(true);
				this.btnStop.setEnabled(false);
				return;
			case running:
				this.btnStart.setEnabled(false);
				this.btnStop.setEnabled(true);
				return;
			case stopped:
				this.btnStart.setEnabled(false);
				this.btnStop.setEnabled(false);
				return;
			}
		}
	}
	
	/**
	 * Add an action callback to the 'Start' control.
	 * @param action callback instance.
	 */
	public void addStartAction(ActionListener action) {
		if (Utils.IS_MACOS) { this.miStart.addActionListener(action); }
		else { this.btnStart.addActionListener(action); }
	}
	
	/**
	 * Add an action callback to the 'Stop' control.
	 * @param action callback instance.
	 */
	public void addStopAction(ActionListener action) {
		if (Utils.IS_MACOS) { this.miStop.addActionListener(action); }
		else { this.btnStop.addActionListener(action); }
	}
	
	/**
	 * Add an action callback to the 'Insert' (floppy) control.
	 * @param action callback instance.
	 */
	public void addInsertFloppyAction(ActionListener action) {
		if (Utils.IS_MACOS) { this.miInsertFloppy.addActionListener(action); }
		else { this.btnInsertFloppy.addActionListener(action); }
	}
	
	/**
	 * Add an action callback to the 'Eject' (floppy) control.
	 * @param action callback instance.
	 */
	public void addEjectFloppyAction(ActionListener action) {
		if (Utils.IS_MACOS) { this.miEjectFloppy.addActionListener(action); }
		else { this.btnEjectFloppy.addActionListener(action); }
	}
	
	/**
	 * Get the state of the (floppy) 'R/O' control.
	 * @return {@code true} if the 'R/O' control is checked/selected.
	 */
	public boolean writeProtectFloppy() {
		if (Utils.IS_MACOS) { return this.ckmiReadOnlyFloppy.isSelected(); }
		return this.ckReadOnlyFloppy.isSelected();
	}
	
	/**
	 * Set the name of the inserted floppy, also controlling the 'enabled' state
	 * of the Insert/Eject buttons.
	 * @param floppyName the (file)name of the floppy; passing {@code null} or an
	 *   empty string will enable the 'Insert' and disable the 'Eject' button,
	 *   passing a non-empty floppy name will invert these states.
	 */
	public void setFloppyName(String floppyName) {
		if (Utils.IS_MACOS) {
			if (floppyName == null || floppyName.length() == 0) {
				this.miFloppyFilename.setText("\u2011"); //macOS doesn't seem to like "-" in menu bar item names
				this.miInsertFloppy.setEnabled(true);
				this.ckmiReadOnlyFloppy.setEnabled(true);
				this.miEjectFloppy.setEnabled(false);
			} else {
				this.miFloppyFilename.setText(floppyName);
				this.miInsertFloppy.setEnabled(false);
				this.ckmiReadOnlyFloppy.setEnabled(false);
				this.miEjectFloppy.setEnabled(true);
			}
		} else {
			if (floppyName == null || floppyName.length() == 0) {
				this.lblFloppyFilename.setText("-");
				this.btnInsertFloppy.setEnabled(true);
				this.ckReadOnlyFloppy.setEnabled(true);
				this.btnEjectFloppy.setEnabled(false);
			} else {
				this.lblFloppyFilename.setText(floppyName);
				this.btnInsertFloppy.setEnabled(false);
				this.ckReadOnlyFloppy.setEnabled(false);
				this.btnEjectFloppy.setEnabled(true);
			}
		}
	}
	
	/**
	 * Determine if fullscreen is supported and if so the available size for the
	 * the Mesa engine display.
	 * 
	 * @return the rectangle having the available size for die Mesa display or {@code null}
	 *      if fullscreen is not supported or possible.
	 */
	public static Rectangle getFullscreenUsableDims() {
		
		// build a dummy UI with the same vertical layout as the real one
		JFrame frame = new JFrame("Fullscreen");
		frame.getContentPane().setLayout(new BorderLayout(2, 2));
		
		if (Utils.IS_MACOS) {
			JMenuBar menuBar = new JMenuBar();
			frame.setJMenuBar(menuBar);
			JMenu menuEngine = new JMenu("Engine");
			menuBar.add(menuEngine);
			JMenuItem miStop = new JMenuItem("Stop");
			miStop.addActionListener(e -> { frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)); System.exit(0); });
			menuEngine.add(miStop);
		} else {
			JToolBar toolBar = new JToolBar();
			toolBar.setFloatable(false);
			toolBar.setOrientation(SwingConstants.HORIZONTAL);
			frame.getContentPane().add(toolBar, BorderLayout.NORTH);
			
			JButton btnStart = new JButton("Start");
			btnStart.setToolTipText("boot the mesa engine");
			toolBar.add(btnStart);
			
			JButton btnStop = new JButton("Stop");
			btnStop.setToolTipText("stop the running engine and persist disk(s) modifications");
			toolBar.add(btnStop);
			btnStop.addActionListener(e -> { frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)); System.exit(0); });
		}
		
		JLabel label = new JLabel("", JLabel.CENTER);
		label.setText("This is not yet in fullscreen mode!");
		label.setOpaque(true);
		frame.getContentPane().add(label, BorderLayout.CENTER);
		
		JLabel statusLine = new JLabel(" This is a dummy status line");
		statusLine.setFont(new Font("Monospaced", Font.BOLD, 12));
		frame.getContentPane().add(statusLine, BorderLayout.SOUTH);
		
		// try to display the dummy UI in fullscreen mode to get the max. size of the Mesa machine display
		Rectangle innerRectangle = null;
		frame.setUndecorated(true);
		frame.setResizable(false);
		GraphicsEnvironment graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = graphics.getDefaultScreenDevice();
		if (device.isFullScreenSupported()) {
			device.setFullScreenWindow(frame); // switch to fullscreen
			innerRectangle = label.getBounds();// the the net display region size
			device.setFullScreenWindow(null);  // leave fullscreen mode
		}
		
		// remove the dummy UI from the display
		frame.setVisible(false);
		frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		try { Thread.sleep(200); } catch (InterruptedException e1) { }
		
		// done
		return innerRectangle;
	}

	/*
	 * (for tests only) display the UI by launching the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					if (!allowMainStartup) { return; }
				MainUI window = new MainUI("Test", "this is a test", 1024, 640, true, false, false);
					window.frmDwarfMesaEngine.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}