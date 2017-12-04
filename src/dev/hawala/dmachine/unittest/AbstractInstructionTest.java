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

package dev.hawala.dmachine.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;

import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.PilotDefs;

/**
 * Parent class for unit test classes, providing common functionality
 * <ul>
 * <li>for building up the environment before executing the instruction</li>
 * <li>for verifying the state of the engine after executing the instruction</li>
 * </ul>
 * <p>
 * for the single tests.
 * </p> 
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public abstract class AbstractInstructionTest {
	
	/** exception thrown in unit tests when a trap or fault ocured */ 
	public class MesaTrapOrFault extends RuntimeException {
		private static final long serialVersionUID = -3272685200316067902L;
	}
	
	// callback that can be registered in the unittest MesaFaultTrapThrower
	// to be called before the MesaTrapOrFault is thrown
	@FunctionalInterface
	protected interface ChkThrowerCheck {
		void check();
	}
	
	// unittest variant for handling faults or traps allowing define if a
	// specific event is expected (having the unittest fail if not expected) to
	// intercept the events instead of dispatching it to the operating system
	// (which is not present in unittests)
	protected class ChkThrower implements Cpu.MesaFaultTrapThrower {
		
		public ChkThrowerCheck beforeCheck;
		
		public boolean expect_signalBreakTrap;
		
		public boolean expect_signalOpcodeTrap;
		
		public boolean expect_signalEscOpcodeTrap;
		
		public boolean expect_signalPointerTrap;
		
		public boolean expect_signalPageFault;
		
		public boolean expect_signalWriteProtectFault;
		
		public boolean expect_signalCodeFault;
		
		public boolean expect_signalStackError;
		
		public boolean expect_signalBoundsTrap;
		
		public boolean expect_signalHardwareError;
		
		public boolean expect_signalDivideZeroTrap;
		
		public boolean expect_signalDivideCheckTrap;
		
		public boolean expect_signalFrameFault;
		
		public boolean expect_signalUnboundTrap;
		
		public boolean expect_signalCodeTrap;
		
		public boolean expect_signalControlTrap;
		
		public boolean expect_ERROR;
		
		public boolean expect_signalRescheduleError;
		
		public boolean expect_signalProcessTrap;
		
		public boolean expect_signalInterruptError;
		
		public boolean expect_nakedTrap;

		@Override
		public void signalOpcodeTrap(int code) {
			if (!this.expect_signalOpcodeTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalEscOpcodeTrap(int code) {
			if (!this.expect_signalEscOpcodeTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalPointerTrap() {
			if (!this.expect_signalPointerTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalPageFault(int faultingLongPointer) {
			if (!this.expect_signalPageFault) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalWriteProtectFault(int faultingLongPointer) {
			if (!this.expect_signalWriteProtectFault) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalStackError() {
			if (!this.expect_signalStackError) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalBoundsTrap() {
			if (!this.expect_signalBoundsTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalHardwareError() {
			if (!this.expect_signalHardwareError) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalDivideZeroTrap() {
			if (!this.expect_signalDivideZeroTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalDivideCheckTrap() {
			if (!this.expect_signalDivideCheckTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalFrameFault(int fsi) {
			if (!this.expect_signalFrameFault) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalUnboundTrap(int dst) {
			if (!this.expect_signalUnboundTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalCodeTrap(int gf) {
			if (!this.expect_signalCodeTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalControlTrap(int src) {
			if (!this.expect_signalCodeTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void ERROR(String reason) {
			if (!this.expect_ERROR) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void trap(int controlLinkIdx) {
			if (!this.expect_nakedTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault(); 
		}

		@Override
		public void signalBreakTrap() {
			if (!this.expect_signalBreakTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault();
		}

		@Override
		public void signalInterruptError() {
			if (!this.expect_signalInterruptError) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault();
		}

		@Override
		public void signalProcessTrap() {
			if (!this.expect_signalProcessTrap) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault();
		}

		@Override
		public void signalRescheduleError() {
			if (!this.expect_signalRescheduleError) { fail("unexpected mesa trap or fault"); }
			if (beforeCheck != null) { beforeCheck.check(); }
			throw new MesaTrapOrFault();
		}
		
	}
	
	// the special values that can be used when preparing or verifying the evaluation stack for a test 
	protected static final int SP = 0xF0FFFF01;      // register SP is here (set or check) 
	protected static final int savedSP = 0xF0FFFF02; // register savedSP is here (set or check) 
	protected static final int any = 0xF0FFFF0F;     // the stack entry hre is irrelevant
	
	// setup the evaluation stack with the given values resp. setting the registers SP/savedSP
	protected void mkStack(int... values) {
		int newSP = -1;
		int newSavedSP = -1;
		
		short[] stack = Cpu.getStack();
		int sp = 0;
		for (int i = 0; i < values.length && sp < PrincOpsDefs.cSTACK_LENGTH; i++) {
			int v = values[i];
			if (v == SP) {
				newSP = sp;
			} else if (v == savedSP) {
				newSavedSP = sp;
			} else if (v == any) {
				sp++;
			} else {
				stack[sp++] = (short)(v & 0xFFFF); 
			}
		}
		
		if (newSP >= 0) { Cpu.SP = newSP; } else { Cpu.SP = sp; }
		if (newSavedSP >= 0) { Cpu.SP = newSavedSP; } else { Cpu.savedSP = sp; }
	}
	
	// verify that the evaluation stack has the given values resp. the registers point at the given positions
	protected void checkStack(int... values) {
		int expSP = -1;
		int expSavedSP = -1;
		
		short[] stack = Cpu.getStack();
		int sp = 0;
		for (int i = 0; i < values.length && sp < PrincOpsDefs.cSTACK_LENGTH; i++) {
			int v = values[i];
			if (v == SP) {
				expSP = sp;
			} else if (v == savedSP) {
				expSavedSP = sp;
			} else if (v == any) {
				sp++;
			} else {
				assertEquals("expected stack[" + sp +"]", (short)(v & 0xFFFF), stack[sp++]); 
			}
		}
		
		assertEquals("expected SP", (expSP >= 0) ? expSP : sp, Cpu.SP);
		if (expSavedSP >= 0) {
			assertEquals("expected savedSP", expSavedSP, Cpu.savedSP);
		}
	}
	
	// special constants when setting up fake code segments 
	protected static final int PC = 0xF0FFFF11;      // let register PC point to this position
	protected static final int savedPC = 0xF0FFFF12; // let register savedPC point to this position
	
	// setup a fake code segment with the given code bytes
	protected void mkCode(int... codeBytes) {
		Cpu.PC = 0;
		Cpu.savedPC = 0;
		
		int codeOffset = 0;
		int pc = 0;
		int w = 0;
		for (int i = 0; i < codeBytes.length; i++) {
			int b = codeBytes[i];
			if (b == PC) {
				Cpu.PC = pc;
			} else if (b == savedPC) {
				Cpu.savedPC = pc;
			} else if ((pc & 1) == 0) {
				w = b << 8;
				pc++;
			} else {
				w = w | (b & 0x00FF);
				pc++;
				Mem.writeWord(Cpu.CB + codeOffset, (short)(w & 0xFFFF));
				codeOffset++;
			}
		}
		Mem.writeWord(Cpu.CB + codeOffset, (short)(w & 0xFFFF));
	}
	
	// fill the local frame (preset register LF) with the given values
	protected void mkLocalFrame(int... locals) {
		for (int i = 0; i < locals.length; i++) {
			int l = locals[i];
			Mem.writeMDSWord(Cpu.LF, i, l);
		}
	}
	
	// verify that the local frame (current register LF) has the given values
	protected void checkLocalFrame(int... expLocals) {
		for (int i = 0; i < expLocals.length; i++) {
			int exp = expLocals[i];
			if (exp == any) { continue; }
			int actual = Mem.readMDSWord(Cpu.LF, i) & 0xFFFF;
			assertEquals("localFrame word #" + i, exp, actual);
		}
	}
	
	// fill the global frame (preset register GF16 and GF32) with the given values
	protected void mkGlobalFrame(int... globals) {
		for (int i = 0; i < globals.length; i++) {
			int g = globals[i];
			Mem.writeWord(Cpu.GF32 + i, (short)g);
		}
	}
	
	// verify that the global frame (current GF16 and GF32) has the given values
	protected void checkGlobalFrame(int... expGlobals) {
		for (int i = 0; i < expGlobals.length; i++) {
			int exp = expGlobals[i];
			if (exp == any) { continue; }
			int actual = Mem.readWord(Cpu.GF32 + i) & 0xFFFF;
			assertEquals("globalFrame word #" + i, exp, actual);
		}
	}
	
	// fill the MDS memory area pointed to by the 'testShortMem' with the given values
	protected void mkShortMem(int... values) {
		for (int i = 0; i < values.length; i++) {
			Mem.writeWord(Cpu.MDS + testShortMem + i, (short)values[i]);
		}
	}
	
	// verify the MDS memory area pointed to by the 'testShortMem' has the given values
	protected void checkShortMem(int... expValues) {
		for (int i = 0; i < expValues.length; i++) {
			int exp = expValues[i];
			if (exp == any) { continue; }
			int actual = Mem.readWord(Cpu.MDS + testShortMem + i) & 0xFFFF;
			assertEquals("shortMem word #" + i, exp, actual);
		}
	}
	
	// fill the general memory area pointed by 'testLongMem' (simulating a heap area) with the given values
	protected void mkLongMem(int... values) {
		for (int i = 0; i < values.length; i++) {
			Mem.writeWord(testLongMem + i, (short)values[i]);
		}
	}
	
	// verify that the general memory area pointed by 'testLongMem' has the given values
	protected void checkLongMem(int... expValues) {
		for (int i = 0; i < expValues.length; i++) {
			int exp = expValues[i];
			if (exp == any) { continue; }
			int actual = Mem.readWord(testLongMem + i) & 0xFFFF;
			assertEquals("longMem word #" + i, exp, actual);
		}
	}
	
	// the fault/trap handler instance replacing the "real" implementation for unittests 
	protected ChkThrower mesaException;
	
	// result of memory initialization for unit tests
	private static boolean memInitialized = false;
	private static int firstUnmappedLongPointer;
	protected static int firstUnmappedPage;
	
	// the memory area used for tests
	protected static int testShortMem;    // POINTER to a memory area in MDS outside a global or local frame
	protected static int testLongMem;     // LONG POINTER to a memory area outside the MDS
	protected static int testLongMemLow;  // low word of 'testLongMem' (for mkStack() etc.) 
	protected static int testLongMemHigh; // high word of 'testLongMem' (for mkStack() etc.)
	
	// clear a memory area starting at a LONG POINTER
	private static void clear(int lp, int count) {
		while(count-- > 0) {
			Mem.writeWord(lp++, (short)0);
		}
	}
	
	// basic register/memory preparation for a single unittest 
	private void prepareCpuCommon() {
		if (!memInitialized) {
			// setup memory for 256 kwords = 512 kbytes real memeory and 512 kwords = 1 mbyzte virtual memory 
			Mem.initializeMemory(PrincOpsDefs.MIN_REAL_ADDRESSBITS, PrincOpsDefs.MIN_REAL_ADDRESSBITS + 1);
			memInitialized = true;
			firstUnmappedLongPointer = 1 << PrincOpsDefs.MIN_REAL_ADDRESSBITS;
			firstUnmappedPage = firstUnmappedLongPointer >>> 8;
		}
		
		/*
		 * setup registers
		 */
		
		// drop values form last run
		Cpu.resetRegisters();
		
		// (absolute) long pointers (to words)
		Cpu.MDS = 128 * 1024; // MDS -> 3rd 64K block
		clear(Cpu.MDS, 64 * 1024);
		Cpu.CB = Cpu.MDS + (66 * 1024); // code block: 2K behind MDS-end
		clear(Cpu.CB, 2048);
		
		
		/*
		 * setup MDS internal tables
		 */
		
		// allocation vector: set all fsi-slots to empty and preallocate local frames as Pilot would do 
		for (int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE; i++) {
			Mem.writeMDSWord(PrincOpsDefs.mALLOCATION_VECTOR, i, PrincOpsDefs.AVITEM_EMPTY);
		}
		int f = 0x0600; // start just behind mETT, f is the raw frame pointer (i.e. points to the 1st overhead word)
		for (int fsi = 0; fsi < PilotDefs.FRAME_SIZE_MAP.length; fsi++) {
			int frameSize = PilotDefs.FRAME_SIZE_MAP[fsi];
			int frameCount = PilotDefs.FRAME_WEIGHT_MAP[fsi];
			
			for (int i = 0; i < frameCount; i++) {
				// restrictions:
				// -> 4 overhead words
				// -> overhead words (and therefore the frame itself) must be quad-word aligned
				// -> the 4 overhead words and the 4 fist frame variables must be on the same page
				f = ((f + 3) & ~0x03); // if not on quad-word address: move to the next one
				int p0 = f & 0xFFFFFF00;
				int p7 = (f + PrincOpsDefs.LOCALOVERHEAD_SIZE + 3) & 0xFFFFFF00;
				if (p0 != p7) { f = p7; }
				
				// build the frame
				int frame = f + PrincOpsDefs.LOCALOVERHEAD_SIZE;
				Mem.writeMDSWord(frame, PrincOpsDefs.LocalOverhead_word, fsi);
				Mem.writeMDSWord(frame, PrincOpsDefs.LocalOverhead_returnlink, 0);
				Mem.writeMDSWord(frame, PrincOpsDefs.LocalOverhead_globallink, 0);
				Mem.writeMDSWord(frame, PrincOpsDefs.LocalOverhead_pc, 0);
				
				// link into AV list for this fsi 
				Mem.writeMDSWord(frame, Mem.readMDSWord(PrincOpsDefs.mALLOCATION_VECTOR, fsi));
				Mem.writeMDSWord(PrincOpsDefs.mALLOCATION_VECTOR, fsi, frame);
				
				// let the next frame start after this one
				f = frame + frameSize;
			}
		}
		
		// system data table
		// TBD...
		
		// escape trap table
		// TBD...
		
		/*
		 * others
		 */
		
		// test memory addresses
		testShortMem = 32 * 1024; // somewhere between local frames and the test global frame
		testLongMem = Cpu.MDS + (96 * 1096); // 32K words to play with
		testLongMemLow = testLongMem & 0xFFFF;
		testLongMemHigh = testLongMem >>> 16;
		clear(testLongMem, 32 * 1024);
		
		// register our exception thrower object to check if the expected exceptions are thrown
		mesaException = new ChkThrower();
		Cpu.thrower = mesaException;
	}
	
	// setup specific registers for PrincOps <= 4.0 ("old-style")
	private void prepareCpuOld() {
		// "create" a global frame at the end of the MDS with almost 4k
		clear(60 * 1024, 4 * 1024);
		Cpu.GF32 = Cpu.MDS + (60 * 1024) + 32; // give 32 words overhead space
		Cpu.GF16 = Cpu.GF32 - Cpu.MDS;
	}
	
	// unittest preparation
	@Before
	public void prepareCpu() {
		this.prepareCpuCommon();
		this.prepareCpuOld(); // TBD: make this configurable to chose between old/new PrincOps...
		
		// get us an 128 word local frame for tests
		int fsi = 14;
		Cpu.LF = Mem.readMDSWord(PrincOpsDefs.mALLOCATION_VECTOR, fsi);
		Mem.writeMDSWord(PrincOpsDefs.mALLOCATION_VECTOR, fsi, Mem.readMDSWord(Cpu.LF));
	}

}
