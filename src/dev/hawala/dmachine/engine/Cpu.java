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

package dev.hawala.dmachine.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import dev.hawala.dmachine.engine.Xfer.XferType;
import dev.hawala.dmachine.engine.agents.Agents;

/**
 * Implementation of the core cpu functionality defined in the PrincOps.
 * That is:
 * <ul>
 * <li>all registers</li>
 * <li>functional register access like evaluation stack push/pop</li>
 * <li>issuing traps and faults</li>
 * <li>the inner mesa interpreter</li>
 * </ul>
 * <p>
 * In addition, the {@code Cpu}-class provides the central logging functions
 * and a (more or less useful) low-level debugger for the instruction execution
 * process. 
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Cpu {
	
	/*
	 * Logging
	 */
	
	private static boolean unsilenced = true; // modified by the low-level debugger
	
	public static void logError(String msg) {
		System.out.println("ERR: -------------------------------------- " + msg);
	}
	
	public static void logWarning(String msg) {
		System.out.println("WRN: -------------------------------------- " + msg);
	}
	
	public static void logInfo(String msg) {
		System.out.println("INF: -------------------------------------- " + msg);
	}
	
	private static final int FLIGHTRECORDER_MAX = 0x1FFFF;
	
	private static final String[] oplog = new String[FLIGHTRECORDER_MAX + 1];
	private static int currOplog = 0;
	private static final StringBuilder oplogSb = new StringBuilder();
	
	private static void opcodesLogf(String format, Object... args) {
		if (Config.LOG_OPCODES_AS_FLIGHTRECORDER) {
			if (Config.FLIGHTRECORDER_WITH_STACK) {
				oplogSb.setLength(0);
				oplogSb.append(String.format("stack[%02d] ", SP));
				for (int i = 0; i < SP; i++) {
					oplogSb.append(String.format(" 0x%04X (", stack[i]));
					char c = (char)((stack[i] >> 8) & 0xFF);
					oplogSb.append((c >= ' ' && c < (char)0x7F) ? c : ' ');
					c = (char)(stack[i] & 0xFF);
					oplogSb.append((c >= ' ' && c < (char)0x7F) ? c : ' ');
					oplogSb.append(")");
				}
				oplogSb.append("\n");
				oplog[currOplog] = oplogSb.toString();
				currOplog = (currOplog + 1) & FLIGHTRECORDER_MAX;
			}
			oplog[currOplog] = String.format(format,  args);
			currOplog = (currOplog + 1) & FLIGHTRECORDER_MAX;
		} else {
			System.out.printf(format, args);
		}
	}
	
	public static void dumpOplog() {
		int idx = currOplog;
		while(true) {
			String line = oplog[currOplog];
			currOplog = (currOplog + 1) & FLIGHTRECORDER_MAX;
			if (line != null) { System.out.print(line); }
			if (idx == currOplog) { break; }
		}
	}
	
	public static void logf(String format, Object... args) {
		if (Config.LOG_OPCODES && unsilenced) {
			opcodesLogf(format, args); 
		}
	}
	
	public static void logOpcode(String opName) {
		if (unsilenced) {
			opcodesLogf("%06d: 0x%08X+0x%04X %s\n", insns, CB, savedPC, opName);
		}
	}
	
	public static void logOpcode_alpha(String opName) {
		if (unsilenced) {
			opcodesLogf("%06d: 0x%08X+0x%04X %s(alpha=0x%02X)\n", insns, CB, savedPC, opName, Mem.peekNextCodeByte());
		}
	}
	
	public static void logOpcode_salpha(String opName) {
		if (unsilenced) {
			int alpha = Mem.peekNextCodeByte();
			int salpha = ((alpha & 0x0080) != 0) ? (alpha | 0xFFFFFF00) : (alpha & 0x0000007F);
			opcodesLogf("%06d: 0x%08X+0x%04X %s(alpha=%d)\n", insns, CB, savedPC, opName, salpha);
		}
	}
	
	public static void logOpcode_pair(String opName) {
		if (unsilenced) {
			opcodesLogf("%06d: 0x%08X+0x%04X %s(pair=0x%02X)\n", insns, CB, savedPC, opName, Mem.peekNextCodeByte());
		}
	}
	
	public static void logOpcode_alphabeta(String opName) {
		if (unsilenced) {
			int w = Mem.peekNextCodeWord();
			opcodesLogf("%06d: 0x%08X+0x%04X %s(alpha=0x%02X , beta=0x%02X)\n", insns, CB, savedPC, opName, w >>> 8, w & 0x00FF);
		}
	}
	
	public static void logOpcode_alphasbeta(String opName) {
		if (unsilenced) {
			int w = Mem.peekNextCodeWord();
			int beta = w & 0x00FF;
			int sbeta = ((beta & 0x0080) != 0) ? (beta | 0xFFFFFF00) : (beta & 0x0000007F);
			opcodesLogf("%06d: 0x%08X+0x%04X %s(alpha=0x%02X , sbeta=%d)\n", insns, CB, savedPC, opName, w >>> 8, sbeta);
		}
	}
	
	public static void logOpcode_word(String opName) {
		if (unsilenced) {
			opcodesLogf("%06d: 0x%08X+0x%04X %s(word=0x%04X)\n", insns, CB, savedPC, opName, Mem.peekNextCodeWord());
		}
	}
	
	public static void logOpcode_sword(String opName) {
		if (unsilenced) {
			int word = Mem.peekNextCodeWord();
			int sword = ((word & 0x8000) != 0) ? (word | 0xFFFF0000) : (word & 0x00007FFF);
			opcodesLogf("%06d: 0x%08X+0x%04X %s(alpha=%d)\n", insns, CB, savedPC, opName, sword);
		}
	}
	
	public static void logEscOpcode(String opName) {
		if (unsilenced) {
			opcodesLogf("%06d: 0x%08X+0x%04X ESC.%s\n", insns, CB, savedPC, opName);
		}
	}
	
	public static void logEscOpcode_alpha(String opName) {
		if (unsilenced) {
			opcodesLogf("%06d: 0x%08X+0x%04X ESC.%s(alpha=0x%02X)\n", insns, CB, savedPC, opName, Mem.peekNextCodeByte());
		}
	}
	
	public static void logEscOpcode_word(String opName) {
		if (unsilenced) {
			opcodesLogf("%06d: 0x%08X+0x%04X ESC.%s(word=0x%04X)\n", insns, CB, savedPC, opName, Mem.peekNextCodeWord());
		}
	}
	
	/*
	 * registers
	 */
	
	// current Main Data Base
	public static int MDS = 0;
	
	// current global frame: this MesaMachine implementation uses several GF-registers, used depending
	// on the PrincOps-level currently active 
	// GF16 : PrincOps up to 4.0 :: POINTER relative to MDS => int & 0xFFFF
	// GF32 : PrincOps after 4.0 ("MDS-relieved") :: (absolute) LONG POINTER managed through the GFI machinery
	// GFI  : PrincOps after 4.0 ("MDS-relieved") :: ([0..16348]) Global Frame Index 
	public static int GF16 = 0;
	public static int GF32 = 0;
	public static int GFI = 0;
	
	// global frame table, only for PrincOps after 4.0 ("MDS-relieved")
	public static final int GFT = PrincOpsDefs.mGLOBAL_FRAME_TABLE;
	
	// current local frame (CARDINAL relative to MDS => int & 0xFFFF)
	public static int LF = 0;
	
	// current Code Base
	public static int CB = 0;
	
	// current instruction pointer (byte offset in the word-address CB)
	public static int PC = 0; // attention: byte-offset 0..0xFFFF
	public static int savedPC = 0;
	
	// evaluation stack (accessible only through push/pop methods)
	private final static short[] stack = new short[PrincOpsDefs.cSTACK_LENGTH];
	private final static int SP_LIMIT = PrincOpsDefs.cSTACK_LENGTH; // no push/recover when SP has this value
	public static int SP = 0; // => index of next(!) free stack position
	public static int savedSP = 0;
	
	// break byte
	public static int breakByte = 0;
	
	// xfer traps
	public static int XTS = 0;
	
	// (immutable) register for the Process Data Area structure
	public static final int PDA = PrincOpsDefs.mPROCESS_DATA_AREA;
	
	// index of currently running process
	public static short PSB = 0; // PsbIndex : 0..1023
	
	// process timeout timer
	public static int PTC = 0; // Ticks = CARDINAL
	
	// wake up pending register
	// -> 32 bit instead of (as in PrincOps) 16 bit to allow for implementation owned interrupts
	// -> AtomicInteger instead of synchronized plain int for faster setting/getting in different
	//    threads (mesa engine resp. UI) 
	public static final AtomicInteger WP = new AtomicInteger();
	
	// wake up mask for this architecture implementation : no reserved interrupt (see WP)
	public static final short WM = 0;
	
	// wake up disable counter register
	public static short WDC = 0;
	
	// interval timer register
	// => resolution of this implementation: 64 microseconds <- System.nanoSecs() with lower 16 bits cut off
	// => 512 IT-steps increment PTC in 32 ms (OK for PrincOps: between cTickMin = 15 and cTickMax = 60 as required)
	public static final int MicrosecondsPerPulse = 64;
	public static final int TimeOutInterval = 512; 
	private static long lastITpulse = 0;
	private static int currIT = 0;
	private static int extIToffset = 0;
	private static int internalIT() {
		long newITpulse = System.nanoTime() & 0xFFFFFFFFFFFF0000L;
		if (newITpulse != lastITpulse) {
			lastITpulse = newITpulse;
			currIT = (int)((newITpulse >>> 16) & 0x00000000FFFFFFFFL);
		}
		return currIT;
	}
	public static int IT() {
		return internalIT() + extIToffset;
	}
	public static void setIT(int newIT) {
		extIToffset = newIT - internalIT();
	}
	
	// processor id
	private static final int[] PID = new int[4];
	public static int getPIDword(int idx) {
		if (idx < 0 || idx >= PID.length) { return 0; }
		return PID[idx];
	}
	public static void setPID(int id0, int id1, int id2) {
		PID[0] = 0; // PrincOps section 3.3.3: first Word is "currently" unused
		PID[1] = id0 & 0xFFFF;
		PID[2] = id1 & 0xFFFF;
		PID[3] = id2 & 0xFFFF;
	}
	
	// maintenance panel code
	private static int MP = 0;
	@FunctionalInterface
	public interface MPHandler {
		void newMP(int mp);
	}
	private static MPHandler mpHandler = null;
	public static void setMPHandler(MPHandler h) {
		mpHandler = h;
		if (mpHandler != null) {
			mpHandler.newMP(MP);
		}
	}
	public static void setMP(int mp) {
		MP = mp;
		if (mpHandler != null) {
			mpHandler.newMP(MP);
		}
	}
	
	public static int getMP() {
		return MP;
	}
	
	
	// running state of the cpu
	public static boolean running = true;
	
	/*
	 * reset/initialize the cpu registers (see 4.7 Initial State)
	 */
	
	public static void resetRegisters() {
		// process registers
		WP.set(0); // no wake ups pending
		WDC = 1; // disable interrupts
		XTS = 0; // no xfer traps	
		Processes.resetPTC(1); // set PTC and time to IT
		running = true;
		
		// context initialization
		savedSP = 0;
		SP = 0;
		breakByte = 0;
		PSB = 0;
		MDS = 0;
		
		// others (initial values not explicitely defined by PrincOps)
		GF16 = 0;
		GF32 = 0;
		GFI = 0;
		LF = 0;
		savedPC = 0;
		PC = 0;
	}
	
	/*
	 * register based utilities
	 */
	
	// getStack() : intended for unittests only!
	public static short[] getStack() {
		return stack;
	}
	
	public static int lengthenPointer(short pointer) {
		return MDS + (pointer & 0xFFFF);
	}
	
	public static int lengthenPointer(int pointer) {
		return MDS + (pointer & 0xFFFF);
	}
	
	/*
	 * evaluation stack operations
	 */
	
	public static void push(short value) {
		if (SP >= SP_LIMIT) {
			stackError();
		}
		stack[SP++] = value;
	}
	
	public static void push(int value) {
		if (SP >= SP_LIMIT) {
			stackError();
		}
		stack[SP++] = (short)(value & 0xFFFF);
	}
	
	public static void pushLong(int value) {
		push((short)(value & 0xFFFF));
		push((short)(value >>> 16));
	}
	
	public static short pop() {
		if (SP <= 0) {
			stackError();
		}
		return stack[--SP];
	}
	
	public static short popRecover() {
		if (SP <= 0) {
			stackError();
		}
		return stack[SP - 1];
	}
	
	public static int popLong() {
		int high = pop() << 16;
		return high | (pop() & 0xFFFF);
	}
	
	public static void recover() {
		if (SP >= SP_LIMIT) {
			stackError();
		}
		SP++;
	}
	
	public static void discard() {
		if (SP <= 0) {
			stackError();
		}
		SP--;
	}
	
	public static void checkEmptyStack() {
		if (SP != 0) {
			stackError();
		}
	}
	
	
	/*
	 * trap & fault handling
	 */
	
	public static final int StateVector_stateWord = 14;
	public static final int StateVector_frame = 15;
	public static final int StateVector_data = 16;
	
	public static final int TransferDescriptor_src = 0;
	public static final int TransferDescriptor_dst = 2;
	
	public static void saveStack(int stateHandle) {
		for (int i = 0; i < PrincOpsDefs.cSTACK_LENGTH; i++) { // we save all stack elements (PrincOps: MIN[SP + 2, StackDepth])
			Mem.writeWord(stateHandle + i, stack[i]);
		}
		int stateWord = ((breakByte << 8) & 0xFF00) | (Cpu.SP & 0x000F);
		Mem.writeWord(stateHandle + StateVector_stateWord, (short)stateWord);
		SP = 0;
		savedSP = 0;
		breakByte = 0;
	}
	
	public static void loadStack(int stateHandle) {
		int stateWord = Mem.readWord(stateHandle + StateVector_stateWord);
		for (int i = 0; i < PrincOpsDefs.cSTACK_LENGTH; i++) { // we load all stack elements (PrincOps: MIN[SP + 2, StackDepth])
			stack[i] = Mem.readWord(stateHandle + i);
		}
		SP = stateWord & 0x000F;
		savedSP = SP;
		breakByte = (stateWord >>> 8) & 0x00FF;
	}
	
	public static boolean validContext() {
		return (PC > (PrincOpsDefs.cCODESEGMENT_SIZE * 2));
	}
	
	/**
	 * Fatal error in the mesa processor, for example due to a violation of invariants
	 * defined by PrincOps.
	 * <p>
	 * This exception is thrown when PrincOps states to raise <b>{@code ERROR}</b>.
	 * </p>
	 */
	public static class MesaERROR extends RuntimeException {
		private static final long serialVersionUID = 7233845554137769184L;

		public MesaERROR(String message) {
			super(message);
		}
		
	}
	
	/**
	 * Dummy error in the mesa processor indicating that the processor was stopped
	 * (not a feature defined by PrincOps).
	 * <p>
	 * This exception is thrown by the instructions SUSPEND and STOPEMULATOR (as well
	 * as by a special interrupt) to gracefully end the interpreter loop and transport
	 * a message indicating the reason for stopping the mesa engine. 
	 * </p>
	 */
	public static class MesaStopped extends RuntimeException {
		private static final long serialVersionUID = -3903865899845557828L;

		public MesaStopped(String message) {
			super(message);
		}
		
	}
	
	/**
	 * Exception signaling that the instruction could not be processed, either
	 * because a trap or a fault occurred and the execution of instructions
	 * should continue in a different context.
	 * <p>
	 * This exception is thrown when PrincOps states to raise <b>{@code Abort}</b>. 
	 * </p>
	 * <p>
	 * In deviation of PrincOps, some BLT instruction do not push the current stack
	 * with each item processed as restart information (to speed up things); instead,
	 * these instruction intercept the {@code MesaAbort} exception, modify the state
	 * saved as specified by PrincOps when the trap or fault occurred to reflect the
	 * current state of the BLT instruction and re-throw the exception. The scheme to
	 * update the saved state is:
	 * <br>&nbsp;-&gt;&nbsp;{@code ex.beginUpdateStack();}
	 * <br>&nbsp;-&gt;&nbsp;<i>push the required restart values on the evaluation stack</i>
	 * <br>&nbsp;-&gt;&nbsp;{@code ex.updateStack();}
	 * </p>
	 */
	public static class MesaAbort extends RuntimeException {
		private static final long serialVersionUID = -8398798334492304000L;
		
		private final int stateHandle; // LONG POINTER TO StateVector
		
		public MesaAbort() {
			this.stateHandle = 0;
		}
		
		public MesaAbort(int savedStackLocation) {
			this.stateHandle = savedStackLocation;
		}
		
		public void beginUpdateStack() {
			if (this.stateHandle == 0) { throw this; }
			
			int stateWord = Mem.readWord(this.stateHandle + StateVector_stateWord);
			Cpu.SP = stateWord & 0x000F;
		}
		
		public MesaAbort updateStack() {
			if (this.stateHandle != 0) {
				for (int i = 0; i < PrincOpsDefs.cSTACK_LENGTH; i++) {
					Mem.writeWord(this.stateHandle + i, stack[i]);
				}
				int oldStateWord = Mem.readWord(this.stateHandle + StateVector_stateWord);
				int newStateWord = (oldStateWord & 0xFF00) | (Cpu.SP & 0x000F);
				Mem.writeWord(this.stateHandle + StateVector_stateWord, (short)newStateWord);
			}
			return this;
		}
	}
	
	/**
	 * Interface defining the methods for raising traps and faults. The only
	 * reason for the existence of this abstraction (in addition to the public
	 * methods of the [@code Cpu}-class) is to allow UnitTests to intercept
	 * traps and faults for verification of instruction behavior.
	 * <br>
	 * The public methods of the [@code Cpu}-class for raising traps and faults
	 * call the currently installed {@code MesaFaultTrapThrower}. 
	 */
	public interface MesaFaultTrapThrower {
		
		// traps
		
		void trap(int controlLinkIdx);
		
		void signalBoundsTrap();
		
		void signalBreakTrap();
		
		void signalCodeTrap(int gf);
		
		void signalControlTrap(int src);
		
		void signalDivideCheckTrap();
		
		void signalDivideZeroTrap();
		
		void signalEscOpcodeTrap(int code);
		
		void signalInterruptError();
		
		void signalOpcodeTrap(int code);
		
		void signalPointerTrap();
		
		void signalProcessTrap();
		
		void signalRescheduleError();
		
		void signalStackError();
		
		void signalUnboundTrap(int dst);
		
		void signalHardwareError();
		
		// faults
		
		void signalPageFault(int faultingLongPointer);
		
		void signalWriteProtectFault(int faultingLongPointer);
		
		void signalFrameFault(int fsi);
		
		// finalizing ERROR exception
		
		void ERROR(String reason);
	}
	
	// default implementation of the {@code MesaFaultTrapThrower} really
	// signalling the traps and faults to the running program (i.e.Pilot)
	private static class RealMesaFaultTrapThrower implements MesaFaultTrapThrower {

		@Override
		public void signalBoundsTrap() {
			this.trapZero(PrincOpsDefs.sBoundsTrap);
		}

		@Override
		public void signalBreakTrap() {
			this.trapZero(PrincOpsDefs.sBreakTrap);
		}

		@Override
		public void signalCodeTrap(int gf) {
			this.trapOne(PrincOpsDefs.sCodeTrap, gf);
		}

		@Override
		public void signalControlTrap(int src) {
			this.trapOne(PrincOpsDefs.sControlTrap, src);
		}

		@Override
		public void signalDivideCheckTrap() {
			this.trapZero(PrincOpsDefs.sDivCheckTrap);
		}

		@Override
		public void signalDivideZeroTrap() {
			this.trapZero(PrincOpsDefs.sDivZeroTrap);
		}

		@Override
		public void signalEscOpcodeTrap(int code) {
			int controlLink = Mem.readMDSDblWord(PrincOpsDefs.getEttMdsPtr(code));
			Cpu.PC = Cpu.savedPC;
			Cpu.SP = Cpu.savedSP;
			if (Cpu.validContext()) {
				Mem.writeMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc, Cpu.PC);
			}
			Xfer.impl.xfer(controlLink, Cpu.LF, XferType.xtrap, false);
			Mem.writeMDSWord(Cpu.LF, code);
			throw new MesaAbort();
		}

		@Override
		public void signalInterruptError() {
			this.trapZero(PrincOpsDefs.sInterruptError);
		}

		@Override
		public void signalOpcodeTrap(int code) {
			this.trapOne(PrincOpsDefs.sOpcodeTrap, code);
		}

		@Override
		public void signalPointerTrap() {
			this.trapZero(PrincOpsDefs.sPointerTrap);
		}

		@Override
		public void signalProcessTrap() {
			this.trapZero(PrincOpsDefs.sProcessTrap);
		}

		@Override
		public void signalRescheduleError() {
			this.trapZero(PrincOpsDefs.sRescheduleError);
		}

		@Override
		public void signalStackError() {
			this.trapZero(PrincOpsDefs.sStackError);
		}

		@Override
		public void signalUnboundTrap(int dst) {
			this.trapTwo(PrincOpsDefs.sUnboundTrap, dst);
		}

		@Override
		public void signalHardwareError() {
			this.trapZero(PrincOpsDefs.sHardwareError);
		}
		
		@Override
		public void ERROR(String reason) {
			throw new MesaERROR(reason);	
		}
		
		@Override
		public void signalPageFault(int faultingLongPointer) {
			Processes.faultTwo(PrincOpsDefs.qPAGE_FAULT, faultingLongPointer);
		}

		@Override
		public void signalWriteProtectFault(int faultingLongPointer) {
			Processes.faultTwo(PrincOpsDefs.qWRITE_PROTECT_FAULT, faultingLongPointer);
		}

		@Override
		public void signalFrameFault(int fsi) {
			Processes.faultOne(PrincOpsDefs.qFRAME_FAULT, (short)(fsi));
		}
		
		/*
		 * 9.5.2 Trap Processing
		 */
		
		@Override
		public void trap(int controlLinkIdx) {
			int controlLink = Mem.readMDSDblWord(PrincOpsDefs.getSdMdsPtr(controlLinkIdx));
			Cpu.PC = Cpu.savedPC;
			Cpu.SP = Cpu.savedSP;
			if (Cpu.validContext()) {
				Mem.writeMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc, Cpu.PC);
				Cpu.logf("   => LF=0x%04X -> overhead.pc=0x%04X\n", Cpu.LF, Mem.readMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc));
			}
			Xfer.impl.xfer(controlLink, Cpu.LF, XferType.xtrap, false);
		}
		
		private void trapZero(int controlLinkIdx) {
			this.trap(controlLinkIdx);
			throw new MesaAbort();
		}
		
		private void trapOne(int controlLinkIdx, int parameter) {
			this.trap(controlLinkIdx);
			Mem.writeMDSWord(Cpu.LF, parameter);
			throw new MesaAbort();
		}
		
		private void trapTwo(int controlLinkIdx, int parameter) {
			this.trap(controlLinkIdx);
			Mem.writeMDSWord(Cpu.LF, parameter & 0x0000FFFF);
			Mem.writeMDSWord(Cpu.LF + 1, parameter >>> 16);
			throw new MesaAbort();
		}
		
	}
	
	// the installed {@code MesaFaultTrapThrower} to be used, overriden for UnitTests
	public static MesaFaultTrapThrower thrower = new RealMesaFaultTrapThrower();
	
	public static void logTrapOrFault(String msg) {
		logTrapOrFault(false, msg);
	}
	
	public static void logTrapOrFault(boolean logOnly, String msg) {
		if (!Config.LOG_OPCODES || !Config.USE_DEBUG_INTERPRETER) { return; }
		if (logOnly) { return; }
		
		if (!Cpu.unsilenced) { return; }

		System.out.printf("## at 0x%08X+0x%04X [insn# %d]\n## %s", CB, savedPC, insns, msg);
		String answer = "";
		while(!"a".equalsIgnoreCase(answer) && !"d".equalsIgnoreCase(answer) && !"c".equalsIgnoreCase(answer)) {
			System.out.printf("(C)ontinue or (D)ebug or (A)bort ? >>> ");
			try {
				answer = cons.readLine();
			} catch (IOException e) {
				answer = "a";
			}
		}
		if ("a".equalsIgnoreCase(answer)) {
			System.out.println("Aborting execution ...");
			System.exit(1);
		}
		if ("d".equalsIgnoreCase(answer)) {
			System.out.println("Falling into low level debugger ...");
			go = false;
			goReturn = false;
		}
	}
	
	public static void boundsTrap() {
		logTrapOrFault(" ## boundsTrap\n");
		thrower.signalBoundsTrap();
	}
	
	public static void breakTrap() {
		logTrapOrFault(" ## breakTrap\n");
		thrower.signalBreakTrap();
	}
	
	public static void codeTrap(int gf) {
		logTrapOrFault(true, String.format(" ## codeTrap gf=0x%04X\n", gf));
		thrower.signalCodeTrap(gf);
	}
	
	public static void controlTrap(int src) {
		logTrapOrFault(String.format(" ## controlTrap src=0x%04X\n", src));
		thrower.signalControlTrap(src);
	}
	
	public static void divCheckTrap() {
		logTrapOrFault(" ## divCheckTrap\n");
		thrower.signalDivideCheckTrap();
	}
	
	public static void divZeroTrap() { 
		logTrapOrFault(" ## divZeroTrap\n");
		thrower.signalDivideZeroTrap();
	}
	
	public static void escOpcodeTrap(int code) {
		logError(String.format("unimplemented ESC-opcode 0x%02X",code & 0xFF));
		logTrapOrFault(String.format("unimplemented ESC-opcode 0x%02X",code & 0xFF));
		thrower.signalEscOpcodeTrap(code);
	}
	
	public static void interruptError() {
		logTrapOrFault(" ## interruptError\n");
		thrower.signalInterruptError();
	}
	
	public static void opcodeTrap(int code) {
		logError(String.format("unimplemented opcode 0x%02X",code & 0xFF));
		logTrapOrFault(String.format("unimplemented opcode 0x%02X",code & 0xFF));
		thrower.signalOpcodeTrap(code);
	}
	
	public static void pointerTrap() {
		logTrapOrFault(" ## pointerTrap\n");
		thrower.signalPointerTrap();
	}
	
	public static void processTrap() {
		logTrapOrFault(" ## processTrap\n");
		thrower.signalProcessTrap();
	}
	
	public static void rescheduleError() {
		logTrapOrFault(" ## rescheduleError\n");
		thrower.signalRescheduleError();
	}
	
	public static void stackError() {
		logTrapOrFault(" ## stackError\n");
		dumpOplog();
		thrower.signalStackError();
	}
	
	public static void unboundTrap(int dst) {
		logTrapOrFault(String.format(" ## unboundTrap dst=0x%08X\n", dst));
		thrower.signalUnboundTrap(dst);
	}
	
	public static void hardwareError() {
		logTrapOrFault(" ## hardwareError\n");
		thrower.signalHardwareError();
	}
	
	public static void ERROR(String reason) {
		System.out.println();
		System.out.flush();
		System.err.printf("\n**\n*** raising ERROR for reason: %s\n**\n", reason);
		System.err.flush();
		logTrapOrFault("** ** ** raising ERROR\n");
		thrower.ERROR(reason);
	}
	
	public static void nakedTrap(int controlLinkIdx) {
		logTrapOrFault(String.format(" ## naked trap for controlLinkIdx: %d\n", controlLinkIdx));
		thrower.trap(controlLinkIdx);
	}
	
	public static void signalPageFault(int faultingLongPointer) {
		logTrapOrFault(String.format(" ## page fault for LP = 0x%08X at 0x%08X+0x%04X [insn# %d ]\n ",
				faultingLongPointer, Cpu.CB, Cpu.savedPC, Cpu.insns));
		thrower.signalPageFault(faultingLongPointer);
	}
	
	public static void signalWriteProtectFault(int faultingLongPointer) {
		logTrapOrFault(String.format(" ## write protect fault for LP = 0x%08X\n ", faultingLongPointer));
		thrower.signalWriteProtectFault(faultingLongPointer);
	}
	
	public static void signalFrameFault(int fsi) {
		logTrapOrFault(String.format(" ## frame fault for fsi = %d\n ", fsi));
		thrower.signalFrameFault(fsi);
	}
	
	/*
	 * 4.1 Interpreter
	 */
	
	public static void initialize() {
		resetRegisters();
		
		int bootLink = Mem.readMDSDblWord(PrincOpsDefs.getSdMdsPtr(PrincOpsDefs.sBoot));
		Xfer.impl.xfer(bootLink, 0, XferType.xcall, false);
	}
	
	/*
	 * Timeout check throttling to avoid getting the system time after *each* instruction.
	 * 
	 * 10.4.5 Timeouts
	 * ...
	 * Timeouts are measured in ticks, where the conversion between ticks and real time is
	 * processor-dependent. A tick is on the order of 40 milliseconds.
	 * ...
	 * 
	 * Appendix A
	 * ...
	 * cTick Mininum and Maximum Tick Size
	 *   cTickMin: CARDINAL = 15;
	 *   cTickMax: CARDINAL = 60;
	 * (milliseconds!)
	 * 
	 * on a 2.4 GHz CoreDuo2 laptop:
	 *   => 32768 gives at the very least 20 Mips on pure arithmetic, stack and local/global frame access instructions 
	 *   => ~50 nanosecs per instruction
	 *   => timeout check interval ~ 1,7 millisecs
	 *   => about factor 10 better than cTickMin!
	 *   => and checking time all ~ 1 ms should give an acceptable load by querying the time (even on faster machines)
	 *   
	 * corrected to 16k instructions, as real pilot based systems have at best 10 mips on same hardware
	 */
	
	private static final int TIMEOUT_THROTTLE_COUNT = 16 * 1024;
	
	/*
	 * number of instructions executed so far for statistics
	 */
	
	public static long insns = 0;
	
	/*
	 * Instruction interpreter proper 
	 */
	
	public static String processor() {
		try {
			initialize();
			int timeoutCountDown = TIMEOUT_THROTTLE_COUNT;
			while(true) {
				try {
					boolean interrupt = Processes.checkforInterrupts();
					boolean timeout = false;
					if (timeoutCountDown < 1) {
						timeout = Processes.checkForTimeouts();
						timeoutCountDown = TIMEOUT_THROTTLE_COUNT; 
					} else {
						timeoutCountDown--;
					}
					
					if (interrupt || timeout) {
						Processes.reschedule(true);
					} else if (running) {
						if (Config.LOG_OPCODES && Config.USE_DEBUG_INTERPRETER) {
							debugInterpreter();
							timeoutCountDown = 0; // reset throttling to force timeout checks 
						}
						savedPC = PC;
						savedSP = SP;
						insns++;
						Opcodes.dispatch(Mem.getNextCodeByte());
					} else {
						Processes.idle(); // wake up on interrupt but at latest after NOT_RUNNING_SLEEP_MSECS
						timeoutCountDown = 0; // force timeout checks after sleeping 
					}
				} catch (MesaAbort ma) {
					continue;
				}
			}
		} catch (MesaERROR me) {
			me.printStackTrace();
			return me.getMessage();
		} catch (MesaStopped ms) {
			return ms.getMessage();
		} catch (RuntimeException re) {
			re.printStackTrace();
			return String.format("Cpu.processor() => %s : %s", re.getClass().getCanonicalName(), re.getMessage());
		}
		
		// never reached ... return "Cpu.processor() ended normally(?)";
	}
	
	/*
	 * low-level debugger
	 */
	
	private static class PcRange {
		
		public final int cb;
		public final int pc1;
		public final int pc2;
		
		public PcRange(int cb, int pc1, int pc2) {
			this.cb = cb;
			this.pc1 = pc1;
			this.pc2 = pc2;
		}
		
		public boolean in() {
			return CB == this.cb && this.pc1 <= PC && PC >= this.pc2; 
		}
	}
	
	private static List<PcRange> iRanges = new ArrayList<>();
	
	private static final int InvHex = -1;
	private static final int InvVal = -1;
	
	private static boolean go = false;
	private static boolean step = false;

	private static int goCount = InvVal;
	private static int gotoCB = InvVal;
	private static int gotoPC = InvVal;
	
	private static boolean goReturn = false;
	private static int goReturnLF = InvVal;
	
	private static int lfCount = 8;
	private static int gfCount = 8;
	
	private static Map<Integer,Short> watchList = new HashMap<>();
	
	private static int silencedCount = 0;
	
//	private static boolean dumpOpcodes = true;
	
	private static boolean dumpIoArea = true;
	
	public static void armDebugInterpreter() {
		// go = false;
		unsilenced = true;
		silencedCount = 0;
		// System.out.println("armDebugInterpreter()");
		
	}
	
	private static void debugInterpreter() {
				
//		if (dumpOpcodes) {
//			for (int i = 0; i < 256; i++) {
//				if ((i % 8) == 0) { System.out.printf("\n        "); }
//				System.out.printf(" \"%s\", ", Opcodes.opcNames[i]);
//			}
//			System.out.println();
//			for (int i = 0; i < 256; i++) {
//				if ((i % 8) == 0) { System.out.printf("\n        "); }
//				System.out.printf(" \"%s\", ", Opcodes.escNames[i]);
//			}
//
//			System.out.println();
//			dumpOpcodes = false;
//		}
		
		if (!unsilenced) { silencedCount--; }
		if (silencedCount < 1) {
			unsilenced = true;
		}
		
		int nextOpcode = Mem.peekNextCodeByte();
		if (nextOpcode == 0 || nextOpcode == 255) {
			go = false;
			
			int insAdr = Cpu.CB + (Cpu.PC / 2);
			
			System.out.printf(
					"\n\t\t!! instruction 0x%02X at word-adress 0x%08X => rp 0x%06X , mf 0x%04X ... dumping virtual page",
					nextOpcode, insAdr, Mem.getVPageRealPage(insAdr >>> 8), Mem.getVPageFlags(insAdr >>> 8));
			int ilp = (Cpu.CB + (Cpu.PC / 2)) & 0xFFFFFF00;
			for (int i = 0; i < 256; i++) {
				if ((i % 16) == 0) {
					System.out.printf("\n\t\t\t0x%08X:", ilp + i);
				}
				System.out.printf(" %04X", Mem.rawRead(ilp + i));
			}
			System.out.printf("\n\n");
			
			int vp = ilp >>> 8;
			short mf = Mem.getVPageFlags(vp);
			int rp = Mem.getVPageRealPage(vp);
			System.out.printf("\t\t\tpage data: vp = 0x%08X -> rp  = 0x%08X , mf = 0x%04X\n\n", vp, rp, mf);
			
//			Ch03_Memory_Organization.dumpMapOps();
		}
		
		for (Entry<Integer, Short> e : watchList.entrySet()) {
			int lp = e.getKey();
			short lastVal = e.getValue();
			short currVal = Mem.rawRead(lp);
			if (currVal != lastVal) {
				System.out.printf(
					"\t\t++ watched mem changed lp = 0x%08X :: 0x%04X (old) => 0x%04X (new) , confirm...",
					lp, lastVal, currVal);
				try { cons.readLine(); } catch (IOException exc) { }
				watchList.put(lp,  currVal);
			}
		}
		
		if (dumpIoArea) {
			Agents.dumpIoArea();
			dumpIoArea = false;
		}
		
		if (isAVtableInvalid()) {
			System.out.println("\n\t**\n\t** AV table invalid\n\t**");
			go = false;
			goReturn = false;
			silencedCount = 0;
			unsilenced = true;
		}
		
		if (go && goCount != InvVal && goCount != insns) {
			dumpState();
			return;
		} else if (go && gotoCB != InvVal && gotoPC != InvVal && (CB != gotoCB || PC != gotoPC)) {
			dumpState();
			return;
		} else if (go) {
			for (PcRange r : iRanges) {
				if (r.in()) {
					dumpState();
					return;
				}
			}
		}
		
		if (goReturn) {
			int op = Mem.peekNextCodeByte();
			if (op != 0xEF) { dumpState(); return; }
			if (goReturnLF != InvVal && goReturnLF != Cpu.LF) { dumpState(); return; }
		}
		
		goReturn = false;
		goReturnLF = InvVal;
		
		gotoCB = InvVal;
		gotoPC = InvVal;
		
		silencedCount = 0;
		unsilenced = true;
		
		boolean firstPrompt = true;
		while(true) {
			List<String> args = readCmd(firstPrompt);
			firstPrompt = false;
			if (args.size() == 0) {
				if (go || step) {
					System.out.println();
					return;
				}
				continue;
			}
			
			go = false;
			step = false;
			
			switch(args.get(0).toLowerCase()) {
			
			case "g":
				go = true;
				System.out.println();
				return;
				
			case "gt":
				if (args.size() == 3) {
					int cb = getHex(args.get(1));
					int pc = getHex(args.get(2));
					if (cb != InvHex && pc != InvHex) {
						go = true;
						gotoCB = cb;
						gotoPC = pc;
						System.out.printf("\t... running up to CB = 0x%08X & PC = 0x%04X\n\n", cb, pc);
						return;
					}
				}
				System.out.println("invalid arg count or arg values for 'gt'");
				break;
				
			case "gc":
				if (args.size() == 2) {
					goCount = getCount(args.get(1), InvVal);
					if (goCount != InvVal) {
						go = true;
						System.out.printf("\t... running up to instruction count = %d\n\n", goCount);
						return;
					}
				}
				System.out.println("invalid arg count or arg values for 'gc'");
				break;
				
			case "r":
				goReturn = true;
				System.out.println();
				return;
				
			case "rr":
				goReturn = true;
				goReturnLF = Cpu.LF;
				System.out.println();
				return;
				
			case "s":
				step = true;
				System.out.println();
				return;
				
			case "i":
				if (args.size() == 4) {
					int cb = getHex(args.get(1));
					int pc1 = getHex(args.get(2));
					int pc2 = getHex(args.get(3));
					if (cb != InvHex && pc1 != InvHex && pc1 != InvHex) {
						iRanges.add(new PcRange(cb, pc1, pc2));
						System.out.printf("added ignored range 0x%08X [ 0x%04X .. 0x%04X ]\n", cb, pc1, pc2);
						break;
					}
				}
				System.out.println("invalid arg count or arg values for 'i'");
				break;
				
			case "id":
				if (args.size() == 3) {
					int cb = getHex(args.get(1));
					int pc1 = getHex(args.get(2));
					List<PcRange> newRanges = new ArrayList<>(iRanges);
					for (PcRange r : iRanges) {
						if (r.cb == cb && r.pc1 == pc1) { newRanges.remove(r); }
					}
					iRanges = newRanges;
					System.out.printf("removed ignored range starting at 0x%08X.0x%04X\n", cb, pc1);
					break;
				}
				System.out.println("invalid arg count or arg values for 'id'");
				break;
				
			case "il":
				System.out.println("-- list of ignored (automatically executed) ranges:");
				for (PcRange r : iRanges) {
					System.out.printf("   0x%08X [ 0x%04X .. 0x%04X ]\n", r.cb, r.pc1, r.pc2);
				}
				System.out.println("--------");
				break;
				
			case "dl":
				if (args.size() > 1) {
					lfCount = getCount(args.get(1), lfCount);
				}
				dumpRegion("local frame", Cpu.lengthenPointer(Cpu.LF), lfCount);
				break;
				
			case "dg":
				if (args.size() > 1) {
					gfCount = getCount(args.get(1), gfCount);
				}
				dumpRegion("global frame", Cpu.GF32, gfCount);
				break;
				
			case "dt":
				dumpTrapLinks();
				break;
				
			case "dp":
				Processes.dumpProcessStatusArea();
				break;
				
			case "da":
				if (args.size() > 1) {
					int fsi = getCount(args.get(1), InvVal);
					if (fsi != InvVal) {
						dumpAVEntry(fsi);
					} else {
						System.out.println("invalid fsi for 'da'");
					}
				} else {
					dumpAV();
				}
				break;
				
			case "dgft":
				dumpGlobalFrames();
				break;
				
			case "d":
				dumpRegion("local frame", Cpu.lengthenPointer(Cpu.LF), lfCount);
				dumpRegion("global frame", Cpu.GF32, gfCount);
				break;
				
			case "d1":
				dump1stArea();
				break;
				
			case "w":
				if (args.size() > 1) {
					int addr = getHex(args.get(1));
					if (addr != InvHex) {
						if (watchList.containsKey(addr)) {
							System.out.printf("already watching word at 0x%08X\n", addr);
						} else {
							watchList.put(addr, Mem.rawRead(addr));
						}
					} else {
						System.out.println("invalid arg for 'w'");
					}
				} else {
					System.out.println("missing arg for 'w'");
				}
				break;
				
			case "uw":
				if (args.size() > 1) {
					int addr = getHex(args.get(1));
					if (addr != InvHex) {
						if (!watchList.containsKey(addr)) {
							System.out.printf("not watching word at 0x%08X\n", addr);
						} else {
							watchList.remove(addr);
						}
					} else {
						System.out.println("invalid arg for 'uw'");
					}
				} else {
					System.out.println("missing arg for 'uw'");
				}
				break;
				
			case "q":
				if (args.size() == 2) {
					silencedCount = getCount(args.get(1), InvVal);
					if (silencedCount != InvVal) {
						unsilenced = false;
						System.out.printf("\t... silencing for instruction count = %d\n\n", silencedCount);
						continue;
					}
					silencedCount = 0;
					unsilenced = true;
				}
				System.out.println("invalid arg count or arg values for 'q'");
				break;
				
			case "h":
			case "he":
			case "hel":
			case "help":
				System.out.println("\ti <cb> <pc1> <pc2>  add CB+PC range to ignore (execute automatically) (all hex!)");
				System.out.println("\tid <cb> <pc1>   remove CB+PC range (all hex!)");
				System.out.println("\til              list CB+PC ranges");
				System.out.println("\tg               (go) run until not in an ignore range");
				System.out.println("\tgt <cb> <pc>    (go to) run until the given code location is reached (stopping before)");
				System.out.println("\tgc <count>      (go count) run insns-counter reaches the given count (stopping before)");
				System.out.println("\tr               run up to the next RET");
				System.out.println("\trr              run up to the RET for the current local frame");
				System.out.println("\ts               single step");
				System.out.println("\t(empty return)  repeat last command if s or g");
				System.out.println("\tdl [<count>]    dump local frame (setting count words or using last count)");
				System.out.println("\tdg [<count>]    dump global frame (setting count words or using last count)");
				System.out.println("\tdt              dump trap link area");
				System.out.println("\tda [fsi]        dump AV table or AV[fsi] entry");
				System.out.println("\tdp              dump process state area");
				System.out.println("\td1              dump first 64 kwords");
				System.out.println("\tdgft            dump global frame table");
				System.out.println("\tw <addr>        watch word at addr (hex)");
				System.out.println("\tuw <addr>       stop watching word at addr (hex)");
				System.out.println("\td               dump local and global frame (using last counts)");
				System.out.println("\tq <count>       silence for 'count' instructions");      
				break;
				
			default:
				System.out.printf("** invalid command: '%s'\n", args.get(0));
			}
		}
	}
	
	private static final BufferedReader cons = new BufferedReader(new InputStreamReader(System.in)); 
	
	private static List<String> readCmd(boolean firstPrompt) {
		if (firstPrompt) {
			// dump GF32 LF registers, then next CB+PC -> instrName
			System.out.printf("\n\t-- GF32 = 0x%08X , LF = 0x%04X , next CB+PC : 0x%08X+0x%04X ( %s ) (insns: %d)\n",
					Cpu.GF32, Cpu.LF, Cpu.CB, Cpu.PC, getOpcodeName(), insns);
			
			// dump first 8 words of global and local frame
			System.out.printf("\t-- global frame:");
			for (int i = 0; i < 8; i++) {
				System.out.printf(" 0x%04X", Mem.readWord(Cpu.GF32 + i));
			}
			System.out.printf("\n\t-- local frame :");
			for (int i = 0; i < 8; i++) {
				System.out.printf(" 0x%04X", Mem.readMDSWord(Cpu.LF + i));
			}
			System.out.printf("\n");
			
			// dump stack [with 2 discard elements]
			System.out.printf("\t-- SP = %-2d stack: ", SP);
			for (int idx = 0; idx < Math.min(SP, PrincOpsDefs.cSTACK_LENGTH); idx++) {
				System.out.printf(" 0x%04X", stack[idx]);
			}
			if (SP < (PrincOpsDefs.cSTACK_LENGTH - 2)) {
				System.out.printf("  [ 0x%04X 0x%04X ]", stack[SP], stack[SP + 1]);
			} else if (SP < (PrincOpsDefs.cSTACK_LENGTH - 1)) {
				System.out.printf("  [ 0x%04X ]", stack[SP]);
			}
			System.out.println();
		}
		
		// issue prompt string
		System.out.printf("\t>>> ");
		
		// get line and tokenize
		ArrayList<String> res = new ArrayList<>();
		try {
			String line = cons.readLine();
			for(String token : line.split(" ")) {
				if (token != null && token.length() > 0) {
					res.add(token);
				}
			}
			
		} catch (IOException e) {
			System.out.flush();
			System.err.println("Error reading from stdin, aborting!!!!");
			System.exit(1);
		}
		
		// done
		return res;
	}
	
	private static String getOpcodeName() {
		int opc = Mem.peekNextCodeByte();
		if (opc == Opcodes.zESC || opc == Opcodes.zESCL) {
			int esc = Mem.peekNextCodeWord();
			return Opcodes.escNames[esc & 0x00FF];
		} else {
			return Opcodes.opcNames[opc];
		}
	}
	
	private static void dumpRegion(String title, int absBase, int count) {
		System.out.printf("\t-- %s (%d words)", title, count);
		for (int i = 0; i < count; i++) {
			if ((i % 8) == 0) {
				System.out.printf("\n\t  0x ");
			}
			System.out.printf(" %04X", Mem.readWord(absBase + i));
		}
		System.out.println("\n\t--------");
	}
	
	private static final int AV = PrincOpsDefs.mALLOCATION_VECTOR;
	
	private static boolean dumpAVEntry(int fsi) {
		if (fsi < 0 || fsi > 255) {
			System.out.printf("\t\t-- invalid fsi:%d\n", fsi);
			return false;
		}
		
		int avItem = Mem.readMDSWord(AV, fsi);
		if (avItem == 0) { return true; }
		
		System.out.printf("\t\t-- fsi:%d\n", fsi);
		boolean done = false;
		while(avItem != 0) {
			int avFlags = avItem & 0x0003;
			System.out.printf("\t\t\t-> avItem: 0x%04X ", avItem);
			switch(avFlags) {
			case 0:
				System.out.printf("(frame)");
				break;
			case 1:
				System.out.printf("(empty)");
				done = true;
				break;
			case 2:
				int newFsi = avItem >>> 2;
				System.out.printf("(indirect => newFsi = %d)", newFsi);
				avItem = 0;
				done = true;
				break;
			case 3:
				System.out.printf("(unused)");
				done = true;
				break;
			default:
				System.out.printf("(?default:%d?)", avFlags); // that's impossible, but keep the compiler happy
				done = true;
				break;
			}
			
			int lf = avItem & 0xFFFC;
			if (lf != 0) {
				int fWord = Mem.readMDSWord(lf, -4);
				if ((fWord & 0x00FF) != fsi) {
					System.out.printf(" !! fsi (%d) != frame.word.fsi (%d) !!", fsi, fWord & 0x00FF);
				}
			} else {
				done = true;
			}
			
			System.out.println();
			if (done) { return true; }
			
			avItem = Mem.readMDSWord(avItem & 0xFFFC);
		}
		return true;
	}
	
	private static void dumpAV() {
		System.out.printf("\t-- AV table in MDS(0x%08X):\n", MDS);
		for (int fsi = 0; fsi < PilotDefs.AVHeapSize; fsi++) {
			dumpAVEntry(fsi);
		}
	}
	
	private static boolean isAVtableInvalid() {
		for (int fsi = 0; fsi < PilotDefs.AVHeapSize; fsi++) {
			int avItem = Mem.readMDSWord(AV, fsi);
			if (avItem == 0) { continue; }

			boolean done = false;
			while(avItem != 0) {
				int avFlags = avItem & 0x0003;
				switch(avFlags) {
				case 0: // frame
					break;
				case 1: // empty
					done = true;
					break;
				case 2: // indirect
					int newFsi = avItem >>> 2;
					if (newFsi > 255) { return true; }
					avItem = 0;
					done = true;
					break;
				case 3: // unused
					done = true;
					break;
				default: // should not be possible
					return true;
				}
				
				int lf = avItem & 0xFFFC;
				if (lf != 0) {
					int fWord = Mem.readMDSWord(lf, -4);
					// was: if ((fWord & 0x00FF) != fsi) { return true; }
					// but Pilot enqueues e.g. a new fsi 31 frame into the fsi 20 list on FrameFault
					int fFsi = fWord & 0x00FF;
					if (fFsi >= PilotDefs.AVHeapSize) { return true;} // this is surely wrong
					if (fFsi < fsi) { return true;} // tolerate a larger frame (assuming that larger fsi is a larger frame)
				} else {
					done = true;
				}
				
				if (done) {
					avItem = 0;
				} else {				
					avItem = Mem.readMDSWord(avItem & 0xFFFC);
				}
			}
		}
		return false;
	}
	
	private static void dumpState() {
//		System.out.printf("\n");
//
//		System.out.printf(
//		  "  GFI = %04X  GF = %08X  CB  = %08X  PC  = %04X  MDS = %08X  LF = %04X\n",
//		  GFI, GF32, CB, PC, MDS, LF);
//
//		System.out.printf("  global frame: ");
//		for(int i = 0; i < 8; i++) { System.out.printf(" 0x%04X", Mem.readWord(GF32 + i)); }
//		System.out.printf("\n");
//
//		System.out.printf("  local frame : ");
//		for(int i = 0; i < 8; i++) { System.out.printf(" 0x%04X", Mem.readMDSWord(LF, i)); }
//		System.out.printf("\n");
//
//		System.out.printf("  stack[%02d] :", SP);
//		for(int i = 0; i < SP; i++) { System.out.printf(" 0x%04X", stack[i]); }
//		System.out.printf("\n");
	}
	
	private static int getCount(String s, int defaultValue) {
		try {
			int newValue = Integer.parseInt(s);
			if (newValue > 0) { return newValue; }
			System.out.printf("invalid count value %d\n", newValue);
		} catch(NumberFormatException nfe) {
			System.out.printf("invalid count value '%s'\n", s);
		}
		return defaultValue;
	}
	
	private static int getHex(String s) {
		try {
			return Integer.parseInt(s, 16);
		} catch(NumberFormatException nfe) {
			return InvHex;
		}
	}
	
	private static void dump1stArea() {
		System.out.printf("\n*******\n");
		int offset = 256;
		for (int i = 0; i < 255; i++) {
			System.out.printf("\n");
			for (int j = 0; j < 256; j++) {
				if ((offset % 16) == 0) {
					System.out.printf("\n%06X :", offset);
				}
				System.out.printf(" %04X", Mem.readWord(offset));
				offset++;
			}
		}
		System.out.printf("\n\n*******\n");
 	}
	
	private static int[] gftGF = new int[16348];
	private static int[] gftCB = new int[16348];
	private static int gftCount = 0;
	
	private static int getMaxCbLength(int forCb) {
		int maxLen = 65536;
		int maxGfi = 0;
		for (int i = 0; i < gftCB.length; i++) {
			int cb = gftCB[i];
			if (cb == 0) { continue; }
			if (cb <= forCb) { continue; }
			int thisLen = cb - forCb;
			if (thisLen < maxLen) {
				maxLen = thisLen;
				maxGfi = i;
			}
		}
		return (maxGfi << 17) | maxLen;
	}
	
	private static void dumpGlobalFrames() {
		for (int i = 0; i < gftGF.length; i++) { gftGF[0] = 0; }
		for (int i = 0; i < gftCB.length; i++) { gftCB[0] = 0; }
		gftCount = 0;
		
		for (int i = 1; i < 16348; i++) {
			int gftLp = PrincOpsDefs.mGLOBAL_FRAME_TABLE + (i * 4);
			int gf = (Mem.rawRead(gftLp + 1) << 16) | (Mem.rawRead(gftLp) & 0xFFFF);
			int cb = (Mem.rawRead(gftLp + 3) << 16) | (Mem.rawRead(gftLp + 2) & 0xFFFF);
			if (gf == 0 || cb == 0) { continue; }
			if (gf == 0xFFFFFFFF || cb == 0xFFFFFFFF) { continue; }
			gftGF[i] = gf;
			gftCB[i] = cb;
			gftCount++;
		}
		
		logf("-> %d plausible GFT entries\n", gftCount);

		for (int i = 1; i < 16348; i++) {
			int gf = gftGF[i];
			int cb = gftCB[i];
			if (gf == 0 || cb == 0) { continue; }
			
			int maxData = getMaxCbLength(cb);
			int maxCbLen = maxData & 0x0001FFFF;
			int nextGfi = maxData >>> 17;
			int maxCbPages = (maxCbLen + 255) / 256;
			int gfVp = gf >>> 8;
			int cbVp = cb >>> 8;
			logf(" GFT[0x%03X] -> globalFrame 0x%08X  - codebase 0x%08X ( maxLen: 0x%04X  maxPages: 0x%03X  nextGfi: 0x%03X)\n",
				i, gf, cb, maxCbLen, maxCbPages, nextGfi);
			logf("               rp 0x%06X mf 0x%04X   - rp 0x%06X mf 0x%04X\n",
				Mem.getVPageRealPage(gfVp), Mem.getVPageFlags(gfVp),
				Mem.getVPageRealPage(cbVp), Mem.getVPageFlags(cbVp)
				);
		}
	}
	
	private static int dumpSingleTrapLink(int at, String name) {
		int w0 = Mem.readMDSWord(at++);
		int w1 = Mem.readMDSWord(at++);
		System.out.printf(" %-16s: %04x %04x ;", name, w0, w1);
		return at;
	}
	
	private static void dumpTrapLinks() {
		int at = PrincOpsDefs.getSdMdsPtr(0);

		System.out.println("\ttrap control links:");
		System.out.printf("\t");
		at = dumpSingleTrapLink(at, "breakTrap");
		at = dumpSingleTrapLink(at, "boot");
		at = dumpSingleTrapLink(at, "stackError");
		at = dumpSingleTrapLink(at, "rescheduleError");
		System.out.println();

		System.out.printf("\t");
		at = dumpSingleTrapLink(at, "xferTrap");
		at = dumpSingleTrapLink(at, "opcodeTrap");
		at = dumpSingleTrapLink(at, "controlTrap");
		at = dumpSingleTrapLink(at, "codeTrap");
		System.out.println();

		System.out.printf("\t");
		at = dumpSingleTrapLink(at, "hardwareError");
		at = dumpSingleTrapLink(at, "unboundTrap");
		at = dumpSingleTrapLink(at, "divZeroTrap");
		at = dumpSingleTrapLink(at, "divCheckTrap");
		System.out.println();

		System.out.printf("\t");
		at = dumpSingleTrapLink(at, "interruptError");
		at = dumpSingleTrapLink(at, "processTrap");
		at = dumpSingleTrapLink(at, "boundsTrap");
		at = dumpSingleTrapLink(at, "pointerTrap");
		System.out.println();
	}
}
