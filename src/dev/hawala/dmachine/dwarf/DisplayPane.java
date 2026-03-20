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

/**
 * Abstract Java swing pane representing the screen of a Dwarf machine, providing
 * a generic display of configurable size including a 16x16 pixel mouse
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
 * @author Dr. Hans-Walter Latz / Berlin (2017,2020)
 */
public abstract class DisplayPane extends JComponent {
	
	private static final long serialVersionUID = 4816229134273103459L;

	// buffer image as backing store for the bitmap currently displayed and
	// used for pseudo-regular transfer from mesa memory to java display
	protected final BufferedImage bi;
	
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
	public DisplayPane(int displayWidth, int displayHeight) {
		// create the bitmap backing store
		this.bi = this.createBackingImage(displayWidth, displayHeight);
		
		// prevent the tab key to swallowed (more precisely used by focus management)
		// so a tab key (de)pressed event can be passed to the mesa machine
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
	// a single cursor is identified by an hashcode computed over the
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
	
	// MP code cursor support: pixel data for digits 0-9 extracted from a 1-bit BMP.
	// The source bitmap is 70x10 pixels: 10 digit tiles each 7 pixels wide (5px digit
	// content + 1px padding on each side) and 10 pixels tall (8px content + 1px
	// padding top and bottom).  Rows are stored in visual top-to-bottom order (y=0..9),
	// each row encoded as 9 bytes (MSB = leftmost pixel).  A '0' bit is an active (black)
	// pixel; a '1' bit is background (transparent). Taken from the Dawn emulator (mpcode.bmp),
	// credits to Don Woodward, 2004.
	private static final byte[] MP_DIGIT_ROWS = {
		// y=0: top border (all background)
		(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFC,
		// y=1
		(byte)0xC7, (byte)0xDE, (byte)0x1C, (byte)0x3E, (byte)0x70, (byte)0x78, (byte)0xC1, (byte)0xC7, (byte)0x8C,
		// y=2
		(byte)0xBB, (byte)0x9F, (byte)0xEF, (byte)0xDE, (byte)0x77, (byte)0xF7, (byte)0xFD, (byte)0xBB, (byte)0x74,
		// y=3
		(byte)0xBB, (byte)0x5F, (byte)0xEF, (byte)0xDD, (byte)0x77, (byte)0xEF, (byte)0xFB, (byte)0xBB, (byte)0x74,
		// y=4
		(byte)0xBB, (byte)0xDF, (byte)0xEE, (byte)0x3D, (byte)0x70, (byte)0xE9, (byte)0xFB, (byte)0xC7, (byte)0x64,
		// y=5
		(byte)0xBB, (byte)0xDF, (byte)0xDF, (byte)0xDB, (byte)0x7F, (byte)0x66, (byte)0xF7, (byte)0xBB, (byte)0x94,
		// y=6
		(byte)0xBB, (byte)0xDF, (byte)0xBF, (byte)0xD8, (byte)0x3F, (byte)0x6E, (byte)0xF7, (byte)0xBB, (byte)0xF4,
		// y=7
		(byte)0xBB, (byte)0xDF, (byte)0x7F, (byte)0xDF, (byte)0x7F, (byte)0x6E, (byte)0xEF, (byte)0xBB, (byte)0xEC,
		// y=8
		(byte)0xC7, (byte)0xDE, (byte)0x0C, (byte)0x3F, (byte)0x70, (byte)0xF1, (byte)0xEF, (byte)0xC7, (byte)0x1C,
		// y=9: bottom border (all background)
		(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFC
	};

	/**
	 * Returns {@code true} if the pixel at tile-column {@code x} and tile-row {@code y}
	 * of the given digit tile (0-9) should be drawn as an opaque black pixel.
	 */
	private static boolean isMpDigitPixelActive(int digit, int x, int y) {
		int globalX   = digit * 7 + x;
		int byteIndex = y * 9 + globalX / 8;
		int bitShift  = 7 - (globalX % 8);
		return ((MP_DIGIT_ROWS[byteIndex] >> bitShift) & 1) == 0; // 0 = black = active
	}

	/**
	 * Set the cursor to display the given 4-digit MP code.  This is called whenever
	 * the MP code is different from 8000 (normal operation) so that the cursor shows
	 * the current machine-state code instead of the Mesa-defined mouse pointer shape.
	 * <p>
	 * The two upper digits are placed side-by-side starting at the top-left of the
	 * cursor image, and the two lower digits are placed slightly inset below them
	 * (matching the reference layout used by the 6085).  Any part of a digit tile
	 * that falls outside the cursor image boundary is silently clipped.
	 * </p>
	 *
	 * @param mp the current MP code (0-9999)
	 */
	public void setMpCodeCursor(int mp) {
		int n1 = mp / 1000;
		int n2 = (mp % 1000) / 100;
		int n3 = (mp % 100)  / 10;
		int n4 = mp % 10;

		int imgWidth  = this.cursorBits.getWidth();
		int imgHeight = this.cursorBits.getHeight();

		DataBufferByte dbb = (DataBufferByte)this.cursorBits.getRaster().getDataBuffer();
		byte[] cdata = dbb.getData();

		// Clear entire cursor image to transparent (A=0 for TYPE_4BYTE_ABGR)
		Arrays.fill(cdata, (byte)0);

		// Each digit tile is 7 wide x 10 tall, but rows 0 and 9 are blank padding.
		// When the cursor image height is < 20 pixels (e.g. 16x16) both rows of digits
		// won't fit with the full 10-row tiles.  In that case "compact" mode strips the
		// blank padding rows: upper content occupies py=0..7, lower content py=8..15,
		// filling the 16px cursor exactly with no overlap and no cutoff (no gap is
		// possible here; a 1px gap would require 17 rows).
		// For cursor images ≥ 20 px the full tiles are used; upper content is at py=1..8
		// and lower starts at lowerDestY=9 so that both tiles share the transparent row
		// at py=9, giving a 1px visible gap between the two content bands.
		// Digit tile positions (pixels), matching the reference Dawn layout:
		//   n1 at (0, 0)   n2 at (7, 0)
		//   n3 at (2, 8)   n4 at (9, 8)   (compact)
		//   n3 at (2, 9)   n4 at (9, 9)   (non-compact, 1px transparent gap between rows)
		boolean compact  = imgHeight < 20;
		int tileRowStart = compact ? 1 : 0;   // first tile row to render (skip top blank)
		int tileRowEnd   = compact ? 9 : 10;  // exclusive end (skip bottom blank)
		int lowerDestY   = compact ? 8 : 9;   // where the lower two digits start

		int[] digits = { n1, n2, n3, n4 };
		int[] destX  = {  0,  7,  2,  9 };
		int[] destY  = {  0,  0, lowerDestY, lowerDestY };

		for (int d = 0; d < 4; d++) {
			int digit = digits[d];
			int dx    = destX[d];
			int dy    = destY[d];
			for (int ty = tileRowStart; ty < tileRowEnd; ty++) {
				int py = dy + (ty - tileRowStart);
				if (py >= imgHeight) { continue; }
				for (int tx = 0; tx < 7; tx++) {
					int px = dx + tx;
					if (px >= imgWidth) { continue; }
					if (isMpDigitPixelActive(digit, tx, ty)) {
						// TYPE_4BYTE_ABGR byte layout per pixel: [A, B, G, R]
						// Opaque black: A=255, B=0, G=0, R=0 (B/G/R already 0 from fill)
						cdata[(py * imgWidth + px) * 4] = (byte)255;
					}
				}
			}
		}

		this.setCursor(this.tk.createCustomCursor(this.cursorBits, new Point(0, 0), "MP"));
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
	
	/*
	 * ******* abstract methods to be implemented by real display panes
	 */
	
	/**
	 * Create the {@code BufferedImage} of the given size backing the display for the
	 * color type of the specific instance (called once by the constructor).
	 * 
	 * @param displayWidth pixel width of the mesa display.
	 * @param displayHeight pixel height of the mesa display. 
	 */
	protected abstract BufferedImage createBackingImage(int displayWidth, int displayHeight);
	
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
	 * @param colorTable mapping of pixel values to color values as array of {@code 0x00rrggbb} color values
	 * @return {@code true} if the backing store of the display bitmap was modifiied, i.e. if
	 *   any of the pageFlags signaled that the mesa display was modified, thus a repaint of
	 *   the Java-UI should be initiated.
	 */
	public abstract boolean copyDisplayContent(short[] mem, int start, int count, short[] pageFlags, int firstPage, int[] colorTable) ;

}