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

import org.junit.Test;

import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Cpu.MesaAbort;
import dev.hawala.dmachine.engine.Cpu.MesaERROR;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Opcodes;
import dev.hawala.dmachine.engine.Processes;

/**
 * Unittests for checking the concept of timeout check throttling
 * (as well as a little performance measuring).
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Misc_Tests extends AbstractInstructionTest {

	@Test
	public void test_SampleCode() {
		// prepare global frame
		mkGlobalFrame(      // offset / content
			0x1234,			// [0] parameter for 2.
			0,				// [1] unused
			testLongMemLow, // [2] address of ...
			testLongMemHigh // [3] long-pointer storage
		);
		
		// prepare local frame
		mkLocalFrame(       // offset / content
			0x1122,			// [0] dbl-data for 5. (low-word)
			0x3344,			// [1] dbl-data for 5. (high-word)
			0x0000,			// [2] target for 4.
			testShortMem	// [3] address of (short-)pointer storage
		);
		
		// prepare short pointer memory
		mkShortMem(         // offset / content
			0x0000,			// [0] unused
			0x0000,			// [1] unused
			0x0000,			// [2] unused
			0x0000,			// [3] unused
			0x5566,			// [4] dbl-data for 6. (low-word)
			0x7788			// [5] dbl-data for 6. (high-word)
		);
		
		// prepare long pointer memory
		mkLongMem(          // offset / content
			0x4321			// [0] parameter for 1.
		);
		
		// hand-crafted loop adding 2 shorts and 2 longs from varying locations in memory
		mkCode(
			// 0. starting point for execution
			PC,
			
			// 1. load word from long pointer [0] using address in global frame[2] 
			0x39,		// LGD2 - Load Global Double 2 					:: get long-pointer from global frame at offset 2
			0x43,		// RL0 - Read Long 0 							:: dereference long-pointer with offset 0
				
			// 2. load word from global frame [0]
			0x34, 		// LG0 - Load Global 0 							:: get word form global frame at offset 0
				
			// 3. add words
			0xB5,		// ADD - Add 									:: word-add stacked values
				
			// 4. store word in local frame [2]
			0x1B,		// SL2 - Store Local 2 							:: store stacked result-word in local frame at offset 2 
				
			// 5. load dbl-word from local frame [0]
			0x0E, 		// LLD0 - Load Local Double 0 					:: load long-value from local frame at offset 0
				
			// 6. load dbl-word from short pointer [4]
			0x04,		// LL3 - Load  Local 3							:: load short address from local frame (offset 3)
			0x46, 0x04, // RDB - Read Double  Byte , alpha = 4			:: load long-value through this pointer with offset 4
				
			// 7. subtract
			0xB8,		// DSUB - Double Subtract 						::subtract long-values
				
			// 8 store dbl-word in long pointer [2] 
			0x39,		// LGD2 - Load Global Double 2 					:: get long-pointer from global frame at offset 2
			0x51, 0x02,	// WDLB - Write Double Long Byte , alpha = 2	:: store long value at long-pointer + 2 
				
			// 9. jump to 1. (offset = -13 = code bytes so far)
			0x88, -13	// JB - Jump Byte , offset = -13 				:: jump back to start 
		);
		
		// initialize the engine to use PrincOps 4.0 instruction (registers are already reset by @Before)
		Opcodes.initializeInstructionsPrincOps40();
		
		// run the loop a defined number of times repeatedly
		final int sleepTime = 40; // 40 milliseconds, give the Java JIT a chance to (re)compile to native
		runLoop();
		sleep(sleepTime);
		runLoop();
		sleep(sleepTime);
		runLoop();
		sleep(sleepTime);
		runLoop();
		sleep(sleepTime);
		runLoop();
		sleep(sleepTime);
		runLoop();
	}
	
	private void sleep(int amount) {
		try {
			Thread.sleep(amount);
		} catch(InterruptedException ie) {
			// ignored
		}
	}
	
	/*
	 * copy of the main interpreter loop (from Cpu) with initial implementation of timeout check throttling
	 */
	
	private static final int timeoutThrottleCount = 32 * 1024;
	/*
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
	 * 2.4 GHz CoreDuo2:
	 *  32768 gives at the very least 20 Mips
	 *   => ~50 nanosecs per instruction
	 *   => timeout check interval ~ 1,7 millisecs
	 *   => better than cTickMin!
	 */
	
	private void runLoop() {
		// simulated code interpreter loop
		final int loopInstructions = 12;
		final int loopCount = 1_000_000;
		final int maxInstructions = loopInstructions * loopCount;
		int count = 0;
		int startPC = Cpu.PC;
		long startMs = System.currentTimeMillis();
		int timeoutCountDown = timeoutThrottleCount;
		try {
			while(count < maxInstructions) {
				try {
					boolean interrupt = Processes.checkforInterrupts();
					boolean timeout = false;
					if (timeoutCountDown < 1) {
						timeout = Processes.checkForTimeouts();
						timeoutCountDown = timeoutThrottleCount; 
					} else {
						timeoutCountDown--;
					}
					if (interrupt || timeout) {
						fail("Simulated interpreter loop should not get interrupts or timeouts");
					} else if (Cpu.running) {
						// execute();
						count++;
						Cpu.savedPC = Cpu.PC;
						Cpu.savedSP = Cpu.SP;
						Opcodes.dispatch(Mem.getNextCodeByte());
					} else {
						fail("Simulated interpreter loop should not stop running");
					}
				} catch (MesaAbort ma) {
					fail("Simulated interpreter loop should not get an MesaAbort");;
				}
			}
		} catch (MesaERROR me) {
			fail("unexpected MesaERROR");
		} catch (RuntimeException re) {
			re.printStackTrace();
			fail("Unexpected RuntimeException: " + re);
		}
		long endMs = System.currentTimeMillis();
		assertEquals("PC after all loops", startPC, Cpu.PC);
		
		long runtime = endMs + 1 - startMs;
		System.out.printf("\n** time elapsed for %d instructions : %d millisecs => %d insns/sec\n",
				maxInstructions, runtime, (maxInstructions * 1000L) / runtime);
	}
	
}
