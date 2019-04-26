/*
Copyright (c) 2019, Dr. Hans-Walter Latz
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

package dev.hawala.dmachine;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import dev.hawala.dmachine.engine.agents.FloppyAgent.DMKFloppyDisk;

/**
 * Conversion utility for creating an equivalent IMD floppy
 * disk image for a DMK image, preferably (because tested with)
 * a 8010 8" disk.
 * <p>
 * The conversion reuses largely Dwarf floppy code: the DMK image is
 * loaded through the implementation for DMK legacy floppies, the
 * {@code DiskConverter} simply subclassing that class for writing
 * the IMD file (named after the original file with the ".imd" extension)
 * directly in the constructor. 
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2018)
 */
public class CnvDmk2Imd extends DMKFloppyDisk {
	
	private boolean doLog = false; // prevent logging the DMK reading
	
	public CnvDmk2Imd(File dmkFile, File imdFile) throws IOException {
		super(dmkFile);
		
		/*
		 * create the IMD from the DMK sectors and disk characteristics
		 */
		
		// re-enable logging for issuing the disk characteristics
		this.doLog = true;
		
		// write the imd file from the sectors found in the dmk file
		logf("# Cyl0Sectors = %s sectors\n", this.cyl0Sectors); // track 0 and 1
		logf("# DataSectors = %s sectors\n", this.dataSectors);
		logf("# cylinders   = %d\n", this.cylinders);
		
		int expectedSectorCount = ((this.cylinders - 1) * this.dataSectors * 2) + (this.cyl0Sectors * 2);
		logf("# expectedSectors = %d ?=? this.sectors.length = %d\n", expectedSectorCount, this.sectors.size());
		
		try (OutputStream imd = new BufferedOutputStream(new FileOutputStream(imdFile))) {
			// write the human readable header
			imd.write("IMD converted from DMK".getBytes());
			imd.write(0x1A); // EOF is end marker for the header
			
			// which sector loaded from the DMK to write next to IMD
			int sectorStartIndex = 0;
			
			// write track 0 (cyl 0, head 0): 128 bytes sector length (sectInfo = 0), mode = 2 (experimental)
			sectorStartIndex = this.writeTrack(imd, 2, 0, 0, this.cyl0Sectors, 0, sectorStartIndex);
			
			// write track 1 (cyl 0, head 1): 256 bytes per sector, mode = 5 from here (experimental)
			sectorStartIndex = this.writeTrack(imd, 5, 0, 1, this.cyl0Sectors, 1, sectorStartIndex);
			
			// write the other sectors
			int cyl = 1;
			int head = 0;
			while(cyl < this.cylinders) {
				sectorStartIndex = this.writeTrack(imd, 5, cyl, head, this.dataSectors, 2, sectorStartIndex);
				head++;
				if (head > 1) {
					head = 0;
					cyl++;
				}
			}
		}
	}
	
	private int writeTrack(OutputStream imd, int mode, int cylNo, int headNo, int numSects, int sectInfo, int sectorStartIndex) throws IOException {
		// track header
		imd.write(mode);
		imd.write(cylNo);
		imd.write(headNo & 0x3F);
		imd.write(numSects);
		imd.write(sectInfo);
		
		// sector numbering map (Xerox floppies have linear ascending sector ordering, as seens so far...)
		for (int i = 0; i < numSects; i++) {
			imd.write(i + 1);
		}
		
		// no sectorCylinder map present, as we write (headNo & 0x80) == 0
		
		// no sectorHead map present, as we write (headNo & 0x40) == 0
		
		// write sectors
		for (int i = 0; i < numSects; i++) {
			this.writeSectorContent(imd, this.sectors.get(sectorStartIndex + i));
		}
		
		// done with this track
		return sectorStartIndex + numSects;
	}
	
	private void writeSectorContent(OutputStream imd, short[] sector) throws IOException {
		// sector content type: full content, no filling with constant byte or 0
		imd.write(1);
		
		// sector bytes
		for (int i = 0; i < sector.length; i++) {
			int word = sector[i];
			int upperByte = (word >> 8) & 0x00FF;
			int lowerByte = word & 0x00FF;
			imd.write(upperByte);
			imd.write(lowerByte);
		}
	}
	
	protected void logf(String template, Object... args) {
		if (this.doLog) {
			System.out.printf(template, args);
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.printf("missing DMK filename parameter\n");
			return;
		}
		
		File dmkFile = new File(args[0]);
		File imdFile = new File(args[0] + ".imd");
		new CnvDmk2Imd(dmkFile, imdFile);

	}

}
