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

/**
 * Implementation of common functionality for PrincOps chapter "9 Control Transfers",
 * this is allocation/freeing local frames as well as the XFER primitive and related
 * functions.
 * <br>
 * The core XFER primitive is present in 2 implementations, allowing to support
 * the 2 variants of PrincOps:
 * <ul>
 * <li>
 * "old" PrincOps up to version 4.0: this variant has all global frames in the
 * main data space, using a 16 bit GF register.
 * </li>
 * <li>
 * "new" PrincOps after version 4.0 (see also document "Changed Chapters"): this
 * variant can have global frames outside the main data space, using a 32 bit GF
 * register and an additional table managing global frames (at location GFT);
 * furthermore the overhead area for global frames has changed.  
 * </li>
 *  </ul>
 *  <p>
 *  The 2 variants are supported through the interface {@code Xferer} having an
 *  implementation for each PrincOps variant. The current implementation for the XFER
 *  primitive is accessed through the public global variable {@code Xfer.impl}, which is
 *  initialized for a PrincOps 4.0 compatible XFER implementation and must be explicitely
 *  changed to the post-4.0 XFER implementation by calling the {@code switchToNewPrincOps()}
 *  method.
 *  </p>
 *  <p>
 *  <b>Important fine point</b>: PrincOps defines the ControlLink type as 32 bit quantity,
 *  accessed either as LONG UNSPECIFIED or as MACHINE DEPENDENT RECORD to access
 *  the subfields of the ControlLink. As Mesa uses a mixed-endian ordering, word 0
 *  in a MACHINE DEPENDENT RECORD identifies the lower word of the 32 bit quantity,
 *  whereas in Java (pure big endian) word 0 is the <i>upper</i> word of a 32 bit quantity!
 *  <br>
 *  A first attempt to resolve this contradiction was to load ControlLinks words
 *  in the order allowing to interpret the 32 bit quantity directly like the mesa
 *  MACHINE DEPENDENT RECORD. However, this approach failed as arbirary POINTERs
 *  can be reinterpreted as ControlLink to yield a frameLink. This resulted in various
 *  possibilities for mis-interpreting the resulting 32 bit quantity when casting a
 *  POINTER as ControlLink.
 *  <br>
 *  So ControlLinks are now handled (read from memory, reinterpreted from POINTERs)
 *  in the standard way to yield a (Java) 32 bit quantity, but the usage of words
 *  0 and 1 during the interpretation as MACHINE DEPENDENT RECORD for a ControlLink
 *  uses the reversed  word order. 
 *  </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Xfer {
	
	/*
	 * local frames
	 */
	
	private static final int AV = PrincOpsDefs.mALLOCATION_VECTOR; 
	private static final int AVITEM_TAGMASK = 0x0003;
	private static final int FSINDEX_LAST = 255;
	
	private static int /* LocalFrameHandle */  AVFrame(int avItem) {
		if ((avItem & AVITEM_TAGMASK) != PrincOpsDefs.AVITEM_FRAME) {
			Cpu.ERROR("AVFrame :: not an AVITEM_FRAME");
		}
		return (short)(avItem & 0xFFFC);
	}
	
	private static int /* AVItem */ AVLink(int avItem) {
		if ((avItem & AVITEM_TAGMASK) != PrincOpsDefs.AVITEM_FRAME) {
			Cpu.ERROR("AVLink :: not an AVITEM_FRAME");
		}
		return (short)(avItem & 0xFFFC);
	}
	
	public static int /* pointer = local-frame */ alloc(/* FSIndex */ int fsi) {
		int item;
		int slot = fsi;
		while(true) {
			item = Mem.readMDSWord(AV, slot) & 0xFFFF;
			if ((item & AVITEM_TAGMASK) != PrincOpsDefs.AVITEM_INDIRECT) {
				break;
			}
			int itemData = item >>> 2;
			if (itemData > FSINDEX_LAST) { Cpu.ERROR("alloc :: itemData > FSINDEX_LAST (invalid 'next' frame size index)"); }
			slot = itemData;
		}
		if ((item & AVITEM_TAGMASK) == PrincOpsDefs.AVITEM_EMPTY) {
			Cpu.signalFrameFault(fsi);
		}
		// read the next frame item from the new frame and store it in the AV[slot]
		Mem.writeMDSWord(AV, slot, Mem.readMDSWord(AVLink(item)));
		// return the new frame
		return AVFrame(item);
	}
	
	public static void free(/* LocalFrameHandle */ int frame) {
		// get the fsi of this frame
		int word = Mem.readMDSWord(frame, PrincOpsDefs.LocalOverhead_word);
		int fsi = word & 0x00FF;
		
		// get the current value at AV[fsi]
		int item = Mem.readMDSWord(AV, fsi);
		Mem.writeMDSWord(frame, item);
		
		// put the frame (implicitly a FRAME-AVItem)
		Mem.writeMDSWord(AV, fsi, frame);
	}
	
	/*
	 * link types
	 */
	
	public enum LinkType {
		frame,
		oldProcedure,
		indirect,
		newProcedure
	};
	
	public static LinkType getControlLinkType(int controlLink) {
		int tag = controlLink & 0x00000003;
		if (tag == 0) { return LinkType.frame; }
		if (tag == 0x00000001) { return LinkType.oldProcedure; }
		if (tag == 0x00000002) { return LinkType.indirect; }
		return LinkType.newProcedure;
	}
	
	public static int /* POINTER */ makeFrameLink(int controlLink) {
		if ((controlLink & 0x00000003) != 0) { Cpu.ERROR("makeFrameLink :: not a frame link"); }
		return (controlLink & 0x0000FFFF);
//		not: return (controlLink >>> 16);
	}
	
	public static int /* POINTER TO ControlLink */ makeIndirectLink(int controlLink) {
		if ((controlLink & 0x00000003) != 0x00000002) { Cpu.ERROR("makeIndirectLink :: not an indirect link"); }
		return (controlLink & 0x0000FFFF);
//		not: return (controlLink >>> 16);
	}
	
	public static int /* UNSPECIFIED */ makeProcDesc_taggedGF(int controlLink) {
		if ((controlLink & 0x00000003) != 0x00000001) { Cpu.ERROR("makeProcDesc_taggedGF :: not a procDesc"); }
		return (controlLink & 0x0000FFFF);
//		not: return controlLink >>> 16;
	}
	
	public static int /* UNSPECIFIED */ makeProcDesc_pc(int controlLink) {
		if ((controlLink & 0x00000003) != 0x00000001) { Cpu.ERROR("makeProcDesc_pc :: not a procDesc"); }
		return (controlLink >>> 16);
//		not: return controlLink  & 0x0000FFFF;
	}
	
	
	/* ****************** */
	
	/*
	 * 9.3 Control Transfer Primitive
	 */
	
	public enum XferType {
		xreturn(0),
		xcall(1),
		xlocalCall(2),
		xport(3),
		xfer(4),
		xtrap(5),
		xprocessSwitch(6),
		xunused(7);
		
		private final int value;
		
		private XferType(int value) { this.value = value; }
		
		public int getValue() { return this.value; }
	};

	/**
	 * Definition of the functionality for the XFER primitive that depend
	 * from the PrincOps version.
	 */
	public interface Xferer {
		// transfer primitive proper
		void xfer(/*ControlLink*/ int dst, /*ShortControlLink*/ int src, XferType xferType, boolean free);
		
		// get control link from global frame or code segment
		int fetchLink(int offset /* offset: BYTE 0..255 */);
		
		// check if xfer traps are requested and possibly trap accordingly 
		void checkForXferTraps(/*ControlLink*/ int dst, XferType xferType);
	}
	
	public static Xferer impl = new XfererPrincops40();
	
	public static void switchToNewPrincOps() {
		impl = new XfererPrincops4x();
	}
	
	/*
	 *  XFER-functionality for PrincOps up to 4.0
	 */
	
	private static class XfererPrincops40 implements Xferer {
		
		/*
		 * 9.3 Control Transfer Primitive
		 */

		@Override
		public void xfer(int dst, int src, XferType xferType, boolean free) {
			int nPC = 0; // new PC
			int nLF = 0; // new LF
			boolean push = false;
			int nDst = dst; // controlLink
			
			if (xferType == XferType.xtrap && free) { Cpu.ERROR("xfer :: xferType = trap && free = true"); }
			
			while(getControlLinkType(nDst) == LinkType.indirect) {
				int link = makeIndirectLink(nDst);
				if (xferType == XferType.xtrap) { Cpu.ERROR("xfer :: xferType = trap in indirect link type upchain"); }
				nDst = Mem.readMDSDblWord(link);
				push = true;
			}
			
			switch(getControlLinkType(nDst)) {
			case oldProcedure:
			case newProcedure:
				// fields of "proc: ProcDesc"
//				not: int proc_taggedGF = nDst >>> 16;
//				not: int proc_pc = nDst & 0x0000FFFF;
				int proc_taggedGF = nDst & 0x0000FFFF;
				int proc_pc = nDst >>> 16;
				
				// set global frame
				Cpu.GF16 = proc_taggedGF & 0xFFFE;
				Cpu.GF32 = Cpu.MDS + Cpu.GF16;
				if (Cpu.GF16 == 0) { Cpu.unboundTrap(dst); }
				
				// set code base
				Cpu.CB = Mem.readMDSDblWord(Cpu.GF16 + PrincOpsDefs.GlobalOverhead40_codebase);
				if ((Cpu.CB & 0x00000001) != 0) { Cpu.codeTrap(Cpu.GF16); }
				
				// check new pc
				nPC = proc_pc;
				if (nPC == 0) { Cpu.unboundTrap(dst); }
				
				// get the local frame index and allocate the new local frame
				int word = Mem.readCode(nPC / 2);		
				nLF = alloc(((nPC & 0x0001) == 0) ? word >>> 8 : word & 0xFF);
				nPC++;
				
				// setup the new local frame (global frame link, return link)
				Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink, Cpu.GF16);
				Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink, src);
				
				break;
				
			case frame:
				int frame = makeFrameLink(nDst);
				if (frame == 0) { Cpu.controlTrap(src); }
				
				nLF = frame;
				
				Cpu.GF16 = Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink);
				Cpu.GF32 = Cpu.MDS + Cpu.GF16;
				if (Cpu.GF16 == 0) { Cpu.unboundTrap(nDst); }
				
				Cpu.CB = Mem.readMDSDblWord(Cpu.GF16, PrincOpsDefs.GlobalOverhead40_codebase);
				if ((Cpu.CB & 0x00000001) != 0) { Cpu.codeTrap(Cpu.GF16); }
				
				nPC = Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_pc);
				if (nPC == 0) { Cpu.unboundTrap(dst); }
				
				if (xferType == XferType.xtrap) {
					Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink, src);
					Processes.disableInterrupts();
				}
				
				break;
				
			default:
				// this cannot happen, as indirect links were already processed
			}
			
			if (push) {
				Cpu.push(dst & 0x0000FFFF);
//				not: Cpu.push(dst >>> 16);
				Cpu.push(src);
				Cpu.discard();
				Cpu.discard();
			}
			
			if (free) {
				free(Cpu.LF);
			}
			
			Cpu.LF = nLF;
			Cpu.PC = nPC;
			
			checkForXferTraps(dst, xferType);
		}
		
		/*
		 * 9.4.2 External Function Calls
		 */
		
		@Override
		public int fetchLink(int offset) {
			int globalWord = Mem.readMDSWord(Cpu.GF16, PrincOpsDefs.GlobalOverhead40_word);
			if ((globalWord & PrincOpsDefs.GlobalLinkage_CodeLinks) == PrincOpsDefs.GlobalLinkage_CodeLinks) {
				return Mem.readDblWord(Cpu.CB - ((offset + 1) * 2));
			} else {
				return Mem.readMDSDblWord(Cpu.GF16 - PrincOpsDefs.GLOBALOVERHEAD40_SIZE - ((offset + 1) * 2));
			}
		}
		
		/*
		 * 9.5.5 Xfer Traps
		 */
		
		@Override
		public void checkForXferTraps(int dst, XferType xferType) {
			if ((Cpu.XTS & 0x0001) != 0) {
				int globalWord = Mem.readMDSWord(Cpu.GF16 + PrincOpsDefs.GlobalOverhead40_word);
				if ((globalWord & PrincOpsDefs.GlobalLinkage_TrapXfers) != 0) {
					Cpu.XTS = Cpu.XTS >>> 1;
					Cpu.nakedTrap(PrincOpsDefs.sXferTrap); // TODO: ?? ! Abort => ERROR
					Mem.writeMDSWord(Cpu.LF, 0, (short)(dst & 0xFFFF));
					Mem.writeMDSWord(Cpu.LF, 1, (short)(dst >>> 16));
					Mem.writeMDSWord(Cpu.LF, 2, (short)xferType.getValue());
					throw new Cpu.MesaAbort();
				}
			} else {
				Cpu.XTS = Cpu.XTS >>> 1;
			}
		}
	}
	
	/*
	 * XFER-functionality for PrincOps after 4.0 ("MDS-relieved")
	 */
	
	private static class XfererPrincops4x implements Xferer {
		
		/*
		 * 9.3 Control Transfer Primitive
		 */
		
		// TODO: hier scheint es in den ChangedChapters und evtl. den Implementierungen
		//		 ein Durcheinander zwischen GlobalFrameIndex und GlobalFrameHandle zu geben
		//        -> GlobalFrameIndex ist der Index in der GFT (1-er Sprünge über 4-word Elemente)
		//        -> GlobalFrameHandle ist der relative POINTER zum Anfang eines Eintrags (4-er Sprünge)

		@Override
		public void xfer(int dst, int src, XferType xferType, boolean free) {
			int nPC = 0; // new PC
			int nLF = 0; // new LF
			boolean push = false;
			int nDst = dst; // controlLink
			
			if (Config.LOG_OPCODES) {
				Cpu.logf(
						  "   xfer dst=0x%08X src=0x%04X xferType=%s free=%s\n"
						+ "     currGFI=0x%04X, currGF32=0x%08X, currCB=0x%08X, currPC=0x%04X, currLF=0x%04X\n",
						dst, src, xferType.toString(), (free)?"true":"false",
						Cpu.GFI, Cpu.GF32, Cpu.CB, Cpu.PC, Cpu.LF
						);
			}
			
			if (xferType == XferType.xtrap && free) { Cpu.ERROR("xfer :: xferType = trap && free = true"); }
			
			while(getControlLinkType(nDst) == LinkType.indirect) {
				int link = makeIndirectLink(nDst);
				if (xferType == XferType.xtrap) { Cpu.ERROR("xfer :: xferType = trap in indirect link type upchain"); }
				nDst = Mem.readMDSDblWord(link);
				if (Config.LOG_OPCODES) { Cpu.logf("     -> indirect-link => new dst=0x%08X\n", nDst); }
				push = true;
			}
			
			switch(getControlLinkType(nDst)) {
			case oldProcedure: {
				// fields of "proc: ProcDesc"
//				not: int proc_taggedGF = nDst >>> 16;
//				not: int proc_pc = nDst & 0x0000FFFF;
				int proc_taggedGF = nDst & 0x0000FFFF;
				int proc_pc = nDst >>> 16;
				
				// get global frame index
				// (gfi seems to be hidden in the overhead 'word' in the (up to 4.0)
				// unused bits above the linkage flags in a pseudo global frame in MDS)
				// unsure: use <= 4.0 or > 4.0 offsets in overhead area?
				int gf = proc_taggedGF & 0xFFFC;
				if (gf == 0) { Cpu.unboundTrap(dst); }
				Cpu.GFI = Mem.readMDSWord(gf, PrincOpsDefs.GlobalOverhead4x_word) & 0xFFFC;
				if (Cpu.GFI == 0) { Cpu.unboundTrap(dst); }
//				int gftItemPtr = Cpu.GFT + (Cpu.GFI * PrincOpsDefs.GFTItem_SIZE); -- does not work...
				int gftItemPtr = Cpu.GFT + Cpu.GFI; // as GFI is already shifted left by 2 bits, GFI is the word offset of the gftItem in GFT
				
				// set global frame
				Cpu.GF32 = Mem.readDblWord(gftItemPtr + PrincOpsDefs.GFTItem_globalFrame);
				
				// set code base
				Cpu.CB = Mem.readDblWord(gftItemPtr + PrincOpsDefs.GFTItem_codebase);
				if ((Cpu.CB & 0x00000001) != 0) { Cpu.codeTrap(Cpu.GFI); }
				
				// check new pc
				nPC = proc_pc;
				if (nPC == 0) { Cpu.unboundTrap(dst); }
				
				// get the local frame index and allocate the new local frame
				int word = Mem.readCode(nPC / 2) & 0xFFFF;
				int nFsi = ((nPC & 0x0001) == 0) ? word >>> 8 : word & 0xFF;
				nLF = alloc(nFsi);
				nPC++;
				
				// setup the new local frame (global frame link, return link)
				Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink, Cpu.GFI);
				Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink, src);
			
				if (Config.LOG_OPCODES) {
					Cpu.logf(
						  "     -> oldProcedure-xfer\n"
						+ "     -> GFI=0x%04X GF32=0x%08X CB=0x%08X newPC=0x%04X newFsi=%d\n"
						+ "     -> newLF=0x%04X newLF.globalLink=0x%04X newLF.returnLink=0x%04X\n",
						Cpu.GFI, Cpu.GF32, Cpu.CB, nPC, nFsi,
						nLF, Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink), Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink)
						);
				}
			}
			break;
				
			case newProcedure: {
					// fields of "proc: ProcDesc"
//					not: int proc_taggedGF = nDst >>> 16;
//					not: int proc_pc = nDst & 0x0000FFFF;
					int proc_taggedGF = nDst & 0x0000FFFF;
					int proc_pc = nDst >>> 16;
					
					// get global frame index
					Cpu.GFI = proc_taggedGF & 0xFFFC;
					if (Cpu.GFI == 0) { Cpu.unboundTrap(dst); }
//					int gftItemPtr = Cpu.GFT + (Cpu.GFI * PrincOpsDefs.GFTItem_SIZE); -- does not work...
					int gftItemPtr = Cpu.GFT + Cpu.GFI; // as GFI is already shifted left by 2 bits, GFI is the word offset of the gftItem in GFT 
					
					// set global frame
					Cpu.GF32 = Mem.readDblWord(gftItemPtr + PrincOpsDefs.GFTItem_globalFrame);
					
					//set code base
					Cpu.CB = Mem.readDblWord(gftItemPtr + PrincOpsDefs.GFTItem_codebase);
					if ((Cpu.CB & 0x00000001) != 0) { Cpu.codeTrap(Cpu.GFI); }
					
					// check new pc
					nPC = proc_pc;
					if (nPC == 0) { Cpu.unboundTrap(dst); }
					
					// get the local frame index and allocate the new local frame
					int word = Mem.readCode(nPC / 2);
					int nFsi = ((nPC & 0x0001) == 0) ? word >>> 8 : word & 0xFF;
					nLF = alloc(nFsi);
					nPC++;
					
					// setup the new local frame (global frame link, return link)
					Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink, Cpu.GFI);
					Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink, src);
			
					if (Config.LOG_OPCODES) {
						Cpu.logf(
							  "     -> newProcedure-xfer\n"
							+ "     -> GFI=0x%04X GF32=0x%08X CB=0x%08X newPC=0x%04X newFsi=%d\n"
							+ "     -> newLF=0x%04X newLF.globalLink=0x%04X newLF.returnLink=0x%04X\n",
							Cpu.GFI, Cpu.GF32, Cpu.CB, nPC, nFsi,
							nLF, Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink), Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink)
							);
					}
				}
				break;
				
			case frame: {
					int frame = makeFrameLink(nDst);
					if (frame == 0) { Cpu.controlTrap(src); }
					
					nLF = frame;
					
					Cpu.GFI = Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink);
					if (Cpu.GFI == 0) { Cpu.unboundTrap(nDst); }
//					int gftItemPtr = Cpu.GFT + (Cpu.GFI * PrincOpsDefs.GFTItem_SIZE); -- does not work...
					int gftItemPtr = Cpu.GFT + Cpu.GFI; // as GFI is already shifted left by 2 bits, GFI is the word offset of the gftItem in GFT
					
					Cpu.GF32 = Mem.readDblWord(gftItemPtr + PrincOpsDefs.GFTItem_globalFrame);
					
					Cpu.CB = Mem.readDblWord(gftItemPtr + PrincOpsDefs.GFTItem_codebase);
					if ((Cpu.CB & 0x00000001) != 0) { Cpu.codeTrap(Cpu.GFI); }
					
					nPC = Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_pc) & 0xFFFF;
					if (nPC == 0) { Cpu.unboundTrap(dst); }
					
					if (xferType == XferType.xtrap) {
						Mem.writeMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink, src);
						Processes.disableInterrupts();
					}
				
					if (Config.LOG_OPCODES) {
						Cpu.logf(
							  "     -> frame-xfer\n"
							+ "     -> GFI=0x%04X GF32=0x%08X CB=0x%08X newPC=0x%04X\n"
							+ "     -> newLF=0x%04X newLF.globalLink=0x%04X newLF.returnLink=0x%04X\n",
							Cpu.GFI, Cpu.GF32, Cpu.CB, nPC,
							nLF, Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_globallink), Mem.readMDSWord(nLF, PrincOpsDefs.LocalOverhead_returnlink)
							);
					}
				}	
				break;
				
			default:
				// this cannot happen, as indirect links were already processed
			}
			
			if (push) {
				Cpu.push(dst & 0x0000FFFF);
//				not: Cpu.push(dst >>> 16);
				Cpu.push(src);
				Cpu.discard();
				Cpu.discard();
			}
			
			if (free) {
				free(Cpu.LF);
			}
			
			Cpu.LF = nLF;
			Cpu.PC = nPC;
			
			checkForXferTraps(dst, xferType);
		}
		
		/*
		 * 9.4.2 External Function Calls
		 */
		
		@Override
		public int fetchLink(int offset) {
			int globalWord = Mem.readWord(Cpu.GF32 + PrincOpsDefs.GlobalOverhead4x_word);
			if ((globalWord & PrincOpsDefs.GlobalLinkage_CodeLinks) == PrincOpsDefs.GlobalLinkage_CodeLinks) {
				return Mem.readDblWord(Cpu.CB - (offset + 1) * 2);
			} else {
				return Mem.readDblWord(Cpu.GF32 - PrincOpsDefs.GLOBALOVERHEAD4x_SIZE - (offset + 1) * 2);
			}
		}
		
		/*
		 * 9.5.5 Xfer Traps
		 */
		
		@Override
		public void checkForXferTraps(int dst, XferType xferType) {
			if ((Cpu.XTS & 0x0001) != 0) {
				int globalWord = Mem.readWord(Cpu.GF32 + PrincOpsDefs.GlobalOverhead4x_word);
				if ((globalWord & PrincOpsDefs.GlobalLinkage_TrapXfers) != 0) {
					Cpu.XTS = Cpu.XTS >>> 1;
					Cpu.nakedTrap(PrincOpsDefs.sXferTrap); // TODO: ?? ! Abort => ERROR
					Mem.writeMDSWord(Cpu.LF, 0, (short)(dst & 0xFFFF));
					Mem.writeMDSWord(Cpu.LF, 1, (short)(dst >>> 16));
					Mem.writeMDSWord(Cpu.LF, 2, (short)xferType.getValue());
					throw new Cpu.MesaAbort();
				}
			} else {
				Cpu.XTS = Cpu.XTS >>> 1;
			}
		}
	}
	
}
