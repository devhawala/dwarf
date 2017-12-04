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
 * Constants defined in the <i>Mesa Processor Principles of Operation Version
 * 4.0 (May85)</i> document as wells as the <i>Changed chapters</i> document,
 * as required/useful for the implementation of the mesa engine.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class PrincOpsDefs {
	
	/*
	 * basic sizes of a word, a page etc.
	 */
	public static final int WORD_BITS = 16;

	public static final int ADDRESSBITS_IN_PAGE = 8;
	public static final int WORDS_PER_PAGE = 256;
	public static final int BYTES_PER_PAGE = 512;
	
	public static final int PAGES_PER_SEGMENT = 256; // giving 64K words per segment
	
	/*
	 * basic values
	 */
	public static final short FALSE = 0;
	public static final short TRUE = 1;
	
	/*
	 * sizes of structures 
	 */
	
	// cSS : Mesa evaluation stack depth
	public static final int cSTACK_LENGTH = 14; // fixed by PrincOps (probably: 16 less first/last as guards for StackError)
	
	// cSV : State vector size
	public static final int cSTATE_VECTOR_SIZE = 18; // SlZE[StateVector] + MAX[SIZE [Control Link], 51ZE[FSlndex], SIZE[LONG POINTER]]
	
	// cWM : wake-up mask
	public static final int cWAKEUP_MASK = 10; // ?? processor dependent... ??
	
	// cWDC : maximum wake-up disable counter
	public static final int cWAKEUP_DISABLE_COUNTER = 7; // minimal value fixed by PrincOps
	public static final int WdcMax = 64; // any value > cWAKEUP_DISABLE_COUNTER will do... 
	
	// cTickMin/cTickMax : minimum / maximum tick size in milliseconds
	public static final int cTICK_MIN = 15;
	public static final int cTICK_MAX = 60;
	
	// TYPE CodeSegment
	public static final int cCODESEGMENT_SIZE = 4; // 4 words available at the begin
	
	/*
	 * Constant memory locations
	 */
	
	// mPDA : process data area (LONG POINTER, absolute)
	public static final int mPROCESS_DATA_AREA = 0x00010000; // 0200000B
	
	// mGFT : global frame table (LONG POINTER, absolute) (PrincOps post-4.0)
	public static final int mGLOBAL_FRAME_TABLE = 0x00020000; // 0400000B
	
	// mAV : allocation vector (POINTER, MDS-relative)
	public static final int mALLOCATION_VECTOR = 0x0100; //  0400B = start of page one in MDS
	
	// mSD : system data table (POINTER, MDS-relative, table element: ControlLink = LONG UNSPECIFIED, count: 256 elements)
	private static final int mSYSTEM_DATA_TBL = 0x0200; // 01000B = start of page two in MDS
	public static int getSdMdsPtr(int index) {
		return mSYSTEM_DATA_TBL + ((index & 0xFF) * 2);
	}
	
	// mETT : ESC trap table (POINTER, MDS-relative, table element: ControlLink = LONG UNSPECIFIED, count: 256 elements)
	private static final int mESC_TRAP_TBL = 0x0400; // 02000B = start of page three in MDS, four pages long
	public static int getEttMdsPtr(int index) {
		return mESC_TRAP_TBL + ((index & 0xFF) * 2);
	}
	
	/*
	 * fault queue indexes (3 of 8 possible entries)
	 */
	
	// qFrameFault
	public static final int qFRAME_FAULT = 0;
	
	// qPagefault
	public static final int qPAGE_FAULT = 1;
	
	// qWriteProtectFault
	public static final int qWRITE_PROTECT_FAULT = 2;
	
	/*
	 * system data table indexes
	 */
	
	public static final int sBoot            =  1; //  1B
	public static final int sBoundsTrap      = 14; // 16B
	public static final int sBreakTrap       =  0; //  0B
	public static final int sCodeTrap        =  7; //  7B
	public static final int sControlTrap     =  6; //  6B
	public static final int sDivCheckTrap    = 11; // 13B
	public static final int sDivZeroTrap     = 10; // 12B
	public static final int sInterruptError  = 12; // 14B
	public static final int sHardwareError   =  8; // 10B
	public static final int sOpcodeTrap      =  5; //  5B
	public static final int sPointerTrap     = 15; // 17B
	public static final int sProcessTrap     = 13; // 15B
	public static final int sRescheduleError =  3; //  3B
	public static final int sStackError      =  2; //  2B
	public static final int sUnboundTrap     =  9; // 11B
	public static final int sXferTrap        =  4; //  4B
	
	 
	/*
	 * VM mapping - flags  
	 */
	
	public static final short MAPFLAGS_MASK = (short)0x0007; // PrincOps defines the lower 3 bits, the upper bits are free for use 
	public static final short MAPFLAGS_CLEAR = (short)0x0000;
	public static final short MAPFLAGS_PROTECTED = (short)0x0004;
	public static final short MAPFLAGS_DIRTY = (short)0x0002;
	public static final short MAPFLAGS_REFERENCED = (short)0x0001;
	public static final short MAPFLAGS_VACANT = (short)0x0006; // PROTECTED and DIRTY but not REFERENCED
	
	/*
	 * AVItem-flag-values
	 */
	
	public static final int AVITEM_FRAME    = 0;
	public static final int AVITEM_EMPTY    = 1;
	public static final int AVITEM_INDIRECT = 2;
	public static final int AVITEM_UNUSED   = 3;
	
	/*
	 * Overhead locations of global and local frames
	 */
	
	// global frame overhead for PrincOps up to 4.0:
	// - global frames are always located in an MDS
	// - register GF is a POINTER (16 bit)
	// - the global frame overhead also holds the code base reference 
	public static final int GLOBALOVERHEAD40_SIZE = 4;
	public static final int GlobalOverhead40_available = -4; // unspecified
	public static final int GlobalOverhead40_word = -3;      // flags: 0x0002 = trapxfers ; 0x0001 = codelinks
	public static final int GlobalOverhead40_codebase = -2;  // long pointer to CodeSegment
	
	// global frame overhead for PrincOps > 4.0:
	// - global frames are outside an MDS (or not necessary in an MDS)
	// - register GF is a LONG POINTER (32 bit)
	// - the global frame overhead lacks the code base reference
	public static final int GLOBALOVERHEAD4x_SIZE = 2;
	public static final int GlobalOverhead4x_available = -2; // unspecified
	public static final int GlobalOverhead4x_word = -1;      // flags: 0x0002 = trapxfers ; 0x0001 = codelinks
	
	// flags in global frame overhead word: again common for all PrincOps versions
	public static final int GlobalLinkage_CodeLinks = 0x0001;
	public static final int GlobalLinkage_TrapXfers = 0x0002;
	
	// local frame overhead
	public static final int LOCALOVERHEAD_SIZE = 4;
	public static final int LocalOverhead_word = -4;       // available: byte , fsi: FSIndex
	public static final int LocalOverhead_returnlink = -3; // ShortControlLink
	public static final int LocalOverhead_globallink = -2; // GlobalFrameHandle
	public static final int LocalOverhead_pc = -1;         // cardinal
	
	/*
	 * offsets in global frame table entries
	 */
	public static final int GFTItem_SIZE = 4; // 4 words long
	public static final int GFTItem_globalFrame = 0; // LONG POINTER (GlobalFrameHandle)
	public static final int GFTItem_codebase = 2; // LONG POINTER (TO CodeSegment)
	
	/*
	 * offsets in a PortLink
	 */
	public static final int Port_inport = 0;
	public static final int Port_outport = 2;
	
	
	/*
	 * not part of PrincOps in the strong sense (ok, not at all)
	 * but in fact limits of this specific mesa engine implementation (Dwarf).
	 */ 
	
	public static final int MIN_REAL_ADDRESSBITS = 18;    // 18 bits =>   1024 pages =>   256 kwords = 512 KByte real memory
	public static final int MAX_REAL_ADDRESSBITS = 23;    // 23 bits =>  32768 pages =>  8192 kwords = 16 MByte real memory

	public static final int MAX_VIRTUAL_ADDRESSBITS = 25; // 25 bits => 131072 pages => 32768 kwords = 64 MByte virtual memory
	
}
