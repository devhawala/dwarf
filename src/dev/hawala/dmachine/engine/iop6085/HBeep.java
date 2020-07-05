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
import dev.hawala.dmachine.engine.iop6085.IOPTypes.IOPCondition;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.NotifyMask;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.TaskContextBlock;
import dev.hawala.dmachine.engine.iop6085.IORegion.IORAddress;

/**
 * IOP device handler for the beeper (dummy, no sounds provided for now).
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class HBeep extends DeviceHandler {
	
	private static final String BeepFCB = "BeepFCB";
	
	private static class FCB implements IORAddress {
		private final int startAddress;
		
		public final TaskContextBlock beepTask;
		public final IOPCondition beepCndt;
		public final NotifyMask beepMask;
		public final Word frequency;
		
		private FCB() {
			this.startAddress = IORegion.syncToSegment() + IORegion.IOR_BASE;
			
			this.beepTask = new TaskContextBlock(BeepFCB, "beepTask");
			this.beepCndt = new IOPCondition(BeepFCB, "beepCndt");
			this.beepMask = new NotifyMask(BeepFCB, "beepMask");
			this.frequency = mkByteSwappedWord(BeepFCB, "frequency");
			
			this.beepMask.byteMaskAndOffset.set(mkMask());
		}


		@Override
		public String getName() {
			return BeepFCB;
		}


		@Override
		public int getRealAddress() {
			return this.startAddress;
		}
	}
	
	/*
	 * implementation of the iop6085 beep device handler
	 */
	
	private final FCB fcb;
	
	public HBeep() {
		super(BeepFCB, Config.IO_LOG_DISPLAY);
		this.fcb = new FCB();
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
		// check if it's for us
		if (notifyMask != this.fcb.beepMask.byteMaskAndOffset.get()) {
			return false;
		}
		
		// simulate beeping on / off
		this.logf("processNotify -> frequency = %d (units??)\n", this.fcb.frequency.get() & 0xFFFF);
			// megaHertz: LONG CARDINAL <- 2764800;
			// fcb.frequency <- ByteSwap[Inline.LowHalf[Inline.LongDiv[megaHertz, MAX[frequency, 43]]]]
		
		// done
		return true;
	}

	@Override
	public void handleLockmem(short lockMask, int realAddress, MemOperation memOp, short oldValue, short newValue) {
		// not relevant
	}

	@Override
	public void handleLockqueue(int vAddr, int rAddr) {
		// not relevant for beep handler
	}
	
	@Override
	public synchronized void refreshMesaMemory() {
		// not relevant
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// nothing to save or shutdown
	}

}