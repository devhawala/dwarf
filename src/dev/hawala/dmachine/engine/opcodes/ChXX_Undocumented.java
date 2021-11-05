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
import dev.hawala.dmachine.engine.agents.ProcessorAgent;

/**
 * Implementation of instructions not documented / mentioned in any available PrincOps document,
 * reconstructed from the Dawn (Don Woodward) and Guam (Yasuhiro Hasegawa) sources. 
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class ChXX_Undocumented {
	
	/*
	 *  VERSION - Microcode Version
	 */
	
	public enum MachineType {
		altoI(1),
		altoII(2),
		altoIIXM(3),
		dolphin(4),
		dorado(5),
		dandelion(6),
		dicentra(7),
		daybreak(8),
		daisy(9),
		kiku(10),
		daylight(11),
		tridlion(12),
		dahlia(13);
		
		private final int tp;
		
		public int getTypeId() {
			return this.tp;
		}
		
		private MachineType(int tp) {
			this.tp = tp;
		}
	}
	
	public static MachineType machineType = MachineType.daybreak;
	
	public static final OpImpl ESC_x2F_VERSION = () -> {
		// comment copied from Don Woodwards MP_Undocumented.c :
		/* Version */
	    /*
	        Not in PrincOps manual; from 6085 microcode.
	        1. Push word 0 of VersionResult.
	        2. Push word 1 of VersionResult.
	        VersionResult: TYPE = MACHINE DEPENDENT RECORD [
	           machineType (0: 0..3): MachineType,
	           majorVersion (0: 4..7): [0..17B],  -- incremented by incompatible changes
	           unused (0: 8..13): [0..77B],
	           floatingPoint (0: 14..14): BOOLEAN,
	           cedar (0: 15..15): BOOLEAN,
	           releaseDate (1): CARDINAL];  -- days since January 1, 1901
	    */
//		System.out.printf("## ESC x2F .. VERSION at 0x%08X+0x%04X [insn# %d]\n", Cpu.CB, Cpu.savedPC, Cpu.insns);
	    Cpu.push((machineType.getTypeId() << 12) | 0x0002); // type + floatingPoint(?)
	    Cpu.push(0x8482);                                   // Jan 1 1993 (?)
	};
	
	/*
	 * STOPEMULATOR - Stop emulator
	 */
	public static final OpImpl ESC_x8B_STOPEMULATOR = () -> {
		int timeToRestart = Cpu.popLong();
		String msg = "Mesa engine stopped by instruction STOPEMULATOR";
		System.out.printf(
				"\n**\n** %s [ timeToRestart: 0x%08X => %s ]\n**\n", 
				msg, timeToRestart, ProcessorAgent.getJavaTime(timeToRestart).toString());
		throw new Cpu.MesaStopped(msg);
	};
	
	/*
	 * SUSPEND - Suspend emulator
	 * (implemented like STOPEMULATOR, as it is unknown how the processor is restarted:
	 * simply continuing after the suspend instruction results in a nonsense looping
	 * without visible progress, meaning that the real behavior proceeds somewhere
	 * else than the instruction following SUSPEND after the machine state saved by
	 * SUSPEND was in-loaded back)
	 */
	public static final OpImpl ESC_x8D_SUSPEND = () -> {
		String msg = "Mesa engine stopped by instruction SUSPEND";
		System.out.printf("\n**\n** %s\n**\n", msg);
		throw new Cpu.MesaStopped(msg);
	};
	
	/*
	 * VMFIND
	 */
	
	// VM.interval : 4 words = struct { CARD32 page ; CARD32 count }
	
	private static final int VMDataInternal_PRun_FIRST = 0;
	private static final int VMDataInternal_countRunPad = 1;
	private static final int VMDataInternal_Run_SIZE = 14; // words
	private static final int VMDataInternal_pRunFirst = VMDataInternal_PRun_FIRST + (VMDataInternal_countRunPad * VMDataInternal_Run_SIZE);
	
	// RunBase: TYPE = LONG BASE POINTER TO ARRAY IndexRun OF Run;
	// PRun: TYPE = RunBase RELATIVE ORDERED POINTER [0..LAST[CARDINAL]] TO Run;
	// Find: PROCEDURE [page: VM.PageNumber, rBase: RunBase, pRunTop: PRun]
	//   RETURNS [found: BOOLEAN, pRun: PRun] = MACHINE CODE {MopcodesExtras2.zVMFIND};

	/* attempted reconstruction of the old mesa procedure Find[] from comments in Yasuhiro Hasegawas implementation ::
	 
	 Find: PROCEDURE [page: VM.PageNumber, rBase: RunBase, pRunTop: PRun]
	   RETURNS [found: BOOLEAN, pRun: PRun] = BEGIN
	   
	 pageTop: VM.PageNumber = FIRST[VM.PageNumber] + StoragePrograms.countVM;  --end of VM
	 indexRunFirst: CARDINAL = (pRunFirst - FIRST[PRun])/SIZE[Run];
	
	 IF page >= pageTop THEN Bug[beyondVM];
	
	 vmDatabaseFullSearches = vmDatabaseFullSearches.SUCC;
	 indexRunLow = indexRunFirst;
	
	 indexRunHigh = (pRunTop - FIRST[PRun])/SIZE[Run];
	 DO  --UNTIL search terminates--
	   indexRun = (indexRunLow + indexRunHigh)/2;
	   pageComp = rBase[FIRST[PRun] + indexRun*SIZE[Run]].interval.page;
	   IF pageComp > page
	     THEN indexRunHigh = indexRun - 1
	          -- note that indexRunHigh, a CARDINAL, might be indexDescLow-1 here.
	     ELSE
	       IF page > pageComp
	         THEN indexRunLow = indexRun + 1
	         ELSE GO TO Exact;
	   IF indexRunHigh < indexRunLow THEN GO TO NotExact;
	 ENDLOOP;  --DO UNTIL search terminates--
	 EXITS
	   Exact => {pRun = FIRST[PRun] + indexRun*SIZE[Run]; found = TRUE};
	   NotExact =>
	     -- Assert: page>"indexRunHigh".page AND page<"indexRunHigh+1".page AND indexRunHigh+1 = indexRunLow.
	     IF indexRunLow = indexRunFirst THEN {pRun = pRunFirst; found = FALSE}
	     ELSE
	       BEGIN
	       pRun = FIRST[PRun] + indexRunHigh*SIZE[Run];
	       IF page < rBase[pRun].interval.page + rBase[pRun].interval.count THEN
	         found = TRUE
	       ELSE {pRun = pRun + SIZE[Run]; found = FALSE};
	       END;
	 END;  --scope of SameAsLastTime--
	 */
	
	public static final OpImpl OPC_xBF_VMFIND = () -> {
		int pRunTop = Cpu.pop() & 0xFFFF;
		int rBase = Cpu.popLong();
		int page = Cpu.popLong();
		
		short found = PrincOpsDefs.FALSE;
		int pRun = 0;
		
		// pageTop: VM.PageNumber = FIRST[VM.PageNumber] + StoragePrograms.countVM;  --end of VM
		int pageTop = 0 + Mem.getVirtualPagesSize();
		
		// indexRunFirst: CARDINAL = (pRunFirst - FIRST[PRun])/SIZE[Run];
		final int indexRunFirst = (VMDataInternal_pRunFirst - VMDataInternal_PRun_FIRST) / VMDataInternal_Run_SIZE;
		
		// IF page >= pageTop THEN Bug[beyondVM];
		if (page >= pageTop) { Cpu.ERROR("VMFIND :: beyondVM (page >= pageTop)"); }
		
		// vmDatabaseFullSearches = vmDatabaseFullSearches.SUCC;
		// indexRunLow = indexRunFirst;
		int indexRunLow = indexRunFirst;

		// indexRunHigh = (pRunTop - FIRST[PRun])/SIZE[Run];
		int indexRunHigh = (pRunTop - VMDataInternal_PRun_FIRST) / VMDataInternal_Run_SIZE;
		
		// DO  --UNTIL search terminates--
		while(true) {
			// indexRun = (indexRunLow + indexRunHigh)/2;
			int indexRun = (indexRunLow + indexRunHigh) / 2;
			
			// pageComp = rBase[FIRST[PRun] + indexRun*SIZE[Run]].interval.page;
			// offset of .interval.page is: 0 words
			int pageComp  = Mem.readDblWord(rBase + VMDataInternal_PRun_FIRST + (indexRun * VMDataInternal_Run_SIZE) + 0);
			
			// IF pageComp > page
			//   THEN indexRunHigh = indexRun - 1
			//     -- note that indexRunHigh, a CARDINAL, might be indexDescLow-1 here.
			//   ELSE
			//     IF page > pageComp
			//       THEN indexRunLow = indexRun + 1
			//       ELSE GO TO Exact;
			if (page < pageComp) {
				indexRunHigh = indexRun - 1;
			} else if (pageComp < page) {
				indexRunLow = indexRun + 1;
			} else {
				// Exact => {pRun = FIRST[PRun] + indexRun*SIZE[Run]; found = TRUE};
				pRun = 0 + indexRun * VMDataInternal_Run_SIZE;
				found = PrincOpsDefs.TRUE;
				break;
			}

			//  IF indexRunHigh < indexRunLow THEN GO TO NotExact;
			if (indexRunHigh < indexRunLow) {
				//  NotExact =>
				//    -- Assert: page>"indexRunHigh".page AND page<"indexRunHigh+1".page AND indexRunHigh+1 = indexRunLow.
				//    IF indexRunLow = indexRunFirst THEN {pRun = pRunFirst; found = FALSE}
				//    ELSE
				//      BEGIN
				//      pRun = FIRST[PRun] + indexRunHigh*SIZE[Run];
				//      IF page < rBase[pRun].interval.page + rBase[pRun].interval.count THEN
				//        found = TRUE
				//      ELSE {pRun = pRun + SIZE[Run]; found = FALSE};
				//      END;
				if (indexRunLow == indexRunFirst) {
					pRun = VMDataInternal_pRunFirst;
					found = PrincOpsDefs.FALSE;
				} else {
					pRun = VMDataInternal_PRun_FIRST + (indexRunHigh * VMDataInternal_Run_SIZE);
					int intervalPage  = Mem.readDblWord(rBase + pRun + 0); // offset of .interval.page is: 0 words
					int intervalCount = Mem.readDblWord(rBase + pRun + 2); // offset of .interval.count is: 2 words
					if (page < (intervalPage + intervalCount)) {
						found = PrincOpsDefs.TRUE;
					} else {
						pRun = pRun + VMDataInternal_Run_SIZE;
						found = PrincOpsDefs.FALSE;
					}
				}
				break;
			}			
		}
		// ENDLOOP;  --DO UNTIL search terminates--
		// EXITS
		//   Exact => {pRun = FIRST[PRun] + indexRun*SIZE[Run]; found = TRUE};
		//   NotExact =>
		//     -- Assert: page>"indexRunHigh".page AND page<"indexRunHigh+1".page AND indexRunHigh+1 = indexRunLow.
		//     IF indexRunLow = indexRunFirst THEN {pRun = pRunFirst; found = FALSE}
		//     ELSE
		//       BEGIN
		//       pRun = FIRST[PRun] + indexRunHigh*SIZE[Run];
		//       IF page < rBase[pRun].interval.page + rBase[pRun].interval.count THEN
		//         found = TRUE
		//       ELSE {pRun = pRun + SIZE[Run]; found = FALSE};
		//       END;
		// END;  --scope of SameAsLastTime--
		
		Cpu.push(found);
		Cpu.push(pRun);
	};
	
	/*
	 * undocumented Fuji-Xerox (?) instructions implemented in Yasuhiro Hasegawa's Guam emulator
	 */
	
	public static final OpImpl ESC_x8C_FujiXerox_undocumented_o214 = () -> {
		//System.out.printf("--- ESC_x8C_FujiXerox_undocumented_o214\n");
		Cpu.popLong();
		Cpu.popLong();
		Cpu.popLong();
		Cpu.push(0);
	};
	
	public static final OpImpl ESC_xC5_FujiXerox_undocumented_o305 = () -> {
		//System.out.printf("--- ESC_xC5_FujiXerox_undocumented_o305\n");
	};
	
	public static final OpImpl ESC_xC6_FujiXerox_undocumented_o306 = () -> {
		//System.out.printf("--- ESC_xC6_FujiXerox_undocumented_o306\n");
		Cpu.pop();
		Cpu.push(0);
	};

}