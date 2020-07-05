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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Functionality implemented on real machines by a combination
 * of the IOPs firmware and the initial microcode loaded from
 * some source.
 * <br>
 * These are:
 * <ul>
 * <li>load the germ file from some source into the boot area of the virtual memory</li>
 * <li>write the boot switches into the loaded germ</li>
 * <li>write the boot request data into the loaded germ</li> 
 * </ul>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class InitialMesaMicrocode {
	
	/*
	 * Germ loading
	 */
	
	// virtual pages where to place the germ pages coming from boot source 
	// the germ has its MDS starting at page 0
	private static final int Germ_page0 = 512;   // the first germ source page is placed at this virtual page
	private static final int Germ_page1 = 1;     // subsequent source pages are transferred starting at this virtual page
	private static final int Germ_maxPages = 96; // the germ extends maximally from page 1 to 95, so max 95 pages + page 0 => 96  
	
	// locations in the germs system data table (in MDS)
	public static final int sFirstGermRequest = 208; // location of the boot request data in the germs system table data
	public static final int sGermSwitchesOffset = 14; // offset in words of the boot switches to the germs entry system table data
	
	/**
	 * Load a sequence of pages as germ into the memory of the mesa engine. 
	 * 
	 * @param pages sequence of 256 word pages containing the germ
	 * @param firstPageIsGFT is this a Post-4.0 PrincOps germ, i.e. is there a
	 *     global-frame-table in the first page?
	 * @return {@code true} if the file loaded is a plausible germ file;
	 *   the {@code false} value indicates that the file was too small or
	 *   too large, however the file has been loaded up to the maximal allowed
	 *   germ size into memory. 
	 * @throws IOException in case of access or plausibility problems with the germ file
	 */
	public static boolean loadGerm(List<short[]> pages, boolean firstPageIsGFT) {
		boolean plausible = true;
		boolean savedDoLog = Mem.doLog;
		int currPageNo = 0;
		
		Mem.doLog = false;
		
		int germPages = pages.size();
		if (germPages > Germ_maxPages) {
			Cpu.logWarning("loadGerm: inplausible page count for germ, using only first " + Germ_maxPages + " pages");
			plausible = false;
			germPages = Germ_maxPages;
		}
		
		// load the first source page to the special location if it is the GFT (Global Frame Table, MDS-relieved Pilot)
		if (firstPageIsGFT) {
			copyPage(pages.get(currPageNo++), Germ_page0);
			germPages--;
		}
		
		// load the rest of the germ into the germs MDS
		int targetPage = Germ_page1;
		while(germPages-- > 0) {
			copyPage(pages.get(currPageNo++), targetPage++);
		}
		
		Mem.doLog = savedDoLog;
		return plausible;
	}
	
	private static void copyPage(short[] pageContent, int targetPage) {
		short mapFlags = Mem.getVPageFlags(targetPage);
		int ptr = targetPage * PrincOpsDefs.WORDS_PER_PAGE;
		for(int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE; i++) {
			Mem.writeWord(ptr++, pageContent[i]);
		}
		Mem.setVPageFlags(targetPage,  mapFlags);
	}
	
	/**
	 * Load a germ file from the filesystem into the memory of the mesa engine. 
	 * 
	 * @param filename the filename of the germ file
	 * @param firstPageIsGFT is this a Post-4.0 PrincOps germ, i.e. is there a
	 *     global-frame-table in the first page?
	 * @return {@code true} if the file loaded is a plausible germ file;
	 *   the {@code false} value indicates that the file was too small or
	 *   too large, however the file has been loaded up to the maximal allowed
	 *   germ size into memory. 
	 * @throws IOException in case of access or plausibility problems with the germ file
	 */
	public static boolean loadGerm(String filename, boolean firstPageIsGFT) throws IOException {
		boolean plausible = true;
		boolean savedDoLog = Mem.doLog;
		try (FileInputStream fis = new FileInputStream(filename)) {
			Mem.doLog = false;
			
			int germSize = fis.available();
			if (germSize < PrincOpsDefs.BYTES_PER_PAGE) {
				throw new IOException("File '" + filename + "' too small for being a germ file");
			}
			if ((germSize % PrincOpsDefs.BYTES_PER_PAGE) != 0) {
				Cpu.logWarning("loadGerm: length of germ file '" + filename +  "' not a multiple of page-size (256 words)");
				plausible = false;
			}
			
			int germPages = germSize / PrincOpsDefs.BYTES_PER_PAGE;
			if (germPages > Germ_maxPages) {
				Cpu.logWarning("loadGerm: inplausible page count for germ file '" + filename +  "', using only first " + Germ_maxPages + " pages");
				plausible = false;
				germPages = Germ_maxPages;
			}
			
			// load the first source page to the special location if it is the GFT (Global Frame Table, MDS-relieved Pilot)
			if (firstPageIsGFT) {
				loadGermPage(fis, Germ_page0);
				germPages--;
			}
			
			// load the rest of the germ into the germs MDS
			int targetPage = Germ_page1;
			while(germPages-- > 0) {
				loadGermPage(fis, targetPage++);
			}
		} finally {
			Mem.doLog = savedDoLog;
		}
		return plausible;
	}
	
//	private static void logf(String pattern, Object... args) {
//		System.out.printf(pattern, args);
//	}
	
//	private static int germFilePage = 0;
	
	private static void loadGermPage(FileInputStream fis, int targetPage) throws IOException {
		short mapFlags = Mem.getVPageFlags(targetPage);
		int ptr = targetPage * PrincOpsDefs.WORDS_PER_PAGE;
		
//		logf("\n** germ file page %d => targetPage %d => virtual mem start 0x%08X", germFilePage++, targetPage, ptr);
		
		for(int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE; i++) {
//			if ((i % 16) == 0) {
//				logf("\n  0x%08X :", ptr);
//			}
			int upper = fis.read() & 0xFF;
			int lower = fis.read() & 0xFF;
			short word = (short)(((upper << 8) | lower) & 0xFFFF);
			Mem.writeWord(ptr++, word);
//			logf(" %04X", word);
		}
		Mem.setVPageFlags(targetPage,  mapFlags);
//		logf("\n-------------------\n");
	}
	
	/*
	 * Boot switches
	 */

	/**
	 * Set the boot switches based on the characters in the passed string, each
	 * characters addressing one of the 256 switch positions through its byte code.
	 * 
	 * @param switches the boot switches string.
	 */
	public static void setBootSwitches(String switches) {
		int switchesInMds = PrincOpsDefs.getSdMdsPtr(sFirstGermRequest) + sGermSwitchesOffset;
		short[] switchesBits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		
		// parse the switches string and accumulate them into the bit positions
		List<Short> switchIndexes = parseSwitches(switches);
		for (short s : switchIndexes) {
			int w = s >>> 4;
			short bit = (short)(0x8000 >> (s & 0x0F));
			switchesBits[w] |= bit;
		}
		
		// implant the switch bits
		for (int i = 0; i < switchesBits.length; i++) {
			Mem.writeMDSWord(switchesInMds, i, switchesBits[i]);
		}
	}
	
	private static List<Short> parseSwitches(String switches) {
		List<Short> v = new ArrayList<>();
		
		int len = switches.length();
		int temp = 0;
		int inOctal = -1;
		for (int i = 0; i < len; i++) {
			char c = switches.charAt(i);
			if (inOctal > -1) {
				if (c >= '0' && c <= '7') {
					temp = (temp << 3) | (c - '0');
					inOctal++;
					if (inOctal > 2) {
						// we had 3 octal digits -> store the value
						inOctal = -1;
						v.add((short)(temp & 0x00FF));
					}
				} else {
					// not an expected octal digit -> abort parsing
					inOctal = -1;
					v.add((short)((c - '\0') & 0x00FF));
				}
			} else {
				if (c == '\\') {
					// start collecting octal digits
					inOctal = 0;
				} else {
					// plain character
					v.add((short)((c - '\0') & 0x00FF));
				}
			}
		}
		
		return v;
	}
	
	/*
	 * Boot requests
	 */
	
	// the following boot request offsets start in the germs MDS at sFirstGermRequest
	// see Dawn :: PrincOps.h
	// see Guam :: Pilot.h
	
	private static final int request_w_requestBasicVersion = 0;
	private static final int request_w_action = 1;
	
	private static final int request_w_location_deviceType = 2;
	private static final int request_w_location_deviceOrdinal = 3;
	
	private static final int request_dbl_location_disk_fileID = 4;
	private static final int request_dbl_location_disk_firstPage = 9;
	private static final int request_w_location_disk_da_cylinder = 11;
	private static final int request_w_location_disk_da_sectorHead = 12;
	
	private static final int request_w_location_ether_bfn1 = 4; // boot file number as HostNumber (3 words)
	private static final int request_w_location_ether_bfn2 = 5;
	private static final int request_w_location_ether_bfn3 = 6;
	private static final int request_w_location_ether_networkNumber1 = 7;
	private static final int request_w_location_ether_networkNumber2 = 8;
	private static final int request_w_location_ether_hostNumber1 = 9;
	private static final int request_w_location_ether_hostNumber2 = 10;
	private static final int request_w_location_ether_hostNumber3 = 11;
	private static final int request_w_location_ether_socket = 12;
	
	private static final int request_w_location_any_a = 4;
	private static final int request_w_location_any_b = 5;
	private static final int request_w_location_any_c = 6;
	private static final int request_w_location_any_d = 7;
	private static final int request_w_location_any_e = 8;
	private static final int request_w_location_any_f = 9;
	private static final int request_w_location_any_g = 10;
	private static final int request_w_location_any_h = 11;
	
	private static final int request_w_requestExtensionVersion = 13;
	
	// switches start here at offset 14 of sFirstGermRequest => sGermSwitches
	private static final int request_w16_switches = 14; // length: 16 words = 256 bits
	
	private static final int request_w_inLoadMode = 30;
	private static final int request_w_session = 31;
	
	private static final int request_SIZE = 32;
	
	// constants for some of the above fields

	private static final short CurrentRequestBasicVersion     = 1838; // octal 03456
	private static final short CurrentRequestExtensionVersion = 4012; // octal 07654
	
	private static final short Action_inLoad             = 0;
	private static final short Action_outLoad            = 1;
	private static final short Action_bootPhysicalVolume = 2;
	private static final short Action_teledebug          = 3;
	private static final short Action_noOp               = 4;
	
	private static final short DeviceType_ethernet       = 6;
	private static final short DeviceType_anyPilotDisk   = 64;
	private static final short DeviceType_anyFloppy      = 17;
	private static final short DeviceType_stream         = 4000;
	
	private static final short Session_continuingAfterOutLoad = 0;
	private static final short Session_newSession             = 1;
	
	private static void putRequestWord(int offset, short value) {
		Mem.writeMDSWord(PrincOpsDefs.getSdMdsPtr(sFirstGermRequest), offset, value);
	}
	
	private static void clearRequest() {
		short zero = 0;
		for (int i = 0; i < request_SIZE; i++) {
			putRequestWord(i, zero);
		}
		putRequestWord(request_w_requestBasicVersion, CurrentRequestBasicVersion);
		putRequestWord(request_w_requestExtensionVersion, CurrentRequestExtensionVersion); // necesssary ?
	}
	
	public static void setBootRequestDisk(short deviceOrdinal) {
		clearRequest();
		
		putRequestWord(request_w_action, Action_bootPhysicalVolume);
		putRequestWord(request_w_location_deviceType, DeviceType_anyPilotDisk);
		putRequestWord(request_w_location_deviceOrdinal, deviceOrdinal);
	}
	
	public static void setBootRequestFloppy(short deviceOrdinal) {
		clearRequest();
		
		putRequestWord(request_w_action, Action_inLoad);
		putRequestWord(request_w_location_deviceType, DeviceType_anyFloppy);
		putRequestWord(request_w_location_deviceOrdinal, deviceOrdinal);
	}

	public static final long BFN_Daybreak_Germ          = 0_25200004037L;
	public static final long BFN_Daybreak_SimpleNetExec = 0_25200004040L;
	public static final long BFN_Daybreak_Installer     = 0_25200004047L;
	
	public static void setBootRequestEthernet(short deviceOrdinal, long bfn) {
		clearRequest();
		
		putRequestWord(request_w_action, Action_inLoad);
		putRequestWord(request_w_location_deviceType, DeviceType_ethernet);
		putRequestWord(request_w_location_deviceOrdinal, deviceOrdinal);
		
		// boot file number to load (some magic number)
		putRequestWord(request_w_location_ether_bfn1, (short)((bfn >> 32) & 0xFFFF));
		putRequestWord(request_w_location_ether_bfn2, (short)((bfn >> 16) & 0xFFFF));
		putRequestWord(request_w_location_ether_bfn3, (short)(bfn & 0xFFFF));
		
		// source: unknown/default network, broadcast address, boot socket
		putRequestWord(request_w_location_ether_networkNumber1, (short)0x0000); // unknown/default network 
		putRequestWord(request_w_location_ether_networkNumber2, (short)0x0000);
		putRequestWord(request_w_location_ether_hostNumber1, (short)0xFFFF);    // broadcast address
		putRequestWord(request_w_location_ether_hostNumber2, (short)0xFFFF);
		putRequestWord(request_w_location_ether_hostNumber3, (short)0xFFFF);
		putRequestWord(request_w_location_ether_socket, (short)0x000A);         // boot socket
		
	}
	
	public static void setBootRequestStream() {
		clearRequest();
		
		putRequestWord(request_w_action, Action_inLoad);
		putRequestWord(request_w_location_deviceType, DeviceType_stream);
		putRequestWord(request_w_location_deviceOrdinal, (short)0);
	}
	
	/*
	 * temp test code
	 */
	
	public static void main(String[] args) {
//		List<Short> switchIndexes = parseSwitches(" \177 \377");
		List<Short> switchIndexes = parseSwitches("8Wy{|}\\346\\347\\350\\377");
		for (int i = 0; i < switchIndexes.size(); i++) {
			System.out.printf("[%d] => %02X = %d\n", i, switchIndexes.get(i), switchIndexes.get(i));
		}
		
		short[] switchesBits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		
		for (short s : switchIndexes) {
			int w = s >>> 4;
			short bit = (short)(0x8000 >> (s & 0x0F));
			switchesBits[w] |= bit;
		}
		
		System.out.printf("=> 0x");
		for (int i = 0; i < switchesBits.length; i++) {
			System.out.printf(" %04X", switchesBits[i]);
		}
		System.out.printf("\n");
	}

}
