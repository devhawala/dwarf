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
 * in chapter: 6 Jump Instructions
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch06_Jump_Instructions {
	
	private static int signExtendByte(int b) {
		if ((b & 0x0080) != 0) { return (short)(b | 0xFFFFFF00); }
		return b & 0x0000007F;
	}
	
	private static int signExtendWord(int b) {
		int result;
		if ((b & 0x8000) != 0) {
			result = b | 0xFFFF0000;
		} else {
			result = b & 0x00007FFF;
		}
		return result;
	}
	
	/*
	 * 6.1 Unconditional Jumps 
	 */
	
	// J2 - Jump 2
	public static final OpImpl OPC_x81_J2 = () -> {
		Cpu.PC = (Cpu.savedPC + 2) & 0xFFFF;
	};
	
	// J3 - Jump 3
	public static final OpImpl OPC_x82_J3 = () -> {
		Cpu.PC = (Cpu.savedPC + 3) & 0xFFFF;
	};
	
	// J4 - Jump 4
	public static final OpImpl OPC_x83_J4 = () -> {
		Cpu.PC = (Cpu.savedPC + 4) & 0xFFFF;
	};
	
	// J5 - Jump 5
	public static final OpImpl OPC_x84_J5 = () -> {
		Cpu.PC = (Cpu.savedPC + 5) & 0xFFFF;
	};
	
	// J6 - Jump 6
	public static final OpImpl OPC_x85_J6 = () -> {
		Cpu.PC = (Cpu.savedPC + 6) & 0xFFFF;
	};
	
	// J7 - Jump 7
	public static final OpImpl OPC_x86_J7 = () -> {
		Cpu.PC = (Cpu.savedPC + 7) & 0xFFFF;
	};
	
	// J8 - Jump 8
	public static final OpImpl OPC_x87_J8 = () -> {
		Cpu.PC = (Cpu.savedPC + 8) & 0xFFFF;
	};
	
	// JB - Jump Byte
	public static final OpImpl OPC_x88_JB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF;
	};
	
	// JW - Jump Word
	public static final OpImpl OPC_x89_JW_sword = () -> {
		int disp = Mem.getNextCodeWord();
		Cpu.PC = (Cpu.savedPC + signExtendWord(disp)) & 0xFFFF;
	};
	
	// JS - Jump Stack
	public static final OpImpl ESC_x19_JS = () -> {
		Cpu.PC = Cpu.pop() & 0xFFFF;
	};
	
	// CATCH - Catch
	public static final OpImpl OPC_x80_CATCH_alpha = () -> {
		/*int alpha =*/ Mem.getNextCodeByte();
	};
	
	/*
	 * 6.2 Equality Jumps
	 */
	
	// JZ3 - Jump Zero 3
	public static final OpImpl OPC_x98_JZ3 = () -> {
		short u = Cpu.pop();
		if (u == 0) { Cpu.PC = (Cpu.savedPC + 3) & 0xFFFF; }
	};
	
	// JZ4 - Jump Zero 4
	public static final OpImpl OPC_x99_JZ4 = () -> {
		short u = Cpu.pop();
		if (u == 0) { Cpu.PC = (Cpu.savedPC + 4) & 0xFFFF; }
	};
	
	// JNZ3 - Jump Not Zero 3
	public static final OpImpl OPC_x9B_JNZ3 = () -> {
		short u = Cpu.pop();
		if (u != 0) { Cpu.PC = (Cpu.savedPC + 3) & 0xFFFF; }
	};
	
	// JNZ4 - Jump Not Zero 4
	public static final OpImpl OPC_x9C_JNZ4 = () -> {
		short u = Cpu.pop();
		if (u != 0) { Cpu.PC = (Cpu.savedPC + 4) & 0xFFFF; }
	};
	
	// JZB - Jump Zero Byte
	public static final OpImpl OPC_x9A_JZB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		short data = Cpu.pop();
		if (data == 0) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JNZB - Jump Not Zero Byte
	public static final OpImpl OPC_x9D_JNZB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		short data = Cpu.pop();
		if (data != 0) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JEB - Jump Equal Byte
	public static final OpImpl OPC_x8B_JEB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		short v = Cpu.pop();
		short u = Cpu.pop();
		if (u == v) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JNEB - Jump Not Equal Byte
	public static final OpImpl OPC_x8E_JNEB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		short v = Cpu.pop();
		short u = Cpu.pop();
		if (u != v) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JDEB - Jump Double Equal Byte
	public static final OpImpl OPC_x9E_JDEB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		int v = Cpu.popLong();
		int u = Cpu.popLong();
		if (u == v) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JDNEB - Jump Double Not Equal Byte
	public static final OpImpl OPC_x9F_JDNEB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		int v = Cpu.popLong();
		int u = Cpu.popLong();
		if (u != v) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JEP - Jump Equal Pair
	public static final OpImpl OPC_x8A_JEP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		int data = Cpu.pop() & 0xFFFF;
		if (data == (pair >>> 4)) { Cpu.PC = (Cpu.savedPC + (pair & 0x0F) + 4) & 0xFFFF; }
	};
	
	// JNEP - Jump Not Equal Pair
	public static final OpImpl OPC_x8D_JNEP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		int data = Cpu.pop() & 0xFFFF;
		if (data != (pair >>> 4)) { Cpu.PC = (Cpu.savedPC + (pair & 0x0F) + 4) & 0xFFFF; }
	};
	
	// JEBB - Jump Equal Byte Byte
	public static final OpImpl OPC_x8C_JEBB_alphasbeta = () -> {
		int b = Mem.getNextCodeByte();
		int disp = Mem.getNextCodeByte();
		int data = Cpu.pop() & 0xFFFF;
		if (data == b) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JNEBB - Jump Not Equal Byte Byte
	public static final OpImpl OPC_x8F_JNEBB_alphasbeta = () -> {
		int b = Mem.getNextCodeByte();
		int disp = Mem.getNextCodeByte();
		int data = Cpu.pop() & 0xFFFF;
		if (data != b) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	/*
	 * 6.3 Signed Jumps
	 */
	
	// JLB - Jump Less Byte
	public static final OpImpl OPC_x90_JLB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		short k = Cpu.pop();
		short j = Cpu.pop();
		if (j < k) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JLEB - Jump Less Equal Byte
	public static final OpImpl OPC_x93_JLEB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		short k = Cpu.pop();
		short j = Cpu.pop();
		if (j <= k) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JGB - Jump Greater Byte
	public static final OpImpl OPC_x92_JGB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		short k = Cpu.pop();
		short j = Cpu.pop();
		if (j > k) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JGEB - Jump Greater Equal Byte
	public static final OpImpl OPC_x91_JGEB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		short k = Cpu.pop();
		short j = Cpu.pop();
		if (j >= k) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	/*
	 * 6.4 Unsigned Jumps
	 */
	
	// JULB - Jump Unsigned Less Byte
	public static final OpImpl OPC_x94_JULB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		int v = Cpu.pop() & 0xFFFF;
		int u = Cpu.pop() & 0xFFFF;
		if (u < v) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JULEB - Jump Unsigned Less Byte
	public static final OpImpl OPC_x97_JULEB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		int v = Cpu.pop() & 0xFFFF;
		int u = Cpu.pop() & 0xFFFF;
		if (u <= v) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JUGB - Jump Unsigned Greater Byte
	public static final OpImpl OPC_x96_JUGB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		int v = Cpu.pop() & 0xFFFF;
		int u = Cpu.pop() & 0xFFFF;
		if (u > v) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	// JUGEB - Jump Unsigned Greater Equal Byte
	public static final OpImpl OPC_x95_JUGEB_salpha = () -> {
		int disp = Mem.getNextCodeByte();
		int v = Cpu.pop() & 0xFFFF;
		int u = Cpu.pop() & 0xFFFF;
		if (u >= v) { Cpu.PC = (Cpu.savedPC + signExtendByte(disp)) & 0xFFFF; }
	};
	
	/*
	 * 6.5 Indexed Jumps
	 */
	
	// JIB - Jump Indexed Byte
	public static final OpImpl OPC_xA0_JIB_word = () -> {
		int base = Mem.getNextCodeWord() & 0xFFFF;
		int limit = Cpu.pop() & 0xFFFF;
		int index = Cpu.pop() & 0xFFFF;
		if (index < limit) {
			int dispPair = Mem.readCode(base + (index/2)) & 0xFFFF;
			int offset = ((index % 2) == 0) ? (dispPair >>> 8) : (dispPair & 0x00FF);
			Cpu.PC = (Cpu.savedPC + offset) & 0xFFFF;
		}
	};
	
	// JIW - Jump Indexed Word
	public static final OpImpl OPC_xA1_JIW_word = () -> {
		int base = Mem.getNextCodeWord() & 0xFFFF;
		int limit = Cpu.pop() & 0xFFFF;
		int index = Cpu.pop() & 0xFFFF;
		if (index < limit) {
			int disp = Mem.readCode(base + index) & 0xFFFF;
			Cpu.PC = (Cpu.savedPC + disp) & 0xFFFF;
		}
	};

}
