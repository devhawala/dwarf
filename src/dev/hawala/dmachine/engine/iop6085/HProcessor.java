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
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkCompoundDblWord;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkDblWord;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkField;
import static dev.hawala.dmachine.engine.iop6085.IORegion.mkWord;

import java.time.LocalDate;
import java.util.Date;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.IOPCondition;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.NotifyMask;
import dev.hawala.dmachine.engine.iop6085.IOPTypes.TaskContextBlock;
import dev.hawala.dmachine.engine.iop6085.IORegion.DblWord;
import dev.hawala.dmachine.engine.iop6085.IORegion.Field;
import dev.hawala.dmachine.engine.iop6085.IORegion.IORAddress;
import dev.hawala.dmachine.engine.iop6085.IORegion.Word;

/**
 * IOP device handler for the processor of a Daybreak/6085 machine for
 * accessing (mostly reading) characteristics of the machine.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class HProcessor extends DeviceHandler {
	
	private static long timeShiftMilliSeconds = 0;
	
	public static void setTimeShiftSeconds(long seconds) {
		timeShiftMilliSeconds = seconds * 1000;
	}
	
	private static final String ProcessorFCB = "ProcessorFCB";

	private enum Command {
		noCommand (0),
		readGMT (1),
		writeGMT (2),
	    readHostID (3),
	    readVMMapDesc (4),
	    readRealMemDesc (5),
	    readDisplayDesc (6),
	    readKeyboardType (7),
	    readPCType (8),
	    bootButton (9),
	    readNumbCSBanks (10),
	    readMachineType(11),
	    
	    invalid(0xFFFF);
	    
	    public final int code;
		private Command(int c) { this.code = c; }
		
		public static Command map(int code) {
			if (code < Command.noCommand.code || code > Command.readMachineType.code) {
				return invalid;
			}
			Command res = Command.values()[code];
			if (res.code != code) {
				throw new IllegalStateException("ERROR mapping command code to HProcessor.Command for code: "  +code);
			}
			return res;
		}
	};
	
	private static class FCB implements IORAddress {
		private final int startAddress;
		
		public final NotifyMask notifiersLockMask;
		public final Word upNotifyBits;
		public final DblWord downNotifyBits;
		public final IOPCondition mesaClientCondition;
		public final Word mesaClientMask;
		public final Word timeOfDayIsValidAndCommand;
		/**/public final Field timeOfDayIsValid;
		/**/public final Field command;
		public final Word data0;
		public final Word data1;
		public final Word data2;
		public final TaskContextBlock processorTCB;
		public final TaskContextBlock clientTCB;
		
		public final DblWord byteSwappedGMT;
		
		private FCB() {
			this.startAddress = IORegion.syncToSegment() + IORegion.IOR_BASE;
			
			this.notifiersLockMask = new NotifyMask(ProcessorFCB, "notifiersLockMask");
			this.upNotifyBits = mkWord(ProcessorFCB, "upNotifyBits");
			this.downNotifyBits = mkDblWord(ProcessorFCB, "downNotifyBits");
			this.mesaClientCondition = new IOPCondition(ProcessorFCB, "mesaClientCondition");
			this.mesaClientMask = mkWord(ProcessorFCB, "mesaClientMask");
			this.timeOfDayIsValidAndCommand = mkWord(ProcessorFCB, "timeOfDayIsValidAndCommand");
			/**/this.timeOfDayIsValid = mkField("timeOfDayIsValid", this.timeOfDayIsValidAndCommand, 0xFF00);
			/**/this.command = mkField("command", this.timeOfDayIsValidAndCommand, 0x00FF);
			this.data0 = mkByteSwappedWord(ProcessorFCB, "data[0]");
			this.data1 = mkByteSwappedWord(ProcessorFCB, "data[1]");
			this.data2 = mkByteSwappedWord(ProcessorFCB, "data[2]");
			this.processorTCB = new TaskContextBlock(ProcessorFCB, "processorTCB");
			this.clientTCB = new TaskContextBlock(ProcessorFCB, "clientTCB");
			
			this.byteSwappedGMT = mkCompoundDblWord(this.data0, this.data1);
			
			this.mesaClientMask.set(mkMask());
		}

		@Override
		public String getName() {
			return ProcessorFCB;
		}

		@Override
		public int getRealAddress() {
			return this.startAddress;
		}
	}
	
	/*
	 * implementation of the iop6085 interface for supporting the mesa-processor
	 */
	
	private final FCB fcb;
	
	// machine ID fetched from configuration via class Cpu
	private final short cpuId0;
	private final short cpuId1;
	private final short cpuId2;
	
	// difference between (our) simulated GMT and (Pilots) expected GMT
	// originally 0 i.e. local time, set to time difference to local time by command writeGMT
	private int gmtCorrection = 0;
	
	// work-around for XDE HeraldWindow having a blinking warning instead of the date
	// if the "current" time is not in the expected time frame (somewhere between the bootfile
	// build date and some (4 or 5) years later)
	// so the system date can be faked (at a 2nd level :-)) to a given date for the first n-thousand
	// instructions, so the HeraldWindow sees a specific date when it checks for a plausible boot time
	// and the "correct" date is returned after that number of instructions when "the rest of XDE" asks
	// for the time
	private static long xdeNoBlinkInsnLimit = 0;
	private static long xdeNoBlinkBaseMSecs = 0;
	private static long xdeNoBlinkDateMSecs = 0;
	
	public static void installXdeNoBlinkWorkAround(LocalDate noBlinkTargetDate, long insnsLimit) {
		xdeNoBlinkInsnLimit = insnsLimit;
		xdeNoBlinkBaseMSecs = (System.currentTimeMillis() / 86_400_000L) * 86_400_000L; // midnight of today
		xdeNoBlinkDateMSecs = noBlinkTargetDate.toEpochDay() * 86_400_000L; // date to be returned until insnsLimit instructions are reached
	}
	
	public HProcessor() {
		super("Processor", Config.IO_LOG_PROCESSOR);
		
		this.fcb = new FCB();
		
		this.fcb.timeOfDayIsValid.set(1); // true; meaning: we can always deliver a valid time
		
		this.cpuId0 = (short)Cpu.getPIDword(1);
		this.cpuId1 = (short)Cpu.getPIDword(2);
		this.cpuId2 = (short)Cpu.getPIDword(3);
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
		if (notifyMask != 0 && notifyMask != this.fcb.mesaClientMask.get()) {
			return false;
		}
		
		// interpret and handle the requested operation
		int cmdCode = this.fcb.command.get();
		Command cmd = Command.map(cmdCode);
		switch(cmd) {
		
		case noCommand:
			this.logf("no command given, why was executeFcbCommand() called ??\n");
			break;
			
		case readGMT:
			this.logf("readGMT\n");
			if (Cpu.insns > xdeNoBlinkInsnLimit) {
				this.fcb.byteSwappedGMT.set(getRawPilotTime() + this.gmtCorrection);
			} else {
				this.fcb.byteSwappedGMT.set(getRawPilotTime(xdeNoBlinkDateMSecs + (System.currentTimeMillis() - xdeNoBlinkBaseMSecs)));
			}
			break;
			
		case writeGMT:
			this.gmtCorrection = this.fcb.byteSwappedGMT.get() - getRawPilotTime();
			this.logf("writeGMT -> new gmtCorrection: %d\n", this.gmtCorrection);
			break;
			
		case readHostID:
			this.logf("readHostID\n");
			// the host-id is expected unswapped, but .data0..2 automatically swap bytes, so swap the words once more time
			this.fcb.data0.set((short)byteSwap(this.cpuId0));
			this.fcb.data1.set((short)byteSwap(this.cpuId1));
			this.fcb.data2.set((short)byteSwap(this.cpuId2));
			break;
		
		case readVMMapDesc:
			this.logf("readVMMapDesc\n");
			this.fcb.data0.set((short)Mem.dBreak_Real_firstMapPage);
			this.fcb.data1.set((short)Mem.dBreak_Real_countMapPages);
			break;
		
		case readRealMemDesc:
			this.logf("readRealMemDesc\n");
			this.fcb.data0.set((short)Mem.dBreak_firstRealPageInVMM);
			this.fcb.data1.set((short)Mem.dBreak_lastRealPageInVMM);
			this.fcb.data2.set((short)Mem.dBreak_countRealPagesInVMM);
			break;
		
		case readDisplayDesc:
			this.logf("readDisplayDesc\n");
			this.fcb.data0.set((short)Mem.dBreak_displayType);
			this.fcb.data1.set((short)Mem.dBreak_Real_firstDisplayBankPage);
			this.fcb.data2.set((short)Mem.dBreak_Real_countDisplayBankPages);
			break;
		
		case readKeyboardType:
			this.logf("readKeyboardType\n");
			this.fcb.data0.set((short)2); // pretend "English Level V" keyboard
			break;
		
		case readPCType:
			this.logf("readPCType\n");
			this.fcb.data0.set((short)0); // == false, no pc extension board present
			break;
		
		case bootButton:
			this.logf("bootButton\n");
			throw new Cpu.MesaStopped("IOP6085::Processor ... bootButton");
		
		case readNumbCSBanks:
			this.logf("readNumbCSBanks\n");
			this.fcb.data0.set((short)1); // one control-store (microcode) bank (4Kwords), whatever this may be used for
			break;
		
		case readMachineType:
			this.logf("readMachineType\n");
			this.fcb.data0.set((short)3); // pretend we're a daybreak ... 
			break;
			
		default:
			this.logf("invalid command-code: %d\n", cmdCode);
		
		}
		
		this.fcb.command.set(Command.noCommand.code); // signal we're done with executing the fcb command
		return true;
	}

	@Override
	public void handleLockmem(short lockMask, int realAddress, MemOperation memOp, short oldValue, short newValue) {
		// nothing to do
	}

	@Override
	public void handleLockqueue(int vAddr, int rAddr) {
		// not relevant for processor handler
	}
	
	@Override
	public synchronized void refreshMesaMemory() {
		// nothing to do
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// nothing to save or shutdown
	}
	
	/*
	 * UNix <-> Mesa time mapping
	 */

	// Java Time base  ::  1970-01-01 00:00:00
	// Pilot Time base ::  1968-01-01 00:00:00
	// => difference is 1 year + 1 leap-year => 731 days.
	private static final int UnixToPilotSecondsDiff = 731 * 86400; // seconds
	
	// this is some unexplainable Xerox constant whatever for, but we have to use it...
	private static final int MesaGmtEpoch = 2114294400;
	
	// get seconds since 1968-01-01 00:00:00 for a given Jaja milliseconds timestamp
	private static int getRawPilotTime(long msecs) {
		long currJavaTimeInSeconds = (msecs + timeShiftMilliSeconds) / 1000;
		return (int)((currJavaTimeInSeconds + UnixToPilotSecondsDiff + MesaGmtEpoch) & 0x00000000FFFFFFFFL);
	}
	
	// get seconds since 1968-01-01 00:00:00 for "now"
	private static int getRawPilotTime() {
		return getRawPilotTime(System.currentTimeMillis());
	}
	
	/**
	 * Get the corresponding Java-{@code Date} for a given mesa-time.
	 *  
	 * @param mesaTime the mesa time value to translate.
	 * @return the Java-{@code Date} corresponding to {@code mesaQTime}.
	 */
	public static Date getJavaTime(int mesaTime) {
		long javaMillis = (mesaTime - UnixToPilotSecondsDiff - MesaGmtEpoch) * 1000L;
		return new Date(javaMillis);
	}
	
}