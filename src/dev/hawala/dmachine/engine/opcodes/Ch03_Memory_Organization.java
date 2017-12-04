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
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;

/**
 * Implementation of instructions defined in PrincOps 4.0
 * in chapter: 3 Memory Organization.  
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch03_Memory_Organization {

	/*
	 * 3.1.2 Memory Map Instructions 
	 */
	
	// SM - Set Map
	public static final OpImpl ESC_x07_SM = () -> {
		short mf = Cpu.pop();
		int rp = Cpu.popLong();
		int vp = Cpu.popLong();
		
		Mem.setMap("SM", vp, rp, mf);
	};
	
	// GMF - Get Map Flags
	public static final OpImpl ESC_x09_GMF = () -> {
		int vp = Cpu.popLong();
		short mf = Mem.getVPageFlags(vp);
		int rp = Mem.getVPageRealPage(vp);
		
		Cpu.push(mf);
		Cpu.pushLong(rp);
	};
	
	// SMF - Set Map Flags
	public static final OpImpl ESC_x08_SMF = () -> {
		short newMf = Cpu.pop();
		int vp = Cpu.popLong();
		short mf = Mem.getVPageFlags(vp);
		int rp = Mem.getVPageRealPage(vp);
		
		Cpu.push(mf);
		Cpu.pushLong(rp);
		if (!Mem.isVacant(mf)) {
			Cpu.logf("   SMF -> setMap(vp, rp, newMf)\n");
			Mem.setMap("SMF", vp, rp, newMf);
		}
	};
	
	/*
	 * 3.2.1 Main Data Space Access
	 */
	
	// LP - Lengthen Pointer
	public static final OpImpl OPC_xF7_LP = () -> {
		short ptr = Cpu.pop();
		Cpu.pushLong((ptr == 0) ? 0 : Cpu.lengthenPointer(ptr));
	};
	
	/*
	 * 3.2.3 Frame Overhead Access
	 */
	
	// ROB - Read Overhead Byte
	public static final OpImpl ESC_x1E_ROB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int ptr = Cpu.pop() & 0xFFFF;
		if (alpha < 1 || alpha > 4) { Cpu.ERROR("ROB :: invalid alpha = " + alpha); }
		Cpu.push(Mem.readMDSWord(ptr - alpha));
	};
	
	// WOB - Write Overhead Byte
	public static final OpImpl ESC_x1F_WOB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int ptr = Cpu.pop() & 0xFFFF;
		if (alpha < 1 || alpha > 4) { Cpu.ERROR("WOB :: invalid alpha = " + alpha); }
		Mem.writeMDSWord(ptr - alpha, Cpu.pop());
	};
	
	/*
	 * 3.3.4 Register Instructions
	 */
	
	// RRIT - Read Register IT
	public static final OpImpl ESC_x7D_RRIT = () -> {
		Cpu.pushLong(Cpu.IT());
	};
	
	// RRMDS - Read Register MDS
	public static final OpImpl ESC_x79_RRMDS = () -> {
		Cpu.push(Cpu.MDS >>> PrincOpsDefs.WORD_BITS);
	};
	
	// RRPSB - Read Register PSB
	public static final OpImpl ESC_x78_RRPSB = () -> {
		Cpu.push(Processes.psbHandle(Cpu.PSB));
	};
	
	// RRPTC - Read Register PTC
	public static final OpImpl ESC_x7C_RRPTC = () -> {
		Cpu.push(Cpu.PTC);
	};
	
	// RRWDC - Read Register WDC
	public static final OpImpl ESC_x7B_RRWDC = () -> {
		Cpu.push(Cpu.WDC);
	};
	
	// RRWP - Read Register WP
	public static final OpImpl ESC_x7A_RRWP = () -> {
		Cpu.push(Cpu.WP.get());
	};
	
	// RRXTS - Read Register XTS
	public static final OpImpl ESC_x7E_RRXTS = () -> {
		Cpu.push(Cpu.XTS);
	};
	
	// WRIT - Write Register IT
	public static final OpImpl ESC_x75_WRIT = () -> {
		Cpu.setIT(Cpu.popLong());
	};
	
	// WRMDS - Write Register MDS
	public static final OpImpl ESC_x71_WRMDS = () -> {
		Cpu.MDS = Cpu.pop() << PrincOpsDefs.WORD_BITS;
	};
	
	// WRMP - Write Register MP
	public static final OpImpl ESC_x77_WRMP = () -> {
		Cpu.setMP(Cpu.pop());
	};
	
	// WRPSB - Write Register PSB
	public static final OpImpl ESC_x70_WRPSB = () -> {
		Cpu.PSB = Processes.psbIndex(Cpu.pop());
	};
	
	// WRPTC - Write Register PTC
	public static final OpImpl ESC_x74_WRPTC = () -> {
		Processes.resetPTC(Cpu.pop() & 0xFFFF);
	};
	
	// WRWDC - Write Register WDC
	public static final OpImpl ESC_x73_WRWDC = () -> {
		Cpu.WDC = Cpu.pop();
	};
	
	// WRWP - Write Register WP
	public static final OpImpl ESC_x72_WRWP = () -> {
		Cpu.WP.set(Cpu.pop() & 0xFFFF);
	};
	
	// WRXTS - Write Register XTS
	public static final OpImpl ESC_x76_WRXTS = () -> {
		Cpu.XTS = Cpu.pop() & 0xFFFF;
	};
	
}
