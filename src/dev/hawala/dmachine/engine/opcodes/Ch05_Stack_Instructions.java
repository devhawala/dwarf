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

package dev.hawala.dmachine.engine.opcodes;

import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Opcodes.OpImpl;

/**
 * Implementation of instructions defined in PrincOps 4.0
 * in chapter: 5 Stack Instructions  
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch05_Stack_Instructions {
	
	private static int signExtendByte(int b) {
		if ((b & 0x0080) != 0) { return (short)(b | 0xFFFFFF00); }
		return b & 0x0000007F;
	}
	
	private static short shiftShort(short value, int shiftBy) {
		if (shiftBy == 0) { return value; }
		if (shiftBy > 0) {
			if (shiftBy < 16) {
				return (short)((value << shiftBy) & 0xFFFF);
			}
			return 0;
		}
		if (shiftBy > -16) {
			return (short)((value & 0xFFFF) >>> -shiftBy);
		}
		return 0;
	}
	
	private static int shiftLong(int value, int shiftBy) {
		if (shiftBy == 0) { return value; }
		if (shiftBy > 0) {
			if (shiftBy < 32) {
				return value << shiftBy;
			}
			return 0;
		}
		if (shiftBy > -32) {
			return value >>> -shiftBy;
		}
		return 0;
	}
	
	private static short rotateShort(short value, int by) {
		if (by == 0) { return value; }
		
		if (by > 0) {
			by = by % 16;
			int tmp = (value & 0xFFFF) << by;
			return (short)((tmp & 0xFFFF) | ((tmp >>> 16) & 0xFFFF));
		}
		
		by = (-by % 16);
		int tmp = ((value & 0xFFFF) << 16) >>> by;
		return (short)((tmp | (tmp >>> 16)) & 0xFFFF);
	}
	
	/*
	 * 5.1 Stack Primitives 
	 */
	
	// REC - Recover
	public static final OpImpl OPC_xA2_REC = () -> {
		Cpu.recover();
	};
	
	// REC2 - Recover Two
	public static final OpImpl OPC_xA3_REC2 = () -> {
		Cpu.recover();
		Cpu.recover();
	};
	
	// DIS - Discard
	public static final OpImpl OPC_xA4_DIS = () -> {
		Cpu.discard();
	};
	
	// DIS2 - Discard Two
	public static final OpImpl OPC_xA5_DIS2 = () -> {
		Cpu.discard();
		Cpu.discard();
	};
	
	// EXCH - Exchange
	public static final OpImpl OPC_xA6_EXCH = () -> {
		short v = Cpu.pop();
		short u = Cpu.pop();
		Cpu.push(v);
		Cpu.push(u);
	};
	
	// DEXCH - Double Exchange
	public static final OpImpl OPC_xA7_DEXCH = () -> {
		int v = Cpu.popLong();
		int u = Cpu.popLong();
		Cpu.pushLong(v);
		Cpu.pushLong(u);
	};
	
	// DUP - Duplicate
	public static final OpImpl OPC_xA8_DUP = () -> {
		short u = Cpu.pop();
		Cpu.push(u);
		Cpu.push(u);
	};
	
	// DDUP - Double Duplicate
	public static final OpImpl OPC_xA9_DDUP = () -> {
		int u = Cpu.popLong();
		Cpu.pushLong(u);
		Cpu.pushLong(u);
	};
	
	// EXDIS - Exchange Discard
	public static final OpImpl OPC_xAA_EXDIS = () -> {
		short u = Cpu.pop();
		/*unused*/ Cpu.pop();
		Cpu.push(u);
	};
	
	/*
	 * 5.2 Check Instructions
	 */
	
	// BNDCK - Bounds Check
	public static final OpImpl OPC_x3C_BNDCK = () -> {
		int range = Cpu.pop() & 0xFFFF;
		int index = Cpu.pop() & 0xFFFF;
		Cpu.push(index);
		if (index >= range) {
			Cpu.boundsTrap();
		}
	};
	
	// BNDCKL - Bounds Check Long
	public static final OpImpl ESC_x24_BNDCKL = () -> {
		long range = Cpu.popLong() & 0xFFFFFFFFL;
		long index = Cpu.popLong() & 0xFFFFFFFFL;
		Cpu.pushLong((int)index);
		if (index >= range) {
			Cpu.boundsTrap();
		}
	};
	
	// NILCK - Nil Check (? undocumented / forgotten ?)
	public static final OpImpl ESC_x25_NILCK = () -> {
		short pointer = Cpu.pop();
		Cpu.push(pointer);
		if (pointer == 0) {
			Cpu.pointerTrap();
		}
	};
	
	// NILCKL - Nil Check Long
	public static final OpImpl ESC_x26_NILCKL = () -> {
		int longPointer = Cpu.popLong();
		Cpu.pushLong(longPointer);
		if (longPointer == 0) {
			Cpu.pointerTrap();
		}
	};
	
	/*
	 * 5.3 Unary Operations
	 */
	
	// NEG - Negate
	public static final OpImpl OPC_xAB_NEG = () -> {
		short i = Cpu.pop();
		Cpu.push(-i);
	};
	
	// INC - Increment
	public static final OpImpl OPC_xAC_INC = () -> {
		int s = Cpu.pop() & 0xFFFF;
		Cpu.push((short)((s + 1) & 0xFFFF));
	};
	
	// DINC - Double Increment
	public static final OpImpl OPC_xAE_DINC = () -> {
		long s = Cpu.popLong() & 0xFFFFFFFFL;
		Cpu.pushLong((int)((s + 1) & 0xFFFFFFFFL));
	};
	
	// DEC - Decrement
	public static final OpImpl OPC_xAD_DEC = () -> {
		int s = Cpu.pop() & 0xFFFF;
		Cpu.push((short)((s - 1) & 0xFFFF));
	};
	
	// ADDSB - Add Signed Byte
	public static final OpImpl OPC_xB4_ADDSB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int i = Cpu.pop();
		Cpu.push((short)(i + signExtendByte(alpha)));
	};
	
	// DBL - Double
	public static final OpImpl OPC_xAF_DBL = () -> {
		short i = Cpu.pop();
		Cpu.push((short)((i * 2) & 0xFFFF));
	};
	
	// DDBL - Double Double
	public static final OpImpl OPC_xB0_DDBL = () -> {
		int i = Cpu.popLong();
		Cpu.pushLong(i * 2);
	};
	
	// TRPL - Triple
	public static final OpImpl OPC_xB1_TRPL = () -> {
		int i = Cpu.pop() & 0xFFFF;
		Cpu.push(i * 3);
	};
	
	// LINT - Lengthen Integer
	public static final OpImpl ESC_x18_LINT = () -> {
		short i = Cpu.pop();
		Cpu.push(i);
		Cpu.push((i < 0) ? (short)-1 : 0);
	};
	
	// SHIFTSB - Shift Signed Byte
	public static final OpImpl OPC_x7C_SHIFTSB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		short u = Cpu.pop();
		int shift = signExtendByte(alpha);
		if (shift < -15 || shift > 15) { Cpu.ERROR("opcode SHIFTSB :: shift < -15 || shift > 15"); }
		Cpu.push(shiftShort(u, shift));
	};
	
	/*
	 * 5.4 Logical Operations
	 */
	
	// AND - And
	public static final OpImpl OPC_xB2_AND = () -> {
		short v = Cpu.pop();
		short u = Cpu.pop();
		Cpu.push((short)(v & u));
	};
	
	// DAND - Double And
	public static final OpImpl ESC_x13_DAND = () -> {
		int v = Cpu.popLong();
		int u = Cpu.popLong();
		Cpu.pushLong(v & u);
	};
	
	// IOR - Inclusive Or
	public static final OpImpl OPC_xB3_IOR = () -> {
		short v = Cpu.pop();
		short u = Cpu.pop();
		Cpu.push((short)(v | u));
	};
	
	// DIOR - Double Inclusise Or
	public static final OpImpl ESC_x14_DIOR = () -> {
		int v = Cpu.popLong();
		int u = Cpu.popLong();
		Cpu.pushLong(v | u);
	};
	
	// XOR - Exclusive Or
	public static final OpImpl ESC_x12_XOR = () -> {
		short v = Cpu.pop();
		short u = Cpu.pop();
		Cpu.push((short)(v ^ u));
	};
	
	// DXOR - Double Exclusive Or
	public static final OpImpl ESC_x15_DXOR = () -> {
		int v = Cpu.popLong();
		int u = Cpu.popLong();
		Cpu.pushLong(v ^ u);
	};
	
	// SHIFT - Shift
	public static final OpImpl OPC_x7B_SHIFT = () -> {
		short shift = Cpu.pop();
		short u = Cpu.pop();
		Cpu.push(shiftShort(u, shift));
	};
	
	// DSHIFT - Double Shift
	public static final OpImpl ESC_x17_DSHIFT = () -> {
		short shift = Cpu.pop();
		int u = Cpu.popLong();
		Cpu.pushLong(shiftLong(u, shift));
	};
	
	// ROTATE - Rotate
	public static final OpImpl ESC_x16_ROTATE = () -> {
		short rotate = Cpu.pop();
		short u = Cpu.pop();
		Cpu.push(rotateShort(u, rotate));
	};
	
	/*
	 * 5.5 Arithmetic Operations
	 */
	
	// ADD - Add
	public static final OpImpl OPC_xB5_ADD = () -> {
		int t = Cpu.pop() & 0xFFFF;
		int s = Cpu.pop() & 0xFFFF;
		Cpu.push((short)((s + t) & 0xFFFF));
	};
	
	// SUB - Subtract
	public static final OpImpl OPC_xB6_SUB = () -> {
		int t = Cpu.pop() & 0xFFFF;
		int s = Cpu.pop() & 0xFFFF;
		Cpu.push((short)((s - t) & 0xFFFF));
	};
	
	// DADD - Double Add
	public static final OpImpl OPC_xB7_DADD = () -> {
		long t = Cpu.popLong() & 0xFFFFFFFFL;
		long s = Cpu.popLong() & 0xFFFFFFFFL;
		Cpu.pushLong((int)((s + t) & 0xFFFFFFFFL));
	};
	
	// DSUB - Double Subtract
	public static final OpImpl OPC_xB8_DSUB = () -> {
		long t = Cpu.popLong() & 0xFFFFFFFFL;
		long s = Cpu.popLong() & 0xFFFFFFFFL;
		Cpu.pushLong((int)((s - t) & 0xFFFFFFFFL));
	};
	
	// ADC - Add Double to Cardinal
	public static final OpImpl OPC_xB9_ADC = () -> {
		int t = Cpu.pop() & 0xFFFF;
		long s = Cpu.popLong() & 0xFFFFFFFFL;
		Cpu.pushLong((int)((s + t) & 0xFFFFFFFFL));
	};
	
	// ACD - Add Cardinal to Double
	public static final OpImpl OPC_xBA_ACD = () -> {
		long t = Cpu.popLong() & 0xFFFFFFFFL;
		int s = Cpu.pop() & 0xFFFF;
		Cpu.pushLong((int)((s + t) & 0xFFFFFFFFL));
	};
	
	// MUL - Multiply
	public static final OpImpl OPC_xBC_MUL = () -> {
		long t = Cpu.pop() & 0xFFFF;
		long s = Cpu.pop() & 0xFFFF;
		Cpu.pushLong((int)((s * t) & 0xFFFFFFFFL));
		Cpu.discard();
	};
	
	// DMUL - Multiply
	public static final OpImpl ESC_x30_DMUL = () -> {
		long t = Cpu.popLong() & 0xFFFFFFFFL;
		long s = Cpu.popLong() & 0xFFFFFFFFL;
		Cpu.pushLong((int)((s * t) & 0xFFFFFFFFL));
	};
	
	// SDIV - Signed Divide
	public static final OpImpl ESC_x31_SDIV = () -> {
		short k = Cpu.pop();
		short j = Cpu.pop();
		if (k == 0) {
			Cpu.divZeroTrap();
		}
		Cpu.push(j / k);
		Cpu.push(j % k);
		Cpu.discard();
	};
	
	// UDIV - Unsigned Divide
	public static final OpImpl ESC_x1C_UDIV = () -> {
		int t = Cpu.pop() & 0xFFFF;
		int s = Cpu.pop() & 0xFFFF;
		if (t == 0) {
			Cpu.divZeroTrap();
		}
		Cpu.push((short)(s / t));
		Cpu.push((short)(s % t));
		Cpu.discard();
	};
	
	// LUDIV - Long Unsigned Divide
	public static final OpImpl ESC_x1D_LUDIV = () -> {
		int t = Cpu.pop() & 0xFFFF;
		long s = Cpu.popLong() & 0xFFFFFFFFL;
		if (t == 0) {
			Cpu.divZeroTrap();
		}
		if ((s >>> 16) >= t) {
			Cpu.divCheckTrap();
		}
		Cpu.push((short)((s / t) & 0xFFFFL));
		Cpu.push((short)((s % t) & 0xFFFFL));
		Cpu.discard();
	};
	
	// SDDIV - Signed Double Divide
	public static final OpImpl ESC_x32_SDDIV = () -> {
		int k = Cpu.popLong();
		int j = Cpu.popLong();
		if (k == 0) {
			Cpu.divZeroTrap();
		}
		Cpu.pushLong(j / k);
		Cpu.pushLong(j % k);
		Cpu.discard();
		Cpu.discard();
	};
	
	// UDDIV - Unsigned Double Divide
	public static final OpImpl ESC_x33_UDDIV = () -> {
		long t = Cpu.popLong() & 0xFFFFFFFFL;
		long s = Cpu.popLong() & 0xFFFFFFFFL;
		if (t == 0) {
			Cpu.divZeroTrap();
		}
		Cpu.pushLong((int)((s / t) & 0xFFFFFFFFL));
		Cpu.pushLong((int)((s % t) & 0xFFFFFFFFL));
		Cpu.discard();
		Cpu.discard();
	};
	
	/*
	 * 5.6 Comparison Operations
	 */
	
	// DCMP - Double Compare
	public static final OpImpl OPC_xBD_DCMP = () -> {
		int k = Cpu.popLong();
		int j = Cpu.popLong();
		Cpu.push((j > k) ? (short)1 : (j < k) ? (short)-1 : (short)0);
	};
	
	// UDCMP - Unsigned Double Compare
	public static final OpImpl OPC_xBE_UDCMP = () -> {
		long t = Cpu.popLong() & 0xFFFFFFFFL;
		long s = Cpu.popLong() & 0xFFFFFFFFL;
		Cpu.push((s > t) ? (short)1 : (s < t) ? (short)-1 : (short)0);
	};
	
	/*
	 * 5.7 Floating Point Operations
	 * 
	 * => unspecified in PrincOps 4.0 except for the mere existence of such instructions
	 * 
	 * => the functionality of instructions used during BWS/GVWin startup was analyzed by comparing
	 *    the stack before and after delegating the instruction to the emulation code
	 *    in Pilot
	 *    (the analysis code is still there, commented out in below code)
	 *    
	 * => this allowed to implement the most important float instructions assuming a basic IEEE compatibility
	 *    (claimed somewhere for the REAL data type); the experiences gained when implementing the
	 *    float instructions for ST80 (Smalltalk-80 virtual machine by the same author) were helpful here
	 *    
	 * => implemented instructions:
	 *    - FADD - Floating Point Add
	 *    - FSUB - Floating Point Subtract
	 *    - FMUL - Floating Point Multiply
	 *    - FDIV - Floating Point Divide
	 *    - FCOMP - Floating Point Compare
	 *    - FLOAT - Floating Point convert to float
	 *    
	 * => instructions not (or only seldomly) used during a BWS/GVWin startup are not implemented, as
	 *    no or too few comparison samples were availably
	 * 
	 * => instructions not implemented are delegated to Cpu.thrower.signalEscOpcodeTrap()
	 *    to suppress the "unimplemented" log line issued by the opcode dispatcher if
	 *    no instruction implementation is present (this will ultimately invoke the emulation
	 *    in Pilot for float instructions)
	 */
	
	private static float popFloat() {
		int floatRepr = Cpu.popLong();
		float value = Float.intBitsToFloat(floatRepr);
		return value;
	}
	
	private static void pushFloat(float value) {
		int floatRepr = Float.floatToRawIntBits(value);
		Cpu.pushLong(floatRepr);
	}
	
	// FADD - Floating Point Add
	public static final OpImpl ESC_x40_FADD = () -> {
		float t = popFloat();
		float s = popFloat();
		float result = s + t;
		pushFloat(result);
		
//		Cpu.recover();
//		Cpu.recover();
//		Cpu.recover();
//		Cpu.recover();
//		checkFloatOp(0x40, "ESC_x40_FADD",
//				() -> { System.out.printf("=== fADD    %f + %f => %f\n", s, t, result); System.out.printf("   -> s = %f\n   -> t = %f\n", peekFloat(2), peekFloat(0)); },
//				() -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
//		Cpu.thrower.signalEscOpcodeTrap(0x40);
	};
	
	// FSUB - Floating Point Subtract
	public static final OpImpl ESC_x41_FSUB = () -> {
		float t = popFloat();
		float s = popFloat();
		float result = s - t;
		pushFloat(result);
		
//		Cpu.recover();
//		Cpu.recover();
//		Cpu.recover();
//		Cpu.recover();
//		checkFloatOp(0x41, "ESC_x41_FSUB",
//				() -> { System.out.printf("=== fSUB    %f - %f => %f\n", s, t, result); System.out.printf("   -> s = %f\n   -> t = %f\n", peekFloat(2), peekFloat(0)); },
//				() -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
//		Cpu.thrower.signalEscOpcodeTrap(0x41);
	};
	
	// FMUL - Floating Point Multiply
	public static final OpImpl ESC_x42_FMUL = () -> {
		float t = popFloat();
		float s = popFloat();
		float result = s * t;
		pushFloat(result);
		
//		Cpu.recover();
//		Cpu.recover();
//		Cpu.recover();
//		Cpu.recover();
//		checkFloatOp(0x42, "ESC_x42_FMUL",
//				() -> { System.out.printf("=== fMUL    %f * %f => %f\n", s, t, result); System.out.printf("   -> s = %f\n   -> t = %f\n", peekFloat(2), peekFloat(0)); },
//				() -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
//		Cpu.thrower.signalEscOpcodeTrap(0x42);
	};
	
	// FDIV - Floating Point Divide
	public static final OpImpl ESC_x43_FDIV = () -> {
		float t = popFloat();
		float s = popFloat();
		if (t == 0.0f) {
			Cpu.divZeroTrap();
		}
		float result = s / t;
		pushFloat(result);
		
//		Cpu.recover();
//		Cpu.recover();
//		Cpu.recover();
//		Cpu.recover();
//		checkFloatOp(0x43, "ESC_x43_FDIV",
//				() -> { System.out.printf("=== fDIV    %f / %f => %f\n", s, t, result); System.out.printf("   -> s = %f\n   -> t = %f\n", peekFloat(2), peekFloat(0)); },
//				() -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
//		Cpu.thrower.signalEscOpcodeTrap(0x43);
	};
	
	// FCOMP - Floating Point Compare
	public static final OpImpl ESC_x44_FCOMP = () -> { // !!!! nicht exakt, offenbar "gleich" wenn Abstand < 0.005 ??
		float t = popFloat();
		float s = popFloat();
		int result = (s > t) ? 1 : (s == t) ? 0 : -1;
		Cpu.push((short)result);

//		Cpu.recover();
//		Cpu.recover();
//		Cpu.recover();
//		Cpu.recover();
//		checkFloatOp(0x44, "ESC_x44_FCOMP",
//				() -> { System.out.printf("=== fCOMP   %f <-> %f => %d\n", s, t, result); System.out.printf("   -> s = %f\n   -> t = %f\n", peekFloat(2), peekFloat(0)); },
//				() -> { System.out.printf("   -> r = %d\n", (short)peekWord(0)); if ((short)peekWord(0) != result) { System.out.printf(" # # # # # # # # # # # # # # # # # # # # # # # # # # DEVIATION !!!\n"); } }
//				);
//		Cpu.thrower.signalEscOpcodeTrap(0x44);
	};
	
	// [not implemented] FIX - Floating Point Fix?
	public static final OpImpl ESC_x45_FIX = () -> {
		// !!!! seems to be float=>integer(to-zero)
//		checkFloatOp(0x45, "ESC_x45_FIX",
//				() -> System.out.printf("   -> f = %f\n", peekFloat(0)),
//				() -> System.out.printf("   -> r = %d\n", peekDWord(0))
//				);
		Cpu.thrower.signalEscOpcodeTrap(0x45);
	};
	
	// FLOAT - Floating Point convert to float
	public static final OpImpl ESC_x46_FLOAT = () -> {
		int s = Cpu.popLong();
		float result = (float)s;
		pushFloat(result);
		
		// LONG INTEGER => FLOAT
//		Cpu.recover();
//		Cpu.recover();
//		checkFloatOp(0x46, "ESC_x46_FLOAT",
//				() -> { System.out.printf("=== fFLOAT  %d => %f\n", s, result); System.out.printf("   -> i = %d\n", peekDWord(0)); },
//				() -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
//		Cpu.thrower.signalEscOpcodeTrap(0x46);
	};
	
	// [not implemented] FIXI - Floating Point convert to integer?
	public static final OpImpl ESC_x47_FIXI = () -> {
		// not used ???
//		checkFloatOp(0x47, "ESC_x47_FIXI",
//				null,// () -> System.out.printf("   -> i = %d\n", peekDWord(0)),
//				null // () -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
		Cpu.thrower.signalEscOpcodeTrap(0x47);
	};
	
	// [not implemented] FIXC - Floating Point convert to cardinal?
	public static final OpImpl ESC_x48_FIXC = () -> {
		// not used ???
//		checkFloatOp(0x48, "ESC_x48_FIXC",
//				null,// () -> System.out.printf("   -> i = %d\n", peekDWord(0)),
//				null // () -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
		Cpu.thrower.signalEscOpcodeTrap(0x48);
	};
	
	// [not implemented] FSTICKY - Floating Point (re)set sticky bit?
	public static final OpImpl ESC_x49_FSTICKY = () -> {
		// not used ???
//		checkFloatOp(0x49, "ESC_x49_FSTICKY",
//				null,// () -> System.out.printf("   -> i = %d\n", peekDWord(0)),
//				null // () -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
		Cpu.thrower.signalEscOpcodeTrap(0x49);
	};
	
	// [not implemented] FREM - Floating Point Remainder?
	public static final OpImpl ESC_x4A_FREM = () -> {
		// not used ???
//		checkFloatOp(0x4A, "ESC_x4A_FREM",
//				null,// () -> System.out.printf("   -> i = %d\n", peekDWord(0)),
//				null // () -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
		Cpu.thrower.signalEscOpcodeTrap(0x4A);
	};
	
	// [not implemented] FROUND - Floating Point Round
	public static final OpImpl ESC_x4B_FROUND = () -> {
		// not used, rounding, FLOAT => ?? 
//		checkFloatOp(0x4B, "ESC_x4B_FROUND",
//				() -> System.out.printf("   -> f = %f\n", peekFloat(0)),
//				null // () -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
		Cpu.thrower.signalEscOpcodeTrap(0x4B);
	};
	
	// [not implemented] FROUNDI - Floating Point Round to integer (handling on overflow??)
	public static final OpImpl ESC_x4C_FROUNDI = () -> {
		// not used ??
//		checkFloatOp(0x4C, "ESC_x4C_FROUNDI",
//				() -> System.out.printf("   -> f = %f\n", peekFloat(0)),
//				() -> System.out.printf("   -> r = %d\n", (short)peekWord(0))
//				);
		Cpu.thrower.signalEscOpcodeTrap(0x4C);
	};
	
	// FROUNDC - Floating Point Round to cardinal?
	public static final OpImpl ESC_x4D_FROUNDC = () -> {
		// not used ??
//		checkFloatOp(0x4D, "ESC_x4D_FROUNDC",
//				() -> System.out.printf("   -> f = %f\n", peekFloat(0)),
//				() -> System.out.printf("   -> r = %d\n", peekWord(0))
//				);
		Cpu.thrower.signalEscOpcodeTrap(0x4D);
	};
	
	// [not implemented] FSQRT - Floating Point Square Root
	public static final OpImpl ESC_x4E_FSQRT = () -> {
		// not used ??
//		checkFloatOp(0x4E, "ESC_x4E_FSQRT",
//				() -> System.out.printf("   -> f = %f\n", peekFloat(0)),
//				() -> System.out.printf("   -> r = %f\n", peekFloat(0))
//				);
		Cpu.thrower.signalEscOpcodeTrap(0x4E);
	};
	
	// FSC - Floating Point SC??
	public static final OpImpl ESC_x4F_FSC = () -> {
		// what does it really do ?? (used once: Input = NaN !!??)
//		checkFloatOp(0x4F, "ESC_x4F_FSC",
//				() -> System.out.printf("   -> f = %f\n", peekFloat(0)),
//				() -> System.out.printf("   -> r = %d\n", peekWord(0))
//				);
		Cpu.thrower.signalEscOpcodeTrap(0x4F);
	};
	
	/*
	 * ## test code for float implementations
	 */
	
	private static long fStartInsns = 0;
	private static int fSavedCB = 0;
	private static int fSavedPC = 0;
	private static int fSavedCodeByte = 0;
	private static OpImpl fPostStackEvaluator = null;
	
	private static int fCount = 0;
	private static long fTotalInsns = 0;
	
	/**
	 * Do the 1st phase of a float instruction analysis, replacing the next instruction
	 * after the analyzed float instruction with our specific 2nd phase instruction.
	 * 
	 * @param code float (ESC) instruction code which is analyzed
	 * @param name the name of the analyzed instruction
	 * @param preStackEvaluator callback to execute before Pilot's emulation of the instruction is executed
	 * @param postStackEvaluator callback to execute after Pilot's emulation of the instruction is executed
	 */
	private static void checkFloatOp(int code, String name, OpImpl preStackEvaluator, OpImpl postStackEvaluator) {
		// tell about it
		fCount++;
		System.out.printf("## %s # %d\n", name, fCount);
		
		// dump current evaluation stack
		System.out.printf("-- insns: %9d  pre-Ersatz Stack [%02d] :: 0x", Cpu.insns, Cpu.SP);
		for (int i = 0; i < Cpu.SP; i++) {
			System.out.printf(" %04X", Cpu.getStack()[i]);
		}
		System.out.printf("\n");
		if (preStackEvaluator != null) {
			preStackEvaluator.execute();
		}
		
		// patch code location for getting the results
		fStartInsns = Cpu.insns;
		fSavedCB = Cpu.CB;
		fSavedPC = Cpu.PC;
		fSavedCodeByte = Mem.getCodeByte(fSavedCB, fSavedPC);
		fPostStackEvaluator = postStackEvaluator;
		Mem.patchCodeByte(fSavedCB, fSavedPC, 0xFE);
		
		// dispatch to software emulation
		Cpu.thrower.signalEscOpcodeTrap(code);
	}
	
	/**
	 * (Dwarf-specific) Debug instruction implementing the 2nd phase of a float
	 * instruction analysis, proceeding with the instruction initially located
	 * directly after the float instruction analyzed.
	 */
	public static final OpImpl OPC_xFE_RestoreAfterFloatOp = () -> {
		// sanity check
		if (Cpu.savedPC != fSavedPC || Cpu.CB != fSavedCB) {
			System.out.printf(
					"## ERROR: Float-Ersatz did not continue at expected place - expected 0x%06X + 0x%04X  -- actual: 0x%06X + 0x%04X\n",
					fSavedCB, fSavedPC,
					Cpu.CB, Cpu.savedPC);
			throw new IllegalStateException("Float-Ersatz did not continue at expected place");
		}
		
		// dump current evaluation stack
		System.out.printf("-- insns: %9d post-Ersatz Stack [%02d] :: 0x", Cpu.insns, Cpu.SP);
		for (int i = 0; i < Cpu.SP; i++) {
			System.out.printf(" %04X", Cpu.getStack()[i]);
		}
		long instrCount = Cpu.insns - fStartInsns; // don't count this substitute instruction, so no "+ 1"
		fTotalInsns += instrCount;
		System.out.printf("\n-- #insns -> %d -- total: %d\n", instrCount, fTotalInsns);
		if (fPostStackEvaluator != null) {
			fPostStackEvaluator.execute();
		}
		
		// restore initial instruction and re-execute at this code location
		Mem.patchCodeByte(fSavedCB, fSavedPC, fSavedCodeByte);
		Cpu.PC = fSavedPC;
	};
	
	private static int peekWord(int stackOffset) {
		int stackPos = Cpu.SP - 1 - stackOffset;
		return Cpu.getStack()[stackPos] & 0xFFFF;
	}
	
	private static int peekDWord(int stackOffset) {
		return (peekWord(stackOffset) << 16) | peekWord(stackOffset + 1);
	}
	
	private static float peekFloat(int stackOffset) {
		int floatRepr = peekDWord(stackOffset);
		float value = Float.intBitsToFloat(floatRepr);
		return value;
	}
	
}
