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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import dev.hawala.dmachine.Dwarf;
import dev.hawala.dmachine.engine.iMesaMachineDataAccessor;
import dev.hawala.dmachine.engine.iUiDataConsumer;
import dev.hawala.dmachine.engine.iUiDataConsumer.PointerBitmapAcceptor;

/**
 * Interface object between the mesa engine and the Dwarf UI: this class
 * implements the callbacks used by the mesa engine to propagate UI
 * changes from the mesa side to the Java side of Dwarf.
 * <p>
 * On the one side, the mesa engine delivers ui relevant changes (display
 * content, new mouse shapes, statistical data, new MP codes) through the
 * callbacks methods provided here. For this, the {@code DwarfUiRefresher}
 * registers itself on construction with the mesa engine through the
 * {@code iUiDataConsumer} provided by the mesa engine.
 * <br>
 * The different data provided by the mesa engine are buffered here or in the
 * backing store of the display.
 * </p>
 * <p>
 * On the other side, the {@code DwarfUiRefresher} is registered with the
 * Java Swing machinery (more precisely a Swing timer) for regular refresh of
 * the Swing components presenting the data buffered from the mesa engine.   
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class UiRefresher implements ActionListener, iMesaMachineDataAccessor, PointerBitmapAcceptor {
	
	// the participants in the data transfer
	private final MainUI mainWindow;
	private final iUiDataConsumer mesaEngine;
	
	// the system milliseconds value when the mesa engine started to work
	private long startMillis = 0;
	
	// is regular refreshing the (Java) useful (e.g. not if the Dwarf application is iconized)? 
	private boolean doRefreshUi = true;
	
	// the pending next mouse shape to use (these will be reset if the new mouse shape was set in Java)
	private short[] newCursorBitmap = null;
	private int newCursorHotspotX = 0;
	private int newCursorHotspotY = 0;
	
	// the pending next status line to set on the Java ui (reset to null when set in the ui) 
	private String newStatusLine = null;
	
	// was the display content modified in the backing store and must therefore painted to the Java window?  
	private boolean doRepaint = false;
	
	// handling for the stop message of the mesa engine, which will alternate with the last statistics line  
	private String engineEndedMessage = null; // will be set when the mesa engine stopped running
	private String lastStatusLine = null; // the content of the status line before the engine stopped
	private long lastStatusLineSwitch = 0; // system milliseconds of the last content switch n the status line
	private boolean statusLineIsEndedMessage = false; // true if currently/now display the stop message
	private static long STATUS_SWITCH_INTERVAL = 2000; // 2 seconds between status line alternations 
	
	// the 2 parts currently making up the status line (allowing to construct the line if one part changes)
	private String statusMpPart = " 0000 ";
	private String statusStatsPart = "no statistics available yet";
	
	/**
	 * constructor.
	 * 
	 * @param window the main window of the Dwarf application
	 * @param consumer the data consumer object provided by the mesa engine
	 */
	public UiRefresher(MainUI window, iUiDataConsumer consumer) {
		// set finals
		this.mainWindow = window;
		this.mesaEngine = consumer;
		
		// register with the mesa engine
		this.mesaEngine.registerPointerBitmapAcceptor(this);
		this.mesaEngine.registerUiDataRefresher(this);
	}
	
	/**
	 * Set the flag activating ui refreshes.
	 * 
	 * @param doRefreshing the new refresh indication flag, should be given
	 *   as {@code false} when the Dwarf UI is not displayed (e.g. the application
	 *   is iconized), to reduce resource consumption (mainly CPU) when refreshing
	 *   is not necessary.
	 */
	public void setDoRefreshUi(boolean doRefreshing) {
		synchronized(this) {
			this.doRefreshUi = doRefreshing;
		}
	}
	
	// callback method regularly invoked from the Java UI thread through a Swing timer
	@Override
	public void actionPerformed(ActionEvent arg) {		
		synchronized(this) {
			// check if the mesa engine started in the meantime
			if (startMillis == 0 && Dwarf.isMesaEngineRunning()) {
				startMillis = System.currentTimeMillis();
			}
			
			// repaint the screen if necessary
			if (this.doRepaint && this.doRefreshUi) {
				this.mainWindow.getDisplayPane().repaint();
				this.doRepaint = false;
			}
			
			// set the new cursor if a new one was given
			if (this.newCursorBitmap != null) {
				this.mainWindow.getDisplayPane().setCursor(this.newCursorBitmap, this.newCursorHotspotX, this.newCursorHotspotY);
				this.newCursorBitmap = null;
			}
			
			// update the status line if there is a new one
			if (this.newStatusLine != null) {
				this.mainWindow.setStatusLine(this.newStatusLine);
				this.lastStatusLine = this.newStatusLine;
				this.newStatusLine = null;
			}
			
			// if there is a stop message from the mesa engine: let it alternate with the last status line
			if (this.engineEndedMessage != null) {
				long now = System.currentTimeMillis();
				if ((now - this.lastStatusLineSwitch) > STATUS_SWITCH_INTERVAL) {
					if (this.statusLineIsEndedMessage) {
						this.mainWindow.setStatusLine(this.lastStatusLine);
					} else {
						this.mainWindow.setStatusLine(this.engineEndedMessage);
					}
					this.statusLineIsEndedMessage = !this.statusLineIsEndedMessage;
					this.lastStatusLineSwitch = now;
				}
			}
		}
	}

	// invoked by the mesa engine when it is opportune to transfer the display memory content to Java space
	@Override
	public void accessRealMemory(short[] realMemory, int memOffset, int memWords, short[] pageFlags, int firstPage) {
		synchronized(this) {
			if (!this.doRefreshUi) { return; }
			this.doRepaint = this.mainWindow.getDisplayPane().copyDisplayContent(
					realMemory,	memOffset, memWords,
					pageFlags,	firstPage);
		}
	}

	// invoked by the mesa engine when the MP code changes
	@Override
	public void acceptMP(int mp) {
		synchronized(this) {
			this.statusMpPart = String.format(" %04d ", mp);
			this.newStatusLine = this.statusMpPart + this.statusStatsPart;
		}	
	}

	// invoked by the mesa engine at more or less regular intervals
	@Override
	public void acceptStatistics(
					long counterInstructions,
					int counterDiskReads,
					int counterDiskWrites,
					int counterFloppyReads,
					int counterFloppyWrites,
					int counterNetworkPacketsReceived,
					int counterNetworkPacketsSent) {
		
		long units = counterInstructions % 1000L;
		long thousands = (counterInstructions / 1000L) % 1000L;
		long millions = (counterInstructions / 1000000L) % 1000L;
		long billions = counterInstructions / 1000000000L;
		String cntString = String.format("%s%s%s%s%s%s%s",
				(billions == 0) ? "   " : String.format("%3d", billions),
				(billions == 0) ? " " : ".",
				(millions == 0) ? "   " : String.format((billions == 0) ? "%3d" : "%03d", millions),
				(millions == 0) ? " " : ".",
				(thousands == 0) ? "   " : String.format((millions == 0) ? "%3d" : "%03d", thousands),
				(thousands == 0) ? " " : ".",
				String.format("%03d", units)
				);
		
		synchronized(this) {
			long upSeconds = (System.currentTimeMillis() - this.startMillis) / 1000;
			
			this.statusStatsPart = String.format(
					"| up: %5d | insns: %s | disk [ rd: %6d wr: %6d ] | floppy [ rd: %4d wr: %4d ] | network [ rcv: %5d snd: %5d ]",
					upSeconds,
					cntString,
					counterDiskReads,
					counterDiskWrites,
					counterFloppyReads,
					counterFloppyWrites,
					counterNetworkPacketsReceived,
					counterNetworkPacketsSent
					);
			
			this.newStatusLine = this.statusMpPart + this.statusStatsPart;
			
		}
	}

	// invoked by the mesa engine when a new mouse shape and hotspot coordinate become available
	@Override
	public void setPointerBitmap(short[] bitmap, int hotspotX, int hotspotY) {
		synchronized(this) {
			this.newCursorBitmap = bitmap;
			this.newCursorHotspotX = hotspotX;
			this.newCursorHotspotY = hotspotY;
		}
	}
	
	/**
	 * Set the reason message for the stopped mesa engine, this text line
	 * will alternate with the last status line (with MP code and statistics).
	 * 
	 * @param msg the stop reason message provided by the mesa engine.
	 */
	public void setEngineEndedMessage(String msg) {
		synchronized(this) {
			this.engineEndedMessage = (msg == null || msg.startsWith(" ")) ? msg : " " + msg; // indent it by one blank
		}
	}
}