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

package dev.hawala.dmachine.engine.agents;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.PilotDefs;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;

/**
 * Agent for a floppy disk drive of a Dwarf machine,
 * <p>
 * One single floppy drive is currently supported (it is also
 * unknown if Pilot can work with more than one drive).
 * <br>
 * The agent works synchronously, meaning that floppy disk i/o occurs during the
 * {@code call()} (resp.the CALLAGENT instruction). The interrupt signaling the
 * end of operation is therefore requested at the end of the {@code call()}
 * method call. 
 * </p>
 * <p>
 * The following floppy formats for floppy images are supported:
 * </p>
 * <ul>
 * <li>IMD for legacy floppies if the file extension is {@code .imd} (case-insensitive</li>
 * <li>DMK for legacy floppies if the file extension is {@code .dmk} (case-insensitive</li>
 * <li>raw format for 3.5" floppies as created by the original emulator on PCs</li> 
 * </ul>
 * <p>
 * The "legacy floppy" means that the image was created from a floppy disk written by
 * a 8010 (8" floppy) or a 6085 (5.25" floppy) workstation with XDE (4.0 or later) or
 * ViewPoint (1.0 or later). Legacy floppy images are mounted in R/O mode, as changes
 * cannot be written back into the original format (IMD or DMK). 
 * <br/>
 * The disk content of the legacy floppy image is implanted in a template 3.5" image
 * based on the XDE sector layout. This allows to read the legacy floppy content.
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2018)
 */
public class FloppyAgent extends Agent {
	
	/*
	 * faked floppy properties for a 1.44MB, 3 1/2" disks
	 */
	private static final int FLOPPY_HEADS = 2;
	private static final int FLOPPY_SECTORS = 18;
	private static final int FLOPPY_CYLS = 80; // nominal number of tracks for a 3 1/2" disk, gives 1440000 bytes
	private static final int FLOPPY_TOTAL_SECTORS = FLOPPY_HEADS * FLOPPY_SECTORS * FLOPPY_CYLS;
	
	
	/*
	 * FloppyFCBType
	 * FloppyDCBType (we support exactly one floppy drive, with a floppy inserted or not)
	 */
	private static final int fcb_lp_nextIOCB = 0;
	private static final int fcb_w_interruptSelector = 2;
	private static final int fcb_w_stopAgent = 3;
	private static final int fcb_w_agentStopped = 4;
	private static final int fcb_w_numberOfDCBs = 5;
	private static final int dcb_w_deviceType = 6;
	private static final int dcb_w_numberOfCylinders = 7;
	private static final int dcb_w_numberOfHeads = 8;
	private static final int dcb_w_sectorsPerTrack = 9;
	private static final int dcb_w_ready = 10;
	private static final int dcb_w_diskChanged = 11;
	private static final int dcb_w_twoSided = 12;
	private static final int dcb_w_suggestedTries = 13;
	private static final int FCB_SIZE = 14;
	
	// status codes (FloppyDiskFace.Status)
	private static final short Status_inProgress       =  0;
	private static final short Status_goodCompletion   =  1;
	private static final short Status_diskChange       =  2;
	private static final short Status_notReady         =  3;
	private static final short Status_cylinderError    =  4;
	private static final short Status_deletedData      =  5;
	private static final short Status_recordNotFound   =  6;
	private static final short Status_headerError      =  7;
	private static final short Status_dataError        =  8;
	private static final short Status_dataLost         =  9;
	private static final short Status_writeFault       = 10;
	private static final short Status_memoryError      = 11;
	private static final short Status_invalidOperation = 12;
	private static final short Status_aborted          = 13;
	private static final short Status_otherError       = 14;
	
	private static final short Status_invalidCHS       = Status_recordNotFound; // TODO: better status code ??
	
	// operations
	private static final int Op_nop = 0;
	private static final int Op_readSector = 1;
	private static final int Op_writeSector = 2;
	private static final int Op_writeDeletedSector = 3;
	private static final int Op_readId = 4;
	private static final int Op_formatTrack = 5;
	private static final String[] OP_NAMES = {
		"nop", "readSector", "writeSector", "writeDeletedSector", "readId", "formatTrack"	
	};
	
	// FloppyIOCBType
	private static final int iocb_w_oper_device = 0;
	private static final int iocb_w_oper_function = 1;
	private static final int iocb_w_oper_address_cylinder = 2;
	private static final int iocb_w_oper_adddress_sectorHead = 3; // head(0..7) , sector(8..15)
	private static final int iocb_lp_oper_dataPtr = 4;
	private static final int iocb_w_oper_incrementDataPtrAndRetries = 6; // incrementDataPointer(6:0..0) , tries(6:1..15)
	private static final int iocb_w_oper_count = 7; // number of sectors to transfer OR number of tracks to format
	private static final int iocb_w_density = 8;
	private static final int iocb_w_sectorLength = 9;
	private static final int iocb_w_sectorsPertrack = 10;
	private static final int iocb_w_status = 11; // FloppyDiskFace.Status
	private static final int iocb_lp_nextIocb = 12;
	private static final int iocb_w_retries = 13;
	private static final int iocb_w_logStatus = 14;
	
	private static final int incrementDataPtrFlag = 0x00008000;
	private static final int retriesMask = 0x00007FFF;
	
	private interface FloppyDisk {
		
		boolean isReadonly();
		
		boolean isInvalidCHS(int cylinder, int head, int sector);
		
		int getLinearSector(int cylinder, int head, int sector);
		
		short readSector(int absSector, int memAddress, int sectorLength);
		
		short writeSector(int absSector, int memAddress, int sectorLength);
		
		short writeDeletedSector(int absSector);
		
		short formatTrack(int cylinder, int head);
		
		void storeSectorNoIntoIocb(int linearSector, int iocb);
		
		void saveFloppy(StringBuilder sb);
	}
	
	private  static class FloppyDisk3dot5 implements FloppyDisk {
		
		public final static int WORD_SIZE = FLOPPY_HEADS * FLOPPY_CYLS * FLOPPY_SECTORS * PrincOpsDefs.WORDS_PER_PAGE;
		private final static int BYTE_SIZE = WORD_SIZE * 2;
		
		protected static final byte[] ioBuffer = new byte[4096];
		
		private final File f;
		private final boolean swapBytes;
		private final boolean readonly;
		
		protected final short[] content;
		
		private boolean changed = false;
		
		private void logf(String template, Object... args) {
			if (Config.AGENTS_LOG_FLOPPY) {
				System.out.printf("FloppyFile[" + f.getName() + "]: " + template, args);
			}
		}
		
		public FloppyDisk3dot5(File f, short[] content) throws IOException {
			if (content == null || content.length < WORD_SIZE) {
				throw new IOException("Invalid file content");
			}
			this.content = content;
			this.f = f;
			this.swapBytes = false;
			this.readonly = true;
		}
		
		public FloppyDisk3dot5(File f, boolean readonly) throws IOException {
			this.content = new short[WORD_SIZE];
			this.f = f;
			if (!f.exists()) {
				logf("file not found\n");
				throw new IOException("Floppy file '" + f.getName() + "' not found");
			}
			if (!f.canWrite()) {
				this.readonly = true;
			} else {
				this.readonly = readonly;
			}
			if (f.length() != BYTE_SIZE) {
				throw new IOException("Floppy file has wrong size " + f.length() + " bytes (instead of " + BYTE_SIZE + ")");
			}
			
			// load floppy content
			synchronized(ioBuffer) {
				try (FileInputStream fis = new FileInputStream(f)) {
					this.swapBytes = this.loadRawContent(fis);
				}
			}
		}
		
		protected boolean loadRawContent(InputStream fis) throws IOException {
			// read first 8 sectors
			fis.read(ioBuffer);
			
			// check signatures on sectors 4 and 6 (counting from 0) to see if bytes must be swapped
			boolean swapBytes                                                       // a xerox formatted floppy has the following signatures:
				 = ioBuffer[2048] == (byte)0xD9 && ioBuffer[2049] == (byte)0xC5  // 5. sector starts with 0xC5D9 => swap if reversed 
				&& ioBuffer[3072] == (byte)0xD6 && ioBuffer[3073] == (byte)0xE5; // 7. sector starts with 0xE5D6 => swap if reversed
			
			// load file content ioBuffer-wise converting byte-pairs to shorts
			int bufferPos = 0;
			for (int i = 0; i < WORD_SIZE; i++) {
				if (bufferPos >= ioBuffer.length) {
					fis.read(ioBuffer);
					bufferPos = 0;
				}
				int b1 = ((swapBytes) ? ioBuffer[bufferPos + 1] : ioBuffer[bufferPos]) & 0xFF;
				int b2 = ((swapBytes) ? ioBuffer[bufferPos] : ioBuffer[bufferPos + 1]) & 0xFF;
				bufferPos += 2;
				short w = (short)((b1 << 8) | b2);
				this.content[i] = w;
			}
			
			// done
			return swapBytes;
		}
		
		@Override
		public boolean isReadonly() {
			return this.readonly;
		}
		
		@Override
		public void saveFloppy(StringBuilder sb) {
			if (this.readonly) { return; }
			if (!this.changed) { return; }
			
			logf("saveFloppy(): writing floppy back back");
			synchronized(ioBuffer) {
				try (FileOutputStream fos = new FileOutputStream(this.f)) {
					int bufferPos = 0;
					for (int i = 0; i < WORD_SIZE; i++) {
						int w = this.content[i] & 0xFFFF;
						int b1 = ((this.swapBytes)) ? w & 0xFF : w >>> 8;
						int b2 = ((this.swapBytes)) ? w >>> 8 : w & 0xFF;
						ioBuffer[bufferPos++] = (byte)b1;
						ioBuffer[bufferPos++] = (byte)b2;
						if (bufferPos >= ioBuffer.length) {
							fos.write(ioBuffer);
							bufferPos = 0;
						}
					}
					if (bufferPos > 0) { // should not happen, just to be sure...
						fos.write(ioBuffer, 0, bufferPos);
					}
				} catch (IOException e) {
					if (sb != null) {
						if (sb.length() > 0) { sb.append("\n"); }
						sb.append("Error writing floppy content back: ").append(e.getMessage());
					}
				}
			}
		}
		
		@Override
		public boolean isInvalidCHS(int cylinder, int head, int sector) {
			return cylinder < 0 || cylinder >= FLOPPY_CYLS
				|| head < 0 || head >= FLOPPY_HEADS
				|| sector < 1 || sector > FLOPPY_SECTORS; // sector counting starts with 1 ...??
		}
		
		@Override
		public int getLinearSector(int cylinder, int head, int sector) {
			int absoluteSector = (cylinder * FLOPPY_HEADS + head) * FLOPPY_SECTORS + sector - 1; // sector counting starts with 1 ... 
			return absoluteSector;
		}
		
		@Override
		public void storeSectorNoIntoIocb(int linearSector, int iocb) {
			int sector = (linearSector % FLOPPY_SECTORS) + 1;
			int track = linearSector / FLOPPY_SECTORS;
			int head = track % FLOPPY_HEADS;
			short cylinder = (short)(track / FLOPPY_HEADS);
			
			short sectorHead = (short)(((head << 8) | (sector & 0xFF)) & 0xFFFF);
			
			Mem.writeWord(iocb + iocb_w_oper_address_cylinder, cylinder);
			Mem.writeWord(iocb + iocb_w_oper_adddress_sectorHead, sectorHead);
		}
		
		@Override
		public short readSector(int absSector, int memAddress, int sectorLength) {
			// log and basic plausibility checks
			logf("    readSector ( absSector = %d , sectorLength = %d , memAddress = 0x%08X => realPage = 0x%06X )\n",
					absSector, sectorLength, memAddress, Mem.getVPageRealPage(memAddress >>> 8));
			if (!Mem.isWritable(memAddress) || !Mem.isWritable(memAddress + PrincOpsDefs.WORDS_PER_PAGE - 1)) {
				logf(" *error* readSector : target memory not writable\n");
				return Status_memoryError;
			}
			int diskWordOffset = absSector * PrincOpsDefs.WORDS_PER_PAGE;
			if ((diskWordOffset + PrincOpsDefs.WORDS_PER_PAGE) > this.content.length) {
				logf(" *error* readSector : absSector out of range\n");
				return Status_invalidCHS;
			}
			
			// copy sector content
			for (int i = 0; i < Math.min(sectorLength, PrincOpsDefs.WORDS_PER_PAGE); i++) {
//				if ((i % 16) == 0) { System.out.printf("\n         0x%02X : 0x", i); }
				short w = this.content[diskWordOffset++];
//				System.out.printf(" %04X", w);
				Mem.writeWord(memAddress++, w);
			}
//			System.out.printf("\n\n");

			// done
			return Status_goodCompletion;
		}
		
		@Override
		public short writeSector(int absSector, int memAddress, int sectorLength) {
			// log and basic plausibility checks
			logf("    writeSector ( absSector = %d , sectorLength = %d , memAddress = 0x%08X => realPage = 0x%06X )\n",
					absSector, sectorLength, memAddress, Mem.getVPageRealPage(memAddress >>> 8));
			if (this.readonly) {
				logf(" *error* writeSector : floppy is readonly\n");
				return Status_writeFault;
			}
			if (!Mem.isReadable(memAddress) || !Mem.isReadable(memAddress + PrincOpsDefs.WORDS_PER_PAGE - 1)) {
				logf(" *error* writeSector : target memory not readable\n");
				return Status_memoryError;
			}
			int diskWordOffset = absSector * PrincOpsDefs.WORDS_PER_PAGE;
			if ((diskWordOffset + PrincOpsDefs.WORDS_PER_PAGE) > this.content.length) {
				logf(" *error* writeSector : absSector out of range\n");
				return Status_invalidCHS;
			}
			
			// copy sector content
			for (int i = 0; i < Math.min(sectorLength, PrincOpsDefs.WORDS_PER_PAGE); i++) {
//				if ((i % 16) == 0) { System.out.printf("\n         0x%02X : 0x", i); }
				short w = Mem.readWord(memAddress++);
				this.content[diskWordOffset++] = w;
//				System.out.printf(" %04X", w);
			}
//			System.out.printf("\n\n");

			// done
			this.changed = true;
			return Status_goodCompletion;
		}
		
		@Override
		public short writeDeletedSector(int absSector) {
			// log and basic plausibility checks
			logf("    writeDeletedSector ( absSector = %d )\n", absSector);
			if (this.readonly) {
				logf(" *error* writeDeletedSector : floppy is readonly\n");
				return Status_writeFault;
			}
			int diskWordOffset = absSector * PrincOpsDefs.WORDS_PER_PAGE;
			if ((diskWordOffset + PrincOpsDefs.WORDS_PER_PAGE) >= this.content.length) {
				logf(" *error* writeDeletedSector : absSector out of range\n");
				return Status_invalidCHS;
			}
			
			// zero out sector content
			for (int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE; i++) {
				this.content[diskWordOffset++] = 0;
			}

			// done
			this.changed = true;
			return Status_goodCompletion;
		}
		
		@Override
		public short formatTrack(int cylinder, int head) {
			// log and basic plausibility checks
			logf("    formatTrack ( ch = ( %d , %d )\n", cylinder, head);
			if (this.isInvalidCHS(cylinder, head, 1)) {
				logf(" *error* formatTrack ch = ( %d , %d  ) out of range\n", cylinder, head);
				return Status_invalidCHS;
			}
			if (this.readonly) {
				logf(" *error* formatTrack : floppy is readonly\n");
				return Status_writeFault;
			}
			
			// zero out sector content
			int diskWordOffset = this.getLinearSector(cylinder, head, 1) * PrincOpsDefs.WORDS_PER_PAGE;
			for (int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE * FLOPPY_SECTORS; i++) {
				this.content[diskWordOffset++] = 0;
			}

			// done
			this.changed = true;
			return Status_goodCompletion;
		}
	}
	
	private static class LegacyFloppyDisk extends FloppyDisk3dot5 {

		// the sectors of the original disk to be transformed
		protected final List<short[]> sectors = new ArrayList<>();
		
		public LegacyFloppyDisk(File f) throws IOException {
			super(f, new short[WORD_SIZE]);
		}
		
		protected void buildNewFloppy() throws IOException {
			this.loadTemplate();
			this.implant();
		}
		
		
		/*
		 * legacy floppy specifics
		 */
		
		protected int cyl0Sectors;
		protected int dataSectors;
		
		protected int cylinders;
		
		protected int track0WordsPerSector;
		protected int track1WordsPerSector;
		protected int dataWordsPerSector;
		
		protected void logf(String template, Object... args) {
			if (Config.AGENTS_LOG_FLOPPY) {
				System.out.printf(template, args);
			}
		}
		
		private int tmplMaxTocEntries;
		private int tmplTocLba;
		private int tmplTocBase;
		
		private void loadTemplate() throws IOException {
			// load the template
			final String templateName = "resources/base144.raw";
			InputStream res = this.getClass().getClassLoader().getResourceAsStream(templateName);
			if (res == null) {
				throw new IOException("Resource not found: " + templateName);
			}
			this.loadRawContent(res);
			res.close();
			
			// get the template characteristics
			int sect9Base = 8 * 256; // sector 9 
			this.tmplTocLba =  this.content[sect9Base + 5];
			this.tmplTocBase = (this.tmplTocLba - 1) * 256;
			if (this.tmplTocBase > (this.content.length - 256)) {
				throw new IOException("Invalid template file (tocBae not in floppy range");
			}
			int tocSeal = this.content[this.tmplTocBase] & 0xFFFF;
			if (tocSeal != 0xB2CB) {
				throw new IOException("Template does not contain a valid Xerox floppy TOC (invalid seal)");
			}
			this.tmplMaxTocEntries = this.content[this.tmplTocBase + 3];
		}
		
		private void implant() throws IOException {
			int tocLba = w(9,5);
			int tocSector = lba2sector(tocLba);
			int tocSeal = w(tocSector,0);
			int tocEntries = w(tocSector,2);
			int tocMaxEntries = w(tocSector,3);
		
			if (tocSeal != 0xB2CB) {
				throw new IOException("IMD file does not contain a valid Xerox floppy TOC (invalid seal)");
			}
			if (tocEntries > (this.tmplMaxTocEntries - 1)) {
				throw new IOException("IMD file cannot be cloned (more than 144 files in TOC)");
			}
			
			// ?? int diffLba = tocLba - this.tmplTocLba; // newLba <- oldLba - diffLba
			
			logf("\n###\n#### begin implanting\n###\n\n");
			
			/*
			 *  find 1st link-block after toc in each floppy
			 */
			
			// old floppy
			int linklba = tocLba - 1;
			int linksector = lba2sector(linklba); // still 1-based !!
			logf("imd 1st link lba : %d\n", linklba);
			if (w(linksector, 0) != 0x32CB) { logf("!! error: imd link-sector %d (before toc) has wrong signature\n", linksector); }
			int countToc = w(linksector, 129);
			linklba += 1 + countToc;
			linksector += 1 + countToc;
			int firstDataLba = linklba;
			logf("imd 2nd link lba : %d (1st data segment = 1st behing toc)\n", linklba);
			if (w(linksector, 0) != 0x32CB) { logf("!! error: imd link-sector %d (behind toc) has wrong signature\n", linksector); }
			
			// template
			int tmplFirstLba = this.tmplTocLba - 1;
			int tmplLinklba = tmplFirstLba;
			int tmplBase = (tmplLinklba -1) * 256;
			logf("tmpl 1st link lba: %d\n", tmplLinklba);
			if ((this.content[tmplBase] & 0xFFFF) != 0x32CB) { logf("!! error: tmpl link-sector %d (before toc) has wrong signature\n", linksector); }
			int tmplCountToc = this.content[tmplBase + 129] & 0xFFFF;
			tmplLinklba += 1 + tmplCountToc;
			tmplBase += (1 + tmplCountToc) * 256;
			int tmplFirstDataLba = tmplLinklba;
			logf("tmpl 2nd link lba: %d (1st data segment = 1st behind toc)\n", tmplLinklba);
			if ((this.content[tmplBase] & 0xFFFF) != 0x32CB) { logf("!! error: tmpl link-sector %d (behind toc) has wrong signature\n", linksector); }
			
			/*
			 * transfer the table of contents
			 */
			
			// the lba difference to apply to the lba field in toc entries
			int diffLba = firstDataLba - tmplFirstDataLba; // newLba <- oldLba - diffLba
			logf("-- diffLba = %d\n\n", diffLba);
			
			int tmplOut = this.tmplTocBase + 4; // start of toc entries
			tmplOut += 5; // skip the first toc entry, which represents the toc itself
			for (int i = 1; i < tocEntries; i++) {
				int entryword0 = tocw(tocSector, 4 + (5*i));
				int entryword1 = tocw(tocSector, 4 + (5*i) + 1);
				int entrytype = tocw(tocSector, 4 + (5*i) + 2);
				int entrylba = tocw(tocSector, 4 + (5*i) + 3);
				int entrylength = tocw(tocSector, 4 + (5*i) + 4);
				this.content[tmplOut++] = (short)entryword0;
				this.content[tmplOut++] = (short)entryword1;
				this.content[tmplOut++] = (short)entrytype;
				this.content[tmplOut++] = (short)(entrylba - diffLba);
				this.content[tmplOut++] = (short)entrylength;
				logf("toc[%2d]: type 0x%04X , lba ( old: %3d - new: %3d ) , len %3d\n", i, entrytype, entrylba, entrylba - diffLba, entrylength);
			}
			
			this.content[this.tmplTocBase + 2] = (short)tocEntries;
			
			
			/*
			 * implant the "link to next" part of old into the template 
			 */
			int nextSegmentLength = w(linksector, 129); // number of sectors in next segment
			this.content[tmplBase + 128] = (short)w(linksector, 128); // unknown, link type ??
			this.content[tmplBase + 129] = (short)nextSegmentLength; 
			this.content[tmplBase + 130] = (short)w(linksector, 130); // unknown
			this.content[tmplBase + 131] = (short)w(linksector, 131); // unknown
			this.content[tmplBase + 132] = (short)w(linksector, 132); // file number of the file in next segment
			this.content[tmplBase + 133] = (short)w(linksector, 133); // unknown
			this.content[tmplBase + 134] = (short)w(linksector, 134); // entry type of the file in next segment
			
			/*
			 * copy link &  data blocks from old to template 
			 */
			logf("\n-- begin copying content\n");
			int lastLba = this.dataSectors * this.cylinders * 2; // 2 heads
			int currLba = linklba + 1; // start with first data sector
			int tmplCurrLba = tmplLinklba;
			while(currLba <= lastLba) {
				int sector = lba2sector(currLba); // still 1-based !!
				tmplCurrLba++;
				tmplBase += 256; // point behind the last link block
				
				if (w(sector, 0) == 0x32CB) {
					logf("link-sector at lba : old %03d - new %03d (%d)\n", currLba, tmplCurrLba, (tmplBase/256)+1);
				}
				
				for (int i = 0; i < 256; i++) {
					this.content[tmplBase + i] = (short)w(sector, i);
				}
				
				currLba++;
			}
			logf("-- done copying content\n");
			
			/*
			 * tmplCurrLba/tmplBase point to the copied closing link sector (last sector) of the old floppy
			 * => if the previous segment was empty: patch the previous link to point to the end of the new floppy
			 * => if the previous segment was data: let this link point to the end of the new floppy as free space 
			 */
			logf("++ handling last segment\n");
			int tmplLastLba = FLOPPY_TOTAL_SECTORS;
			int prevEntryType = this.content[tmplBase + 7];
			final int remainingFreeSectors;
			if (prevEntryType == 0) {
				// previous segment was free space
				int preceedingFree = this.content[tmplBase + 2] & 0xFFFF;
				logf("++ ... last segment was free space: %d sectors\n", preceedingFree);
				
				// free blocks := preceeding + (here to end)
				remainingFreeSectors = preceedingFree + (tmplLastLba - tmplCurrLba);
				
				// remove the signature from this one
				this.content[tmplBase] = 0;
				
				// move back to the previous link sector
				tmplBase -= (preceedingFree + 1) * 256;
			} else {
				// previous segment was data => free are the blocks after this to before floppy end
				remainingFreeSectors = tmplLastLba - tmplCurrLba - 1;
				logf("++ ... last segment was content\n");
			}
			logf("++ remainingFreeSectors: %d\n", remainingFreeSectors);
			this.content[tmplBase + 129] = (short)remainingFreeSectors;
			this.content[tmplBase + 132] = 0; // next block is not a file
			this.content[tmplBase + 134] = 0; // file type for "free space"
			
			/*
			 * put the length of the last free segment in the final link page of the new floppy
			 */
			tmplBase = (tmplLastLba - 1) * 256;
			if ((this.content[tmplBase] & 0xFFFF) != 0x32CB) { logf("!! error: tmpl link-sector %d (floppy end) has wrong signature\n", linksector); }
			this.content[tmplBase + 2] = (short)remainingFreeSectors;
			
			/*
			 * patch the special counter in the prefix area of the template floppy (see diffs log)
			 * and transfer the floppy label
			 */
			int sect9Base = 8 * 256; // sector 9
			this.content[sect9Base + 18] = (short)w(9, 18);
			
			int labelLengthBytes = Math.min(w(9, 21), 40);
			int labelLengthWords = (labelLengthBytes + 1) / 2;
			this.content[sect9Base + 21] = (short)labelLengthBytes;
			for (int i = 0; i < labelLengthWords; i++) {
				this.content[sect9Base + 22 + i] = (short)w(9, 22 + i);
			}
			
			/*
			 * this was it hopefully
			 */
			
			logf("\n###\n#### done implanting\n###\n\n");
			
			logf("++ start segment links in final floppy\n");
			int lba = tmplFirstLba;
			while(lba <= tmplLastLba) {
				int linkBase = (lba - 1) * 256;
				int signature = this.content[linkBase];
				if (signature != 0x32CB) {
					logf("*** invalid link signature => aborting link traversal\n");
					break;
				}
				
				int sig1 = this.content[linkBase + 1] & 0xFFFF;
				int count1 = this.content[linkBase + 2] & 0xFFFF;
				int num1 = this.content[linkBase + 5] & 0xFFFF;
				int type1 = this.content[linkBase + 7] & 0xFFFF;
				logf("\n prev :: sig = 0x%04X  count = %4d  filenum = %3d  type = 0x%04X  -- link lba = %04d\n",
						sig1, count1, num1, type1, lba);
				
				int sig2 = this.content[linkBase + 128] & 0xFFFF;
				int count2 = this.content[linkBase + 129] & 0xFFFF;
				int num2 = this.content[linkBase + 132] & 0xFFFF;
				int type2 = this.content[linkBase + 134] & 0xFFFF;
				logf(" next :: sig = 0x%04X  count = %4d  filenum = %3d  type = 0x%04X\n",
						sig2, count2, num2, type2);
				
				lba += 1 + count2;
			}
			logf("\n++ end segment links in final floppy\n");
		}
		

		
		/*
		 * utilities
		 */
		
		private int w(int sector, int word) {
			return this.sectors.get(sector - 1)[word] & 0xFFFF;
		}
		
		// works only for data sectors! (not sectors in cyl. 0)
		// (lba ~ logical block address ~ as if cyl. 0 had #dataSectors instead of #cyl0Sectors)
		private int lba2sector(int lba) {
			int sector = lba + ((cyl0Sectors - dataSectors) * 2);
			return sector;
		}
		
		private int tocw(int tocsector, int word) {
			while(word > 255) {
				word -= 256;
				tocsector++;
			}
			return w(tocsector, word);
		}
		
		protected void dumpStructure() {
			logf("sector 9\n");
			logf("--------\n\n");
			logf("#tracks      (9,2): %d\n", w(9,2));
			logf("#heads       (9,3): %d\n", w(9,3));
			logf("#sectors     (9,4): %d\n", w(9,4));
			logf("#toc-lba     (9,5): %d\n", w(9,5));
			logf("#toc-sectors (9,8): %d\n", w(9,8));
			
			logf("toc entry count (9,18) : %d\n\n", w(9,18));
			
			logf("table-of-content\n");
			logf("----------------\n\n");
			int toclba = w(9,5);
			int tocsector = lba2sector(toclba);
			int tocseal = w(tocsector,0);
			int tocentries = w(tocsector,2);
			logf("seal         : 0x%04X (%s)\n", tocseal, (tocseal == 0xB2CB) ? "OK" : "!! not OK !!");
			logf("#toc-entries : %d\n", tocentries);
			logf("#toc-max-entr: %d\n", w(tocsector,3));
			for (int i = 0; i < tocentries; i++) {
				int entrytype = tocw(tocsector, 4 + (5*i) + 2);
				int entrylba = tocw(tocsector, 4 + (5*i) + 3);
				int entrylength = tocw(tocsector, 4 + (5*i) + 4);
				String typename;
				switch(entrytype) {
				case 0x0806: typename = "directory"; break;
				case 0x0804: typename = "file"; break;
				case 0x0809: typename = "subdirectory"; break;
				case 0x0000: typename = "free space"; break;
				case 0xFFFF: typename = "UNUSED"; break;
				default: typename = "invalid";
				}
				logf(" toc-entry[%2d] - lba = %4d , length = %4d , type = 0x%04X (%s)\n",
						i, entrylba, entrylength, entrytype, typename);
			}
			
			logf("\nlinking info\n");
			int linklba = toclba - 1;
			int linksector = lba2sector(linklba); // 1-based !!
			boolean done = false;
			while(linksector <= this.sectors.size() && !done) {
				int signature = w(linksector, 0);
				if (signature != 0x32CB) {
					logf("*** invalid link signature => aborting\n");
					return;
				}
				
				int sig1 = w(linksector, 1);
				int count1 = w(linksector, 2);
				int num1 = w(linksector, 5);
				int type1 = w(linksector, 7);
				logf("\n prev :: sig = 0x%04X  count = %4d  filenum = %3d  type = 0x%04X  -- link lba = %04d - link sector: %4d\n",
						sig1, count1, num1, type1, linklba, linksector);
				
				int sig2 = w(linksector, 128);
				int count2 = w(linksector, 129);
				int num2 = w(linksector, 132);
				int type2 = w(linksector, 134);
				logf(" next :: sig = 0x%04X  count = %4d  filenum = %3d  type = 0x%04X\n",
						sig2, count2, num2, type2);
				
				linksector += 1 + count2;
				linklba += 1 + count2;
			}
			logf("\n---- done linking info (floppy sector count = %d)\n", this.sectors.size());
		}
		
		private void dumpContent() {
			int lba = 1;
			for (short[] sector : this.sectors) {
				logf("\n");
				logf("LBA %d\n", lba);
				for (int i = 0; i < sector.length; i++) {
					if ((i % 16) == 0) {
						logf("\n[%3d] ", i);
					}
					logf(" %04X", sector[i]);
				}
				logf("\n");
				lba++;
			}
		}
	}
	
	private static class IMDFloppyDisk extends LegacyFloppyDisk {
		
		public IMDFloppyDisk(File f) throws IOException {
			super(f);
			
			this.loadIMD(f);
			this.dumpStructure();
			
			this.buildNewFloppy();
		}
		
		/*
		 * IMD specifics
		 */
		
		private static final int sectInfo2Bytesize[] = { 128 , 256 , 512 , 1024 , 2048 , 4096 , 8192 };
		
		private short readWord(BufferedInputStream bis) throws IOException {
			int h = bis.read();
			if (h < 0) { throw new IOException("Premature EOF"); }
			int l = bis.read();
			if (l < 0) { throw new IOException("Premature EOF"); }
			int w = ((h << 8) & 0xFF00) | (l & 0xFF);
			return (short)w;
		}
		
		// returns sector content
		private short[] readSector(BufferedInputStream bis, int sectorSize) throws IOException {
			int contentType = bis.read();
			if (contentType < 0) { throw new IOException("Premature EOF"); }
			
			short[] sector = new short[sectorSize];
			switch(contentType) {
			case 1:
			case 3:
			case 5:
			case 7: {
					for(int i = 0; i < sectorSize; i++) {
						sector[i] = readWord(bis);
					}
				}
				break;
				
			case 2:
			case 4:
			case 6:
			case 8: {
					int b = bis.read();
					int w = ((b << 8) & 0xFF) | (b & 0xFF);
					for(int i = 0; i < sectorSize; i++) {
						sector[i] = (short)w;
					}
				}
				break;
			
			default:
				for(int i = 0; i < sectorSize; i++) {
					sector[i] = 0;
				}
			}
			
			return sector;
		}
		
		// returns number of sectors
		private int readTrack(BufferedInputStream bis) throws IOException {
			// get track characteristics
			int mode = bis.read();
			int cylNo = bis.read();
			int headNo = bis.read();
			int numSects = bis.read();
			int sectInfo = bis.read();
			if (mode < 0 || cylNo < 0 || headNo < 0 || numSects < 0 || sectInfo < 0) {
				throw new IOException("Premature EOF");
			}
			
			// sanity checks
			if (numSects == 0) { return numSects; }
			if (sectInfo > sectInfo2Bytesize.length) {
				throw new IOException("Invalid IMD file, sectInfo not in [0..5]");
			}
			int sectorByteSize = sectInfo2Bytesize[sectInfo];
			int sectorWordSize = sectorByteSize / 2;
			
			// logf("Track cyl %d head %d : numSects = %d sectorByteSize = %d\n", cylNo, headNo, numSects, sectorByteSize);
			
			// skip sector numbering map (Xerox floppies have linear ascending sector ordering, as seens so far...)
			for (int i = 0; i < numSects; i++) {
				if (bis.read() < 0) { throw new IOException("Premature EOF"); }
			}
			
			// skip sectorCylinder map if present
			if ((headNo & 0x80) != 0) {
				for (int i = 0; i < numSects; i++) {
					if (bis.read() < 0) { throw new IOException("Premature EOF"); }
				}
			}
			
			// skip sectorHead map if present
			if ((headNo & 0x40) != 0) {
				for (int i = 0; i < numSects; i++) {
					if (bis.read() < 0) { throw new IOException("Premature EOF"); }
				}
			}
			
			// read sectors and append to sector list
			for (int i = 0; i < numSects; i++) {
				this.sectors.add(this.readSector(bis, sectorWordSize));
			}
			
			// done for this track
			return numSects;
		}
		
		private void loadIMD(File f) throws IOException {
			if (!f.exists()) {
				logf("file not found\n");
				throw new IOException("Floppy file '" + f.getName() + "' not found");
			}
			
			logf("** start loading IMD floppy: %s\n", f.getName());
			
			try (FileInputStream fis = new FileInputStream(f);
				 BufferedInputStream bis = new BufferedInputStream(fis)) {
				// skip human readable IMD header
				StringBuilder sb = new StringBuilder();
				int b = bis.read();
				while (b >= 0 && b != 0x1A) { // ASCII EOF = end of readable header
					if (b == 0x0A) { // LF
						logf("header :: %s\n", sb.toString());
						sb.setLength(0);
					} else if (b != 0x0D) { // CR 
						sb.append((char)b);
					}
					b = bis.read();
				}
				if (sb.length() > 0) { logf("header :: %s\n", sb.toString()); }
				
				// read track 0
				this.cyl0Sectors = this.readTrack(bis);
				this.track0WordsPerSector = this.sectors.get(0).length;
				
				// read track 1
				int track1SectorCount = this.readTrack(bis);
				if (track1SectorCount != this.cyl0Sectors) {
					logf("### ERROR sector count in track1 (%d) and track0 (%d) differ\n", track1SectorCount, this.cyl0Sectors);
					throw new IOException("Invalid IMD file: sector count in track0 and track1 differ");
				}
				this.track1WordsPerSector = this.sectors.get(this.cyl0Sectors).length;
				
				// read data tracks
				this.dataSectors = this.readTrack(bis);
				this.dataWordsPerSector = this.sectors.get(this.cyl0Sectors * 2).length;
				int dataTracks = 1;
				while(bis.available() > 0) {
					this.readTrack(bis);
					dataTracks++;
				}
				if ((dataTracks % 2) == 1) {
					throw new IOException("Invalid IMD file: invalid number of data tracks");
				}
				this.cylinders= (dataTracks / 2) + 1; // +1: cylinder 0
			}
			
			logf("** IMD characteristics: cyl0Sectors          = %d\n", cyl0Sectors);
			logf("** IMD characteristics: track0WordsPerSector = %d\n", track0WordsPerSector);
			logf("** IMD characteristics: track1WordsPerSector = %d\n", track1WordsPerSector);
			logf("** IMD characteristics: dataSectors          = %d\n", dataSectors);
			logf("** IMD characteristics: dataWordsPerSector   = %d\n", dataWordsPerSector);
			logf("** IMD characteristics: cylinders            = %d\n", cylinders);
			logf("\n");
		}
	}
	
	private static class DMKFloppyDisk extends LegacyFloppyDisk {

		public DMKFloppyDisk(File f) throws IOException {
			super(f);
			
			this.loadDMK(f);
			this.dumpStructure();
			
			this.buildNewFloppy();
		}
		
		private int readByte(BufferedInputStream bis) throws IOException {
			return bis.read() & 0xFF;
		}
		
		private int readWord(BufferedInputStream bis) throws IOException {
			return this.readByte(bis) | (this.readByte(bis) << 8);
		}
		
		private void readTrack(int trackNo, BufferedInputStream bis, int rawTrackLength) throws IOException {
			logf("\n-- begin track %d\n", trackNo);
			// read the max. 64 sector infos
			int[] offsets = new int[64];
			boolean[] isDD = new boolean[64];
			int sectCount = 0;
			for (int i = 0; i < offsets.length; i++) {
				int idam = this.readWord(bis);
				isDD[i] = (idam & 0x8000) != 0; 
				offsets[i] = (idam & 0x3FFF) - 0x80;
				if (offsets[i] == 0) { continue; } // not used sector
				sectCount++;
			}
			
			// read the sector contents
			int trackLength = rawTrackLength - 0x80;
			byte[] sectorData = new byte[trackLength];
			if (bis.read(sectorData) != sectorData.length) {
				throw new IOException("Short read on sector data");
			}
			
			int[] rawSectLengths = new int[sectCount];
			int lastOffset = trackLength;
			for (int i = sectCount - 1; i >= 0; i--) {
				rawSectLengths[i] = lastOffset - offsets[i];
				lastOffset = offsets[i];
			}
			
			if (trackNo == 0) {
				// xerox disks have single density 128 bytes in first track => all bytes are doubled in DMK
				// (ok, this is lazy, this info must be somehow in the metadata of the track or disk or ...)
				for (int i = 0; i < trackLength/2; i++) {
					sectorData[i] = sectorData[i*2]; 
				}
				trackLength /= 2;
				for (int i = 0; i < sectCount; i++) {
					offsets[i] /= 2;
					rawSectLengths[i] /= 2;
				}
			}
			
			for (int i = 0; i < sectCount; i++) {
				logf("- sector[%2d] : offset = 0x%03X (%04d) , isDD: %5s, length = %d\n", i, offsets[i], offsets[i], (isDD[i]) ? "true" : "false", rawSectLengths[i]);
				logf("    ");
				
				int offset = offsets[i]; // start of raw data for this the sector
				
				for (int idx = 0; idx < 32; idx++) {
					logf(" %02X", sectorData[offset + idx]);
				}
				logf(" ...\n");
				
				int dataStart = offset;
				int signature = sectorData[dataStart++] & 0xFF;
				int cyl = sectorData[dataStart++] & 0xFF;
				int head = sectorData[dataStart++] & 0xFF;
				int sect = sectorData[dataStart++] & 0xFF;
				int sizeCode = sectorData[dataStart++] & 0xFF;
				dataStart += 2; // skip CRC
				
				// overread gap
				for (int gapIdx = 0; gapIdx < 50; gapIdx++) { // MAX_ID_GAP 50
					int b = sectorData[dataStart++] & 0xFF;
					if ((b >= 0xF8) && (b <= 0xFB)) {
						break;
					}
				}
				
				// we should now be at the real sector content start
				int size = 128 << sizeCode;
				logf("    sign 0x%02X  cyl %d  head %d  sect %d  sizeCode %d  size %d :\n",
						signature, cyl, head, sect, sizeCode, size);
//				logf("    ");
//				for (int idx = 0; idx < 32; idx++) {
//					logf(" %02X", sectorData[dataStart + idx]);
//				}
//				logf(" ...\n");
				
				// get the sector content
				short[] sectContent = new short[size / 2];
				for (int idx = 0; idx < size; idx += 2) {
					int b1 = sectorData[dataStart + idx] & 0xFF;
					int b2 = sectorData[dataStart + idx + 1] & 0xFF;
					int w = (b1 << 8) | b2;
					sectContent[idx / 2] = (short)w;
				}
				this.sectors.add(sectContent); // TODO: make sure the position is correct should the sector sequence not be ascendent
				
				// save the legacy metadata if necessary
				if (i == 0) {
					if (trackNo == 0) {        // special: cyl 0, head 0
						this.cyl0Sectors = sectCount;
						this.track0WordsPerSector = sectContent.length;
					} else if (trackNo == 1) { // special: cyl 0, head 1
						this.track1WordsPerSector = sectContent.length;
					} else if (trackNo == 2) { // data:    any track in cyl 1 .. ?
						this.dataSectors = sectCount;
						this.dataWordsPerSector = sectContent.length;
					}
				}
			}
			
			logf("-- end track %d\n", trackNo);
		}		
		
		private void loadDMK(File f) throws IOException {
			if (!f.exists()) {
				logf("file not found\n");
				throw new IOException("Floppy file '" + f.getName() + "' not found");
			}
			
			logf("** start loading DMK floppy: %s\n", f.getName());
			
			try (FileInputStream fis = new FileInputStream(f);
				 BufferedInputStream bis = new BufferedInputStream(fis)) {
				
				int roFlag = this.readByte(bis);
				int trackCount = this.readByte(bis);
				int trackLength = this.readWord(bis);
				int diskOptions = this.readByte(bis);
				
				// skip 7 bytes (5 bytes read so far)
				for (int i = 0; i < 7; i++) { this.readByte(bis); }
				
				int native1 = this.readWord(bis);
				int native2 = this.readWord(bis);
				
				int realTrackLen = trackLength - 0x80;
				
				// up to here we have read 16 bytes
				
				logf("DMK: r/o: 0x%02X , trackCount = %d , trackLength = %d (real: %d) , options = 0x%02X , native = 0x%04X%04X\n",
						roFlag, trackCount, trackLength, realTrackLen, diskOptions, native2, native1);
				
				for (int i = 0; i < (trackCount * 2); i++) {
					this.readTrack(i, bis, trackLength);
					if ((i % 2) == 0) { this.cylinders++; }
				}
			}
			
			logf("** DMK characteristics: cyl0Sectors          = %d\n", cyl0Sectors);
			logf("** DMK characteristics: track0WordsPerSector = %d\n", track0WordsPerSector);
			logf("** DMK characteristics: track1WordsPerSector = %d\n", track1WordsPerSector);
			logf("** DMK characteristics: dataSectors          = %d\n", dataSectors);
			logf("** DMK characteristics: dataWordsPerSector   = %d\n", dataWordsPerSector);
			logf("** DMK characteristics: cylinders            = %d\n", cylinders);
			logf("\n");
		}
	}
	
	public FloppyAgent(int fcbAddress) {
		super(AgentDevice.floppyAgent, fcbAddress, FCB_SIZE);
		this.enableLogging(Config.AGENTS_LOG_FLOPPY);
	}
	
	@Override
	public void shutdown(StringBuilder errMsgTarget) {
		// save current floppy if present
		if (this.currFloppy != null) {
			this.currFloppy.saveFloppy(errMsgTarget);
		}
	}
	
	@Override
	public synchronized void refreshMesaMemory() {
		if (this.nextFloppy != null || this.nextEjected) {
			// save current floppy if present
			if (this.currFloppy != null) {
				this.currFloppy.saveFloppy(null);
			}
			
			// load the floppy
			this.currFloppy = this.nextFloppy;
			this.floppyChanged = (this.currFloppy != null);
			
			// reset the "next" data
			this.nextFloppy = null;
			this.nextEjected = false;
		}
	}
	
	private FloppyDisk nextFloppy = null;
	private boolean nextEjected = false;
	
	private FloppyDisk currFloppy = null;
	private boolean floppyChanged = false;
	
	public boolean insertFloppy(File f, boolean readonly) throws IOException {
		
		String fname = f.getName();
		String[] fnameParts = fname.split("\\.");
		boolean isImd = (fnameParts.length > 1) && ("imd".equalsIgnoreCase(fnameParts[fnameParts.length - 1]));
		boolean isDmk = (fnameParts.length > 1) && ("dmk".equalsIgnoreCase(fnameParts[fnameParts.length - 1]));
		
		if (isImd) {
			this.nextFloppy = new IMDFloppyDisk(f);
		} else if (isDmk) {
			this.nextFloppy = new DMKFloppyDisk(f);
		} else {
			this.nextFloppy = new FloppyDisk3dot5(f, readonly);
		}
		this.nextEjected = false;
		return this.nextFloppy.isReadonly();
	}
	
	public void ejectFloppy() {
		this.nextFloppy = null;
		this.nextEjected = true;
	}
	
	private int reads = 0;
	private int writes = 0;
	
	public int getReads() { return this.reads; }
	
	public int getWrites() { return this.writes; }

	@Override
	public void call() {
		// process the stopAgent-flag 
		boolean stopAgent = (this.getFcbWord(fcb_w_stopAgent) != PrincOpsDefs.FALSE);
		logf("call() - stopAgent = %s\n", (stopAgent) ? "true" : "false");
		if (stopAgent) {
			logf("call() - stop agent\n");
			this.setFcbWord(fcb_w_agentStopped, PrincOpsDefs.TRUE);
			return;
		}
		this.setFcbWord(fcb_w_agentStopped, PrincOpsDefs.FALSE);

		// get the (first) io control block to process
		int iocb = this.getFcbDblWord(fcb_lp_nextIOCB);
		if (iocb == 0) {
			logf("call() - iocb == 0 (done, no iocb)\n");
			return; // no IOCB, nothing to be done,,,
		}

		// get the interrupt to use when i/o is complete
		short interruptSelector = this.getFcbWord(fcb_w_interruptSelector);
		logf("call() - interruptSelector = 0x%04X\n", interruptSelector & 0xFFFF);
		
		// process all io control blocks passed
		while(iocb != 0) {
			// fetch the IOCB data
			int deviceIndex = Mem.readWord(iocb + iocb_w_oper_device) & 0xFFFF;
			int function = Mem.readWord(iocb + iocb_w_oper_function) & 0xFFFF;
			int cylinder = Mem.readWord(iocb + iocb_w_oper_address_cylinder) & 0xFFFF;
			int sectorHead = Mem.readWord(iocb + iocb_w_oper_adddress_sectorHead) & 0xFFFF;
			int head = sectorHead >>> 8;
			int sector = sectorHead & 0xFF;
			int dataPtr = Mem.readDblWord(iocb + iocb_lp_oper_dataPtr);
			int incrementDataPtrAndRetries = Mem.readWord(iocb + iocb_w_oper_incrementDataPtrAndRetries) & 0xFFFF;
			boolean incrementDataPtr = (incrementDataPtrAndRetries & incrementDataPtrFlag) != 0;
			int retries = incrementDataPtrAndRetries & retriesMask;
			int count = Mem.readWord(iocb + iocb_w_oper_count) & 0xFFFF;
			int density = Mem.readWord(iocb + iocb_w_density) & 0xFFFF;
			int sectorLength = Mem.readWord(iocb + iocb_w_sectorLength) & 0xFFFF;
			int sectorsPerTrack = Mem.readWord(iocb + iocb_w_sectorsPertrack) & 0xFFFF;
			short status = Mem.readWord(iocb + iocb_w_status);
			
			// log the next operation to perform
			String functionName = (function < OP_NAMES.length) ? OP_NAMES[function] : "*invalid*";
			logf(
				"  iocb = 0x%08X : function = %d (%s)\n" + 
				"       -> chs = ( %d , %d , %d ) , dataPtr = 0x%08X, incrDataPtr = %s\n" + 
				"       -> retries = %d , count = %d , density = %d , sectorLength = %d , sectorsPerTrack = %d\n",
				iocb, function, functionName,
				cylinder, head, sector, dataPtr, (incrementDataPtr) ? "true" : "false",
				retries, count, density, sectorLength, sectorsPerTrack
				);
			
			// check if the only floppy drive is accessed and a floppy is inserted
			if (deviceIndex != 0) {
				Mem.writeWord(iocb + iocb_w_status, Status_otherError);
				iocb = Mem.readDblWord(iocb + iocb_lp_nextIocb);
				continue;
			}
			if (this.currFloppy == null) {
				Mem.writeWord(iocb + iocb_w_status, Status_notReady);
				iocb = Mem.readDblWord(iocb + iocb_lp_nextIocb);
				continue;
			}
			
			// do the operation requested in this iocb
			switch(function) {
			
			case Op_nop: {
					status = Status_goodCompletion;
				}
				break;
				
			case Op_readSector: {
					// check the specified sector address
					if (this.currFloppy.isInvalidCHS(cylinder, head, sector)) { 
						status = Status_invalidCHS;
						break;
					}
					
					// initialize and process the number of requested sectors
					int linearSector = this.currFloppy.getLinearSector(cylinder, head, sector);
					int memAddress = dataPtr;
					status = Status_goodCompletion;
					while (count > 0 && linearSector < FLOPPY_TOTAL_SECTORS) {
						// do it
						status = this.currFloppy.readSector(linearSector, memAddress, sectorLength);
						if (status != Status_goodCompletion) { break; }
						
						// decrement the sector count and inform Pilot
						count--;
						Mem.writeWord(iocb + iocb_w_oper_count, (short)(count & 0xFFFF));
						
						// go to the next sector and inform Pilot
						linearSector++;
						this.currFloppy.storeSectorNoIntoIocb(linearSector, iocb);
						
						// go to next memory target block and inform Pilot (if requested) 
						if (incrementDataPtr) {
							memAddress += sectorLength;
							Mem.writeDblWord(iocb + iocb_lp_oper_dataPtr, memAddress);
						}
						
						// update the stats
						this.reads++;
					}
				}
				break;
				
			case Op_writeSector: {
					// write a different disk without reading it is a problem 
					if (this.floppyChanged) {
						status = Status_diskChange;
						break;
					}
					// check the specified sector address
					if (this.currFloppy.isInvalidCHS(cylinder, head, sector)) { 
						status = Status_invalidCHS;
						break;
					}
					
					// initialize and process the number of requested sectors
					int linearSector = this.currFloppy.getLinearSector(cylinder, head, sector);
					int memAddress = dataPtr;
					status = Status_goodCompletion;
					while (count > 0 && linearSector < FLOPPY_TOTAL_SECTORS) {
						// do it
						status = this.currFloppy.writeSector(linearSector, memAddress, sectorLength);
						if (status != Status_goodCompletion) { break; }
						
						
						// decrement the sector count and inform Pilot
						count--;
						Mem.writeWord(iocb + iocb_w_oper_count, (short)(count & 0xFFFF));
						
						// go to the next sector and inform Pilot
						linearSector++;
						this.currFloppy.storeSectorNoIntoIocb(linearSector, iocb);
						
						// go to next memory source block and inform Pilot (if requested) 
						Mem.writeWord(iocb + iocb_w_oper_count, (short)(count & 0xFFFF));
						if (incrementDataPtr) {
							memAddress += sectorLength;
							Mem.writeDblWord(iocb + iocb_lp_oper_dataPtr, memAddress);
						}
						
						// update the stats
						this.writes++;
					}
				}
				break;
				
			case Op_writeDeletedSector: {
					// write a different disk without reading it is a problem 
					if (this.floppyChanged) {
						status = Status_diskChange;
						break;
					}
					// check the specified sector address
					if (this.currFloppy.isInvalidCHS(cylinder, head, sector)) { 
						status = Status_invalidCHS;
						break;
					}
					
					// initialize and process the number of requested sectors
					int linearSector = this.currFloppy.getLinearSector(cylinder, head, sector);
					status = Status_goodCompletion;
					while (count > 0 && linearSector < FLOPPY_TOTAL_SECTORS) {
						// do it
						status = this.currFloppy.writeDeletedSector(linearSector);
						if (status != Status_goodCompletion) { break; }
						
						// decrement the sector count and inform Pilot
						count--;
						Mem.writeWord(iocb + iocb_w_oper_count, (short)(count & 0xFFFF));
						
						// go to the next sector and inform Pilot
						linearSector++;
						this.currFloppy.storeSectorNoIntoIocb(linearSector, iocb);
						
						// do the stats
						this.writes++;
					}
				}
				break;
				
			case Op_readId: {
					status = Status_invalidOperation;
				}
				break;
				
			case Op_formatTrack: {
					// write a different disk without reading it is a problem 
					if (this.floppyChanged) {
						status = Status_diskChange;
						break;
					}
					// check the specified sector address
					if (this.currFloppy.isInvalidCHS(cylinder, head, sector)) { 
						status = Status_invalidCHS;
						break;
					}
					
					// format the requested number of tracks
					while (count > 0) {
						// do it
						status = this.currFloppy.formatTrack(cylinder, head);
						if (status != Status_goodCompletion) { break; }
						
						// decrement the track count and inform Pilot
						count--;
						Mem.writeWord(iocb + iocb_w_oper_count, (short)(count & 0xFFFF));
						
						// switch to the next track
						head++;
						if (head >= FLOPPY_HEADS) {
							head = 0;
							cylinder++;
							if (cylinder > FLOPPY_CYLS) { break; }
						}
						
						// do the stats
						this.writes++;
					}
				}
				break;
				
			default:
				status = Status_notReady;
			}
			
			logf("  iocb = 0x%08X  => status = %d\n", iocb, status);
			
			// put the status of this io control block
			Mem.writeWord(iocb + iocb_w_status, status);
			
			// go to next IOCB
			iocb = Mem.readDblWord(iocb + iocb_lp_nextIocb);
		}
		
		// Pilot was already informed that the disk changed, so it is already used on the next operation
		this.floppyChanged = false;
		
		logf("call() - done\n");
		
		// unconditionally enqueue an interrupt `
		Processes.requestMesaInterrupt(interruptSelector);
	}

	@Override
	protected void initializeFcb() {
		this.setFcbDblWord(fcb_lp_nextIOCB, 0);
		this.setFcbWord(fcb_w_interruptSelector, 0);
		this.setFcbWord(fcb_w_stopAgent, PrincOpsDefs.FALSE);
		this.setFcbWord(fcb_w_agentStopped, PrincOpsDefs.TRUE);
		this.setFcbWord(fcb_w_numberOfDCBs, 1);
		
		this.setFcbWord(dcb_w_deviceType, PilotDefs.Device_microFloppy);
		this.setFcbWord(dcb_w_numberOfHeads, FLOPPY_HEADS);
		this.setFcbWord(dcb_w_sectorsPerTrack, FLOPPY_SECTORS);
		this.setFcbWord(dcb_w_numberOfCylinders, FLOPPY_CYLS);
		this.setFcbWord(dcb_w_ready, PrincOpsDefs.FALSE);
		this.setFcbWord(dcb_w_diskChanged, PrincOpsDefs.TRUE);
		this.setFcbWord(dcb_w_twoSided, PrincOpsDefs.TRUE);
		this.setFcbWord(dcb_w_suggestedTries, 1);
	}

}
