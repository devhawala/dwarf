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

import static dev.hawala.dmachine.engine.iop6085.IORegion.*;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.eLevelVKey;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.TaskContextBlock;
import dev.hawala.dmachine.engine.iop6085.IORegion.IORAddress;

/**
 * IOP device handler for the keyboard and mouse of a Daybreak/6085 machine.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class HKeyboardMouse extends DeviceHandler {
	
	/*
	 * key states
	 */
	private static final int KEYBITS_WORDS = 9;
	
	private static final short ALL_KEYS_UP = (short)0xFFFF;

	private short[] uiKeys = new short[KEYBITS_WORDS];
	private boolean uiKeysChanged = false;
	
	/*
	 * Function Control Block
	 */
	
	private static final String KeyMoFCB = "KeyboardMouseFCB";
	
	private static class FCB  implements IORAddress {
		private final int startAddress;
		
		public final TaskContextBlock keyBoardAndMouseTask;
		public final Word hexValue_convertKeyCodeToBit;
		public final Word frameErrorCnt;
		public final Word overRunErrorCnt;
		public final Word parityErrorCnt;
		public final Word spuriousIntCnt;
		public final Word watchDogCnt;
		public final Word badInterruptCnt;
		public final Word mouseX;
		public final Word mouseY;
		public final Word[] kBbase = new Word[KEYBITS_WORDS];
		public final Word[] kBindex = new Word[128];
		
		private FCB() {
			this.startAddress = IORegion.syncToSegment() + IORegion.IOR_BASE;
			
			this.keyBoardAndMouseTask = new TaskContextBlock(KeyMoFCB, "displayTCB");
			this.hexValue_convertKeyCodeToBit = mkWord(KeyMoFCB, "hexValue+convertKeyCodeToBit");
			this.frameErrorCnt = mkByteSwappedWord(KeyMoFCB, "frameErrorCnt");
			this.overRunErrorCnt = mkByteSwappedWord(KeyMoFCB, "overRunErrorCnt");
			this.parityErrorCnt = mkByteSwappedWord(KeyMoFCB, "parityErrorCnt");
			this.spuriousIntCnt = mkByteSwappedWord(KeyMoFCB, "spuriousIntCnt");
			this.watchDogCnt = mkByteSwappedWord(KeyMoFCB, "watchDogCnt");
			this.badInterruptCnt = mkByteSwappedWord(KeyMoFCB, "badInterruptCnt");
			this.mouseX = mkWord(KeyMoFCB, "mouseX");
			this.mouseY = mkWord(KeyMoFCB, "mouseY");
			for (int i = 0; i < this.kBbase.length; i++) {
				this.kBbase[i] = mkWord(KeyMoFCB, "kBbase[" + i +"]");
			}
			for (int i = 0; i < this.kBindex.length; i++) {
				this.kBindex[i] = mkWord(KeyMoFCB, "kBindex[" + i +"]");
			}
			
		}

		@Override
		public String getName() {
			return KeyMoFCB;
		}

		@Override
		public int getRealAddress() {
			return this.startAddress;
		}
		
	}

	/*
	 * implementation of the iop6085 keyboard and mouse interface
	 */
	
	private final FCB fcb;
	
	public HKeyboardMouse() {
		super(KeyMoFCB, Config.IO_LOG_KEYBOARD | Config.IO_LOG_MOUSE);
		
		this.fcb = new FCB();
		
		for (int i = 0; i < KEYBITS_WORDS; i++) {
			this.fcb.kBbase[i].set(ALL_KEYS_UP);
			this.uiKeys[i] = ALL_KEYS_UP;
		}
		this.uiKeysChanged = false;
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
		// no active notifications by client
		return false;
	}

	@Override
	public void handleLockmem(short lockMask, int realAddress, MemOperation memOp, short oldValue, short newValue) {
		// no synchronization necessary with client (access to fcb fields serialized through refreshMesaMemory(), see below)
	}

	@Override
	public void handleLockqueue(int vAddr, int rAddr) {
		// not relevant for keyboard/mouse handler
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// nothing to save or shutdown
	}
	
	@Override
	public synchronized void refreshMesaMemory() {
		// transfer keyboard states from UI area to mesa memory
		if (this.uiKeysChanged) {
			for (int i = 0; i < KEYBITS_WORDS; i++) {
				this.fcb.kBbase[i].set(this.uiKeys[i]);
			}
			this.uiKeysChanged = false;
		}
	}

	public synchronized void handleKeyUsage(eLevelVKey key, boolean isPressed) {
		if (isPressed) {
			key.setPressed(this.uiKeys);
		} else {
			key.setReleased(this.uiKeys);
		}
		this.uiKeysChanged = true;
		this.logf("handleKeyUsage( key = %s, isPressed = %s )\n", key.toString(), (isPressed) ? "true" : "false");
		Processes.requestDataRefresh();
	}
	
	public synchronized void resetKeys() {
		for (int i = 0; i < KEYBITS_WORDS; i++) {
			this.uiKeys[i] = ALL_KEYS_UP;
		}
		this.uiKeysChanged = true;
		Processes.requestDataRefresh();
	}
	
	// must be called by the display device during a refreshMesaMemory() method, i.e. from the mesa processor thread!
	public void setNewCursorPosition(short x, short y) {
		this.fcb.mouseX.set(x);
		this.fcb.mouseY.set(y);
	}

}