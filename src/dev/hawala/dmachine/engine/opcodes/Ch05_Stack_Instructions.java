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
	 * => these instructions are not implemented here, but emulated in (mesa) software
	 * 
	 * => but: we delegate known instructions directly to Cpu.thrower.signalEscOpcodeTrap()
	 *    to suppress the "unimplemented" log line issued by the opcode dispatcher if
	 *    no instruction implementation is present
	 */
	
	// FADD - Floating Point Add
	public static final OpImpl ESC_x40_FADD = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x40);
	};
	
	// FSUB - Floating Point Subtract
	public static final OpImpl ESC_x41_FSUB = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x41);
	};
	
	// FMUL - Floating Point Multiply
	public static final OpImpl ESC_x42_FMUL = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x42);
	};
	
	// FDIV - Floating Point Divide
	public static final OpImpl ESC_x43_FDIV = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x43);
	};
	
	// FCOMP - Floating Point Compare
	public static final OpImpl ESC_x44_FCOMP = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x44);
	};
	
	// FIX - Floating Point Fix?
	public static final OpImpl ESC_x45_FIX = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x45);
	};
	
	// FLOAT - Floating Point convert to float? 
	public static final OpImpl ESC_x46_FLOAT = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x46);
	};
	
	// FIXI - Floating Point convert to integer?
	public static final OpImpl ESC_x47_FIXI = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x47);
	};
	
	// FIXC - Floating Point convert to cardinal?
	public static final OpImpl ESC_x48_FIXC = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x48);
	};
	
	// FSTICKY - Floating Point (re)set sticky bit?
	public static final OpImpl ESC_x49_FSTICKY = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x49);
	};
	
	// FREM - Floating Point Remainder?
	public static final OpImpl ESC_x4A_FREM = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x4A);
	};
	
	// FROUND - Floating Point Round?
	public static final OpImpl ESC_x4B_FROUND = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x4B);
	};
	
	// FROUNDI - Floating Point Round to integer?
	public static final OpImpl ESC_x4C_FROUNDI = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x4C);
	};
	
	// FROUNDC - Floating Point Round to cardinal?
	public static final OpImpl ESC_x4D_FROUNDC = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x4D);
	};
	
	// FSQRT - Floating Point Square Root
	public static final OpImpl ESC_x4E_FSQRT = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x4E);
	};
	
	// FSC - Floating Point SC??
	public static final OpImpl ESC_x4F_FSC = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x4F);
	};
	
}
