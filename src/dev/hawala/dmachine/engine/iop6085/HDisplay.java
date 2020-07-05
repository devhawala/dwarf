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

package dev.hawala.dmachine.engine.iop6085;

import static dev.hawala.dmachine.engine.iop6085.IORegion.byteSwap;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkByteSwappedWord;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkField;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkWord;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.iUiDataConsumer;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.ClientCondition;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.TaskContextBlock;
import dev.hawala.dmachine.engine.iop6085.IORegion.Field;
import dev.hawala.dmachine.engine.iop6085.IORegion.IORAddress;
import dev.hawala.dmachine.engine.iop6085.IORegion.Word;

/**
 * IOP device handler for the display of a Daybreak/6085 machine.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class HDisplay extends DeviceHandler {
	
	/*
	 * state data of the ui components 
	 */
	
	// keyboard/mouse device handler for forwarding mouse movements
	// (handling mouse positions need the hotspot position of the cursor
	// image, which is known here)
	private final HKeyboardMouse keyMoHandler;
	
	// mouse shape bits taken from the FCB until transferred to the UI
	private final short[] mesaCursor = new short[16];
	
	// the current mouse position as transmitted to the mesa engine
	private int mesaCurrX;
	private int mesaCurrY;
	
	// the hotspot position in the mouse bitmap
	private int mouseHotspotX = 0;
	private int mouseHotspotY = 0;
	
	// the mouse pointer bitmap if setting a new mouse pointer shape is pending
	private short[] newCursorBitmap = null;
	
	// the mouse position coming from the ui (accessing these must be synchronized(
	private int uiCurrX = 0; // last position passed to the mesa machine
	private int uiCurrY = 0;
	private int uiNextX = 0; // new position from the ui to be passed to the mesa engine 
	private int uiNextY = 0;
	private boolean mouseMoved = false; // is a new mouse position waiting to be transferred to the mesa machine?
	
	
	/*
	 * Function Control Block
	 */
	
	private static final String DisplayFCB = "DisplayFCB";
	
	private static final short cursorPosChangedMask = (short)byteSwap((short)0x8000);
	private static final short cursorMapChangedMask = (short)byteSwap((short)0x4000);
	private static final short borderPatChangedMask = (short)byteSwap((short)0x2000);
	private static final short backGrndChangedMask  = (short)byteSwap((short)0x1000);
	private static final short displInfoChangedMask = (short)byteSwap((short)0x0800);
	private static final short pictureBorderPatChangedMask = (short)byteSwap((short)0x2800);
	private static final short alignmentChangedMask = (short)byteSwap((short)0x0400);
	private static final short allInfoChangedMask = (short)byteSwap((short)0xFC00);
	private static final short invInfoChangedMask = (short)byteSwap((short)0x03FF);
	
	private static final short headAllInfoChangedMask = (short)byteSwap((short)0xF800); // used by head for TurnOn (missing the flag 'alignmentChanged')
	
	private static class FCB implements IORAddress {
		private final int startAddress;
		
		public final TaskContextBlock displayTCB;
		public final Word displayLock;
		public final Word chngdInfo;
		public final ClientCondition vertRetraceEvent;
		public final Word cursorXCoord;
		public final Word cursorYCoord;
		public final Word border;
		/**/public final Field borderOddpairs;
		/**/public final Field borderEvenpairs;
		public final Word[] cursorPattern = new Word[16];
		public final Word displCntl;
		/**/public final Field displCntl_dataCursor;
		/**/public final Field displCntl_picture;
		/**/public final Field displCntl_mixRule;
		// begin CRTConfig crtConfig
		public final Word crtConfig_numberBitsPerLine;
		public final Word crtConfig_numberDisplayLines;
		public final Word crtConfig_configInfo;
		public final Word crtConfig_colorParams;
		public final Word crtConfig_xCoordOffset;
		public final Word crtConfig_yCoordOffset;
		public final Word crtConfig_pixelsRefresh;
		// end CRTConfig crtConfig
		// begin "Info in IO region that Mesa does not use"
		public final Word bitMapOrg;
		public final Word numberQuadWords;
		public final Word verticalCounts;
		public final Word horizontalcounts;
		public final Word displayIntrCnts;
		public final Word displayWdtCntsAndcursorUser;
		public final Word displayHWInitProc;
		public final Word cursorPositionProc;
		public final Word cursorPatternProc;
		public final Word borderPatternProc;
		public final Word backgndProc;
		public final Word commandProc;
		// end "Info in IO region that Mesa does not use"
		
		public final Field crtConfig_pixels;
		public final Field crtConfig_refresh;
		
		private FCB() {
			this.startAddress = IORegion.syncToSegment() + IORegion.IOR_BASE;
			
			this.displayTCB = new TaskContextBlock(DisplayFCB, "displayTCB");
			this.displayLock = mkWord(DisplayFCB, "displayLock");
			this.chngdInfo = mkWord(DisplayFCB, "chngdInfo");
			this.vertRetraceEvent = new ClientCondition(DisplayFCB, "vertRetraceEvent");
			this.cursorXCoord = mkByteSwappedWord(DisplayFCB, "cursorXCoord");
			this.cursorYCoord = mkByteSwappedWord(DisplayFCB, "cursorYCoord");
			this.border = mkWord(DisplayFCB, "border");
			/**/this.borderOddpairs = mkField("borderOddpairs", this.border, 0xFF00);
			/**/this.borderEvenpairs = mkField("borderEvenpairs", this.border, 0x00FF);
			for (int i = 0; i < this.cursorPattern.length; i++) {
				this.cursorPattern[i] = mkWord(DisplayFCB, "border[" + i + "]");
			}
			this.displCntl = mkWord(DisplayFCB, "displCntl");
			/**/this.displCntl_dataCursor = mkField("dataCursor", this.displCntl, 0xF000);
			/**/this.displCntl_picture = mkField("picture", this.displCntl, 0x0800);
			/**/this.displCntl_mixRule = mkField("mixRule", this.displCntl, 0x00F00);
			
			this.crtConfig_numberBitsPerLine = mkByteSwappedWord(DisplayFCB, "crtConfig.numberBitsPerLine");
			this.crtConfig_numberDisplayLines = mkByteSwappedWord(DisplayFCB, "crtConfig.numberDisplayLines");
			this.crtConfig_configInfo = mkWord(DisplayFCB, "crtConfig.configInfo");
			this.crtConfig_colorParams = mkByteSwappedWord(DisplayFCB, "crtConfig.colorParams");
			this.crtConfig_xCoordOffset = mkByteSwappedWord(DisplayFCB, "crtConfig.xCoordOffset");
			this.crtConfig_yCoordOffset = mkByteSwappedWord(DisplayFCB, "crtConfig.yCoordOffset");
			this.crtConfig_pixelsRefresh = mkWord(DisplayFCB, "crtConfig.pixelsRefresh");
			/**/this.crtConfig_pixels = mkField("pixels", this.crtConfig_pixelsRefresh, 0xFF00);
			/**/this.crtConfig_refresh = mkField("refresh", this.crtConfig_pixelsRefresh, 0x00FF);
			
			this.bitMapOrg = mkWord(DisplayFCB, "bitMapOrg");
			this.numberQuadWords = mkWord(DisplayFCB, "numberQuadWords");
			this.verticalCounts = mkWord(DisplayFCB, "verticalCounts");
			this.horizontalcounts = mkWord(DisplayFCB, "horizontalcounts");
			this.displayIntrCnts = mkWord(DisplayFCB, "displayIntrCnts");
			this.displayWdtCntsAndcursorUser = mkWord(DisplayFCB, "displayWdtCntsAndcursorUser");
			this.displayHWInitProc = mkWord(DisplayFCB, "displayHWInitProc");
			this.cursorPositionProc = mkWord(DisplayFCB, "cursorPositionProc");
			this.cursorPatternProc = mkWord(DisplayFCB, "cursorPatternProc");
			this.borderPatternProc = mkWord(DisplayFCB, "borderPatternProc");
			this.backgndProc = mkWord(DisplayFCB, "backgndProc");
			this.commandProc = mkWord(DisplayFCB, "commandProc");
			
			this.displayLock.set(mkMask());
		}

		@Override
		public String getName() {
			return DisplayFCB;
		}

		@Override
		public int getRealAddress() {
			return this.startAddress;
		}
		
	}
	
	/*
	 * implementation of the iop6085 display interface
	 */
	
	private final FCB fcb;
	
	public HDisplay(HKeyboardMouse keyMoHandler) {
		super(DisplayFCB, Config.IO_LOG_DISPLAY);
		this.keyMoHandler = keyMoHandler;
		
		// allocate FCB data
		this.fcb = new FCB();
		
		// fill display configuration values (handler -> head)
		this.fcb.crtConfig_numberBitsPerLine.set((short)Mem.displayPixelWidth);
		this.fcb.crtConfig_numberDisplayLines.set((short)Mem.displayPixelHeight);
		this.fcb.crtConfig_configInfo.set((short)0);  // Daybreak (b7=0), interlaced (b6=0)
		this.fcb.crtConfig_colorParams.set((short)0); // non-color (b15=0)
		this.fcb.crtConfig_pixels.set(80);            // 80 pixels/inch
		this.fcb.crtConfig_refresh.set(60);           // 60 refreshs/second
		
		// run the vertical retrace notifier
		this.startVertRetraceNotifier();
	}

	@Override
	public int getFcbRealAddress() {
		return this.fcb.getRealAddress();
	}

	@Override
	public short getFcbSegment() {
		return this.fcb.getIOPSegment();
	}

	@Override
	public boolean processNotify(short notifyMask) {
		// not relevant, the display device is triggered by LOCKMEM only
		return false;
	}

	@Override
	public void handleLockmem(short lockMask, int realAddress, MemOperation memOp, short oldValue, short newValue) {
		// check if it's for us
		if (lockMask != this.fcb.displayLock.get()) { return; }
		
		// check if it is a valid command
		short changedInfo = newValue; // this.fcb.chngdInfo.get();
		if ((changedInfo & invInfoChangedMask) != 0) {
			throw new IllegalArgumentException(String.format(
					"Unplausible chngdInfo for HDisplay.fcb: 0x%04X (wrong byte-swap?)", changedInfo));
		}
		this.slogf("\n");
		this.logf("IOP::HDisplay.handleLockmem(rAddr = 0x%06X , memOp = %s , oldValue = 0x%04X , newValue = 0x%04X) -> changedInfo = 0x%04X\n",
				realAddress, memOp.toString(), oldValue, newValue, changedInfo & 0xFFFF);
		
		// check for change details
		if ((changedInfo & allInfoChangedMask) == 0) {
			return; // nothing changed...?
		}
		
		if (changedInfo == headAllInfoChangedMask) {
			// display was "turned on", do specific action for this special case
			this.logf(" => ***************** display turned ON *****************\n");
			this.allowVertRetraceIntr(this.fcb.vertRetraceEvent.maskValue.get());
			// ... scan VMMap for location of real display memory in VM
			boolean found = Mem.locateRealDisplayMemoryInVMMap();
			this.logf("   => locateRealDisplayMemoryInVMMap() -> found = %s\n", Boolean.toString(found));
			return;
		}
		
		if (changedInfo == cursorMapChangedMask) {
			this.logf(" => cursorMapChanged\n");
			logf("    +---------------------------------+\n");
			for (int i = 0; i < this.mesaCursor.length; i++) {
				short line = this.fcb.cursorPattern[i].get();
				this.mesaCursor[i] = line;
				logf("    | %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s %s |\n",
					((line & 0x8000) != 0) ? "x" : " ",
					((line & 0x4000) != 0) ? "x" : " ",
					((line & 0x2000) != 0) ? "x" : " ",
					((line & 0x1000) != 0) ? "x" : " ",
					((line & 0x0800) != 0) ? "x" : " ",
					((line & 0x0400) != 0) ? "x" : " ",
					((line & 0x0200) != 0) ? "x" : " ",
					((line & 0x0100) != 0) ? "x" : " ",
					((line & 0x0080) != 0) ? "x" : " ",
					((line & 0x0040) != 0) ? "x" : " ",
					((line & 0x0020) != 0) ? "x" : " ",
					((line & 0x0010) != 0) ? "x" : " ",
					((line & 0x0008) != 0) ? "x" : " ",
					((line & 0x0004) != 0) ? "x" : " ",
					((line & 0x0002) != 0) ? "x" : " ",
					((line & 0x0001) != 0) ? "x" : " "
					);
			}
			logf("    +---------------------------------+\n");
			
			// save the new shape until transferred to the UI
			this.newCursorBitmap = this.mesaCursor;
		} else if ((changedInfo & cursorMapChangedMask) != 0) {
			this.logf(" => cursorMapChanged-> ignored (as not single change)\n");
		}
		
		if (changedInfo == cursorPosChangedMask) {
			short newX = this.fcb.cursorXCoord.get();
			short newY = this.fcb.cursorYCoord.get();
			
			if (this.newCursorBitmap != null) {
				// compute hotspot position and register the new mouse shape with the ui
				logf(" => cursorPosChanged : setPosition with newCursorBitmap :: newX(%d) , newY(%d) ; mesaCurrX = %d , mesaCurrY = %d ; uiCurrX = %d , uiCurrY = %d\n",
						newX, newY, this.mesaCurrX, this.mesaCurrY, this.uiCurrX, this.uiCurrY);
				
				int deltaHotspotX = this.mesaCurrX - newX;
				int deltaHotspotY = this.mesaCurrY - newY;
				this.mouseHotspotX = Math.max(0,  Math.min(15, this.mouseHotspotX + deltaHotspotX));
				this.mouseHotspotY = Math.max(0,  Math.min(15, this.mouseHotspotY + deltaHotspotY));
				logf("  => deltaHotspotX = %d , deltaHotspotY = %d  ==> newHotspotX = %d , newHotspotY = %d\n",
						deltaHotspotX, deltaHotspotY, this.mouseHotspotX, this.mouseHotspotY);
				if (this.uiPointerBitmapAcceptor != null) {
					uiPointerBitmapAcceptor.setPointerBitmap(this.newCursorBitmap, this.mouseHotspotX, this.mouseHotspotY);
					this.logf("  => new cursor registered in UI\n");
				}
				this.newCursorBitmap = null;

				this.logf("  => uiCurrX = %d , uiCurrY = %d\n", this.uiCurrX, this.uiCurrY);
				this.mesaCurrX = this.uiCurrX - this.mouseHotspotX;
				this.mesaCurrY = this.uiCurrY - this.mouseHotspotY;
				this.logf("  => new mesaCurrX = %d , mesaCurrY = %d\n", this.mesaCurrX, this.mesaCurrY);
			} else {
				this.logf(" => cursorPosChanged : new position [ %d , %d ]  ( mesaCurrX = %d , mesaCurrY = %d ; uiCurrX = %d , uiCurrY = %d )\n",
						newX, newY, this.mesaCurrX, this.mesaCurrY, this.uiCurrX, this.uiCurrY);
				// TODO: reposition the "real" mouse pointer?
				// TODO: what to do with the new coordinates?
			}
		} else if ((changedInfo & cursorPosChangedMask) != 0) {
			this.logf(" => cursorPosChanged -> ignored (as not single change)\n");
		}
		
		if ((changedInfo & borderPatChangedMask) != 0) {
			this.logf(" => borderPatChangedMask (ignored)\n");
		}
		
		if ((changedInfo & backGrndChangedMask) != 0) {
			this.logf(" => backGrndChangedMask (ignored)\n");
		}
		
		if ((changedInfo & displInfoChangedMask) != 0) {
			// displayInfoChange *alone* <==> TurnOff
			this.logf(" => ***************** display turned OFF *****************\n");
			this.disallowVertRetraceIntr();
		}
		
		if ((changedInfo & pictureBorderPatChangedMask) != 0) {
			this.logf(" => pictureBorderPatChanged (ignored)\n");
		}
		
		if ((changedInfo & alignmentChangedMask) != 0) {
			this.logf(" => alignmentChanged (ignored)\n");
		}
	}
	
	@Override
	public void cleanupAfterLockmem(short lockMask, int realAddress) {
		if (lockMask != this.fcb.displayLock.get()) { return; }
		this.fcb.chngdInfo.set((short)0);
	}

	@Override
	public void handleLockqueue(int vAddr, int rAddr) {
		// not relevant for display handler
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		this.disallowVertRetraceIntr();
	}
	
	@Override
	public synchronized void refreshMesaMemory() {
		// transfer mouse position to mesa memory
		if (this.mouseMoved) {
			this.uiCurrX = this.uiNextX;
			this.uiCurrY = this.uiNextY;
			this.mesaCurrX = this.uiCurrX - this.mouseHotspotX;
			this.mesaCurrY = this.uiCurrY - this.mouseHotspotY;
			
//			this.logf("refreshMesaMemory( uiX = %d , uiY = %d ) => mesaX = %d , mesaY = %d) -> keyMoHandler.setNewCursorPosition()\n",
//					this.uiCurrX, this.uiCurrY, this.mesaCurrX, this.mesaCurrY);
			
			this.keyMoHandler.setNewCursorPosition((short)this.mesaCurrX, (short)this.mesaCurrY);
			this.mouseMoved = false;
		}
	}
	
	public synchronized void recordMouseMoved(int toX, int toY) {
		this.uiNextX = toX;
		this.uiNextY = toY;
		this.mouseMoved = true;
		Processes.requestDataRefresh();
		//this.logf("recordMouseMoved( toX = %d, toY = %d )\n", this.uiNextX, this.uiNextY);
	}
	
	/*
	 * ui interface
	 */
	
	private iUiDataConsumer.PointerBitmapAcceptor uiPointerBitmapAcceptor = null;
			
	public void setPointerBitmapAcceptor(iUiDataConsumer.PointerBitmapAcceptor acceptor) {
		this.uiPointerBitmapAcceptor = acceptor; 
	}
	
	/*
	 * vertical retrace interrupts
	 * (these interrupts are crucial as they trigger events in Pilot,
	 * like checking for mouse movements and keyboard changes)
	 * (this is probably the "stimulus" thread described somewhere in the programming manuals)
	 */
	
	private boolean doVertRetraceInterrupts = false;     // actively trigger interrupts?
	private short vertRetraceIntrMask = 0;               // interrupt mask to trigger
	private final Object vertRetraceLock = new Object(); // synch-lock for accessing the above state
	
	private void allowVertRetraceIntr(short mask) {
		synchronized(this.vertRetraceLock) {
			this.doVertRetraceInterrupts = true;
			this.vertRetraceIntrMask = mask;
			this.logf(" => started vertical retrace interrupts with mask: 0x%04X\n", this.vertRetraceIntrMask);
		}
	}
	
	private synchronized void disallowVertRetraceIntr() {
		synchronized(this.vertRetraceLock) {
			this.doVertRetraceInterrupts = false;
			this.vertRetraceIntrMask = 0;
		}
	}
	
	private Thread vertRetraceThread = null;
	private final Runnable verticalRetraceNotifier = () -> {
		try {
			while(true) {
				Thread.sleep(25); // 25 ms => 40 interrupts/second
				synchronized(this.vertRetraceLock) {
					if (this.doVertRetraceInterrupts) {
						Processes.requestMesaInterrupt(this.vertRetraceIntrMask);
					}
				}
			}
		} catch (InterruptedException e) {
			// nothing to do: interrupted means no more retrace events
		}
	};
	
	private void startVertRetraceNotifier() {
		this.vertRetraceThread = new Thread(this.verticalRetraceNotifier);
		this.vertRetraceThread.setDaemon(true);
		this.vertRetraceThread.start();
	}
	
}