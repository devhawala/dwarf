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

import static dev.hawala.dmachine.engine.iop6085.IORegion.mkByteSwappedWord;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkIOPBoolean;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkWord;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.ClientCondition;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.IOPCondition;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.NotifyMask;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.TaskContextBlock;
import dev.hawala.dmachine.engine.iop6085.IORegion.IOPBoolean;
import dev.hawala.dmachine.engine.iop6085.IORegion.IORAddress;
import dev.hawala.dmachine.engine.iop6085.IORegion.Word;

/**
 * IOP device handler for the unsupported TTY device of a Daybreak/6085 machine.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class HTTY extends DeviceHandler {
	
	/*
	 * Function Control Block
	 */
	
	private static final String TTYFCB = "TTYFCB";
	
	private static class WorkListType {
		private final IOPBoolean writeBaudRate;
		
		private WorkListType(String name) {
			this.writeBaudRate = mkIOPBoolean(name, "writeBaudRate");
		}
	}
	
	private static class FCB implements IORAddress {
		private final int startAddress;
		
		public final TaskContextBlock txTcb;
		public final TaskContextBlock specRxTcb;
		public final TaskContextBlock rxTaskChBTcb;
		
		public final Word ttyLockMask;
		public final NotifyMask ttyWorkMask;
		public final ClientCondition ttyClientCondition;
		public final IOPCondition ttyWorkCondition;
		
		public final Word txBuffer; // CHARACTER
		public final Word rxBuffer; // CHARACTER
		
		public final Word ttyWorkList; // MACHINE DEPENDENT with 16 BOOLEANs
		
		public final Word ttyBaudRate;
		public final Word wr1_wr3;
		public final Word wr4_wr5;
		
		public final Word iopSystemInputPort_rr0;
		public final Word rr1_rr2;
		
		public final Word ttyStatusWord;
		
		public final Word eepromImage_type; // MACHINE DEPENDENT {none(0), DCE(2), (LAST [CARDINAL])}
		public final Word eepromImage_attributes1;
		public final Word eepromImage_attributes2;
		public final Word eepromImage_attributes3;
		public final Word eepromImage_attributes4;
		
		private FCB() {
			this.startAddress = IORegion.syncToSegment() + IORegion.IOR_BASE;
			
			this.txTcb = new TaskContextBlock(TTYFCB, "txTcb");
			this.specRxTcb = new TaskContextBlock(TTYFCB, "specRxTcb");
			this.rxTaskChBTcb = new TaskContextBlock(TTYFCB, "rxTaskChBTcb");
			
			this.ttyLockMask = mkWord(TTYFCB, "ttyLockMask");
			this.ttyWorkMask = new NotifyMask(TTYFCB, "ttyWorkMask");
			this.ttyClientCondition = new ClientCondition(TTYFCB, "ttyClientCondition");
			this.ttyWorkCondition = new IOPCondition(TTYFCB, "ttyWorkCondition");
			
			this.txBuffer = mkWord(TTYFCB, "txBuffer");
			this.rxBuffer = mkWord(TTYFCB, "rxBuffer");
			
			this.ttyWorkList = mkWord(TTYFCB, "ttyWorkList");
			
			this.ttyBaudRate = mkByteSwappedWord(TTYFCB, "ttyBaudRate");
			this.wr1_wr3 = mkWord(TTYFCB, "wr1+wr3");
			this.wr4_wr5 = mkWord(TTYFCB, "wr4+wr5");
			
			this.iopSystemInputPort_rr0 = mkWord(TTYFCB, "iopSystemInputPort+rr0");
			this.rr1_rr2 = mkWord(TTYFCB, "rr1+rr2");
			
			this.ttyStatusWord = mkWord(TTYFCB, "ttyStatusWord");
			
			this.eepromImage_type = mkWord(TTYFCB, "eepromImage.type");
			this.eepromImage_attributes1 = mkWord(TTYFCB, "eepromImage_attributes[1]");
			this.eepromImage_attributes2 = mkWord(TTYFCB, "eepromImage_attributes[2]");
			this.eepromImage_attributes3 = mkWord(TTYFCB, "eepromImage_attributes[3]");
			this.eepromImage_attributes4 = mkWord(TTYFCB, "eepromImage_attributes[4]");
			
			// initialize notification mask
			this.ttyWorkMask.byteMaskAndOffset.set(mkMask());
		}

		@Override
		public String getName() {
			return TTYFCB;
		}

		@Override
		public int getRealAddress() {
			return this.startAddress;
		}
		
	}
	
	/*
	 * implementation of the iop6085 tty interface
	 */
	
	private final FCB fcb;
	
	public HTTY() {
		super(TTYFCB, Config.IO_LOG_TTY);
		
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
		if (notifyMask != this.fcb.ttyWorkMask.byteMaskAndOffset.get()) {
			return false;
		}
		
		this.logf("IOP::HTTY.processNotify() - unimplemented yet ...\n");
		
		return true;
	}

	@Override
	public void handleLockmem(short lockMask, int realAddress, MemOperation memOp, short oldValue, short newValue) {
		// TTY device currently unsupported/unused
	}

	@Override
	public void handleLockqueue(int vAddr, int rAddr) {
		// TTY device currently unsupported/unused
	}

	@Override
	public void refreshMesaMemory() {
		// TTY device currently unsupported/unused
	}

	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// TTY device currently unsupported/unused
	}

}