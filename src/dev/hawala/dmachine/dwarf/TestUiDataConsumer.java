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

import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.eLevelVKey;
import dev.hawala.dmachine.engine.iMesaMachineDataAccessor;
import dev.hawala.dmachine.engine.iUiDataConsumer;

/**
 * Mesa UI data consumer for test purposes, allowing to test
 * the user interface without running a mesa engine.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class TestUiDataConsumer implements iUiDataConsumer, Runnable {
	
	private static final int displayMemStart = 1024; // simulate 4 pages offset for display mem
	
	final int displayWidth;
	final int displayHeight;
	final short[] displayMem;
	final short[] memFlags;
	final int firstDisplayPage;
	
	final Thread myself;
	
	public TestUiDataConsumer(int displayWidth, int displayHeight) {
		this.displayWidth = displayWidth;
		this.displayHeight = displayHeight;
		int wordCount = displayMemStart + ( (displayWidth * displayHeight + 15) / 16);
		this.displayMem = new short[wordCount];
		this.memFlags = new short[(wordCount + 255) / 256];
		this.firstDisplayPage = displayMemStart / 256;
		
		this.myself = new Thread(this);
		this.myself.start();
	}
	
	private PointerBitmapAcceptor pointerBitmapAcceptor = null;
	
	private iMesaMachineDataAccessor displayRefresher = null;

	@Override
	public void acceptKeyboardKey(eLevelVKey key, boolean isPressed) {
		System.out.printf("=> xerox key %s %s\n", key.toString(), (isPressed) ? "pressed" : "released");
	}
	
	@Override
	public void resetKeys() {
		System.out.printf("=> reset keys\n");
	}

	@Override
	public void acceptMouseKey(int key, boolean isPressed) {
		System.out.printf("=> mouse key %d %s\n", key, (isPressed) ? "pressed" : "released");
		if (key == 1) {
			this.acceptKeyboardKey(eLevelVKey.Point, isPressed);
		} else if (key == 2) {
			this.acceptKeyboardKey(eLevelVKey.Menu, isPressed);
		} else if (key == 3) {
			this.acceptKeyboardKey(eLevelVKey.Adjust, isPressed);
		} else {
			System.out.println("   ... unknown mouse key!");
		}
	}

	@Override
	public void acceptMousePosition(int x, int y) {
		System.out.printf("=> mouse moved to x = %4d , y = %4d\n", x, y);
	}

	@Override
	public void registerPointerBitmapAcceptor(PointerBitmapAcceptor acpt) {
		synchronized(this) {
			this.pointerBitmapAcceptor = acpt;
		}
		System.out.printf("** registerPointerBitmapAcceptor() => %s\n", acpt.toString());
	}

	@Override
	public void registerUiDataRefresher(iMesaMachineDataAccessor refresher) {
		synchronized(this) {
			this.displayRefresher = refresher;
		}
	}
	
	private final short[] DisplayFillTemplate = {
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
	
	private void refillDisplay(int patternStart) {
		int displayWord = displayMemStart;
		int wordsPerLine = displayWidth / PrincOpsDefs.WORD_BITS;
		int ti = patternStart % DisplayFillTemplate.length;
		for (int i = 0; i < displayHeight; i++) {
			for (int j = 0; j < wordsPerLine; j++) {
				this.displayMem[displayWord++] = DisplayFillTemplate[ti];
			}
			ti++;
			if (ti >= DisplayFillTemplate.length) { ti = 0; }
		}
	}
	
	private final short[][] CursorBitmaps = {
		
		// first cursor
		{
			(short)0b0000001001000000,
			(short)0b0000001001000000,
			(short)0b0000001001000000,
			(short)0b0000001001000000,
			(short)0b0000001001000000,
			(short)0b0000001001000000,
			(short)0b1111111001111111,
			(short)0b0000000110000000,
			(short)0b0000000110000000,
			(short)0b1111111001111111,
			(short)0b0000001001000000,
			(short)0b0000001001000000,
			(short)0b0000001001000000,
			(short)0b0000001001000000,
			(short)0b0000001001000000,
			(short)0b0000001001000000
		},
		
		// second cursor
		{
			(short)0b0000000000000000,
			(short)0b0000000000000000,
			(short)0b0000111111110000,
			(short)0b0001000000001000,
			(short)0b0010000000000100,
			(short)0b0010000000000100,
			(short)0b0010000000000100,
			(short)0b0010000110000100,
			(short)0b0010000110000100,
			(short)0b0010000000000100,
			(short)0b0010000000000100,
			(short)0b0010000000000100,
			(short)0b0001000000001000,
			(short)0b0000111111110000,
			(short)0b0000000000000000,
			(short)0b0000000000000000
		},
		
		// third cursor
		{
			(short)0b0111111111111110,
			(short)0b0100000000000010,
			(short)0b0010000000000100,
			(short)0b0001000000001000,
			(short)0b0000111000110000,
			(short)0b0000011111100000,
			(short)0b0000001001000000,
			(short)0b0000000110000000,
			(short)0b0000000110000000,
			(short)0b0000001001000000,
			(short)0b0000010000100000,
			(short)0b0000100000010000,
			(short)0b0001000100001000,
			(short)0b0010011111000100,
			(short)0b0111111111111110,
			(short)0b0111111111111110
		},
		
		// fourth cursor
		{
			(short)0b0000000100000000,
			(short)0b0000001110000000,
			(short)0b0000011111000000,
			(short)0b0000111111100000,
			(short)0b0001111111110000,
			(short)0b0011111111111000,
			(short)0b0000001110000000,
			(short)0b0000001110000000,
			(short)0b0000001110000000,
			(short)0b0000001110000000,
			(short)0b0000001110000000,
			(short)0b0000001110000000,
			(short)0b0000001110000000,
			(short)0b0000001110000000,
			(short)0b0000001110000000,
			(short)0b0000000000000000
		}
	};

	@Override
	public void run() {
		System.out.println("TestUiDataConsumer thread started.");
		
		long wakeups = 0;
		int displayTemplateStart = 0;
		int mouseCursorIndex = 0;
		int currentMP = 0;
		
		int[] colorTable = { 0x00000000 , 0x00FFFFFF };
		
		boolean firstRefresh = true;
		
		while(true) {
			// wait ~ 40 ms => ~ 25 refreshs per second
			try {
				Thread.sleep(39);
			} catch (InterruptedException e) {
				return; // done if interrupted
			}
			wakeups++;
			
			int newMP = (900 + mouseCursorIndex) % 10000;
			
			synchronized(this) {
				if (this.displayRefresher != null) {
					int endDirty = (firstRefresh) ? this.memFlags.length : this.memFlags.length / 2;
					for (int i = 0; i < endDirty; i++) { this.memFlags[i] = (short)0x0006; } // read + written
					firstRefresh = false;
					this.refillDisplay(displayTemplateStart++);
					this.displayRefresher.accessRealMemory(
							this.displayMem,
							displayMemStart,
							this.displayMem.length - displayMemStart,
							this.memFlags,
							this.firstDisplayPage,
							colorTable);
					
					if (newMP != currentMP) {
						currentMP = newMP;
						this.displayRefresher.acceptMP(currentMP);
					}
					
					this.displayRefresher.acceptStatistics(
							wakeups,              // instruction count
							(int)(wakeups / 7),   // disk reads
							(int)(wakeups / 17),  // disk writes
							0,                    // floppy reads
							0,                    // floppy writes
							(int)(wakeups / 47),  // network received packets
							(int)(wakeups / 31)); // network sent packets
//					this.displayRefresher = null;
				}
				
				if ((wakeups % 50) == 0) {
					mouseCursorIndex++;
					if (this.pointerBitmapAcceptor != null) {
						this.pointerBitmapAcceptor.setPointerBitmap(CursorBitmaps[mouseCursorIndex % 4], 0, 0);
					}
				}
			}
		}
	}

}