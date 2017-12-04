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
 * in chapter: 7 Assignment Instructions
 * 
 * Furthermore some instruction from the "changed chapters" document
 * are implemented here.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch07_Assignment_Instructions {
	
	/*
	 * 7.1 Immediate Instructions
	 */
	
	// LIN1 - Load Immediate Negative One
	public static final OpImpl OPC_xCB_LIN1 = () -> {
		Cpu.push((short)-1);
	};
	
	// LINI - Load Immediate Negative Infinity
	public static final OpImpl OPC_xCC_LINI = () -> {
		Cpu.push((short)0x8000);
	};
	
	// LID0 - Load Immediate Double Zero
	public static final OpImpl OPC_xD1_LID0 = () -> {
		Cpu.push((short)0); // a bit faster than ...
		Cpu.push((short)0); // ... pushLong(0)
	};
	
	// LI0 - Load Immediate 0
	public static final OpImpl OPC_xC0_LI0 = () -> {
		Cpu.push((short)0);
	};
	
	// LI1 - Load Immediate 1
	public static final OpImpl OPC_xC1_LI1 = () -> {
		Cpu.push((short)1);
	};
	
	// LI2 - Load Immediate 2
	public static final OpImpl OPC_xC2_LI2 = () -> {
		Cpu.push((short)2);
	};
	
	// LI3 - Load Immediate 3
	public static final OpImpl OPC_xC3_LI3 = () -> {
		Cpu.push((short)3);
	};
	
	// LI4 - Load Immediate 4
	public static final OpImpl OPC_xC4_LI4 = () -> {
		Cpu.push((short)4);
	};
	
	// LI5 - Load Immediate 5
	public static final OpImpl OPC_xC5_LI5 = () -> {
		Cpu.push((short)5);
	};
	
	// LI6 - Load Immediate 6
	public static final OpImpl OPC_xC6_LI6 = () -> {
		Cpu.push((short)6);
	};
	
	// LI7 - Load Immediate 7
	public static final OpImpl OPC_xC7_LI7 = () -> {
		Cpu.push((short)7);
	};
	
	// LI8 - Load Immediate 8
	public static final OpImpl OPC_xC8_LI8 = () -> {
		Cpu.push((short)8);
	};
	
	// LI9 - Load Immediate 9
	public static final OpImpl OPC_xC9_LI9 = () -> {
		Cpu.push((short)9);
	};
	
	// LI10 - Load Immediate 10
	public static final OpImpl OPC_xCA_LI10 = () -> {
		Cpu.push((short)10);
	};
	
	// LIB - Load Immediate Byte
	public static final OpImpl OPC_xCD_LIB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(alpha);
	};
	
	// LINB - Load Immediate Negative Byte
	public static final OpImpl OPC_xCF_LINB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(alpha | 0xFF00);
	};
	
	// LIHB - Load Immediate High Byte
	public static final OpImpl OPC_xD0_LIHB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(alpha << 8);
	};
	
	// LIW - Load Immediate Word
	public static final OpImpl OPC_xCE_LIW_word = () -> {
		int u = Mem.getNextCodeWord();
		Cpu.push(u);
	};
	
	/*
	 * 7.2 Frame Instructions
	 * 
	 * 7.2.1 Local Frame Access
	 */
	
	// LA0 - Local Address 0
	public static final OpImpl OPC_xD2_LA0 = () -> {
		Cpu.push(Cpu.LF);
	};
	
	// LA1 - Local Address 1
	public static final OpImpl OPC_xD3_LA1 = () -> {
		Cpu.push(Cpu.LF + 1);
	};
	
	// LA2 - Local Address 2
	public static final OpImpl OPC_xD4_LA2 = () -> {
		Cpu.push(Cpu.LF + 2);
	};
	
	// LA3 - Local Address 3
	public static final OpImpl OPC_xD5_LA3 = () -> {
		Cpu.push(Cpu.LF + 3);
	};
	
	// LA6 - Local Address 6
	public static final OpImpl OPC_xD6_LA6 = () -> {
		Cpu.push(Cpu.LF + 6);
	};
	
	// LA8 - Local Address 8
	public static final OpImpl OPC_xD7_LA8 = () -> {
		Cpu.push(Cpu.LF + 8);
	};
	
	// LAB - Local Address Byte
	public static final OpImpl OPC_xD8_LAB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(Cpu.LF + alpha);
	};
	
	// LAW - Local Address Word
	public static final OpImpl OPC_xD9_LAW_word = () -> {
		int word = Mem.getNextCodeWord();
		Cpu.push(Cpu.LF + word);
	};
	
	/*
	 * 7.2.1.1 Load Local
	 */
	
	// LL0 - Load Local 0
	public static final OpImpl OPC_x01_LL0 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF));
	};
	
	// LL1 - Load Local 1
	public static final OpImpl OPC_x02_LL1 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 1));
	};
	
	// LL2 - Load Local 2
	public static final OpImpl OPC_x03_LL2 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 2));
	};
	
	// LL3 - Load Local 3
	public static final OpImpl OPC_x04_LL3 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 3));
	};
	
	// LL4 - Load Local 4
	public static final OpImpl OPC_x05_LL4 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 4));
	};
	
	// LL5 - Load Local 5
	public static final OpImpl OPC_x06_LL5 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 5));
	};
	
	// LL6 - Load Local 6
	public static final OpImpl OPC_x07_LL6 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 6));
	};
	
	// LL7 - Load Local 7
	public static final OpImpl OPC_x08_LL7 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 7));
	};
	
	// LL8 - Load Local 8
	public static final OpImpl OPC_x09_LL8 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 8));
	};
	
	// LL9 - Load Local 9
	public static final OpImpl OPC_x0A_LL9 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 9));
	};
	
	// LL10 - Load Local 10
	public static final OpImpl OPC_x0B_LL10 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 10));
	};
	
	// LL11 - Load Local 11
	public static final OpImpl OPC_x0C_LL11 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 11));
	};
	
	// LLB - Load Local Byte
	public static final OpImpl OPC_x0D_LLB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(Mem.readMDSWord(Cpu.LF, alpha));
	};
	
	// LLD0 - Load Local Double 0
	public static final OpImpl OPC_x0E_LLD0 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF));
		Cpu.push(Mem.readMDSWord(Cpu.LF, 1));
	};
	
	// LLD1 - Load Local Double 1
	public static final OpImpl OPC_x0F_LLD1 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 1));
		Cpu.push(Mem.readMDSWord(Cpu.LF, 2));
	};
	
	// LLD2 - Load Local Double 2
	public static final OpImpl OPC_x10_LLD2 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 2));
		Cpu.push(Mem.readMDSWord(Cpu.LF, 3));
	};
	
	// LLD3 - Load Local Double 3
	public static final OpImpl OPC_x11_LLD3 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 3));
		Cpu.push(Mem.readMDSWord(Cpu.LF, 4));
	};
	
	// LLD4 - Load Local Double 4
	public static final OpImpl OPC_x12_LLD4 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 4));
		Cpu.push(Mem.readMDSWord(Cpu.LF, 5));
	};
	
	// LLD5 - Load Local Double 5
	public static final OpImpl OPC_x13_LLD5 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 5));
		Cpu.push(Mem.readMDSWord(Cpu.LF, 6));
	};
	
	// LLD6 - Load Local Double 6
	public static final OpImpl OPC_x14_LLD6 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 6));
		Cpu.push(Mem.readMDSWord(Cpu.LF, 7));
	};
	
	// LLD7 - Load Local Double 7
	public static final OpImpl OPC_x15_LLD7 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 7));
		Cpu.push(Mem.readMDSWord(Cpu.LF, 8));
	};
	
	// LLD8 - Load Local Double 8
	public static final OpImpl OPC_x16_LLD8 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 8));
		Cpu.push(Mem.readMDSWord(Cpu.LF, 9));
	};
	
	// LLD10 - Load Local Double 10
	public static final OpImpl OPC_x17_LLD10 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.LF, 10));
		Cpu.push(Mem.readMDSWord(Cpu.LF, 11));
	};
	
	// LLDB - Load Local Double Byte
	public static final OpImpl OPC_x18_LLDB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(Mem.readMDSWord(Cpu.LF, alpha));
		Cpu.push(Mem.readMDSWord(Cpu.LF, alpha + 1));
	};
	
	/*
	 * 7.2.1.2 Store Local
	 */
	
	// SL0 - Store Local 0
	public static final OpImpl OPC_x19_SL0 = () -> {
		Mem.writeMDSWord(Cpu.LF, Cpu.pop());
	};
	
	// SL1 - Store Local 1
	public static final OpImpl OPC_x1A_SL1 = () -> {
		Mem.writeMDSWord(Cpu.LF, 1, Cpu.pop());
	};
	
	// SL2 - Store Local 2
	public static final OpImpl OPC_x1B_SL2 = () -> {
		Mem.writeMDSWord(Cpu.LF, 2, Cpu.pop());
	};
	
	// SL3 - Store Local 3
	public static final OpImpl OPC_x1C_SL3 = () -> {
		Mem.writeMDSWord(Cpu.LF, 3, Cpu.pop());
	};
	
	// SL4 - Store Local 4
	public static final OpImpl OPC_x1D_SL4 = () -> {
		Mem.writeMDSWord(Cpu.LF, 4, Cpu.pop());
	};
	
	// SL5 - Store Local 5
	public static final OpImpl OPC_x1E_SL5 = () -> {
		Mem.writeMDSWord(Cpu.LF, 5, Cpu.pop());
	};
	
	// SL6 - Store Local 6
	public static final OpImpl OPC_x1F_SL6 = () -> {
		Mem.writeMDSWord(Cpu.LF, 6, Cpu.pop());
	};
	
	// SL7 - Store Local 7
	public static final OpImpl OPC_x20_SL7 = () -> {
		Mem.writeMDSWord(Cpu.LF, 7, Cpu.pop());
	};
	
	// SL8 - Store Local 8
	public static final OpImpl OPC_x21_SL8 = () -> {
		Mem.writeMDSWord(Cpu.LF, 8, Cpu.pop());
	};
	
	// SL9 - Store Local 9
	public static final OpImpl OPC_x22_SL9 = () -> {
		Mem.writeMDSWord(Cpu.LF, 9, Cpu.pop());
	};
	
	// SL10 - Store Local 10
	public static final OpImpl OPC_x23_SL10 = () -> {
		Mem.writeMDSWord(Cpu.LF, 10, Cpu.pop());
	};
	
	// SLB - Store Local Byte
	public static final OpImpl OPC_x24_SLB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Mem.writeMDSWord(Cpu.LF, alpha, Cpu.pop());
	};
	
	// SLD0 - Store Local Double 0
	public static final OpImpl OPC_x25_SLD0 = () -> {
		Mem.writeMDSWord(Cpu.LF, 1, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, Cpu.pop());
	};
	
	// SLD1 - Store Local Double 1
	public static final OpImpl OPC_x26_SLD1 = () -> {
		Mem.writeMDSWord(Cpu.LF, 2, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, 1, Cpu.pop());
	};
	
	// SLD2 - Store Local Double 2
	public static final OpImpl OPC_x27_SLD2 = () -> {
		Mem.writeMDSWord(Cpu.LF, 3, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, 2, Cpu.pop());
	};
	
	// SLD3 - Store Local Double 3
	public static final OpImpl OPC_x28_SLD3 = () -> {
		Mem.writeMDSWord(Cpu.LF, 4, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, 3, Cpu.pop());
	};
	
	// SLD4 - Store Local Double 4
	public static final OpImpl OPC_x29_SLD4 = () -> {
		Mem.writeMDSWord(Cpu.LF, 5, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, 4, Cpu.pop());
	};
	
	// SLD5 - Store Local Double 5
	public static final OpImpl OPC_x2A_SLD5 = () -> {
		Mem.writeMDSWord(Cpu.LF, 6, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, 5, Cpu.pop());
	};
	
	// SLD6 - Store Local Double 6
	public static final OpImpl OPC_x2B_SLD6 = () -> {
		Mem.writeMDSWord(Cpu.LF, 7, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, 6, Cpu.pop());
	};
	
	// SLD8 - Store Local Double 8
	public static final OpImpl OPC_x2C_SLD8 = () -> {
		Mem.writeMDSWord(Cpu.LF, 9, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, 8, Cpu.pop());
	};
	
	// SLDB - Store Local Double Byte
	public static final OpImpl OPC_x75_SLDB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Mem.writeMDSWord(Cpu.LF, alpha + 1, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, alpha, Cpu.pop());
	};
	
	/*
	 * 7.2.1.3 Put Local
	 */
	
	// PL0 - Put Local 0
	public static final OpImpl OPC_x2D_PL0 = () -> {
		Mem.writeMDSWord(Cpu.LF, Cpu.popRecover());
	};
	
	// PL1 - Put Local 1
	public static final OpImpl OPC_x2E_PL1 = () -> {
		Mem.writeMDSWord(Cpu.LF, 1, Cpu.popRecover());
	};
	
	// PL2 - Put Local 2
	public static final OpImpl OPC_x2F_PL2 = () -> {
		Mem.writeMDSWord(Cpu.LF, 2, Cpu.popRecover());
	};
	
	// PL3 - Put Local 3
	public static final OpImpl OPC_x30_PL3 = () -> {
		Mem.writeMDSWord(Cpu.LF, 3, Cpu.popRecover());
	};
	
	// PLB - Put Local Byte
	public static final OpImpl OPC_x31_PLB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Mem.writeMDSWord(Cpu.LF, alpha, Cpu.popRecover());
	};
	
	// PLD0 - Put Local Double 0
	public static final OpImpl OPC_x32_PLD0 = () -> {
		Mem.writeMDSWord(Cpu.LF, 1, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, Cpu.pop());
		Cpu.recover();
		Cpu.recover();
	};
	
	// PLDB - Put Local Double Byte
	public static final OpImpl OPC_x33_PLDB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Mem.writeMDSWord(Cpu.LF, alpha + 1, Cpu.pop());
		Mem.writeMDSWord(Cpu.LF, alpha, Cpu.pop());
		Cpu.recover();
		Cpu.recover();
	};
	
	/*
	 * 7.2.1.4 Add Local
	 */
	
	// AL0IB - Add Local Zero to Immediate Byte
	public static final OpImpl OPC_xBB_AL0IB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push((short)((Mem.readMDSWord(Cpu.LF) & 0xFFFF) + alpha)); // assuming that UNSPECIFIED is unsigned
	};
	
	/*
	 * 7.2.2 Global Frame Access
	 * 
	 * post 4.0-PrincOps assumption:
	 * - if an old-procedure is running, it will expect the global frame in MDS
	 * - and Pilot must honor this assumption (the old compilers did no know better!) 
	 * - so GF32 is assumed to be a 32 bit address pointing into the MDS
	 * - as the MDS base address is 64Kword aligned, the lower 16 bit of GF32 can
	 *   therefore be expected to be the valid POINTER to the global frame in the MDS
	 * 
	 * (although pushing an int as word will only use the lower 16 bit, this is
	 * made explicit in the code by using ' & 0xFFFF ')
	 */
	
	// GA0 - Global Address 0
	public static final OpImpl OPCo_xDA_GA0 = () -> {
		Cpu.push(Cpu.GF16);
	};
	public static final OpImpl OPCn_xDA_GA0 = () -> {
		Cpu.push(Cpu.GF32 & 0xFFFF); // post 4.0-PrincOps : push 16 bit subset of 32 quantity
	};
	
	// GA1 - Global Address 1
	public static final OpImpl OPCo_xDB_GA1 = () -> {
		Cpu.push(Cpu.GF16 + 1);
	};
	public static final OpImpl OPCn_xDB_GA1 = () -> {
		Cpu.push((Cpu.GF32 + 1) & 0xFFFF); // post 4.0-PrincOps : push 16 bit subset of 32 quantity
	};
	
	// GAB - Global Address Byte
	public static final OpImpl OPCo_xDC_GAB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(Cpu.GF16 + alpha);
	};
	public static final OpImpl OPCn_xDC_GAB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push((Cpu.GF32 + alpha) & 0xFFFF); // post 4.0-PrincOps : push 16 bit subset of 32 quantity
	};
	
	// GAW - Global Address Word
	public static final OpImpl OPCo_xDD_GAW_word = () -> {
		int word = Mem.getNextCodeWord();
		Cpu.push(Cpu.GF16 + word);
	};
	public static final OpImpl OPCn_xDD_GAW_word = () -> {
		int word = Mem.getNextCodeWord();
		Cpu.push((Cpu.GF32 + word) & 0xFFFF); // post 4.0-PrincOps : push 16 bit subset of 32 quantity
	};
	
	/*
	 * 7.2.2 Global Frame Access (changed chapters)
	 * 
	 * (post 4.0-PrincOps only, old-procedures to not know/use these instructions)
	 */
	
	// LGA0 - Long Global Address 0
	public static final OpImpl OPCn_xFA_LGA0 = () -> {
		Cpu.pushLong(Cpu.GF32);
	};
	
//	// LGA1 - Long Global Address 1
//	// (specified, but no instruction code assigned and there's not room at the logical position between 0xFA and 0xFB)
//	public static final OpImpl OPC_x??_LGA1 = () -> {
//		Cpu.pushLong(Cpu.GF32 + 1);
//	};
	
	// LGAB - Long Global Address Byte
	public static final OpImpl OPCn_xFB_LGAB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.pushLong(Cpu.GF32 + alpha);
	};
	
	// LGAW - Long Global Address Word
	public static final OpImpl OPCn_xFC_LGAW_word = () -> {
		int word = Mem.getNextCodeWord();
		Cpu.pushLong(Cpu.GF32 + word);
	};
	
	
	/*
	 * 7.2.2.1 Load Global
	 */
	
	// LG0 - Load Global 0
	public static final OpImpl OPCo_x34_LG0 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.GF16));
	};
	public static final OpImpl OPCn_x34_LG0 = () -> {
		Cpu.push(Mem.readWord(Cpu.GF32));
	};
	
	// LG1 - Load Global 1
	public static final OpImpl OPCo_x35_LG1 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.GF16, 1));
	};
	public static final OpImpl OPCn_x35_LG1 = () -> {
		Cpu.push(Mem.readWord(Cpu.GF32 + 1));
	};
	
	// LG2 - Load Global 2
	public static final OpImpl OPCo_x36_LG2 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.GF16, 2));
	};
	public static final OpImpl OPCn_x36_LG2 = () -> {
		Cpu.push(Mem.readWord(Cpu.GF32 + 2));
	};
	
	// LGB - Load Global Byte
	public static final OpImpl OPCo_x37_LGB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(Mem.readMDSWord(Cpu.GF16, alpha));
	};
	public static final OpImpl OPCn_x37_LGB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(Mem.readWord(Cpu.GF32 + alpha));
	};
	
	// LGD0 - Load Global Double 0
	public static final OpImpl OPCo_x38_LGD0 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.GF16));
		Cpu.push(Mem.readMDSWord(Cpu.GF16, 1));
	};
	public static final OpImpl OPCn_x38_LGD0 = () -> {
		Cpu.push(Mem.readWord(Cpu.GF32));
		Cpu.push(Mem.readWord(Cpu.GF32 + 1));
	};
	
	// LGD2 - Load Global Double 2
	public static final OpImpl OPCo_x39_LGD2 = () -> {
		Cpu.push(Mem.readMDSWord(Cpu.GF16, 2));
		Cpu.push(Mem.readMDSWord(Cpu.GF16, 3));
	};
	public static final OpImpl OPCn_x39_LGD2 = () -> {
		Cpu.push(Mem.readWord(Cpu.GF32 + 2));
		Cpu.push(Mem.readWord(Cpu.GF32 + 3));
	};
	
	// LGDB - Load Global Double Byte
	public static final OpImpl OPCo_x3A_LGDB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(Mem.readMDSWord(Cpu.GF16, alpha));
		Cpu.push(Mem.readMDSWord(Cpu.GF16, alpha + 1));
	};
	public static final OpImpl OPCn_x3A_LGDB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.push(Mem.readWord(Cpu.GF32 + alpha));
		Cpu.push(Mem.readWord(Cpu.GF32 + alpha + 1));
	};
	
	/*
	 * 7.2.2.2 Store Global
	 */
	
	// SGB - Store Global Byte
	public static final OpImpl OPCo_x3B_SGB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Mem.writeMDSWord(Cpu.GF16, alpha, Cpu.pop());
	};
	public static final OpImpl OPCn_x3B_SGB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Mem.writeWord(Cpu.GF32 + alpha, Cpu.pop());
	};
	
	// SGDB - Store Global Double Byte
	public static final OpImpl OPCo_x76_SGDB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Mem.writeMDSWord(Cpu.GF16, alpha + 1, Cpu.pop());
		Mem.writeMDSWord(Cpu.GF16, alpha, Cpu.pop());
	};
	public static final OpImpl OPCn_x76_SGDB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Mem.writeWord(Cpu.GF32 + alpha + 1, Cpu.pop());
		Mem.writeWord(Cpu.GF32 + alpha, Cpu.pop());
	};
	
	/*
	 * 7.3 Pointer Instructions
	 * 
	 * 7.3.1 Direct Pointer Instructions
	 * 
	 * 7.3.1.1 Read Direct
	 */
	
	// R0 - Read 0
	public static final OpImpl OPC_x40_R0 = () -> {
		short pointer = Cpu.pop();
		Cpu.push(Mem.readMDSWord(pointer));
	};
	
	// R1 - Read 1
	public static final OpImpl OPC_x41_R1 = () -> {
		short pointer = Cpu.pop();
		Cpu.push(Mem.readMDSWord(pointer, 1));
	};
	
	// RB - Read Byte
	public static final OpImpl OPC_x42_RB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		short pointer = Cpu.pop();
		Cpu.push(Mem.readMDSWord(pointer, alpha));
	};
	
	// RL0 - Read Long Zero
	public static final OpImpl OPC_x43_RL0 = () -> {
		int longPointer = Cpu.popLong();
		Cpu.push(Mem.readWord(longPointer));
	};
	
	// RLB - Read Long Byte
	public static final OpImpl OPC_x44_RLB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int longPointer = Cpu.popLong();
		Cpu.push(Mem.readWord(longPointer + alpha));
	};
	
	// RD0 - Read Double Zero
	public static final OpImpl OPC_x45_RD0 = () -> {
		short pointer = Cpu.pop();
		short u = Mem.readMDSWord(pointer);
		short v = Mem.readMDSWord(pointer, 1);
		Cpu.push(u);
		Cpu.push(v);
	};
	
	// RDB - Read Double Byte
	public static final OpImpl OPC_x46_RDB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		short pointer = Cpu.pop();
		short u = Mem.readMDSWord(pointer, alpha);
		short v = Mem.readMDSWord(pointer, alpha + 1);
		Cpu.push(u);
		Cpu.push(v);
	};
	
	// RDL0 - Read Double Long Zero
	public static final OpImpl OPC_x47_RDL0 = () -> {
		int longPointer = Cpu.popLong();
		short u = Mem.readWord(longPointer);
		short v = Mem.readWord(longPointer + 1);
		Cpu.push(u);
		Cpu.push(v);
	};
	
	// RDLB - Read Double Long Byte
	public static final OpImpl OPC_x48_RDLB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int longPointer = Cpu.popLong();
		short u = Mem.readWord(longPointer + alpha);
		short v = Mem.readWord(longPointer + alpha + 1);
		Cpu.push(u);
		Cpu.push(v);
	};
	
	// RC - Read Code
	public static final OpImpl ESC_x1B_RC_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int offset = Cpu.pop() & 0xFFFF;
		Cpu.push(Mem.readCode(offset + alpha));
	};
	
	/*
	 * 7.3.1.2 Write Direct
	 */
	
	// W0 - Write Zero
	public static final OpImpl OPC_x49_W0 = () -> {
		short pointer = Cpu.pop();
		Mem.writeMDSWord(pointer, Cpu.pop());
	};
	
	// WB - Write Byte
	public static final OpImpl OPC_x4A_WB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		short pointer = Cpu.pop();
		Mem.writeMDSWord(pointer, alpha, Cpu.pop());
	};
	
	// WLB - Write Long Byte
	public static final OpImpl OPC_x4C_WLB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int longPointer = Cpu.popLong();
		Mem.writeWord(longPointer + alpha, Cpu.pop());
	};
	
	// WDB - Write Double Byte
	public static final OpImpl OPC_x4E_WDB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		short pointer = Cpu.pop();
		Mem.writeMDSWord(pointer, alpha + 1, Cpu.pop());
		Mem.writeMDSWord(pointer, alpha, Cpu.pop());
	};
	
	// WDLB - Write Double Long Byte
	public static final OpImpl OPC_x51_WDLB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int longPointer = Cpu.popLong();
		Mem.writeWord(longPointer + alpha + 1, Cpu.pop());
		Mem.writeWord(longPointer + alpha, Cpu.pop());
	};
	
	/*
	 * 7.3.1.3 Put Swapped Direct
	 */
	
	// PSB - Put Swapped Byte
	public static final OpImpl OPC_x4B_PSB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		short u = Cpu.pop();
		short pointer = Cpu.pop();
		Mem.writeMDSWord(pointer, alpha, u);
		Cpu.recover();
	};
	
	// PSD0 - Put Swapped Double Zero
	public static final OpImpl OPC_x4F_PSD0 = () -> {
		short v = Cpu.pop();
		short u = Cpu.pop();
		short pointer = Cpu.pop();
		Mem.writeMDSWord(pointer, 1, v);
		Mem.writeMDSWord(pointer, u);
		Cpu.recover();
	};
	
	// PSDB - Put Swapped Double Byte
	public static final OpImpl OPC_x50_PSDB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		short v = Cpu.pop();
		short u = Cpu.pop();
		short pointer = Cpu.pop();
		Mem.writeMDSWord(pointer, alpha + 1, v);
		Mem.writeMDSWord(pointer, alpha, u);
		Cpu.recover();
	};
	
	// PSLB - Put Swapped Long Byte
	public static final OpImpl OPC_x4D_PSLB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		short u = Cpu.pop();
		int longPointer = Cpu.popLong();
		Mem.writeWord(longPointer + alpha, u);
		Cpu.recover();
		Cpu.recover();
	};
	
	// PSDLB - Put Swapped Double Long Byte
	public static final OpImpl OPC_x52_PSDLB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		short v = Cpu.pop();
		short u = Cpu.pop();
		int longPointer = Cpu.popLong();
		Mem.writeWord(longPointer + alpha + 1, v);
		Mem.writeWord(longPointer + alpha, u);
		Cpu.recover();
		Cpu.recover();
	};
	
	/*
	 * 7.3.2 Indirect Pointer Instructions
	 * 
	 * 7.3.2.1 Read Indirect
	 */
	
	// RLI00 - Read Local Indirect Zero 0
	public static final OpImpl OPC_x53_RLI00 = () -> {
		short pointer = Mem.readMDSWord(Cpu.LF);
		Cpu.push(Mem.readMDSWord(pointer));
	};
	
	// RLI01 - Read Local Indirect Zero 1
	public static final OpImpl OPC_x54_RLI01 = () -> {
		short pointer = Mem.readMDSWord(Cpu.LF);
		Cpu.push(Mem.readMDSWord(pointer, 1));
	};
	
	// RLI02 - Read Local Indirect Zero 2
	public static final OpImpl OPC_x55_RLI02 = () -> {
		short pointer = Mem.readMDSWord(Cpu.LF);
		Cpu.push(Mem.readMDSWord(pointer, 2));
	};
	
	// RLI03 - Read Local Indirect Zero 3
	public static final OpImpl OPC_x56_RLI03 = () -> {
		short pointer = Mem.readMDSWord(Cpu.LF);
		Cpu.push(Mem.readMDSWord(pointer, 3));
	};
	
	// RLIP - Read Local Indirect Pair
	public static final OpImpl OPC_x57_RLIP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		short pointer = Mem.readMDSWord(Cpu.LF + (pair >>> 4));
		Cpu.push(Mem.readMDSWord(pointer + (pair & 0x0F)));
	};
	
	// RLILP - Read Local Indirect Long Pair
	public static final OpImpl OPC_x58_RLILP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		int longPointer = Mem.readMDSDblWord(Cpu.LF + (pair >>> 4));
		Cpu.push(Mem.readWord(longPointer + (pair & 0x0F)));
	};
	
	// RGIP - Read Global Indirect Pair
	public static final OpImpl OPCo_x5C_RGIP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		short pointer = Mem.readMDSWord(Cpu.GF16 + (pair >>> 4));
		Cpu.push(Mem.readMDSWord(pointer + (pair & 0x0F)));
	};
	public static final OpImpl OPCn_x5C_RGIP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		short pointer = Mem.readWord(Cpu.GF32 + (pair >>> 4));
		Cpu.push(Mem.readMDSWord(pointer + (pair & 0x0F)));
	};
	
	// RGILP - Read Global Indirect Long Pair
	public static final OpImpl OPCo_x5D_RGILP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		int longPointer = Mem.readMDSDblWord(Cpu.GF16 + (pair >>> 4));
		Cpu.push(Mem.readWord(longPointer + (pair & 0x0F)));
	};
	public static final OpImpl OPCn_x5D_RGILP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		int longPointer = Mem.readDblWord(Cpu.GF32 + (pair >>> 4));
		Cpu.push(Mem.readWord(longPointer + (pair & 0x0F)));
	};
	
	// RLID00 - Read Local Double Indirect Zero Zero
	public static final OpImpl OPC_x59_RLDI00 = () -> {
		short pointer = Mem.readMDSWord(Cpu.LF);
		short u = Mem.readMDSWord(pointer);
		short v = Mem.readMDSWord(pointer + 1);
		Cpu.push(u);
		Cpu.push(v);
	};
	
	// RLDIP - Read Local Double Indirect Pair
	public static final OpImpl OPC_x5A_RLDIP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		short pointer = Mem.readMDSWord(Cpu.LF + (pair >>> 4));
		short u = Mem.readMDSWord(pointer + (pair & 0x0F));
		short v = Mem.readMDSWord(pointer + (pair & 0x0F) + 1);
		Cpu.push(u);
		Cpu.push(v);
	};
	
	// RLDILP - Read Local Double Indirect Long Pair
	public static final OpImpl OPC_x5B_RLDILP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		int longPointer = Mem.readMDSDblWord(Cpu.LF + (pair >>> 4));
		short u = Mem.readWord(longPointer + (pair & 0x0F));
		short v = Mem.readWord(longPointer + (pair & 0x0F) + 1);
		Cpu.push(u);
		Cpu.push(v);
	};
	
	/*
	 * 7.3.2.2 Write Indirect
	 */
	
	// WLIP - Write Local Indirect Pair
	public static final OpImpl OPC_x5E_WLIP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		short pointer = Mem.readMDSWord(Cpu.LF + (pair >>> 4));
		Mem.writeMDSWord(pointer + (pair & 0x0F), Cpu.pop());
	};
	
	// WLILP - Write Local Indirect Long Pair
	public static final OpImpl OPC_x5F_WLILP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		int longPointer = Mem.readMDSDblWord(Cpu.LF + (pair >>> 4));
		Mem.writeWord(longPointer + (pair & 0x0F), Cpu.pop());
	};
	
	// WLDILP - Write Local Double Indirect Long Pair
	public static final OpImpl OPC_x60_WLDILP_pair = () -> {
		int pair = Mem.getNextCodeByte();
		int longPointer = Mem.readMDSDblWord(Cpu.LF + (pair >>> 4));
		Mem.writeWord(longPointer + (pair & 0x0F) + 1, Cpu.pop());
		Mem.writeWord(longPointer + (pair & 0x0F), Cpu.pop());
	};
	
	/*
	 * 7.4 String Instructions
	 * 
	 * 7.4.1 Read String
	 */
	
	// RS - Read String
	public static final OpImpl OPC_x61_RS_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int index = Cpu.pop() & 0xFFFF;
		int pointer = Cpu.pop() & 0xFFFF;
		Cpu.push(Mem.fetchByte(Cpu.lengthenPointer(pointer), alpha + index)); // LengthenPointer[pointer] : forgotten in PrincOps?
	};
	
	// RLS - Read Long String
	public static final OpImpl OPC_x62_RLS_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int index = Cpu.pop() & 0xFFFF;
		int longPointer = Cpu.popLong();
		Cpu.push(Mem.fetchByte(longPointer, alpha + index));
	};
	
	/*
	 * 7.4.2 WS - Write String 
	 */
	
	// WS - Write String
	public static final OpImpl OPC_x63_WS_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int index = Cpu.pop() & 0xFFFF;
		int pointer = Cpu.pop() & 0xFFFF;
		short data = (short)(Cpu.pop() & 0x00FF);
		Mem.storeByte(Cpu.lengthenPointer(pointer), alpha + index, data); // LengthenPointer[pointer] : forgotten in PrincOps?
	};
	
	// WLS - Write Long String
	public static final OpImpl OPC_x64_WLS_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int index = Cpu.pop() & 0xFFFF;
		int longPointer = Cpu.popLong();
		short data = (short)(Cpu.pop() & 0x00FF);
		Mem.storeByte(longPointer, alpha + index, data);
	};
	
	/*
	 * 7.5 Field Instructions
	 * 
	 * 7.5.1 Read Field
	 */
	
	// RF - Read Field
	public static final OpImpl OPC_x66_RF_word = () -> {
		int fieldDesc = Mem.getNextCodeWord();
		int pointer = Cpu.pop() & 0x0FFFF;
		Cpu.push(Mem.readField(Mem.readMDSWord(pointer, fieldDesc >>> 8), fieldDesc & 0xFF));
	};
	
	// R0F - Read Zero Field
	public static final OpImpl OPC_x65_R0F_alpha = () -> {
		int spec = Mem.getNextCodeByte();
		int pointer = Cpu.pop() & 0x0FFFF;
		Cpu.push(Mem.readField(Mem.readMDSWord(pointer), spec));
	};
	
	// RLF - Read Long Field
	public static final OpImpl OPC_x68_RLF_word = () -> {
		int fieldDesc = Mem.getNextCodeWord();
		int longPointer = Cpu.popLong();
		Cpu.push(Mem.readField(Mem.readWord(longPointer + (fieldDesc >>> 8)), fieldDesc & 0xFF));
	};
	
	// RL0F - Read Long Zero Field
	public static final OpImpl OPC_x67_RL0F_alpha = () -> {
		int spec = Mem.getNextCodeByte();
		int longPointer = Cpu.popLong();
		Cpu.push(Mem.readField(Mem.readWord(longPointer), spec));
	};
	
	// RLFS - Read Long Field Stack
	public static final OpImpl OPC_x69_RLFS = () -> {
		int fieldDesc = Cpu.pop() & 0xFFFF;
		int longPointer = Cpu.popLong();
		Cpu.push(Mem.readField(Mem.readWord(longPointer + (fieldDesc >>> 8)), fieldDesc & 0xFF));
	};
	
	// RCFS - Read Code Field Stack
	public static final OpImpl ESC_x1A_RCFS = () -> {
		int fieldDesc = Cpu.pop() & 0xFFFF;
		int offset = Cpu.pop() & 0xFFFF;
		Cpu.push(Mem.readField(Mem.readCode(offset + (fieldDesc >>> 8)), fieldDesc & 0xFF));
	};
	
	// RLIPF - Read Local Indirect Pair Field 
	public static final OpImpl OPC_x6A_RLIPF_alphabeta = () -> {
		int pair = Mem.getNextCodeByte();
		int spec = Mem.getNextCodeByte();
		int pointer = Mem.readMDSWord(Cpu.LF, pair >>> 4) & 0x0FFFF;
		Cpu.push(Mem.readField(Mem.readMDSWord(pointer, pair & 0x0F), spec));
	};
	
	// RLILPF - Read Local Indirect Long Pair Field 
	public static final OpImpl OPC_x6B_RLILPF_alphabeta = () -> {
		int pair = Mem.getNextCodeByte();
		int spec = Mem.getNextCodeByte();
		int longPointer = Mem.readMDSDblWord(Cpu.LF, pair >>> 4);
		Cpu.push(Mem.readField(Mem.readWord(longPointer + (pair & 0x0F)), spec));
	};
	
	/*
	 * 7.5.2 Write Field
	 */
	
	// WF - Write Field
	public static final OpImpl OPC_x6D_WF_word = () -> {
		int fieldDesc = Mem.getNextCodeWord();
		int pointer = Cpu.pop() & 0xFFFF;
		short data = Cpu.pop();
		int offset = fieldDesc >>> 8;
		short src = Mem.readMDSWord(pointer, offset);
		Mem.writeMDSWord(pointer, offset, Mem.writeField(src, fieldDesc & 0xFF, data));
	};
	
	// W0F - Write Zero Field
	public static final OpImpl OPC_x6C_W0F_alpha = () -> {
		int spec = Mem.getNextCodeByte();
		int pointer = Cpu.pop() & 0x0FFFF;
		short data = Cpu.pop();
		short src = Mem.readMDSWord(pointer);
		Mem.writeMDSWord(pointer, Mem.writeField(src, spec, data));
	};
	
	// WLF - Write Long Field
	public static final OpImpl OPC_x72_WLF_word = () -> {
		int fieldDesc = Mem.getNextCodeWord();
		int longPointer = Cpu.popLong();
		short data = Cpu.pop();
		longPointer += fieldDesc >>> 8;
		short src = Mem.readWord(longPointer);
		Mem.writeWord(longPointer, Mem.writeField(src, fieldDesc & 0xFF, data));
	};
	
	// WL0F - Write Long Zero Field
	public static final OpImpl OPC_x71_WL0F_alpha = () -> {
		int spec = Mem.getNextCodeByte();
		int longPointer = Cpu.popLong();
		short data = Cpu.pop();
		short src = Mem.readWord(longPointer);
		Mem.writeWord(longPointer, Mem.writeField(src, spec, data));
	};
	
	// WLFS - Write Long Field Stack
	public static final OpImpl OPC_x74_WLFS = () -> {
		int fieldDesc = Cpu.pop() & 0xFFFF;
		int longPointer = Cpu.popLong();
		short data = Cpu.pop();
		longPointer += fieldDesc >>> 8;
		short src = Mem.readWord(longPointer);
		Mem.writeWord(longPointer, Mem.writeField(src, fieldDesc & 0xFF, data));
	};
	
	// WS0F - Write Swapped Zero Field
	public static final OpImpl OPC_x70_WS0F_alpha = () -> {
		int spec = Mem.getNextCodeByte();
		short data = Cpu.pop();
		int pointer = Cpu.pop() & 0x0FFFF;
		short src = Mem.readMDSWord(pointer);
		Mem.writeMDSWord(pointer, Mem.writeField(src, spec, data));
	};
	
	/*
	 * 7.5.3 Put Swapped Field
	 */
	
	// PS0F - Put Swapped Zero Field
	public static final OpImpl OPC_x6F_PS0F = () -> {
		OPC_x70_WS0F_alpha.execute();
		Cpu.recover();
	};
	
	// PSF - Put Swapped Field
	public static final OpImpl OPC_x6E_PSF_word = () -> {
		int fieldDesc = Mem.getNextCodeWord();
		short data = Cpu.pop();
		int pointer = Cpu.pop() & 0x0FFFF;
		int offset = fieldDesc >>> 8;
		short src = Mem.readMDSWord(pointer, offset);
		Mem.writeMDSWord(pointer, offset, Mem.writeField(src, fieldDesc & 0xFF, data));
		Cpu.recover();
	};
	
	// PSLF - Put Swapped Long Field
	public static final OpImpl OPC_x73_PSLF_word = () -> {
		int fieldDesc = Mem.getNextCodeWord();
		short data = Cpu.pop();
		int longPointer = Cpu.popLong();
		longPointer += fieldDesc >>> 8;
		short src = Mem.readWord(longPointer);
		Mem.writeWord(longPointer, Mem.writeField(src, fieldDesc & 0xFF, data));
		Cpu.recover();
		Cpu.recover();
	};
	
}
