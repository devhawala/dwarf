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

package dev.hawala.dmachine.dwarf;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;

import dev.hawala.dmachine.engine.PrincOpsDefs;

/**
 * Java swing pane representing the screen of a Dwarf machine, providing
 * a Black&amp;White display of configurable size including a 16x16 pixel mouse
 * pointer with modifiable shape.
 * <p>
 * The pane takes a double buffering approach for the display bitmap, using
 * a {@code BufferedImage} as backing store for the currenty bitmap onscreen.
 * This bitmap is updated asynchronously from the display memory in the mesa
 * address space.
 * </p>
 * <p>
 * The pane also provides access to the mouse pointer shape displayed when
 * the system cursor is in the panes area. All cursors created through the
 * {@code setCursor()} method are cached, so resource usage can be reduced by
 * re-using already defined cursor shapes.
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class DisplayBwPane extends JComponent {
	
	private static final long serialVersionUID = 8238134602921644866L;
	
	// buffer image as backing store for the bitmap currently display and
	// for transfer from mesa memory to java display
	private final BufferedImage bi;
	
	// custom cursor construction support
	private final BufferedImage cursorBits;
	private final int cursorBitsSkipPerLine;
	private final Toolkit tk;
	
	// cached cursors
	private final List<CachedCursor> cachedCursors = new ArrayList<>();

	/**
	 * Create the panel of the given size.
	 * 
	 * @param displayWidth pixel width of the mesa display.
	 * @param displayHeight pixel height of the mesa display. 
	 */
	public DisplayBwPane(int displayWidth, int displayHeight) {
		// create the bitmap backing store
		this.bi = new BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_BYTE_BINARY);;
		
		// prevent the tab key to swallowed (more precisely used by focus management)
		// so we tab key (de)pressed event can be passed to the mesa machine
		this.setFocusTraversalKeysEnabled(false);
		
		// get the environments cursor geometry characteristics, assuming the cursor square
		// is multiples of 16 and create the image buffer for creating new cursors later.
		this.tk = Toolkit.getDefaultToolkit();
		Dimension cursorDims = this.tk.getBestCursorSize(16, 16);
		double cursorWidth = cursorDims.getWidth();
		// System.out.printf("cursorDims: w = %f , h = %f\n", cursorWidth, cursorDims.getHeight());
		if (cursorWidth > 63.0d) {
			this.cursorBits = new BufferedImage(64, 64, BufferedImage.TYPE_4BYTE_ABGR);
			this.cursorBitsSkipPerLine = 48;
			// System.out.println("+++ created cursorBits as: BufferedImage.TYPE_4BYTE_ABGR - 64 x 64");
		} else if (cursorWidth > 47.0d) {
			this.cursorBits = new BufferedImage(48, 48, BufferedImage.TYPE_4BYTE_ABGR);
			this.cursorBitsSkipPerLine = 32;
			// System.out.println("+++ created cursorBits as: BufferedImage.TYPE_4BYTE_ABGR - 48 x 48");
		} else if (cursorWidth > 31.0d) {
			this.cursorBits = new BufferedImage(32, 32, BufferedImage.TYPE_4BYTE_ABGR);
			this.cursorBitsSkipPerLine = 16;
			// System.out.println("+++ created cursorBits as: BufferedImage.TYPE_4BYTE_ABGR - 32 x 32");
		} else {
			this.cursorBits = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);
			this.cursorBitsSkipPerLine = 0;
			// System.out.println("+++ created cursorBits as: BufferedImage.TYPE_4BYTE_ABGR - 16 x 16");
		}
	}
	
	/**
	 * Get the backing store of the display.
	 * @return the displays backing store.
	 */
	public BufferedImage getBufferedImage() {
		return this.bi;
	}
	
	@Override
	public void paint(Graphics g) {
		g.drawImage(bi, 0, 0, bi.getWidth(), bi.getHeight(), null);
	}
	
	// cursor cache class
	// a single cursor is identified by a hascode computed over the
	// cursor bits and the hotspot position
	private static class CachedCursor {
		
		// the characteristics of the mesa cursor
		private final short[] cursor;
		private final int hotspotX;
		private final int hotspotY;
		private final int hashcode; 
		
		// the Java Swing allocated cursor
		private final Cursor uiCursor;
		
		// constructor with all final member data
		public CachedCursor(short[] cursor, int hotspotX, int hotspotY, int hashcode, Cursor uiCursor) {
			this.cursor = cursor;
			this.hotspotX = hotspotX;
			this.hotspotY = hotspotY;
			this.hashcode = hashcode;
			this.uiCursor = uiCursor;
		}
		
		public int getHashcode() { return this.hashcode; }
		
		public Cursor getCursor() { return this.uiCursor; }

		public static int computeHashcode(short[] cursor, int hotspotX, int hotspotY) {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(cursor);
			result = prime * result + hotspotX;
			result = prime * result + hotspotY;
			return result;
		}

		public boolean is(short[] cursor, int hotspotX, int hotspotY) {
			if (!Arrays.equals(this.cursor, cursor)) { return false; }
			if (this.hotspotX != hotspotX) { return false; }
			if (this.hotspotY != hotspotY) { return false; }
			return true;
		}
	}
	
	/**
	 * Set a new cursor for the Dwarf screen, possibly re-using an already
	 * (previously) created cursor having the same display characteristics. 
	 * 
	 * @param cursor the 16x16 pixel shape of the cursor, thus the array should
	 *   have 16 elements.
	 * @param hotspotX the hotspot x coordinate, should be in the range 0..15
	 * @param hotspotY the hotspot y coordinate, should be in the range 0..15
	 */
	public void setCursor(short[] cursor, int hotspotX, int hotspotY) {
		// check if the cursor is already cached, if so re-use it
		int hashcode = CachedCursor.computeHashcode(cursor, hotspotX, hotspotY);
		for (CachedCursor cc : this.cachedCursors) {
			if (cc.getHashcode() == hashcode && cc.is(cursor, hotspotX, hotspotY)) {
				this.setCursor(cc.getCursor());
				return;
			}
		}
		
		// create the cursor instance and cache it before using it
		DataBufferByte dbb = (DataBufferByte)this.cursorBits.getRaster().getDataBuffer();
		byte[] cdata = dbb.getData();
		
		DataBufferByte alphaDdb = (DataBufferByte)this.cursorBits.getAlphaRaster().getDataBuffer();
		byte[] alpha = alphaDdb.getData();
		
		final byte zero = (byte)0;
		final byte one = (byte)255;
		
		int nibbleOffset = 0;
		for (int i = 0; i < Math.min(16,  cursor.length); i++) {
			int cursorLine = ~cursor[i];
			int bit = 0x8000;
			for (int j = 0; j < 16; j++) {
				if ((cursorLine & bit) != 0) {
					cdata[nibbleOffset++] = zero;
					cdata[nibbleOffset++] = one;
					cdata[nibbleOffset++] = one;
					alpha[nibbleOffset++] = one;
				} else {
					cdata[nibbleOffset++] = one;
					cdata[nibbleOffset++] = zero;
					cdata[nibbleOffset++] = zero;
					alpha[nibbleOffset++] = zero;
				}
				bit >>>= 1;
			}
			nibbleOffset += this.cursorBitsSkipPerLine * 4; // 4 array positions per bit
		}
		
		Cursor newCursor = tk.createCustomCursor(
				this.cursorBits,
				new Point(hotspotX, hotspotY),
				"?"
				);
		CachedCursor newCachedCursor = new CachedCursor(cursor, hotspotX, hotspotY, hashcode, newCursor);
		this.cachedCursors.add(newCachedCursor);
		
		this.setCursor(newCursor);
	}
	
	/**
	 * Copy modified pages from the real memory of the mesa engine into the bitmap
	 * backing store for this Dwarf display. 
	 * 
	 * @param mem the real memory of the mesa engine from where to copy the screen content
	 * @param start the start offset (address) for the mesa display memory in {@code mem}.
	 * @param count length of the mesa display memory in {@code mem}.
	 * @param pageFlags the virtual page map of the mesa engine allowing to check if a display
	 *   memory page was modified, allowing to copy only modified pages from mesa memory space.
	 * @param firstPage index of the first entry in {@code pageFlags} to use, corresponding
	 *   to the {@code start} index.
	 * @return {@code true} if the backing store of the display bitmap was modifiied, i.e. if
	 *   any of the pageFlags signaled that the mesa display was modified, thus a repaint of
	 *   the Java-UI should be initiated.
	 */
	public boolean copyDisplayContent(short[] mem, int start, int count, short[] pageFlags, int firstPage) {
		DataBufferByte dbb = (DataBufferByte)bi.getRaster().getDataBuffer();
		byte[] data = dbb.getData();
		
		boolean bitmapWasUpdated = false;
		int end = start + Math.min(Math.min(count, data.length / 2), mem.length - start);
		int bitmapIdx = 0;
		int memIdx = start;
		int pageIdx = firstPage;
		int pageCount = (count + PrincOpsDefs.WORDS_PER_PAGE - 1) / PrincOpsDefs.WORDS_PER_PAGE;
		while (pageCount-- > 0) {
			short flags = pageFlags[pageIdx++];
			if ((flags & PrincOpsDefs.MAPFLAGS_DIRTY) == 0) {
				memIdx += PrincOpsDefs.WORDS_PER_PAGE;
				bitmapIdx += PrincOpsDefs.WORDS_PER_PAGE * 2;
				continue;
			}
			for (int i = 0; i < PrincOpsDefs.WORDS_PER_PAGE && memIdx < end; i++) {
				short w = (short)(mem[memIdx++] ^ 0xFFFF); // TODO: really invert manually ??
				data[bitmapIdx++] = (byte)((w >>> 8));
				data[bitmapIdx++] = (byte)((w & 0x00FF));
			}
			bitmapWasUpdated = true;
		}
		
		return bitmapWasUpdated;
	}

}
