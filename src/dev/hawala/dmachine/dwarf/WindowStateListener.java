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

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.function.BooleanSupplier;

import javax.swing.JOptionPane;

import dev.hawala.dmachine.Duchess;
import dev.hawala.dmachine.engine.Processes;

/**
 * State listener for the Mesa-Emulator main window.
 * <p>
 * The main purpose is to handle closing the main window while
 * the mesa engine is running: a confirmation dialog is showed and
 * if closing is confirmed, the mesa engine is halted gracefully
 * by shutting down the devices, which saves buffered data of
 * the harddisk resp. the currently loaded floppy disk.
 * </p>
 * <p>
 * Additionally refreshing of the Java UI is paused if the application
 * is iconized.
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class WindowStateListener implements WindowListener {
	
	private final MainUI mainWindow;
	private final UiRefresher uiRefresher;
	private final BooleanSupplier runningIndicator;
	private final Runnable windowCloser;
	
	/**
	 * Constructor.
	 * 
	 * @param mainWindow the main window of the Dwarf application
	 * @param uiRefresher the ui refresher handling the Java ui updates
	 * @param runningIndicator function indicating if the emulator engine is currently running
	 * @param windowCloser function for allowing to close the main window when the engine finally stops
	 */
	public WindowStateListener(MainUI mainWindow, UiRefresher uiRefresher, BooleanSupplier runningIndicator, Runnable windowCloser) {
		this.mainWindow = mainWindow;
		this.uiRefresher = uiRefresher;
		this.runningIndicator = runningIndicator;
		this.windowCloser = windowCloser;
	}

	@Override
	public void windowOpened(WindowEvent e) { }

	@Override
	public void windowClosing(WindowEvent e) {
		// check if the mesa engine is running and handle accordingly
		if (!this.runningIndicator.getAsBoolean()) {
			// simply terminate the program
			System.exit(0);
		}
		
		// the mesa engine is running, so ask what to do
		int result = JOptionPane.showConfirmDialog(
						this.mainWindow.getFrame(),
						"OK to close window and shutdown the running mesa engine?",
						"Dwarf: Mesa Engine is currently running",
						JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION) {
			// halt the engine, let Agent states be saved and let the program be stopped
			Processes.requestMesaEngineStop(); // this stops the engine and lets the mesa engine thread shut down the agents
			this.windowCloser.run(); // this closes the main window when the mesa engine thread is done
		}
	}

	@Override
	public void windowClosed(WindowEvent e) { }

	@Override
	public void windowIconified(WindowEvent e) {
		this.uiRefresher.setDoRefreshUi(false);
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		this.uiRefresher.setDoRefreshUi(true);
	}

	@Override
	public void windowActivated(WindowEvent e) { }

	@Override
	public void windowDeactivated(WindowEvent e) { }
	
}