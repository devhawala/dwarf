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

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Opcodes;
import dev.hawala.dmachine.engine.Opcodes.OpImpl;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;
import dev.hawala.dmachine.engine.Xfer;
import dev.hawala.dmachine.engine.Cpu.MesaAbort;
import dev.hawala.dmachine.engine.Xfer.XferType;

/**
 * Implementation of instructions defined in PrincOps 4.0
 * in chapter: 9 Control Transfers  
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch09_Control_Transfers {
	
	/*
	 * 9.2.3 Frame Allocation Instructions
	 */
	
	// AF - Allocate Frame
	public static final OpImpl ESC_x0A_AF = () -> {
		int fsi = Cpu.pop() & 0xFFFF;
		Cpu.push(Xfer.alloc(fsi));
	};
	
	// FF - Free Frame
	public static final OpImpl ESC_x0B_FF = () -> {
		int frame = Cpu.pop() & 0xFFFF;
		Xfer.free(frame);
	};
	
	/*
	 * 9.4.1 Local Function Calls
	 */
	
	// LFC - Local Function Call
	public static final OpImpl OPCo_xED_LFC_word = () -> {
		//	Mem.writeMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc, Cpu.PC);
		//	int nPC = Mem.getNextCodeWord();
		//	if (nPC == 0) { Cpu.unboundTrap(0); }
		// ATTENTION: deviation from PrincOps: the above code matches PrincOps, but will
		// result in an return PC too low by 2, as the the return PC is stored in the
		// source local frame before the target procedure is taken from the code!
		// the following code gets us the correct return PC stored in the source local frame:
		int nPC = Mem.getNextCodeWord();
		Mem.writeMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc, Cpu.PC);
		if (nPC == 0) { Cpu.unboundTrap(0); }
		
		// get the local frame index and allocate the new local frame
		int word = Mem.readCode(nPC / 2);
		int nFsi = ((nPC & 0x0001) == 0) ? word >>> 8 : word & 0xFF;
		int nLF = Xfer.alloc(nFsi);
		nPC++;
		
		// setup the new local frame (global frame link, return link)
		Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink, Cpu.GF16);
		Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink, Cpu.LF);

		if (Config.LOG_OPCODES) {
			Cpu.logf(
					  "   local function call xfer\n"
					+ "     -> GFI=0x%04X GF32=0x%08X CB=0x%08X newPC=0x%04X newFsi=%d\n"
					+ "     -> newLF=0x%04X newLF.globalLink=0x%04X newLF.returnLink=0x%04X\n",
					Cpu.GFI, Cpu.GF32, Cpu.CB, nPC, nFsi,
					nLF, Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink), Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink)
					);
		}
		
		// switch context
		Cpu.LF = nLF;
		Cpu.PC = nPC;
		
		Xfer.impl.checkForXferTraps(((Cpu.GF16 | 0x0001) << 16) | ((Cpu.PC - 1) & 0x0000FFFF), XferType.xlocalCall);
	};
	public static final OpImpl OPCn_xED_LFC_word = () -> {
		//	Mem.writeMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc, Cpu.PC);
		//	int nPC = Mem.getNextCodeWord();
		//	if (nPC == 0) { Cpu.unboundTrap(0); }
		// ATTENTION: deviation from PrincOps: the above code matches PrincOps, but will
		// result in an return PC too low by 2, as the the return PC is stored in the
		// source local frame before the target procedure is taken from the code!
		// the following code gets us the correct return PC stored in the source local frame:
		int nPC = Mem.getNextCodeWord();
		Mem.writeMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc, Cpu.PC);
		if (nPC == 0) { Cpu.unboundTrap(0); }
		
		// get the local frame index and allocate the new local frame
		int word = Mem.readCode(nPC / 2);
		int nLF = Xfer.alloc(((nPC & 0x0001) == 0) ? word >>> 8 : word & 0xFF);
		nPC++;
		
		// setup the new local frame (global frame link, return link)
		Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink, Cpu.GFI);
		Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink, Cpu.LF);
		
		// switch context
		Cpu.LF = nLF;
		Cpu.PC = nPC;
		
		// TODO: check if GFI is right instead of: (Cpu.GF16 | 0x0001) << 16 ??
		Xfer.impl.checkForXferTraps((Cpu.GFI << 16) | ((Cpu.PC - 1) & 0x0000FFFF), XferType.xlocalCall);
	};
	
	/*
	 * 9.4.2 External Function Calls
	 */
	
	private static void call(int controlLink) {
		Mem.writeMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc, Cpu.PC);
		Xfer.impl.xfer(controlLink, Cpu.LF, XferType.xcall, false);
	}
	
	// EFC0 - External Function Call 0 
	public static final OpImpl OPC_xDF_EFC0 = () -> {
		call(Xfer.impl.fetchLink(0));
	};
	
	// EFC1 - External Function Call 1
	public static final OpImpl OPC_xE0_EFC1 = () -> {
		call(Xfer.impl.fetchLink(1));
	};
	
	// EFC2 - External Function Call 2
	public static final OpImpl OPC_xE1_EFC2 = () -> {
		call(Xfer.impl.fetchLink(2));
	};
	
	// EFC3 - External Function Call 3
	public static final OpImpl OPC_xE2_EFC3 = () -> {
		call(Xfer.impl.fetchLink(3));
	};
	
	// EFC4 - External Function Call 4
	public static final OpImpl OPC_xE3_EFC4 = () -> {
		call(Xfer.impl.fetchLink(4));
	};
	
	// EFC5 - External Function Call 5
	public static final OpImpl OPC_xE4_EFC5 = () -> {
		call(Xfer.impl.fetchLink(5));
	};
	
	// EFC6 - External Function Call 6
	public static final OpImpl OPC_xE5_EFC6 = () -> {
		call(Xfer.impl.fetchLink(6));
	};
	
	// EFC7 - External Function Call 7
	public static final OpImpl OPC_xE6_EFC7 = () -> {
		call(Xfer.impl.fetchLink(7));
	};
	
	// EFC8 - External Function Call 8
	public static final OpImpl OPC_xE7_EFC8 = () -> {
		call(Xfer.impl.fetchLink(8));
	};
	
	// EFC9 - External Function Call 9
	public static final OpImpl OPC_xE8_EFC9 = () -> {
		call(Xfer.impl.fetchLink(9));
	};
	
	// EFC10 - External Function Call 10
	public static final OpImpl OPC_xE9_EFC10 = () -> {
		call(Xfer.impl.fetchLink(10));
	};
	
	// EFC11 - External Function Call 11
	public static final OpImpl OPC_xEA_EFC11 = () -> {
		call(Xfer.impl.fetchLink(11));
	};
	
	// EFC12 - External Function Call 12
	public static final OpImpl OPC_xEB_EFC12 = () -> {
		call(Xfer.impl.fetchLink(12));
	};
	
	// EFCB - External Function Call Byte
	public static final OpImpl OPC_xEC_EFCB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		call(Xfer.impl.fetchLink(alpha));
	};
	
	// SFC - Stack Function Call
	public static final OpImpl OPC_xEE_SFC = () -> {
		int controlLink = Cpu.popLong();
		call(controlLink);
	};
	
	// KFCB - Kernel Function Call Byte
	public static final OpImpl OPC_xF0_KFCB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int controlLink = Mem.readMDSDblWord(PrincOpsDefs.getSdMdsPtr(alpha));
		call(controlLink);
	};
	
	/*
	 * 9.4.3 Nested Function Calls
	 */
	
	// LKB - Link Byte
	public static final OpImpl OPC_x7A_LKB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.recover();
		int shortControlLink = Cpu.pop() & 0xFFFF;
		Mem.writeMDSWord(Cpu.LF, shortControlLink - alpha);
	};
	
	/*
	 * 9.4.4 Returns
	 */
	
	// RET - Return
	public static final OpImpl OPC_xEF_RET = () -> {
		int controlLink = Mem.readMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_returnlink);
		Xfer.impl.xfer(controlLink, 0, XferType.xreturn, true);
	};
	
	/*
	 * 9.4.5 Coroutine Transfers
	 */
	
	// PO - Port Out
	public static final OpImpl ESC_x0D_PO = () -> {
		/*int reserved =*/ Cpu.pop();
		int portLink = Cpu.pop();
		Mem.writeMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc, Cpu.PC);
		Mem.writeMDSWord(portLink, PrincOpsDefs.Port_inport, Cpu.LF);
		int outport = Mem.readMDSDblWord(portLink, PrincOpsDefs.Port_outport);
		Xfer.impl.xfer(outport, portLink, XferType.xport, false);
	};
	
	// POR - Port Out Responding
	public static final OpImpl ESC_x0E_POR = () -> {
		ESC_x0D_PO.execute();
	};
	
	// PI - Port In
	public static final OpImpl ESC_x0C_PI = () -> {
		Cpu.recover();
		Cpu.recover();
		int src = Cpu.pop();
		int port = Cpu.pop();
		Mem.writeMDSWord(port, PrincOpsDefs.Port_inport, 0);
		if (src != 0) {
			Mem.writeMDSWord(port, PrincOpsDefs.Port_outport, src);
		}
	};
	
	/*
	 * 9.4.6 Link Instructions
	 */
	
	private static int controlLinkAsLongPointer(int controlLink) {
		return controlLink;
	}
	
	// LLKB - Load Link Byte
	public static final OpImpl OPC_x77_LLKB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		Cpu.pushLong(Xfer.impl.fetchLink(alpha));
	};
	
	// RKIB - Read Link Indirect Byte
	public static final OpImpl OPC_x78_RKIB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int longPointer = controlLinkAsLongPointer(Xfer.impl.fetchLink(alpha));
		Cpu.push(Mem.readWord(longPointer));
	};
	
	// RKDIB - Read Link Double Indirect Byte
	public static final OpImpl OPC_x79_RKDIB_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int longPointer = controlLinkAsLongPointer(Xfer.impl.fetchLink(alpha));
		Cpu.push(Mem.readWord(longPointer));
		Cpu.push(Mem.readWord(longPointer + 1));
	};
	
	/*
	 * 9.5.3 Trap Handlers
	 */
	
	// DSK - Dump Stack
	public static final OpImpl ESC_x20_DSK_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int state = Cpu.LF + alpha;
		Cpu.saveStack(Cpu.lengthenPointer(state));
	};
	
	// LSK - Load Stack
	public static final OpImpl ESC_x23_LSK_alpha = () -> {
		int alpha = Mem.getNextCodeByte();
		int state = Cpu.LF + alpha;
		Cpu.loadStack(Cpu.lengthenPointer(state));
	};
	
	// XF - XFER and Free
	public static final OpImpl ESC_x22_XF_alpha = () -> {
		int ptr = Cpu.LF + Mem.getNextCodeByte();
		Xfer.impl.xfer(
				Mem.readMDSDblWord(ptr, Cpu.TransferDescriptor_dst),
				Mem.readMDSWord(ptr, Cpu.TransferDescriptor_src),
				XferType.xfer,
				true);
	};
	
	// XE - XFER and Enable
	public static final OpImpl ESC_x21_XE_alpha = () -> {
		// start of: BEGIN ENABLE Abort => ERROR;
		try {
			int ptr = Cpu.LF + Mem.getNextCodeByte();
			Mem.writeMDSWord(Cpu.LF, PrincOpsDefs.LocalOverhead_pc, Cpu.PC);
			Xfer.impl.xfer(
					Mem.readMDSDblWord(ptr, Cpu.TransferDescriptor_dst),
					Mem.readMDSWord(ptr, Cpu.TransferDescriptor_src),
					XferType.xfer,
					false);
			Processes.enableInterrupts();
		} catch(MesaAbort mf) {
			// end of: BEGIN ENABLE Abort => ERROR;
			Cpu.ERROR("saveProcess :: received Abort-exception");
		}
	};
	
	/*
	 * 9.5.4 Breakpoints
	 */
	
	// BRK - Break
	public static final OpImpl OPC_x3D_BRK = () -> {
		if (Cpu.breakByte == 0) {
			Cpu.breakTrap();
		} else {
			Opcodes.dispatch(Cpu.breakByte);
			Cpu.breakByte = 0;
		}
	};
	
	/*
	 * (changed chapters) 9.1.4.2 Descriptor Instruction
	 */
	
	// DESC - Descriptor
	public static final OpImpl OPCn_xFD_DESC_word = () -> {
		int word = Mem.getNextCodeWord();
		Cpu.push(Cpu.GFI | 0x0003);
		Cpu.push(word);
	};

}
