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
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

/**
 * Main UI frame for a Dwarf machine.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class MainUI {
	
	/**
	 * possible states of the mesa engine, controlling the 'enabled' states of the
	 * Start/Stop buttons.
	 */
	public enum RunningState { notRunning , running , stopped };
	
	// control if this class can startup as main program (Eclipse UI builder automatically adds an main())
	private static final boolean allowMainStartup = false;

	// the top-level frame for the UI
	private JFrame frmDwarfMesaEngine;
	
	private final String title;
	private final int displayWidth;
	private final int displayHeight;
	
	private JToolBar toolBar;
	private DisplayBwPane displayPanel;
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
	 * @param title the title text for the window
	 * @param displayWidth the pixel width of the mesa display
	 * @param displayHeight the pixel height of the mesa display
	 * @param resizable should the top level window be resizable?
	 */
	public MainUI(String title, int displayWidth, int displayHeight, boolean resizable) {
		this.title = title;
		this.displayWidth = displayWidth;
		this.displayHeight = displayHeight;
		initialize(resizable);
	}

	// Initialize the contents of the frame.
	private void initialize(boolean resizable) {
		this.frmDwarfMesaEngine = new JFrame();
		this.frmDwarfMesaEngine.setTitle("Dwarf Mesa Engine - " + this.title);
		this.frmDwarfMesaEngine.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.frmDwarfMesaEngine.getContentPane().setLayout(new BorderLayout(2, 2));
		
		this.toolBar = new JToolBar();
		this.toolBar.setFloatable(false);
		this.toolBar.setOrientation(SwingConstants.HORIZONTAL);
		this.frmDwarfMesaEngine.getContentPane().add(this.toolBar, BorderLayout.NORTH);
		
		this.btnStart = new JButton("Start");
		this.btnStart.setToolTipText("boot the mesa engine");
		this.toolBar.add(btnStart);
		
		this.btnStop = new JButton("Stop");
		this.btnStop.setToolTipText("stop the running engine and persist disk(s) modifications");
		this.toolBar.add(btnStop);
		
		this.lblSep1 = new JLabel("   Floppy: ");
		this.toolBar.add(lblSep1);
		
		this.btnInsertFloppy = new JButton("Insert");
		this.btnInsertFloppy.setToolTipText("insert a floppy disk image (*.img), possibly in read-only mode\nif the R/O checkbox is checked ");
		this.toolBar.add(btnInsertFloppy);
		
		this.ckReadOnlyFloppy = new JCheckBox("R/O");
		this.ckReadOnlyFloppy.setToolTipText("if checked: force the next floppy inserted to be read-only");
		this.toolBar.add(ckReadOnlyFloppy);
		
		this.btnEjectFloppy = new JButton("Eject");
		this.btnEjectFloppy.setToolTipText("eject the floppy currently inserted");
		this.toolBar.add(btnEjectFloppy);
		
		this.toolBar.add(new JLabel(" "));
		
		this.lblFloppyFilename = new JLabel("...");
		this.lblFloppyFilename.setFont(new Font("Dialog", Font.PLAIN, 12));
		this.toolBar.add(lblFloppyFilename);
		
		this.displayPanel = new DisplayBwPane(this.displayWidth, this.displayHeight);
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
	public DisplayBwPane getDisplayPane() { return this.displayPanel; }
	
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
	 * Change the running state for setting the 'enabled' states of the
	 * Start/Stop buttons.
	 * 
	 * @param state the new state of the mesa engine
	 */
	public void setRunningState(RunningState state) {
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
	
	/**
	 * Add an action callback to the 'Start' button.
	 * @param action callback instance.
	 */
	public void addStartAction(ActionListener action) {
		this.btnStart.addActionListener(action);
	}
	
	/**
	 * Add an action callback to the 'Stop' button.
	 * @param action callback instance.
	 */
	public void addStopAction(ActionListener action) {
		this.btnStop.addActionListener(action);
	}
	
	/**
	 * Add an action callback to the 'Insert' (floppy) button.
	 * @param action callback instance.
	 */
	public void addInsertFloppyAction(ActionListener action) {
		this.btnInsertFloppy.addActionListener(action);
	}
	
	/**
	 * Add an action callback to the 'Eject' (floppy) button.
	 * @param action callback instance.
	 */
	public void addEjectFloppyAction(ActionListener action) {
		this.btnEjectFloppy.addActionListener(action);
	}
	
	/**
	 * Get the state of the (floppy) 'R/O' checkbox.
	 * @return checked {@code true} if the 'R/O' checkbox is checked.
	 */
	public boolean writeProtectFloppy() {
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

	/*
	 * (for tests only) display the UI by launching the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					if (!allowMainStartup) { return; }
					MainUI window = new MainUI("this is a test", 1024, 640, true);
					window.frmDwarfMesaEngine.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
