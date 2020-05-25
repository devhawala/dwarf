/*
Copyright (c) 2020, Dr. Hans-Walter Latz
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

package dev.hawala.dmachine.dwarf;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import dev.hawala.dmachine.engine.PrincOpsDefs;

/**
 * Java swing pane representing the screen of a Dwarf machine, providing
 * a 8-bit lookup table color display.
 * <p>
 * The basic functionality for the Dwarf UI is inherited from the parent
 * class {@code DisplayPane}.
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2020)
 */
public class Display8BitColorPane extends DisplayPane {

	private static final long serialVersionUID = 1679737299827954144L;

	public Display8BitColorPane(int displayWidth, int displayHeight) {
		super(displayWidth, displayHeight);
	}

	@Override
	protected BufferedImage createBackingImage(int displayWidth, int displayHeight) {
		return new BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_INT_RGB);
	}

	@Override
	public boolean copyDisplayContent(short[] mem, int start, int count, short[] pageFlags, int firstPage, int[] colorTable) {
		DataBufferInt dbb = (DataBufferInt)bi.getRaster().getDataBuffer();
		int[] pixels = dbb.getData();
		
		boolean bitmapWasUpdated = false;
		int end = start + Math.min(Math.min(count, pixels.length / 2), mem.length - start);
		int bitmapIdx = 0;
		int memIdx = start;
		int pageIdx = firstPage;
		int pageCount = (count + PrincOpsDefs.WORDS_PER_PAGE - 1) / PrincOpsDefs.WORDS_PER_PAGE;
		while (pageCount-- > 0) {
			short flags = pageFlags[pageIdx++];
			if ((flags & PrincOpsDefs.MAPFLAGS_DIRTY) == 0) {
				memIdx += PrincOpsDefs.WORDS_PER_PAGE;
				bitmapIdx += PrincOpsDefs.WORDS_PER_PAGE * 2; // 2 pixels per mesa word in display data
				continue;
			}
			for (int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE && memIdx < end; i++) {
				int w = mem[memIdx++] & 0xFFFF;
				pixels[bitmapIdx++] = colorTable[w >>> 8];
				pixels[bitmapIdx++] = colorTable[w & 0x00FF];
			}
			bitmapWasUpdated = true;
		}
		
		return bitmapWasUpdated;
	}

}