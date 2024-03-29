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

import java.security.InvalidParameterException;
import java.util.List;

import dev.hawala.dmachine.engine.PilotDefs.DisplayType;
import dev.hawala.dmachine.engine.iop6085.IORegion;
import dev.hawala.dmachine.engine.iop6085.IORegion.Field;
import dev.hawala.dmachine.engine.iop6085.IORegion.IORAddress;

/**
 * Implementation of the mesa engine real and virtual memory including
 * display memory in mesa megine address space.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Mem {

	// the real memory
	protected static short[] mem = null;
	
	// virtual memory part 1: map virtual-page => real-page-base address
	private static int[] pageMap = null; // != PrincOps: this hold the real page base address (to speed up things), not the real page no
	
	// virtual memory part 2:  map virtual-page => page-flags
	// (defined at package-level to allow class Processes to access it for handling UI screen refresh) 
	static short[] pageFlags = null;
	
	// number of address-bits in virtual and real addresses (externally configured)
	private static int addressBitsVirtual = 0;
	private static int addressBitsReal = 0;
	
	// memory limits resulting from 'addressBitsVirtual' and 'addressBitsReal' 
	private static int lastVirtualAddress;
	private static int lastVirtualPage;
	private static int lastRealPage;
	
	// display memory characteristics
	private static int effectivePixelsPerLine; // pixels on a scan line in memory
	public static int displayPixelWidth;      // pixels on a scan line displayed
	public static int displayPixelHeight;     // number of vertical scan lines
	public static int displayFirstRealPage;
	public static int displayPageSize;        // total number of pages for the display memory
	static int displayFirstMappedVirtualPage;  // (package-level to allow class Processes to access it)
	
	/*
	 * Machine-Type GUAM: memory and virtual-memory-map setup at machine start
	 */
	
	// reserve the half of the first bank for IO devices 
	private static final int IOAREA_PAGECOUNT = PrincOpsDefs.PAGES_PER_SEGMENT / 2;
	
	// first virtual page of the ioArea
	private static final int IOAREA_START_VPAGE = PrincOpsDefs.PAGES_PER_SEGMENT - IOAREA_PAGECOUNT;
	
	// virtual address of the ioArea-start (LONG POINTER) 
	public static final int ioArea = IOAREA_START_VPAGE * PrincOpsDefs.WORDS_PER_PAGE;
	
	public static void initializeMemoryGuam(int addrBitsVirtual, int addrBitsReal) {
		initializeMemoryGuam(addrBitsVirtual, addrBitsReal, PilotDefs.DisplayType.monochrome, 960, 720);
	}
	
	public static void initializeMemoryGuam(
			int addrBitsVirtual,
			int addrBitsReal,
			PilotDefs.DisplayType displayType,
			int displayWidth,
			int displayHeight) {
		if (mem != null) {
			throw new IllegalStateException("MesaEngine memory already initialized");
		}
		if (addrBitsVirtual > PrincOpsDefs.MAX_VIRTUAL_ADDRESSBITS) {
			throw new InvalidParameterException("Requested virtual memory address bit count exceeds limit");
		}
		if ((displayWidth % 16) != 0) {
			throw new InvalidParameterException("Requested displayWidth not a multiple of 16");
		}
		if (displayType != PilotDefs.DisplayType.monochrome && displayType != PilotDefs.DisplayType.byteColor) {
			throw new IllegalArgumentException("Unsupported 'displayType' = " + displayType);
		}
		
		// Real mem: min. 1024 pages .. max. 32768 pages (512 Kbyte .. 16 MByte)
		addressBitsReal = Math.max(PrincOpsDefs.MIN_REAL_ADDRESSBITS,  Math.min(addrBitsReal, PrincOpsDefs.MAX_REAL_ADDRESSBITS));
		
		// Virtual mem: min. real-pages .. max. 131072 pages (real-mem .. 64 MByte)
		addressBitsVirtual = Math.min(PrincOpsDefs.MAX_VIRTUAL_ADDRESSBITS, Math.max(addrBitsVirtual, addressBitsReal));
		
		// Pilot/BWS seems to use multiple of 512 pixels per line for bytecolor
		// which is the number of pixels pas memory page (2 pixels / word => 1 page == 512 pixels)
		effectivePixelsPerLine = (displayType == PilotDefs.DisplayType.monochrome)
				? displayWidth
				: ((displayWidth + 511) / 512) * 512;
		
		// additional real memory required for display
		int displayMemoryNeededWords
				= (((effectivePixelsPerLine * displayType.getBitDepth()) + PrincOpsDefs.WORD_BITS - 1)
				/ PrincOpsDefs.WORD_BITS)
				* displayHeight;
		displayPageSize = (displayMemoryNeededWords + PrincOpsDefs.WORDS_PER_PAGE - 1) / PrincOpsDefs.WORDS_PER_PAGE;
		displayPixelWidth = displayWidth;
		displayPixelHeight = displayHeight;
		activeDisplayType = displayType;
		
		// allocate our memory and state arrays
		// -> mem is a contiguous word array containing
		//     - the real memory available to the mesa processor for mapping to virtual pages
		//     - plus the display memory appended to real memory
		// -> real pages available for mapping are [0..lastRealPage)
		// -> display memory real pages are [lastRealPage+1..lastRealPage+displayPageSize)
		int wordCount = PrincOpsDefs.WORDS_PER_PAGE << (addressBitsReal - PrincOpsDefs.ADDRESSBITS_IN_PAGE);
		int virtualPageCount = 1 << (addressBitsVirtual - PrincOpsDefs.ADDRESSBITS_IN_PAGE);
		int realPageCount = 1 << (addressBitsReal - PrincOpsDefs.ADDRESSBITS_IN_PAGE);
		mem = new short[wordCount + (displayPageSize * PrincOpsDefs.WORDS_PER_PAGE)];
		pageMap = new int[virtualPageCount + displayPageSize];
		pageFlags = new short[virtualPageCount + displayPageSize];
		lastVirtualAddress = (PrincOpsDefs.WORDS_PER_PAGE * virtualPageCount) - 1;
		lastVirtualPage = virtualPageCount - 1;
		lastRealPage = realPageCount - 1;
		displayFirstRealPage = lastRealPage + 1;
		
		// initialize virtual and display memory
		createInitialPageMappingGuam();
		if (activeDisplayType == PilotDefs.DisplayType.monochrome) {
			initializeDisplayMemoryGuam();
		} else {
			initializeColorDisplayMemoryGuam();
		}
	}
	
	public static void createInitialPageMappingGuam() {
		/*
		 * for some reasons not documented in PrincOps, the IO area in the first (virtual) segment
		 * has to be mapped starting at real page/address 0 
		 * (germ? ; pilot?)
		 */
		
		// variables to build initial page mappings
		int currRealAddress = 0;
		int currRealPage = 0;
		int currVirtualPage = IOAREA_START_VPAGE;
		
		// map IOArea (real pages starting at 0, virtual pages starting at IOAREA_START_VPAGE)
		while(currRealPage < IOAREA_PAGECOUNT) {
			pageMap[currVirtualPage] = currRealAddress;
			pageFlags[currVirtualPage] = PrincOpsDefs.MAPFLAGS_CLEAR;
			currRealPage++;
			currVirtualPage++;
			currRealAddress += PrincOpsDefs.WORDS_PER_PAGE;
		}
		
		// map the rest of the first segment (virtual pages starting at 0, real pages behind (real) io area)
		currVirtualPage = 0;
		while(currRealPage < PrincOpsDefs.PAGES_PER_SEGMENT) {
			pageMap[currVirtualPage] = currRealAddress;
			pageFlags[currVirtualPage] = PrincOpsDefs.MAPFLAGS_CLEAR;
			currRealPage++;
			currVirtualPage++;
			currRealAddress += PrincOpsDefs.WORDS_PER_PAGE;
		}
		
		// map the remaining real pages to the same virtual page addresses
		currRealAddress = PrincOpsDefs.PAGES_PER_SEGMENT * PrincOpsDefs.WORDS_PER_PAGE;
		for (int i = PrincOpsDefs.PAGES_PER_SEGMENT; i <= lastRealPage; i++) {
			pageMap[i] = currRealAddress;
			pageFlags[i] = PrincOpsDefs.MAPFLAGS_CLEAR;
			currRealAddress += PrincOpsDefs.WORDS_PER_PAGE;
		}
		
		// set the rest of virtual memory pages to "unmapped"
		for (int i = (lastRealPage + 1); i <= lastVirtualPage; i++) {
			pageMap[i] = 0;
			pageFlags[i] = PrincOpsDefs.MAPFLAGS_VACANT;
		}
	}
	
	// return the number of addressable virtual pages
	public static int getVirtualPagesSize() {
		return lastVirtualPage + 1;
	}
	
	// return the number of real pages in mem[] available for mapping to virtual pages 
	public static int getRealPagesSize() {
		return lastRealPage + 1;
	}
	
	// put some pattern into display memory indicating that the display has still
	// not been initialized by the OS being booted (i.e. Pilot resp. its client XDE or ViewPoint/GlobalView)
	private static void initializeDisplayMemoryGuam() {
		int displayWord = mem.length - (displayPageSize * PrincOpsDefs.WORDS_PER_PAGE);
		int wordsPerLine = displayPixelWidth / PrincOpsDefs.WORD_BITS;
		short[] template = {
			(short)0b1000000000000001,
			(short)0b0100000000000010,
			(short)0b0010000000000100,
			(short)0b0001000000001000,
			(short)0b0000100000010000,
			(short)0b0000010000100000,
			(short)0b0000001001000000,
			(short)0b0000000110000000,
			(short)0b0000000110000000,
			(short)0b0000001001000000,
			(short)0b0000010000100000,
			(short)0b0000100000010000,
			(short)0b0001000000001000,
			(short)0b0010000000000100,
			(short)0b0100000000000010,
			(short)0b1000000000000001
		};
		int ti = 0;
		for (int i = 0; i < displayPixelHeight; i++) {
			for (int j = 0; j < wordsPerLine; j++) {
				mem[displayWord++] = template[ti];
			}
			ti++;
			if (ti >= template.length) { ti = 0; }
		}
	}
	
	private static void initializeColorDisplayMemoryGuam() {
		int[] template = {
			0x0100 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0001 ,
			0x0001 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0100 ,
			0x0000 , 0x0100 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0001 , 0x0000 ,
			0x0000 , 0x0001 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0100 , 0x0000 ,
			0x0000 , 0x0000 , 0x0100 , 0x0000 , 0x0000 , 0x0001 , 0x0000 , 0x0000 ,
			0x0000 , 0x0000 , 0x0001 , 0x0000 , 0x0000 , 0x0100 , 0x0000 , 0x0000 ,
			0x0000 , 0x0000 , 0x0000 , 0x0100 , 0x0001 , 0x0000 , 0x0000 , 0x0000 ,
			0x0000 , 0x0000 , 0x0000 , 0x0001 , 0x0100 , 0x0000 , 0x0000 , 0x0000 ,
			0x0000 , 0x0000 , 0x0000 , 0x0001 , 0x0100 , 0x0000 , 0x0000 , 0x0000 ,
			0x0000 , 0x0000 , 0x0000 , 0x0100 , 0x0001 , 0x0000 , 0x0000 , 0x0000 ,
			0x0000 , 0x0000 , 0x0001 , 0x0000 , 0x0000 , 0x0100 , 0x0000 , 0x0000 ,
			0x0000 , 0x0000 , 0x0100 , 0x0000 , 0x0000 , 0x0001 , 0x0000 , 0x0000 ,
			0x0000 , 0x0001 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0100 , 0x0000 ,
			0x0000 , 0x0100 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0001 , 0x0000 ,
			0x0001 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0100 ,
			0x0100 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0000 , 0x0001	
		};
		int displayWord = mem.length - (displayPageSize * PrincOpsDefs.WORDS_PER_PAGE);
		int wordsPerLine = effectivePixelsPerLine / 2;
		int tl = 0;
		for (int i = 0; i < displayPixelHeight; i++) {
			for (int j = 0; j < wordsPerLine; j++) {
				mem[displayWord++] = (short)template[ tl + (displayWord % 8) ];
			}
			tl += 8;
			if (tl >= template.length) { tl = 0; }
		}
	}
	
	/*
	 * Machine type Daybreak (Dove 6085): memory and virtual-memory-map setup at machine start
	 */
	
	public static void initializeMemoryDaybreak(boolean largeScreen) {
		
		// fixed real and virtual sizes for this 6085 implementation
		addressBitsReal = 21;    // 13 bits for page address + 8 bits for word in page address == 8192 pages == 2 MWords == 4 MBytes
		addressBitsVirtual = 24; // 16 MWords == 32 MBytes == 65536 pages == 256 VMM pages (1 word per VM-page: 13 bit page address + 3 status-bits)
		
		// screen data
		if (largeScreen) {
			// "18 quadwords x 861 lines"
			displayPixelWidth = 1152; // not 1184 (1184 = 18.5 quadwords)
			displayPixelHeight = 861; // not 925
			dBreak_displayType = 5;
		} else  {
			// "13 quadwords x 633 lines"
			displayPixelWidth = 832;  // not 880
			displayPixelHeight = 633; // not 697
			dBreak_displayType = 1;
		}
		displayPageSize = 256; // largescreen: 18 * 4 * 861 / 256 => 242.15625 pages
		activeDisplayType = PilotDefs.DisplayType.monochrome;
		
		// allocate real memory and virtual memory map
		int realPageCount = 1 << (addressBitsReal - PrincOpsDefs.ADDRESSBITS_IN_PAGE);
		int virtualPageCount = 1 << (addressBitsVirtual - PrincOpsDefs.ADDRESSBITS_IN_PAGE);
		
		mem = new short[ realPageCount * PrincOpsDefs.WORDS_PER_PAGE ];
		pageMap = new int[virtualPageCount];
		pageFlags = new short[virtualPageCount];
		lastVirtualAddress = (PrincOpsDefs.WORDS_PER_PAGE * virtualPageCount) - 1;
		lastVirtualPage = virtualPageCount - 1;
		lastRealPage = realPageCount - 1;
		
		// initialize virtual and display memory
		createInitialPageMappingDBreak();
		initializeDisplayMemoryDBreak();
	}
	
	public static final int IORegion_Real_StartPage = 32; // skip the first 8 KWord = 16 KByte (why-ever)
	public static final int IORegion_PageCount = 64; // = 16 KWord = 32 KByte (? sufficient space?)
	
	public static final int IORegion_Virtual_PageAfterEnd = 256;
	public static final int IORegion_Virtual_StartPage = IORegion_Virtual_PageAfterEnd - IORegion_PageCount; // IORegion must be placed at the end of the first VM bank
	
	public static final int IORegion_VM_StartAddress = IORegion_Virtual_StartPage * PrincOpsDefs.WORDS_PER_PAGE;
	public static final int IORegion_VM_EndAddressPlusOne = IORegion_Virtual_PageAfterEnd * PrincOpsDefs.WORDS_PER_PAGE;
	
	public static final int DBreak_Real_VMM_PageCount = 256;
	public static final int DBreak_Real_DisplayMem_PageCount = 256;
	
	public static int dBreak_firstRealPageInVMM;
	public static int dBreak_lastRealPageInVMM;
	public static int dBreak_countRealPagesInVMM;
	
	public static int dBreak_Real_firstMapPage;
	public static int dBreak_Real_countMapPages;
	
	public static int dBreak_Real_firstDisplayBankPage;
	public static int dBreak_Real_countDisplayBankPages;
	public static int dBreak_displayType;
	
	private static void createInitialPageMappingDBreak() {
		
		/* real memory (total 8192 pages):
		 *   - 32 pages unused: why??
		 *   - 64 pages IORegion
		 *   - 7584 pages for virtual memory
		 *   - 256 pages VM map
		 *   - 256 pages display memory
		 */
		
		/* (initial) virtual memory mapping
		 *   - 192 pages  => real[96 .. 256)     == free
		 *   - 64 pages   => real[32 .. 96)      == IORegion
		 *   - 7424 pages => real[256 .. 7680)   == free 
		 *   - rest       => unmapped
		 */
		
		int realPagesTotal = lastRealPage + 1;
		
		dBreak_firstRealPageInVMM = IORegion_Real_StartPage;
		dBreak_lastRealPageInVMM = realPagesTotal - DBreak_Real_VMM_PageCount - DBreak_Real_DisplayMem_PageCount - 1;
		dBreak_countRealPagesInVMM = dBreak_lastRealPageInVMM - dBreak_firstRealPageInVMM + 1;
		
		dBreak_Real_firstDisplayBankPage = realPagesTotal - DBreak_Real_DisplayMem_PageCount;
		dBreak_Real_countDisplayBankPages = DBreak_Real_DisplayMem_PageCount;
		
		dBreak_Real_firstMapPage = dBreak_Real_firstDisplayBankPage - DBreak_Real_VMM_PageCount;
		dBreak_Real_countMapPages = DBreak_Real_VMM_PageCount;
		
		int currVirtualPage = 0;
		
		// map real pages after the IORegion up to the VM start of the IORegion
		int currRealAddress = (IORegion_Real_StartPage + IORegion_PageCount) * PrincOpsDefs.WORDS_PER_PAGE;
		while(currVirtualPage < IORegion_Virtual_StartPage) {
			pageMap[currVirtualPage] = currRealAddress;
			pageFlags[currVirtualPage] = PrincOpsDefs.MAPFLAGS_CLEAR;
			currVirtualPage++;
			currRealAddress += PrincOpsDefs.WORDS_PER_PAGE;
		}
		int behindIORegionContinueRealAddress = currRealAddress;
		
		// map IORegion into the expected VM location (=> first VM bank is the mapped)
		currRealAddress = IORegion_Real_StartPage * PrincOpsDefs.WORDS_PER_PAGE;
		for (int i = 0; i < IORegion_PageCount; i++) {
			pageMap[currVirtualPage] = currRealAddress;
			pageFlags[currVirtualPage] = PrincOpsDefs.MAPFLAGS_CLEAR;
			currVirtualPage++;
			currRealAddress += PrincOpsDefs.WORDS_PER_PAGE;
		}
		
		// map remaining real pages up to the start of reserved real memory into VM
		currRealAddress = behindIORegionContinueRealAddress;
		int realAddressLimit = dBreak_lastRealPageInVMM * PrincOpsDefs.WORDS_PER_PAGE; 
		while(currRealAddress <= realAddressLimit) {
			pageMap[currVirtualPage] = currRealAddress;
			pageFlags[currVirtualPage] = PrincOpsDefs.MAPFLAGS_CLEAR;
			currVirtualPage++;
			currRealAddress += PrincOpsDefs.WORDS_PER_PAGE;
		}
		
		// define the remaining pages in VM as unmapped
		while(currVirtualPage <= lastVirtualPage) {
			pageMap[currVirtualPage] = 0;
			pageFlags[currVirtualPage] = PrincOpsDefs.MAPFLAGS_VACANT;
			currVirtualPage++;
		} 
	}
	
	private static void initializeDisplayMemoryDBreak() {
		int displayWord = dBreak_Real_firstDisplayBankPage * PrincOpsDefs.WORDS_PER_PAGE;
		int wordsPerLine = displayPixelWidth / PrincOpsDefs.WORD_BITS;
		short[] template = {
			(short)0b1111000000001111,
			(short)0b0000110000110000,
			(short)0b0000001111000000,
			(short)0b0000110000110000,
			(short)0b0001000000001000,
			(short)0b0001000000001000,
			(short)0b0000100000010000,
			(short)0b0000010000100000,
			(short)0b0000001001000000,
			(short)0b0000000110000000,
			(short)0b0000001001000000,
			(short)0b0000010000100000,
			(short)0b0000100000010000,
			(short)0b0001000000001000,
			(short)0b0010000000000100,
			(short)0b0100000000000010
		};
		int ti = 0;
		for (int i = 0; i < displayPixelHeight; i++) {
			for (int j = 0; j < wordsPerLine; j++) {
				mem[displayWord++] = template[ti];
			}
			ti++;
			if (ti >= template.length) { ti = 0; }
		}
		
		displayFirstRealPage = dBreak_Real_firstDisplayBankPage;
		displayPageSize = ((wordsPerLine * displayPixelHeight) + PrincOpsDefs.WORDS_PER_PAGE - 1) / PrincOpsDefs.WORDS_PER_PAGE;
	}
	
	/*
	 * instruction support
	 */
	
	public static boolean isProtected(short flags) {
		return (flags & PrincOpsDefs.MAPFLAGS_PROTECTED) != 0;
	}
	
	public static boolean isDirty(short flags) {
		return (flags & PrincOpsDefs.MAPFLAGS_DIRTY) != 0;
	}
	
	public static boolean isReferenced(short flags) {
		return (flags & PrincOpsDefs.MAPFLAGS_REFERENCED) != 0;
	}
	
	public static boolean isVacant(short flags) {
		return isProtected(flags) && isDirty(flags) && !isReferenced(flags);
	}
	
	// for opcode SM - Set Map
	public static void setMap(String by, int virtualPageNo, int realPageNo, short flags) {
		setMap(by, virtualPageNo, realPageNo, flags, false);
	}
	
	// for generic setting the map
	public static void setMap(String by, int virtualPageNo, int realPageNo, short flags, boolean isDisplayMem) {
		if (virtualPageNo < 0 || virtualPageNo > lastVirtualPage) {
			Cpu.ERROR("SM / setMap :: virtualPageNo out of range: " + virtualPageNo);
			return; // ERROR() does not return
		}
		
		// clear real page caches
		// TODO: keep updated when more caches are added!
		int oldVirtualPageBase = virtualPageNo << 8;
		if (_lastLpVpageRead == oldVirtualPageBase) { _lastLpVpageRead = -1; }
		if (_lastLpVpageWritten == oldVirtualPageBase) { _lastLpVpageWritten = -1; }
		if (_lastMdsVpageRead == oldVirtualPageBase) { _lastMdsVpageRead = -1; }
		if (_lastMdsVpageWritten == oldVirtualPageBase) { _lastMdsVpageWritten = -1; }
		if (_lastCodeVpageRead == oldVirtualPageBase) { _lastCodeVpageRead = -1; }
		// end clear page caches
		
		if (isVacant(flags)) {
			pageMap[virtualPageNo] = 0;
		} else if (isDisplayMem && realPageNo >= lastRealPage && realPageNo <= (lastRealPage + displayPageSize)) {
			pageMap[virtualPageNo] = realPageNo << PrincOpsDefs.ADDRESSBITS_IN_PAGE;
		} else if (realPageNo < 0 || realPageNo > lastRealPage) {
			// we can't allow access outside real memory, so this is a big error!
			pageFlags[virtualPageNo] = PrincOpsDefs.MAPFLAGS_VACANT;
			pageMap[virtualPageNo] = 0;
			Cpu.ERROR("SM / setMap :: realPageNo out of range: " + realPageNo);
			return; // ERROR() does not return, but the compiler does not know...
		} else {
			pageMap[virtualPageNo] = realPageNo << PrincOpsDefs.ADDRESSBITS_IN_PAGE;
		}
		pageFlags[virtualPageNo] = flags;
		
		if (Config.LOG_OPCODES && (!"SMF".equals(by) || isVacant(flags))) {
			Cpu.logf(
				"    %s :: setMap() => map[ 0x%06X ] -> rp = 0x%08X , flags = 0x%04X\n", 
				by, virtualPageNo, pageMap[virtualPageNo], pageFlags[virtualPageNo]);
		}
	}
	
	// for opcode GMF - Get Map Flags
	public static short getVPageFlags(int virtualPageNo) {
		if (virtualPageNo < 0 || virtualPageNo > lastVirtualPage) {
			return PrincOpsDefs.MAPFLAGS_VACANT;
		}
		return pageFlags[virtualPageNo];
	}
	
	// for opcode GMF - Get Map Flags
	public static int getVPageRealPage(int virtualPageNo) {
		if (virtualPageNo < 0 || virtualPageNo > lastVirtualPage) {
			return 0;
		}
		return pageMap[virtualPageNo] >>> PrincOpsDefs.ADDRESSBITS_IN_PAGE;
	}
	
	// for InitialMesaMicrocode.loadGermPage()
	public static short setVPageFlags(int virtualPageNo, short newFlags) {
		if (virtualPageNo < 0 || virtualPageNo > lastVirtualPage) {
			return PrincOpsDefs.MAPFLAGS_VACANT;
		}
		short currFlags = pageFlags[virtualPageNo];
		if (!isVacant(currFlags)) {
			pageFlags[virtualPageNo] = newFlags;
		}
		return currFlags;
	}
	
	// for devices/agents: check if the LONG POINTER can be read
	public static boolean isReadable(int lp) {
		short flags = getVPageFlags(lp >>> PrincOpsDefs.ADDRESSBITS_IN_PAGE);
		return !isVacant(flags);
	}
	
	// for devices/agents: check if the LONG POINTER can be written
	public static boolean isWritable(int lp) {
		short flags = getVPageFlags(lp >>> PrincOpsDefs.ADDRESSBITS_IN_PAGE);
		return !isProtected(flags);
	}
	
	public static void dumpPreStartGerm(boolean isPrincOpsPost40, int pageCount) {
		System.out.printf("\npre-start germmemory dump: isPrincOpsPost40 = %s , pageCount = %d\n", isPrincOpsPost40, pageCount);
		
		if (isPrincOpsPost40) {
			dumpVPage(0x200, " - 1st GFT page");
		}
		
		for (int i = 0; i < pageCount; i++) {
			dumpVPage(i, "");
		}
		
		dumpVmMap(0, 1280);
	}
	
	private static void dumpVPage(int pageNo, String comment) {
		System.out.printf("\npage 0x%04X = %d%s", pageNo, pageNo, comment);
		int lp = pageNo << 8;
		for (int i = 0; i < 256; i++) {
			if ((i % 16) == 0) {
				System.out.printf("\n  0x%02X :", i);
			}
			System.out.printf(" %04X", rawRead(lp++));
		}
		System.out.println();
	}
	
	// for devices/agents debugging
	public static void dumpVmMap(int firstPage, int pageCount) {
		int limit = firstPage + pageCount;
		for (int page = firstPage; page < limit; page++) {
			if (page < 0 || page > pageMap.length) {
				System.out.printf("page 0x%06X -> out of VM\n", page);
			} else {
				int flags = pageFlags[page] & PrincOpsDefs.MAPFLAGS_MASK;
				if (flags == PrincOpsDefs.MAPFLAGS_VACANT) {
					System.out.printf("page 0x%06X -> vacant\n", page);
				} else {
					System.out.printf("page 0x%06X -> real 0x%06X %s%s%s\n",
							page, 
							pageMap[page],
							(flags & PrincOpsDefs.MAPFLAGS_PROTECTED) != 0 ? "R/O" : "r/w",
							(flags & PrincOpsDefs.MAPFLAGS_REFERENCED) != 0? " ref'ed" : "",
							(flags & PrincOpsDefs.MAPFLAGS_DIRTY) != 0 ? " dirty" : ""
							);
				}
			}
		}
	}
	
	/*
	 * memory access support
	 */
	
	public static int getRealAddress(int longPointer, boolean forWrite) {
		if (longPointer < 0 || longPointer > lastVirtualAddress) {
			Cpu.pointerTrap();
			return 0; // dummy as a trap does not return, but the compiler does not known that
		}
		
		int pageNo = longPointer >> PrincOpsDefs.ADDRESSBITS_IN_PAGE;
		
		if (pageNo == 0) {
			Cpu.pointerTrap(); // nullPointer
			return 0; // dummy, pointerTrap() does not return
		}
		
		short flags = pageFlags[pageNo];
		if (flags == PrincOpsDefs.MAPFLAGS_VACANT) {
			Cpu.signalPageFault(longPointer);
			return 0; // dummy, signalPageFault() does not return
		}
		
		if (forWrite) {
			if (isProtected(flags)) {
				Cpu.signalWriteProtectFault(longPointer);
				return 0; // dummy, signalPageFault() does not return
			}
			flags |= PrincOpsDefs.MAPFLAGS_REFERENCED | PrincOpsDefs.MAPFLAGS_DIRTY;
		} else {
			flags |= PrincOpsDefs.MAPFLAGS_REFERENCED;
		}
		pageFlags[pageNo] = flags;
		
		int realBasePointer = pageMap[pageNo];
		if (Config.LOG_OPCODES && realBasePointer == 0 && (longPointer & 0xFFFFFF00) != 0x00008000) {
			System.out.printf("\n**\n**** getRealAddress() :: using real page 0 for vLp = 0x%08X\n**\n\n", longPointer);
			Cpu.logTrapOrFault("memory mapping problem\n");
		}
		
		return realBasePointer + (longPointer & 0x000000FF);
	}
	
	/*
	 * special read function for debugging, returning 0xFFFF when unmapped 
	 */
	
	public static short rawRead(int longPointer) {
		if (longPointer < 0 || longPointer > lastVirtualAddress) {
			return (short)0xFFFF;
		}
		
		int pageNo = longPointer >> PrincOpsDefs.ADDRESSBITS_IN_PAGE;
		short flags = pageFlags[pageNo];
		if (isVacant(flags)) {
			return (short)0xFFFF;
		}
		
		return mem[pageMap[pageNo] + (longPointer & 0x000000FF)];
	}
	
	/*
	 * memory access logging
	 */
	
	public static boolean doLog = true;
	
	private static void memLogf(String format, Object... args) {
		if (Config.LOG_MEM_ACCESS && doLog) { Cpu.logf(format, args); }
	}
	
	/*
	 * LONG POINTER access (with caching)
	 */
	
	private static int _lastLpVpageRead = 0;
	private static int _lastLpRpageRead = 0;
	
	private static short _readLpWord(int ptr) {
		int vPage = ptr & 0xFFFFFF00;
		if (vPage != _lastLpVpageRead) {
			_lastLpRpageRead = getRealAddress(vPage, false);
			_lastLpVpageRead = vPage;
		}
		
		if (Config.IOR_LOG_MEM_ACCESS) {
			int realAddr = _lastLpRpageRead | (ptr & 0x000000FF);
			final IORAddress iorAddr;
			final String prefix;
			if (ptr < IORegion_VM_EndAddressPlusOne && ptr >= IORegion_VM_StartAddress) {
				iorAddr = IORegion.resolveRealAddress(realAddr);
				prefix = "read IORegion";
			} else if (_lastLpRpageRead < 0xFFFF) {
				iorAddr = IORegion.resolveLastKnownStructure(realAddr);
				prefix = "+++ read IOStruct";
			} else {
				iorAddr = null;
				prefix = null;
			}
			if (iorAddr != null && iorAddr.getName().startsWith("Floppy")) {
				System.out.printf(
						"%s: [ virtual: 0x%06X , real: 0x%06X ] -> 0x%04X - %s\n",
						prefix, ptr, realAddr, mem[realAddr], iorAddr.getName()
						);
				List<Field> fields = iorAddr.getFields();
				if (fields != null) {
					for (Field f : fields) {
						f.dump("  ");
					}
				}
				if (realAddr == 0x002024) {
					System.out.printf(
							"++\n++ access to IOP FCB-table entry for TTY (real = 0x002024) at 0x%08X+0x%04X [ insn# %d ]\n++\n ",
							Cpu.CB, Cpu.savedPC, Cpu.insns);
				}
			}
		}
		
		return mem[_lastLpRpageRead | (ptr & 0x000000FF)];
	}
	
	private static int _lastLpVpageWritten = 0;
	private static int _lastLpRpageWritten = 0;
	
	private static void _writeLpWord(int ptr, short value) {
		int vPage = ptr & 0xFFFFFF00;
		if (vPage != _lastLpVpageWritten) {
			_lastLpRpageWritten = getRealAddress(vPage, true);
			_lastLpVpageWritten = vPage;
		}
		mem[_lastLpRpageWritten | (ptr & 0x000000FF)] = value;
		
		if (Config.IOR_LOG_MEM_ACCESS) {
			int realAddr = _lastLpRpageWritten | (ptr & 0x000000FF);
			final IORAddress iorAddr;
			final String prefix;
			if (ptr < IORegion_VM_EndAddressPlusOne && ptr >= IORegion_VM_StartAddress) {
				iorAddr = IORegion.resolveRealAddress(realAddr);
				prefix = "write IORegion";
			} else if (_lastLpRpageWritten < 0xFFFF) {
				iorAddr = IORegion.resolveLastKnownStructure(realAddr);
				prefix = "+++++ write IOStruct";
			} else {
				iorAddr = null;
				prefix = null;
			}
			if (iorAddr != null && iorAddr.getName().startsWith("Floppy")) {
				System.out.printf(
						"%s: [ virtual: 0x%06X , real: 0x%06X ] <- 0x%04X - %s\n",
						prefix, ptr, realAddr, value, iorAddr.getName()
						);
				List<Field> fields = iorAddr.getFields();
				if (fields != null) {
					for (Field f : fields) {
						f.dump("  ");
					}
				}
			}
		}
	}
	
	public static short readWord(int longPointer) {
		short w = _readLpWord(longPointer);
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. readWord( lp = 0x%08X )  -> 0x%04X\n", longPointer, w & 0xFFFF);
		}
		return w;
	}
	
	public static void writeWord(int longPointer, short word) {
		_writeLpWord(longPointer, word);
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. writeWord( lp = 0x%08X , 0x%04X )\n", longPointer, word & 0xFFFF);
		}
	}
	
	public static int readDblWord(int longPointer) {
		short low = _readLpWord(longPointer);
		short high = _readLpWord(longPointer + 1);
		int dbl = (high << 16) | (low & 0x0000FFFF);
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. readDblWord( lp = 0x%08X )  -> 0x%08X\n", longPointer, dbl);
		}
		return dbl;
	}
	
	public static void writeDblWord(int longPointer, int dblword) {
		_writeLpWord(longPointer, (short)(dblword & 0xFFFF));
		_writeLpWord(longPointer + 1, (short)(dblword >>> 16));
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. writeDblWord( lp = 0x%08X , 0x%08X )\n", longPointer, dblword);
		}
	}
	
	/*
	 * MDS access (with caching)
	 */
	
	private static int _lastMdsVpageRead = 0;
	private static int _lastMdsRpageRead = 0;
	
	private static short _readLengthenedMDSWord(int ptr) {
		int vPage = ptr & 0xFFFFFF00;
		if (vPage != _lastMdsVpageRead) {
			_lastMdsRpageRead = getRealAddress(vPage, false);
			_lastMdsVpageRead = vPage;
		}
		
		if (Config.IOR_LOG_MEM_ACCESS) {
			int realAddr = _lastMdsRpageRead | (ptr & 0x000000FF);
			final IORAddress iorAddr;
			final String prefix;
			if (ptr < IORegion_VM_EndAddressPlusOne && ptr >= IORegion_VM_StartAddress) {
				iorAddr = IORegion.resolveRealAddress(realAddr);
				prefix = "MDS-read IORegion";
			} else if (_lastMdsRpageRead <= 0xFFFF) {
				iorAddr = IORegion.resolveLastKnownStructure(realAddr);
				prefix = "+++ MDS-read IOStruct";
			} else {
				iorAddr = null;
				prefix = null;
			}
			if (iorAddr != null) {
				System.out.printf(
						"%s: [ virtual: 0x%06X , real: 0x%06X ] -> 0x%04X - %s\n",
						prefix, ptr, realAddr, mem[realAddr], iorAddr.getName()
						);
				List<Field> fields = iorAddr.getFields();
				if (fields != null) {
					for (Field f : fields) {
						f.dump("  ");
					}
				}
			}
		}
		
		return mem[_lastMdsRpageRead | (ptr & 0x000000FF)];
	}
	
	private static int _lastMdsVpageWritten = 0;
	private static int _lastMdsRpageWritten = 0;
	
	private static void _writeLengthenedMDSWord(int ptr, short value) {
		int vPage = ptr & 0xFFFFFF00;
		if (vPage != _lastMdsVpageWritten) {
			_lastMdsRpageWritten = getRealAddress(vPage, true);
			_lastMdsVpageWritten = vPage;
		} 
		mem[_lastMdsRpageWritten | (ptr & 0x000000FF)] = value;
		
		if (Config.IOR_LOG_MEM_ACCESS) {
			int realAddr = _lastMdsRpageWritten | (ptr & 0x000000FF);
			final IORAddress iorAddr;
			final String prefix;
			if (ptr < IORegion_VM_EndAddressPlusOne && ptr >= IORegion_VM_StartAddress) {
				iorAddr = IORegion.resolveRealAddress(realAddr);
				prefix = "MDS-write IORegion";
			} else if (_lastMdsRpageWritten < 0xFFFF) {
				iorAddr = IORegion.resolveLastKnownStructure(realAddr);
				prefix = "+++++ MDS-write IOStruct";
			} else {
				iorAddr = null;
				prefix = null;
			}
			if (iorAddr != null) {
				System.out.printf(
						"%s: [ virtual: 0x%06X , real: 0x%06X ] <- 0x%04X - %s\n",
						prefix, ptr, realAddr, value, iorAddr.getName()
						);
				List<Field> fields = iorAddr.getFields();
				if (fields != null) {
					for (Field f : fields) {
						f.dump("  ");
					}
				}
			}
		}
	}
	
	public static short readMDSWord(int pointer) {
		short w = _readLengthenedMDSWord(Cpu.lengthenPointer(pointer));
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. readMDSWord( p = 0x%04X )  -> 0x%04X\n", pointer, w & 0xFFFF);
		}
		return w;
	}
	
	public static short readMDSWord(int pointer, int offset) {
		short w = _readLengthenedMDSWord(Cpu.lengthenPointer(pointer + offset));
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. readMDSWord( p = 0x%04X [0x%04X+0x%04X] )  -> 0x%04X\n", pointer+offset, pointer, offset, w & 0xFFFF);
		}
		return w;
	}
	
	public static void writeMDSWord(int pointer, short value) {
		_writeLengthenedMDSWord(Cpu.lengthenPointer(pointer), value);
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. writeMDSWord( p = 0x%04X , 0x%04X )\n", pointer, value & 0xFFFF);
		}
	}
	
	public static void writeMDSWord(int pointer, int offset, short value) {
		_writeLengthenedMDSWord(Cpu.lengthenPointer(pointer + offset), value);
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. writeMDSWord( p = 0x%04X [0x%04X+0x%04X] , 0x%04X )\n", pointer+offset, pointer, offset, value & 0xFFFF);
		}
	}
	
	public static void writeMDSWord(int pointer, int value) {
		_writeLengthenedMDSWord(Cpu.lengthenPointer(pointer), (short)(value & 0xFFFF));
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. writeMDSWord( p = 0x%04X , 0x%04X )\n", pointer, value & 0xFFFF);
		}
	}
	
	public static void writeMDSWord(int pointer, int offset, int value) {
		_writeLengthenedMDSWord(Cpu.lengthenPointer(pointer + offset), (short)(value & 0xFFFF));
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. writeMDSWord( p = 0x%04X [0x%04X+0x%04X] , 0x%04X )\n", pointer+offset, pointer, offset, value & 0xFFFF);
		}
	}
	
	public static int readMDSDblWord(int pointer) {
		int ptr = Cpu.lengthenPointer(pointer);
		short low = _readLengthenedMDSWord(ptr);
		short high = _readLengthenedMDSWord(ptr + 1);
		int dbl = (high << 16) | (low & 0x0000FFFF);
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. readMDSDblWord( p = 0x%04X )  -> 0x%08X\n", pointer, dbl);
		}
		return dbl;
	}
	
	public static int readMDSDblWord(int pointer, int offset) {
		int ptr = Cpu.lengthenPointer(pointer + offset);
		short low = _readLengthenedMDSWord(ptr);
		short high = _readLengthenedMDSWord(ptr + 1);
		int dbl = (high << 16) | (low & 0x0000FFFF);
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. readMDSDblWord( p = 0x%04X [0x%04X+0x%04X] )  -> 0x%08X\n", pointer+offset, pointer, offset, dbl);
		}
		return dbl;
	}
	
	public static void writeMDSDblWord(int pointer, int value) {
		int ptr = Cpu.lengthenPointer(pointer);
		_writeLengthenedMDSWord(ptr, (short)(value & 0xFFFF));
		_writeLengthenedMDSWord(ptr + 1, (short)(value >>> 16));
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. writeMDSDblWord( p = 0x%04X , 0x%08X )\n", pointer, value);
		}
	}
	
	public static void writeMDSDblWord(int pointer, int offset, int value) {
		int ptr = Cpu.lengthenPointer(pointer + offset);
		_writeLengthenedMDSWord(ptr, (short)(value & 0xFFFF));
		_writeLengthenedMDSWord(ptr + 1, (short)(value >>> 16));
		if (Config.LOG_MEM_ACCESS) {
			memLogf(".. writeMDSDblWord( p = 0x%04X [0x%04X+0x%04X] , 0x%08X )\n", pointer+offset, pointer, offset, value);
		}
	}	
	
	/*
	 * code access (with caching)
	 */
	
	public static int getCodeByte(int cb, int pc) {
		int vPtr = cb + (pc >> 1);
		int vPage = vPtr & 0xFFFFFF00;
		int rPtr = getRealAddress(vPage, false) | (vPtr & 0x000000FF); 
		boolean isHighByte = (pc & 0x0001) == 0;
		
		int codeWord = mem[rPtr];
		if (isHighByte) {
			return (codeWord >> 8) & 0x00FF;
		} else {
			return (codeWord & 0x00FF);
		}
	}
	
	public static void patchCodeByte(int cb, int pc, int codeByte) {
		int vPtr = cb + (pc >> 1);
		int vPage = vPtr & 0xFFFFFF00;
		int rPtr = getRealAddress(vPage, false) | (vPtr & 0x000000FF); 
		boolean isHighByte = (pc & 0x0001) == 0;
		
		codeByte &= 0x00FF;
		int codeWord = mem[rPtr];
		if (isHighByte) {
			codeWord = (codeByte << 8) | (codeWord & 0x00FF);
		} else {
			codeWord = (codeWord & 0xFF00) | codeByte;
		}
		mem[rPtr] = (short)codeWord;
	}
	
	private static int _lastCodeVpageRead = 0;
	private static int _lastCodeRpageRead = 0;
	
	private static short _readLengthenedCodeWord(int ptr) {
		int vPage = ptr & 0xFFFFFF00;
		if (vPage != _lastCodeVpageRead) {
			_lastCodeRpageRead = getRealAddress(vPage, false);
			_lastCodeVpageRead = vPage;
		} 
		return mem[_lastCodeRpageRead | (ptr & 0x000000FF)];
	}
	
	public static int /* 0..255 */ getNextCodeByte() {
		int codeWord = _readLengthenedCodeWord(Cpu.CB + (Cpu.PC >> 1)) & 0x0000FFFF;
		boolean getHighByte = (Cpu.PC & 0x0001) == 0;
		Cpu.PC++;
		if (getHighByte) {
			return codeWord >>> 8;
		} else {
			return codeWord & 0x00FF;
		}
	}
	
	public static int /* 0..65535 */ getNextCodeWord() {
		int b1 = getNextCodeByte();
		int b2 = getNextCodeByte();
		return (b1 << 8) | b2;
	}
	
	public static int peekNextCodeByte() {
		int currPC = Cpu.PC;
		int value = getNextCodeByte();
		Cpu.PC = currPC;
		return value;
	}
	
	public static int peekNextCodeWord() {
		int currPC = Cpu.PC;
		int value = getNextCodeWord();
		Cpu.PC = currPC;
		return value;
	}
	
	public static short readCode(short offset) {
		return _readLengthenedCodeWord(Cpu.CB + (offset & 0xFFFF)); // make offset unsigned
	}
	
	public static short readCode(int offset) {
		return _readLengthenedCodeWord(Cpu.CB + (offset & 0xFFFF)); // let offset wrap inside the 64K-block
	}
	
	/*
	 * String access
	 */
	
	public static short fetchByte(int longPointer, /* LONG CARDINAL */ int offset) {
		short word = readWord(longPointer + ((offset & 0x7FFFFFFF) / 2));
		if ((offset & 1) == 0) {
			return (short)((word >> 8) & 0x00FF);
		} else {
			return (short)(word & 0x00FF);
		}
	}
	
	public static short fetchByte(int longPointer, short offset) {
		return fetchByte(longPointer, offset & 0xFFFF);
	}
	
	public static void storeByte(int longPointer, int /* LONG CARDINAL */ offset, short b) {
		int addr = longPointer + ((offset & 0x7FFFFFFF) / 2);
		short word = readWord(addr);
		if ((offset & 1) == 0) {
			word = (short)(((b << 8) & 0xFF00) | (word & 0x00FF));
		} else {
			word = (short)((word & 0xFF00) | (b & 0x00FF));
		}
		writeWord(addr, word);
	}
	
	public static void storeByte(int longPointer, short offset, byte b) {
		storeByte(longPointer, offset & 0xFFFF, b);
	}
	
	/*
	 * Intra-word field access
	 */
	
	private static final short[] FIELD_MASKS = {
		0x0001, 0x0003, 0x0007, 0x000f, 0x001f, 0x003f, 0x007f, 0x00ff,
		0x01ff, 0x03ff, 0x07ff, 0x0fff, 0x1fff, 0x3fff, 0x7fff, (short)0xffff	
	};
	
	public static short readField(short sourceWord, int spec8) {
		int pos = (spec8 >>> 4) & 0x0F;
		int len = spec8 & 0x0F;
		int totalLen = pos + len + 1;
		if (totalLen > PrincOpsDefs.WORD_BITS) {
			Cpu.ERROR("readField :: fieldSpec[ pos + len + 1 ] > PrincOpsDefs.WORD_BITS");
		}
		int shiftBy = PrincOpsDefs.WORD_BITS - totalLen;
		return (short)((sourceWord >> shiftBy) & FIELD_MASKS[len]);
	}
	
	public static short writeField(short sourceWord, int spec8, short data) {
		int pos = (spec8 >>> 4) & 0x0F;
		int len = spec8 & 0x0F;
		int totalLen = pos + len + 1;
		if (totalLen > PrincOpsDefs.WORD_BITS) {
			Cpu.ERROR("writeField :: fieldSpec[ pos + len + 1 ] > PrincOpsDefs.WORD_BITS");
		}
		int shiftBy = PrincOpsDefs.WORD_BITS - totalLen;
		short mask = FIELD_MASKS[len];
		data &= mask;
		data <<= shiftBy;
		mask <<= shiftBy;
		return (short)((sourceWord & (~mask)) | data);
	}
	
	/*
	 * display mapping and access
	 */
	
	private static int vDisplayFrom = 0;
	private static int vDisplayTo = 0;
	private static int pixelsPerWord = 1;
	private static int displayWordsPerLine = 1;
	
	public static void mapDisplayMemory(int toVirtualPage) {
		int realDispMem = lastRealPage + 1;
		Cpu.logTrapOrFault(
				String.format(
						" ## mapDisplayMemory( toVirtualPage = 0x%08X ) :: realDispMem = 0x%08X , displayPageSize =%d\n",
						toVirtualPage, realDispMem, displayPageSize)
				);
		for (int i = 0; i < displayPageSize; i++) {
			setMap("mapDisplayMemory", toVirtualPage + i, realDispMem + i, PrincOpsDefs.MAPFLAGS_CLEAR, true); 
		}
		displayFirstMappedVirtualPage = toVirtualPage;
		
		vDisplayFrom = displayFirstMappedVirtualPage * PrincOpsDefs.WORDS_PER_PAGE;
		vDisplayTo = (displayFirstMappedVirtualPage + displayPageSize) * PrincOpsDefs.WORDS_PER_PAGE;
		pixelsPerWord = (activeDisplayType == DisplayType.byteColor) ? 2 : 16;
		displayWordsPerLine = effectivePixelsPerLine / pixelsPerWord;
		
//		System.out.printf("## mapDisplayMemory => vDisplayFrom = 0x%08X, vDisplayTo = 0x%08X, displayWordsPerLine = %d\n", 
//				vDisplayFrom, vDisplayTo, displayWordsPerLine);
	}
	
	public static boolean isInDisplayMemory(int vAddr) {
		return vAddr >= vDisplayFrom && vAddr < vDisplayTo;
	}
	
	public static int getDisplayY(int vAddr, int pixelOffset) {
		if (!isInDisplayMemory(vAddr)) { return -1; }
		int wordOffset = (vAddr - vDisplayFrom) + (pixelOffset / pixelsPerWord);
		return wordOffset  / displayWordsPerLine;
	}
	
	public static int getDisplayX(int vAddr, int pixelOffset) {
		if (!isInDisplayMemory(vAddr)) { return -1; }
		int wordOffset = (vAddr - vDisplayFrom) + (pixelOffset / pixelsPerWord);
		wordOffset -= (wordOffset  / displayWordsPerLine) * displayWordsPerLine; 
		return (wordOffset * pixelsPerWord) + (pixelOffset % pixelsPerWord);
	}
	
	public static short[] getDisplayRealMemory() {
		return mem;
	}
	
	public static int getDisplayVirtualPage() {
		return displayFirstMappedVirtualPage;
	}
	
	public static int getDisplayRealPage() {
		return displayFirstRealPage;  
	}
	
	public static int getDisplayPageSize() {
		return displayPageSize;
	}

	private static PilotDefs.DisplayType activeDisplayType = PilotDefs.DisplayType.monochrome;
	
	public static PilotDefs.DisplayType getDisplayType() {
		return activeDisplayType;
	}
	
	public static int getDisplayPixelWidth() {
		return displayPixelWidth;
	}
	
	public static int getDisplayPixelHeight() {
		return displayPixelHeight;
	}
	
	public static void setDisplayMemoryDirty() {
		if (displayFirstMappedVirtualPage == 0) { return; } // display memory yet not mapped
		for (int i = 0; i < displayPageSize; i++) {
			setVPageFlags(displayFirstMappedVirtualPage + i, PrincOpsDefs.MAPFLAGS_DIRTY);
		}
	}
	
	public static void resetDisplayPagesFlags() {
		if (displayFirstMappedVirtualPage == 0) { return; }
		int currAddr = displayFirstMappedVirtualPage;
		int currPage = displayFirstMappedVirtualPage;
		for (int i = 0; i < displayPageSize; i++) {
			if (currAddr != _lastLpVpageWritten) {
				pageFlags[currPage] = PrincOpsDefs.MAPFLAGS_CLEAR;
			}
			currAddr += PrincOpsDefs.WORDS_PER_PAGE;
			currPage++;
		}
	}
	
	public static boolean locateRealDisplayMemoryInVMMap() {
		int displayMemBaseAddress = displayFirstRealPage * PrincOpsDefs.WORDS_PER_PAGE;
		displayFirstMappedVirtualPage = 0;
		for (int page = 0; page < pageMap.length; page++) {
			if (pageMap[page] == displayMemBaseAddress) {
				// we found the location of the display in virtual memory
				displayFirstMappedVirtualPage = page;
				// make sure the screen is refreshed (e.g. after display is turned on after a world-swap)
				for (int i = 0; i < displayPageSize; i++) {
					pageFlags[displayFirstMappedVirtualPage + i] |= PrincOpsDefs.MAPFLAGS_DIRTY;
				}
				return true;
			}
		}
		return false;
	}
}