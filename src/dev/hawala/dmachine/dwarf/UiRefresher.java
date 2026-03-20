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
import java.util.function.Supplier;

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

	// last cursor shape sent by the Mesa engine, kept so it can be restored when MP returns to 8000
	private short[] lastNormalCursorBitmap = null;
	private int lastNormalCursorHotspotX = 0;
	private int lastNormalCursorHotspotY = 0;

	// current MP code and a flag indicating the cursor should be updated to show the MP code
	private int currentMp = 8000;
	private boolean mpCursorDirty = false;
	// system-millisecond deadline after which the cursor reverts to normal once MP returns to 8000
	private long mpLingerEndTime = 0;
	private static final long MP_CURSOR_LINGER_MS = 3000; // 3 s linger after MP returns to 8000
	
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
	
	// string format for the stats part of the status line, depending on the sreeen size 
	private final String statusLineFormat;
	
	// color tables
	private int[] defaultColorTable = { 0x00FFFFFF, 0x00000000 };
	private final Supplier<int[]> colorTableSupplier;
	
	/**
	 * constructor.
	 * 
	 * @param window the main window of the Dwarf application
	 * @param consumer the data consumer object provided by the mesa engine
	 */
	public UiRefresher(MainUI window, iUiDataConsumer consumer, boolean compactStatusLine) {
		// set finals
		this.mainWindow = window;
		this.mesaEngine = consumer;
		
		// register with the mesa engine
		this.mesaEngine.registerPointerBitmapAcceptor(this);
		Supplier<int[]> cltSupplier = this.mesaEngine.registerUiDataRefresher(this);
		this.colorTableSupplier = (cltSupplier != null)
				? cltSupplier
				: () -> defaultColorTable ;
		
		// choose the status line format
		this.statusLineFormat = compactStatusLine
				? "| %5d | %s | dsk [ r: %6d w: %6d ] | flp [ r: %4d w: %4d ] | net [ r: %5d s: %5d ]"
				: "| up: %5d | insns: %s | disk [ rd: %6d wr: %6d ] | floppy [ rd: %4d wr: %4d ] | network [ rcv: %5d snd: %5d ]";
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
	
	/**
	 * Start the millisecond counter for the uptime in the status line.
	 */
	public void engineStarted() {
		synchronized(this) {
			if (startMillis == 0) {
				startMillis = System.currentTimeMillis();
			}
		}
	}
	
	// callback method regularly invoked from the Java UI thread through a Swing timer
	@Override
	public void actionPerformed(ActionEvent arg) {		
		synchronized(this) {
			// repaint the screen if necessary
			if (this.doRepaint && this.doRefreshUi) {
				this.mainWindow.getDisplayPane().repaint();
				this.doRepaint = false;
			}
			
			// set the cursor: MP code cursor only when the status bar is hidden;
			// when the status bar is visible, use normal Mesa cursor handling
			if (!this.mainWindow.isStatusLineVisible()) {
				// status bar hidden: show MP code on cursor
				if (this.mpCursorDirty) {
					this.mainWindow.getDisplayPane().setMpCodeCursor(this.currentMp);
					this.mpCursorDirty = false;
					this.newCursorBitmap = null;
				} else if (this.mpLingerEndTime > 0) {
					// MP is back to 8000 but still within the linger window; once it expires
					// immediately restore the last Mesa-defined cursor.
					if (System.currentTimeMillis() >= this.mpLingerEndTime) {
						this.mpLingerEndTime = 0;
						if (this.lastNormalCursorBitmap != null) {
							this.mainWindow.getDisplayPane().setCursor(
									this.lastNormalCursorBitmap,
									this.lastNormalCursorHotspotX,
									this.lastNormalCursorHotspotY);
						}
						this.newCursorBitmap = null;
					}
				} else if (this.newCursorBitmap != null) {
					this.mainWindow.getDisplayPane().setCursor(this.newCursorBitmap, this.newCursorHotspotX, this.newCursorHotspotY);
					this.newCursorBitmap = null;
				}
			} else {
				// status bar visible: clear any pending MP cursor state and apply normal cursor
				this.mpCursorDirty = false;
				this.mpLingerEndTime = 0;
				if (this.newCursorBitmap != null) {
					this.mainWindow.getDisplayPane().setCursor(this.newCursorBitmap, this.newCursorHotspotX, this.newCursorHotspotY);
					this.newCursorBitmap = null;
				}
			}
			
			// update the status line if there is a new one
			if (this.newStatusLine != null) {
				this.mainWindow.setStatusLine(this.newStatusLine);
				this.lastStatusLine = this.newStatusLine;
				this.newStatusLine = null;
			}
			
			// if there is a stop message from the mesa engine: show in title bar when the status line is hidden, otherwise let it alternate with the last status line
			if (this.engineEndedMessage != null) {
				if (!this.mainWindow.isStatusLineVisible()) {
					// status bar hidden: append the message to the window title
					this.mainWindow.setTitleSuffix(this.engineEndedMessage);
				} else {
					// status bar visible: clear any title suffix and alternate the status line
					this.mainWindow.setTitleSuffix(null);
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
	}

	// invoked by the mesa engine when it is opportune to transfer the display memory content to Java space
	@Override
	public void accessRealMemory(short[] realMemory, int memOffset, int memWords, short[] pageFlags, int firstPage) {
		synchronized(this) {
			if (!this.doRefreshUi) { return; }
			this.doRepaint = this.mainWindow.getDisplayPane().copyDisplayContent(
					realMemory,	memOffset, memWords,
					pageFlags,	firstPage,
					this.colorTableSupplier.get());
		}
	}

	// invoked by the mesa engine when the MP code changes
	@Override
	public void acceptMP(int mp) {
		synchronized(this) {
			int prevMp = this.currentMp;
			this.currentMp = mp;
			this.statusMpPart = String.format(" %04d ", mp);
			this.newStatusLine = this.statusMpPart + this.statusStatsPart;
			if (mp != 8000) {
				// show the MP code in the cursor instead of the normal mouse pointer
				this.mpCursorDirty = true;
				this.mpLingerEndTime = 0; // cancel any pending linger
			} else if (prevMp != 8000) {
				// MP returned to normal operation: show 8000 in the cursor, then linger
				// before restoring the Mesa-defined cursor
				this.mpCursorDirty = true; // trigger one render of the 8000 cursor
				this.mpLingerEndTime = System.currentTimeMillis() + MP_CURSOR_LINGER_MS;
			}
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
					statusLineFormat,
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
			this.lastNormalCursorBitmap = bitmap;
			this.lastNormalCursorHotspotX = hotspotX;
			this.lastNormalCursorHotspotY = hotspotY;
			if (this.currentMp == 8000 && this.mpLingerEndTime == 0) {
				this.newCursorBitmap = bitmap;
				this.newCursorHotspotX = hotspotX;
				this.newCursorHotspotY = hotspotY;
			}
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