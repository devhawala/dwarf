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

import java.util.HashMap;
import java.util.Map;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.Opcodes.OpImpl;
import dev.hawala.dmachine.engine.PilotDefs.DisplayType;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.Processes;

/**
 * Implementation of instructions defined in PrincOps 4.0
 * in chapter: 8 Block Transfers
 * <br>
 * Also here: the (undocumented) successors/extensions to BITBLT:
 * <ul>
 * <li> BITBLTX (apparently not used by Pilot 15.x based XDE or GVWIN)</li>
 * <li>COLORBLT</li>
 * </ul>
 * <p>
 * The instruction implementations deviate from the PrincOps specification
 * by not pushing back the current state on the stack after each processed
 * unit. Instead the instruction state:
 * </p>
 * <ul>
 * <li>is pushed if an interrupt is pending</li>
 * <li>is added to the already saved state by the page fault (feature of the MesaAbort exception).</li>
 * </ul> 
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch08_Block_Transfers {
	
	/*
	 * 8.1 Word Boundary Block Transfers
	 */

	// BLT - Block Transfer
	public static final OpImpl OPC_xF3_BLT = new OpImpl() { // flaw in Java 8 spec or bug in Eclipse-Java8-Compiler?
		public void execute() {
			int dest = Cpu.pop() & 0xFFFF;
			int count = Cpu.pop() & 0xFFFF;
			int source = Cpu.pop() & 0xFFFF;
			
			try {
				while(true) {
					if (count == 0) { return; }
					Mem.writeMDSWord(dest, Mem.readMDSWord(source)); // this may throw a MesaAbort on page fault
					count--;
					source++;
					dest++;
					if (Processes.interruptPending() && count > 0) {
						Cpu.push((short)source);
						Cpu.push((short)count);
						Cpu.push((short)dest);
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			} catch(Cpu.MesaAbort e) {
				e.beginUpdateStack();
				Cpu.push((short)source);
				Cpu.push((short)count);
				Cpu.push((short)dest);
				throw e.updateStack();
			}
		}
	};
	
	// BLTL - Block Transfer Long
	public static final OpImpl OPC_xF4_BLTL = new OpImpl() { // flaw in Java 8 spec or bug in Eclipse-Java8-Compiler?
		public void execute() {
			int dest = Cpu.popLong();
			int count = Cpu.pop() & 0xFFFF;
			int source = Cpu.popLong();
			
			try {
				while(true) {
					if (count == 0) { return; }
					Mem.writeWord(dest, Mem.readWord(source)); // this may throw a MesaAbort on page fault
					count--;
					source++;
					dest++;
					if (Processes.interruptPending() && count > 0) {
						Cpu.pushLong(source);
						Cpu.push((short)count);
						Cpu.pushLong(dest);
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			} catch(Cpu.MesaAbort e) {
				e.beginUpdateStack();
				Cpu.pushLong(source);
				Cpu.push((short)count);
				Cpu.pushLong(dest);
				throw e.updateStack();
			}
		}
	};

	// BLTLR - Block Transfer Long Reversed
	// (corrected according to "PrincOps-Corrections")
	public static final OpImpl ESC_x27_BLTLR = new OpImpl() { // flaw in Java 8 spec or bug in Eclipse-Java8-Compiler?
		public void execute() {
			int dest = Cpu.popLong();
			int count = Cpu.pop() & 0xFFFF;
			int source = Cpu.popLong();
					
			try {
				while(true) {
					if (count == 0) { return; }
					Mem.writeWord(dest + count - 1, Mem.readWord(source + count - 1)); // this may throw a MesaAbort on page fault
					count--;
					if (Processes.interruptPending() && count > 0) {
						Cpu.pushLong(source);
						Cpu.push((short)count);
						Cpu.pushLong(dest);
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			} catch(Cpu.MesaAbort e) {
				e.beginUpdateStack();
				Cpu.pushLong(source);
				Cpu.push((short)count);
				Cpu.pushLong(dest);
				throw e.updateStack();
			}
		}
	};

	// BLTC - Block Transfer Code
	public static final OpImpl OPC_xF5_BLTC = new OpImpl() { // flaw in Java 8 spec or bug in Eclipse-Java8-Compiler?
		public void execute() {
			int dest = Cpu.pop() & 0xFFFF;
			int count = Cpu.pop() & 0xFFFF;
			int source = Cpu.pop() & 0xFFFF;
			
			try {
				while(true) {
					if (count == 0) { return; }
					Mem.writeMDSWord(dest, Mem.readCode(source)); // this may throw a MesaAbort on page fault
					count--;
					source++;
					dest++;
					if (Processes.interruptPending() && count > 0) {
						Cpu.push((short)source);
						Cpu.push((short)count);
						Cpu.push((short)dest);
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			} catch(Cpu.MesaAbort e) {
				e.beginUpdateStack();
				Cpu.push((short)source);
				Cpu.push((short)count);
				Cpu.push((short)dest);
				throw e.updateStack();
			}
		}
	};

	// BLTCL - Block Transfer Code Long
	public static final OpImpl OPC_xF6_BLTCL = new OpImpl() { // flaw in Java 8 spec or bug in Eclipse-Java8-Compiler?
		public void execute() {
			int dest = Cpu.popLong();
			int count = Cpu.pop() & 0xFFFF;
			int source = Cpu.pop() & 0xFFFF;
			
			try {
				while(true) {
					if (count == 0) { return; }
					Mem.writeWord(dest, Mem.readCode(source)); // this may throw a MesaAbort on page fault
					count--;
					source++;
					dest++;
					if (Processes.interruptPending() && count > 0) {
						Cpu.push((short)source);
						Cpu.push((short)count);
						Cpu.pushLong(dest);
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			} catch(Cpu.MesaAbort e) {
				e.beginUpdateStack();
				Cpu.push((short)source);
				Cpu.push((short)count);
				Cpu.pushLong(dest);
				throw e.updateStack();
			}
		}
	};

	// CKSUM - Checksum
	public static final OpImpl ESC_x2A_CKSUM = new OpImpl() { // flaw in Java 8 spec or bug in Eclipse-Java8-Compiler?
		public void execute() {
			int source = Cpu.popLong();
			int count = Cpu.pop() & 0xFFFF;
			int cksum = Cpu.pop() & 0xFFFF;
			try {
				while(true) {
					if (count == 0) { break; }
					cksum = checksum(cksum, Mem.readWord(source)); // this may throw a MesaAbort on page fault
					count--;
					source++;
					if (Processes.interruptPending() && count > 0) {
						Cpu.push((short)cksum);
						Cpu.push((short)count);
						Cpu.pushLong(source);
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			} catch(Cpu.MesaAbort e) {
				e.beginUpdateStack();
				Cpu.push((short)cksum);
				Cpu.push((short)count);
				Cpu.pushLong(source);
				throw e.updateStack();
			}
			if (cksum == 0177777) { cksum = 0; }
			Cpu.push((short)(cksum & 0xFFFF));
		}
	};
	
	private static int checksum(int cksum, short data) {
		int temp = (cksum + (data & 0xFFFF)) & 0xFFFF;
		if (cksum > temp) { temp = temp + 1; }
		if (temp >= 0100000) {
			temp = (temp * 2) + 1;
		} else {
			temp = temp * 2;
		}
		return temp & 0xFFFF;
	}
	
	/*
	 * 8.2 Block Comparisons
	 */

	// BLEL - Block Equal Long
	public static final OpImpl ESC_x28_BLEL = new OpImpl() { // flaw in Java 8 spec or bug in Eclipse-Java8-Compiler?
		public void execute() {
			int ptr1 = Cpu.popLong();
			int count = Cpu.pop() & 0xFFFF;
			int ptr2 = Cpu.popLong();
			try {
				while(true) {
					if (count == 0) {
						Cpu.push((short)1); // true
						return;
					}
					if (Mem.readWord(ptr1) != Mem.readWord(ptr2)) { // this may throw a MesaAbort on page fault
						Cpu.push(0); // false
						return;
					}
					count--;
					ptr1++;
					ptr2++;
					if (Processes.interruptPending()) {
						if (count == 0) {
							Cpu.push((short)1); // true
							return;
						}
						Cpu.pushLong(ptr2);
						Cpu.push((short)count);
						Cpu.pushLong(ptr1);
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			} catch(Cpu.MesaAbort e) {
				e.beginUpdateStack();
				Cpu.pushLong(ptr2);
				Cpu.push((short)count);
				Cpu.pushLong(ptr1);
				throw e.updateStack();
			}
		}
	};

	// BLECL - Block Equal Code Long
	public static final OpImpl ESC_x29_BLECL = new OpImpl() { // flaw in Java 8 spec or bug in Eclipse-Java8-Compiler?
		public void execute() {
			int ptr = Cpu.popLong();
			int count = Cpu.pop() & 0xFFFF;
			int offset = Cpu.pop() & 0xFFFF;
			try {
				while(true) {
					if (count == 0) {
						Cpu.push((short)1); // true
						return;
					}
					if (Mem.readWord(ptr) != Mem.readCode(offset)) { // this may throw a MesaAbort on page fault
						Cpu.push(0); // false
						return;
					}
					count--;
					ptr++;
					offset++;
					if (Processes.interruptPending()) {
						if (count == 0) {
							Cpu.push((short)1); // true
							return;
						}
						Cpu.push(offset);
						Cpu.push((short)count);
						Cpu.pushLong(ptr);
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			} catch(Cpu.MesaAbort e) {
				e.beginUpdateStack();
				Cpu.push(offset);
				Cpu.push((short)count);
				Cpu.pushLong(ptr);
				throw e.updateStack();
			}
		}
	};
	
	/*
	 * 8.3 Byte Boundary Block Transfers 
	 */

	// BYTBLT - Byte Block Transfer
	public static final OpImpl ESC_x2D_BYTBLT = new OpImpl() { // flaw in Java 8 spec or bug in Eclipse-Java8-Compiler?
		public void execute() {
			int sourceOffset = Cpu.pop() & 0xFFFF;
			int sourceBase = Cpu.popLong();
			int count = Cpu.pop() & 0xFFFF;
			int destOffset = Cpu.pop() & 0xFFFF;
			int destBase = Cpu.popLong();
			
			sourceBase += sourceOffset / 2;
			sourceOffset = sourceOffset % 2;
			destBase += destOffset / 2;
			destOffset = destOffset % 2;
			
			try {
				while(count != 0) {
					Mem.storeByte(destBase, destOffset, Mem.fetchByte(sourceBase, sourceOffset));
					count--;
					sourceOffset++;
					destOffset++;
					if (Processes.interruptPending() && count > 0) {
						Cpu.pushLong(destBase);
						Cpu.push((short)(destOffset));
						Cpu.push((short)(count));
						Cpu.pushLong(sourceBase);
						Cpu.push((short)(sourceOffset));
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			} catch(Cpu.MesaAbort e) {
				e.beginUpdateStack();
				Cpu.pushLong(destBase);
				Cpu.push((short)(destOffset));
				Cpu.push((short)(count));
				Cpu.pushLong(sourceBase);
				Cpu.push((short)(sourceOffset));
				throw e.updateStack();
			}
		}
	};

	// BYTBLTR - Byte Block Transfer Reversed
	public static final OpImpl ESC_x2E_BYTBLTR = new OpImpl() { // flaw in Java 8 spec or bug in Eclipse-Java8-Compiler?
		public void execute() {
			int sourceOffset = Cpu.pop() & 0xFFFF;
			int sourceBase = Cpu.popLong();
			int count = Cpu.pop() & 0xFFFF;
			int destOffset = Cpu.pop() & 0xFFFF;
			int destBase = Cpu.popLong();

			sourceBase += sourceOffset / 2;
			sourceOffset = sourceOffset % 2;
			destBase += destOffset / 2;
			destOffset = destOffset % 2;
			
			int srcPos = sourceOffset + count;
			int dstPos = destOffset + count;
			
			try {
				while(count != 0) {
					srcPos--;
					dstPos--;
					Mem.storeByte(destBase, dstPos, Mem.fetchByte(sourceBase, srcPos));
					count--;
					if (Processes.interruptPending() && count > 0) {
						Cpu.pushLong(destBase);
						Cpu.push((short)(destOffset));
						Cpu.push((short)(count));
						Cpu.pushLong(sourceBase);
						Cpu.push((short)(sourceOffset));
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			} catch(Cpu.MesaAbort e) {
				e.beginUpdateStack();
				Cpu.pushLong(destBase);
				Cpu.push((short)(destOffset));
				Cpu.push((short)(count));
				Cpu.pushLong(sourceBase);
				Cpu.push((short)(sourceOffset));
				throw e.updateStack();
			}
		}
	};
	
	/*
	 * 8.4 Bit Boundary Block Transfers
	 */
	
	private enum Direction { forward, backward };
	
	private enum PixelType { bit, display };
	
	private enum SrcFunc { fnull, fcomplement };
	
	private enum DstFunc { src, srcIfDstLE1, srcIf0, srcIfDstNot0, srcIfNot0, srcIfDst0, pixelXor, srcXorDst };
	private final static DstFunc[] DSTFUNC_MAP_COLORBLT = {
		DstFunc.src,
		DstFunc.srcIfDstLE1,
		DstFunc.srcIf0,
		DstFunc.srcIfDstNot0,
		DstFunc.srcIfNot0,
		DstFunc.srcIfDst0,
		DstFunc.pixelXor,
		DstFunc.srcXorDst
	};
	private final static DstFunc[] DSTFUNC_MAP_BITBLT = {
			DstFunc.src,
			DstFunc.srcIfDstNot0,
			DstFunc.srcIfDst0,
			DstFunc.srcXorDst
	};
	
	private interface PixelSource {
		
		/**
		 * Get the current pixel value.
		 *  
		 * @return the value of the next pixel or 0 if getting over the last
		 *   pixel in the line. 
		 */
		int getCurrPixel();
		
		/**
		 * Move the current pixel position one further.
		 */
		void moveToNextPixel();
		
		/**
		 * Switch to next line and cache the 
		 */
		void moveToNextLine();
		
		/**
		 * Initial load the cache for the first line.
		 */
		void loadLineCache();
	}
	
	private interface PixelSink extends PixelSource {
		
		/**
		 * Overwrite the last pixel fetched in cache.
		 * 
		 * @param value the new pixel value;
		 */
		void setCurrPixel(int value);
		
		/**
		 * Flush the cached word
		 */
		void flush();
		
	}
	
	@FunctionalInterface
	private interface PixelCombiner {
		int combine(int left, int right);
	}
		
	private static PixelCombiner getCombiner(SrcFunc srcFunc, DstFunc dstFunc) {
		PixelCombiner dstOp;
		
		switch(dstFunc) {
		
		case src:
			dstOp = (s,d) -> s;
			break;
			
		case srcIfDstLE1:
			dstOp = (s,d) -> (d > 1) ? d : s;
			break;
		
		case srcIf0:
			dstOp = (s,d) -> (s == 0) ? 0 : d;
			break;
			
		case srcIfDstNot0:
			dstOp = (s,d) -> (d == 0) ? 0 : s;
			break;
			
		case srcIfNot0:
			dstOp = (s,d) -> (s == 0) ? d : s;
			break;
			
		case srcIfDst0:
			dstOp = (s,d) -> (d == 0) ? s : d;
			break;
			
		case pixelXor:
			dstOp = (s,d) -> ((s < 1 && d < 1) || (s > 0 && d > 0)) ? 0 : 1;
			break;
			
		case srcXorDst:
			dstOp = (s,d) -> s ^ d;
			break;
			
		default:
			// how did we get there ??
			System.out.println("################### unhandled dstFunc: " + dstFunc);
			dstOp = (s,d) -> s;
		}
		
		if (srcFunc == SrcFunc.fnull) {
			return dstOp;
		}
		
		return (s,d) -> (s == 0) ? dstOp.combine(1, d) : dstOp.combine(0, d);
	}

	/**
	 * Pixel source or sink for true bitmaps in memory, with caching
	 * of complete pixel lines allowing to always process the pixels on a line
	 * from left to right, even if the direction is backward. 
	 */
	private static class PixmapForwardPixelSink implements PixelSink {
		
		private final int bitsPerPixel; // 1, 4, 8 ?
		
		private final int pixelMask; // mask to get a pixel out of a word after shifting to low bits
		
		private final int pixelsPerLine; // line length in pixels, as per initialization
		private final int bitsPerLine;   // line length in bits (pixelsPerLine * bitsPerPixel)
		
		private final int pixelTransferWidth; // pixels to transfer per line
		
		private final int wordsPerLine; // number of words to buffer for a single line
		
		private final boolean isBackward; // is the next line to process before (above) or after (below) the current line?
		
		private final int[] lineCache;
		
		private int lpLineStart; // LongPointer to 1st word of current pixel line 
		
		private int pixelOffset; // position of 1st pixel in *lpLineStart
		
		private int currPixWordOffs = 0; // offset of current word with pixels last read/written from Mem
		
		
		private int pixWord; // the word containing the current pixel
		private int pixWordOffs; // offset of the word with the current pixel
		private int pixShift; // distance to shift the bits extracted with pixMask to the lower end of the word
		
//		private final boolean logLineChanges;
		
		public PixmapForwardPixelSink(
				int lpLineStart,
				int pixelOffset,
				int pixelsPerLine,
				int transferWidth,
				int bitsPerPixel,
				boolean backward) {
			
			if (bitsPerPixel != 1 && bitsPerPixel != 4 && bitsPerPixel != 8) {
				throw new IllegalArgumentException("bitsPerPixel not one of 1,4,8");
			}
			
//			logLineChanges = (transferWidth == 6) && Mem.isInDisplayMemory(lpLineStart);
//			if (logLineChanges) {
//				System.out.printf(
//					"PixmapForwardPixelSink( lpLineStart = 0x%08X , pixelOffset = %d , pixelsPerLine = %d , transferWidth = %d , bitsPerPixel = %d , backward = %s )\n", 
//					lpLineStart,
//					pixelOffset,
//					pixelsPerLine,
//					transferWidth,
//					bitsPerPixel,
//					backward ? "true":"false");
//			}
			
			int pixelsPerWord = PrincOpsDefs.WORD_BITS / bitsPerPixel;
			
			// setup finals
			this.bitsPerPixel = bitsPerPixel;
			int tmpMask = 1;
			for (int i = 1; i < bitsPerPixel; i++) {
				tmpMask = (tmpMask << 1) | 1;
			}
			this.pixelMask = tmpMask;
			this.pixelsPerLine = Math.abs(pixelsPerLine); // negative if backward...
			this.bitsPerLine = this.pixelsPerLine * this.bitsPerPixel;
			this.pixelTransferWidth = transferWidth;
			this.wordsPerLine 
				= (((Math.abs(pixelOffset) + Math.abs(transferWidth)) * bitsPerPixel) + PrincOpsDefs.WORD_BITS - 1) / PrincOpsDefs.WORD_BITS
				+ (((Math.abs(transferWidth) % 16) != 0) ? 1 : 0);
			this.lineCache = new int[this.wordsPerLine];
			this.isBackward = backward;
			
			// setup other members
			int bitsOffset = pixelOffset * this.bitsPerPixel;
			this.lpLineStart = lpLineStart + (bitsOffset / PrincOpsDefs.WORD_BITS);
			this.pixelOffset = pixelOffset - ((this.lpLineStart - lpLineStart) * pixelsPerWord);
			this.pixShift = PrincOpsDefs.WORD_BITS - (bitsOffset % PrincOpsDefs.WORD_BITS) - this.bitsPerPixel;
			
			this.pixWordOffs = 0;
			this.currPixWordOffs = 0;
			this.pixWord = 0;
			
//			if (logLineChanges) {
//				System.out.printf(" => pixelOffset = %d , lpLineStart = 0x%08X , pixShift = %d\n",
//						this.pixelOffset,
//						this.lpLineStart,
//						this.pixShift
//						);
//				System.out.printf(" => x = %d , y = %d\n",
//						Mem.getDisplayX(this.lpLineStart, this.pixelOffset), Mem.getDisplayY(this.lpLineStart, this.pixelOffset));
//			}

		}

		@Override
		public void moveToNextLine() {
			int oldBitsOffset = this.pixelOffset * this.bitsPerPixel;
			
			if (this.isBackward) {
				int nextBitsOffset = oldBitsOffset - this.bitsPerLine;
				int wordsToSubtract = Math.abs((nextBitsOffset - PrincOpsDefs.WORD_BITS + 1) / PrincOpsDefs.WORD_BITS);
				int newBitsOffset = nextBitsOffset + (wordsToSubtract * PrincOpsDefs.WORD_BITS);
				
				this.lpLineStart -= wordsToSubtract;
				this.pixelOffset = newBitsOffset / this.bitsPerPixel;
				this.pixShift = PrincOpsDefs.WORD_BITS - newBitsOffset - this.bitsPerPixel;
			} else {
				int nextBitsOffset = oldBitsOffset + this.bitsPerLine;
				int wordsToAdd = nextBitsOffset / PrincOpsDefs.WORD_BITS;
				int newBitsOffset = nextBitsOffset % PrincOpsDefs.WORD_BITS;

				this.lpLineStart += wordsToAdd;
				this.pixelOffset = newBitsOffset / this.bitsPerPixel;
				this.pixShift = PrincOpsDefs.WORD_BITS - newBitsOffset - this.bitsPerPixel;
			}
			
//			if (logLineChanges) {
//				System.out.printf("++ moveToNextLine() -> lpLineStart = 0x%08X , pixelOffset = %3d  , pixShift = %3d -> x = %d , y = %d\n",
//						this.lpLineStart, this.pixelOffset, this.pixShift,
//						Mem.getDisplayX(this.lpLineStart, this.pixelOffset), Mem.getDisplayY(this.lpLineStart, this.pixelOffset));
//			}
		}
		
		@Override
		public void loadLineCache() {
			int wordsToCache = (((this.pixelOffset + this.pixelTransferWidth) * this.bitsPerPixel) + PrincOpsDefs.WORD_BITS - 1) / PrincOpsDefs.WORD_BITS;
			if (wordsToCache > wordsPerLine) {
				System.out.flush();
				System.err.printf(
						"## wordsToCache(%d) > wordsPerLine(%d) <<== pixelOffset(%d) , pixelsPerLine(%d)\n", 
						wordsToCache, wordsPerLine, pixelOffset, pixelsPerLine);
			}
			for (int i = 0; i < wordsToCache /*this.wordsPerLine*/; i++) {
				this.lineCache[i] = Mem.readWord(this.lpLineStart + i);
			}
			this.pixWordOffs = 0;
			this.currPixWordOffs = 0;
			this.pixWord = this.lineCache[this.currPixWordOffs];
		}
		
		@Override
		public void moveToNextPixel() {
			this.pixShift -= this.bitsPerPixel;
			if (this.pixShift < 0) {
				this.pixWordOffs++;
				this.pixShift = PrincOpsDefs.WORD_BITS - this.bitsPerPixel;
			}
		}

		@Override
		public int getCurrPixel() {
			if (this.currPixWordOffs != this.pixWordOffs) {
				this.flush();
				this.pixWord = this.lineCache[this.pixWordOffs];
				this.currPixWordOffs = this.pixWordOffs;
			}
			
			return (this.pixWord >> this.pixShift) & this.pixelMask;
		}

		@Override
		public void setCurrPixel(int newValue) {
			if (this.currPixWordOffs != this.pixWordOffs) {
				this.flush();
				this.pixWord = this.lineCache[this.pixWordOffs];
				this.currPixWordOffs = this.pixWordOffs;
			}
			
			this.pixWord
				= (this.pixWord & ~(this.pixelMask << this.pixShift))
				| ((newValue & this.pixelMask) << this.pixShift);
		}
		
		@Override
		public void flush() {
			Mem.writeWord(this.lpLineStart + this.currPixWordOffs, (short)(this.pixWord & 0xFFFF));
		}
		
	}

	/**
	 * Readonly- (i.e. Source-only) variant of a PixmapForwardPixelSink, preventing
	 * writing to memory should it be used as target instead of a pure pixel source.
	 */
	private static class PixmapForwardPixelSource extends PixmapForwardPixelSink {

		public PixmapForwardPixelSource(
					int lpLineStart,
					int pixelOffset,
					int pixelsPerLine,
					int transferWidth,
					int bitsPerPixel,
					boolean backward) {
			super(lpLineStart, pixelOffset, pixelsPerLine, transferWidth, bitsPerPixel, backward);
		}
		
		@Override
		public void flush() { } // prevent writes for a pixel source
	}
	
	/**
	 * Pixel source for a packed monochrome pixel pattern. Pixels are the bits
	 * of the pattern words as originally defined for BITBLT. 
	 */
	private static class MonochromePackedPatternPixelSource implements PixelSource {
		
		private final int wordsWidth; // pattern width in words = GrayParm.widthMinusOne + 1
		
		private final int pixelsHeight; // pattern height in lines = GrayParm.heightMinusOne + 1
		
		private final int xOffset; // 1st pixel-bit to start in each pattern line 
		
		private final int lpPatternStart; // long pointer to the true 1st word of the pattern
		
		private int yOffset; // current pattern line: [0..pixelsHeight)
		
		private short[] patternWords = null; // loaded at first access => restartability
		
		private int currWordInLine; // relative position in wordsWidth
		private int currWordOffset; // absolute offset in patternsWord
		private int currWord; // the pixels-word currently in use 
		private int currBitMask; // mask to extract the pattern bit from currWord
		
		public MonochromePackedPatternPixelSource(
				int argSrcWord,    // address of the 1st word at line yOffset(!) of the pattern
				int argSrcBit,     // position of 1st pixel in 1st line-word to transfer on each line
				int yOffset,       // as of reinterpreted arg.srcBpl
				int widthMinusOne, // as of reinterpreted arg.srcBpl
				int heightMinusOne // as of reinterpreted arg.srcBpl
				) {
			this.wordsWidth = widthMinusOne + 1;
			this.pixelsHeight = heightMinusOne + 1;
			this.xOffset = argSrcBit;
			this.lpPatternStart = argSrcWord - (yOffset * this.wordsWidth);
			this.yOffset = yOffset;
		}
		
		@Override
		public void loadLineCache() {
			if (patternWords == null) {
				short[] words = new short[this.wordsWidth * this.pixelsHeight];
				for (int i = 0; i < words.length; i++) {
					words[i] = Mem.readWord(this.lpPatternStart + i);
				}
				this.patternWords = words;
				this.yOffset--;
				this.moveToNextLine();
			}
		}

		@Override
		public void moveToNextLine() {
			this.yOffset++;
			if (this.yOffset >= this.pixelsHeight) {
				this.yOffset = 0;
			}
			this.currWordInLine = 0;
			this.currWordOffset = this.yOffset * this.wordsWidth;
			this.currWord = this.patternWords[this.currWordOffset];
			this.currBitMask = 0x8000 >>> this.xOffset;
		}

		@Override
		public int getCurrPixel() {
			return ((this.currWord & this.currBitMask) == 0) ? 0 : 1;
		}

		@Override
		public void moveToNextPixel() {
			this.currBitMask >>>= 1;
			if (this.currBitMask == 0) {
				this.currWordInLine++;
				if (this.currWordInLine >= this.wordsWidth) {
					this.currWordInLine = 0;
					this.currWordOffset = this.yOffset * this.wordsWidth;
				} else {
					this.currWordOffset++;
				}
				this.currWord = this.patternWords[this.currWordOffset];
				this.currBitMask = 0x8000;
			}
		}
	}
	
	/**
	 * Pixel source for an unpacked pixel pattern. A single pattern pixel is represented
	 * by a word. Usage of this pattern type is triggered by the unpacked bit in the
	 * BITBLTX resp. COLORBLT parameters.  
	 */
	private static class UnpackedPatternPixelSource implements PixelSource {
		
		private final int pixelsWidth; // pattern width in words = GrayParm.widthMinusOne + 1
		
		private final int pixelsHeight; // pattern height in lines = GrayParm.heightMinusOne + 1
		
		private final int xOffset; // 1st pixel-bit to start in each pattern line 
		
		private final int lpPatternStart; // long pointer to the true 1st word of the pattern
		
		private final boolean isMonochrome;
		
		private int yOffset; // current pattern line: [0..pixelsHeight)
		
		private int[] patternPixels = null; // loaded at first access => restartability
		
		private int currX;
		
		private int baseIdx;
		
		public UnpackedPatternPixelSource(
				int argSrcWord,    // address of the 1st word at line yOffset(!) of the pattern
				int argSrcBit,     // position of 1st pixel  to transfer on each line
				int yOffset,       // as of reinterpreted arg.srcBpl
				int widthMinusOne, // as of reinterpreted arg.srcBpl
				int heightMinusOne,// as of reinterpreted arg.srcBpl
				boolean monochrome // use the whole unpacked value or only 0/not-0?
				) {
			this.pixelsWidth = widthMinusOne + 1;
			this.pixelsHeight = heightMinusOne + 1;
			this.xOffset = argSrcBit % this.pixelsWidth;
			this.lpPatternStart = argSrcWord - (yOffset * this.pixelsWidth);
			this.yOffset = yOffset % this.pixelsHeight;
			this.isMonochrome = monochrome;
		}

		@Override
		public void loadLineCache() {
			if (patternPixels == null) {
				int[] pixels = new int[this.pixelsWidth * this.pixelsHeight];
				for (int i = 0; i < pixels.length; i++) {
					short pixelValue = Mem.readWord(this.lpPatternStart + i);
					System.out.printf("  -- UnpackedPatternPixelSource :: %s - pixelValue = 0x%04X\n", this.isMonochrome ? "mono" : "color", pixelValue);
					if (this.isMonochrome) {
						pixels[i] = (pixelValue == 0) ? 0 : 1; // ?? condition; ((pixelValue & 0x8000) == 0
					} else {
						pixels[i] = pixelValue;
					}
				}
				this.patternPixels = pixels;
				this.yOffset--;
				this.moveToNextLine();
			}
		}

		@Override
		public int getCurrPixel() {
			return this.patternPixels[this.baseIdx + this.currX];
		}

		@Override
		public void moveToNextPixel() {
			this.currX++;
			if (this.currX >= this.pixelsWidth) {
				this.currX = 0;
			}
		}

		@Override
		public void moveToNextLine() {
			this.yOffset++;
			if (this.yOffset >= this.pixelsHeight) {
				this.yOffset = 0;
			}
			this.currX = this.xOffset;
			this.baseIdx = (this.yOffset * this.pixelsWidth);
		}
	}
	
	/**
	 * Pixel source for a pattern having all bits in the same color (either
	 * all pixels black or all pixels white for an b/w display resp. a single
	 * color index on a color display).
	 */
	private static class UnipixelPatternSource implements PixelSource {
		
		private final int pixel;
		
		public UnipixelPatternSource(int pixel) {
			this.pixel = pixel;
		}

		@Override
		public int getCurrPixel() {
			return this.pixel;
		}

		@Override
		public void moveToNextPixel() {
			// irrelevant
		}

		@Override
		public void moveToNextLine() {
			// irrelevant
		}

		@Override
		public void loadLineCache() {
			// irrelevant
		}
	}
	
	/**
	 * Core implementation of the BITBLT-type operations, allowing to load
	 * the parameters for the instruction variants (BITBLT, BITBLTX and COLORBLT)
	 * to a uniform structure, ensuring for restartability and providing the common
	 * execution engine for all instruction variants.
	 */
	private static class BitBltArgs {

		/*
		 * restartability things: unique identification of this operation
		 * when the state must be saved on a pending interrupt or page fault.
		 */
		private static int lastPendingBitBltId = 0;
		
		private final int id;
		
		public BitBltArgs() {
			this.id = ++lastPendingBitBltId;	
		}
		
		public int getId() {
			return this.id;
		}
		
		/*
		 * operation parameters
		 */
		
		// Address dst
		private int dstWord; // long pointer
		private int dstPixel; // cardinal
		
		private short dstPpl; // integer
		
		// Address src
		private int srcWord; // long pointer
		private int srcPixel; // cardinal
		
		// srcPpl or PatternParm(=GrayParm)
		private short srcPpl;
		private int patReserved; // nibble
		private boolean patUnpacked; // patReserved != 0
		private int patYOffset; // nibble
		private int patWidthMinusOne; // nibble
		private int patHeightMinusOne; // nibble
		
		// geometry
		private int width; // cardinal
		private int height; // cardinal
		
		// ColorBltFlags
		private Direction direction = Direction.forward;
		private PixelType srcType = PixelType.bit;
		private PixelType dstType = PixelType.bit;
		private boolean pattern = false;
		private SrcFunc srcFunc = SrcFunc.fnull;
		private DstFunc dstFunc = DstFunc.src;
		@SuppressWarnings("unused") private short flgReserved = 0;
		
		// ColorMapping
		private short[] colorMapping = { 0, 1 };
		
		// the working objects doing the real work
		private PixelCombiner combiner = null;
		private PixelSource pixelSource = null;
		private PixelSink pixelSink = null;
		private int remainingLines = 0;
		
		// initialize for COLORBLT: load 13 words from *pointer
		public BitBltArgs loadFromColorBltArgs(short pointer, String logMsg) {
			this.dstWord = Mem.readMDSDblWord(pointer);
			this.dstPixel = Mem.readMDSWord(pointer, 2) & 0xFFFF;
			
			this.dstPpl = Mem.readMDSWord(pointer, 3);
			
			this.srcWord = Mem.readMDSDblWord(pointer, 4);
			this.srcPixel = Mem.readMDSWord(pointer, 6) & 0xFFFF;
			
			this.srcPpl = Mem.readMDSWord(pointer, 7);
			this.patReserved = this.srcPpl >>> 12;
			this.patUnpacked = (this.patReserved != 0);
			this.patYOffset = (this.srcPpl >> 8) & 0x000F;
			this.patWidthMinusOne = (this.srcPpl >> 4) & 0x000F;
			this.patHeightMinusOne = this.srcPpl & 0x000F;
			
			this.width = Mem.readMDSWord(pointer, 8) & 0xFFFF;
			this.height = Mem.readMDSWord(pointer, 9) & 0xFFFF;
			
			short tmp = Mem.readMDSWord(pointer, 10);
			this.direction = ((tmp & 0x8000) == 0) ? Direction.forward : Direction.backward;
			this.srcType = ((tmp & 0x4000) == 0) ? PixelType.bit : PixelType.display;
			this.dstType = ((tmp & 0x2000) == 0) ? PixelType.bit : PixelType.display;
			this.pattern = (tmp & 0x1000) != 0;
			this.srcFunc = ((tmp & 0x0800) == 0) ? SrcFunc.fnull : SrcFunc.fcomplement;
			this.dstFunc = DSTFUNC_MAP_COLORBLT[((tmp & 0x0700) >>> 8)];
			this.flgReserved = (short)(tmp & 0x00FF);
			
			this.colorMapping[0] = Mem.readMDSWord(pointer, 11);
			this.colorMapping[1] = Mem.readMDSWord(pointer, 12);

			// logging if enabled and the special key is pressed (see KeyboardMapper.pressed(key) => F1) 
			int dstX = Mem.getDisplayX(this.dstWord, this.dstPixel);
			int dstY = Mem.getDisplayY(this.dstWord, this.dstPixel);
			if (Config.LOG_BITBLT_INSNS && Config.dynLogBitblts) { // if (Config.LOG_BITBLT_INSNS && dstX >= 700 && dstY >= 0 && dstY < 32) {
				if (logMsg != null) { System.out.println(logMsg); }
				System.out.printf("\n++ loadFromColorBltArgs( 0x%04X )\n", pointer);
				System.out.printf("++ dstWord: 0x%08X , dstPixel: 0x%04X, dstPpl: %d\n", this.dstWord, this.dstPixel, this.dstPpl);
				System.out.printf("++ srcWord: 0x%08X , srcPixel: 0x%04X, srcPpl: %d\n", this.srcWord, this.srcPixel, this.srcPpl);
				if (Mem.isInDisplayMemory(srcWord)) {
					System.out.printf("++ src :: x: %d , y: %d , width: %d , height: %d \n", 
							Mem.getDisplayX(this.srcWord, this.srcPixel), Mem.getDisplayY(this.srcWord, this.srcPixel),
							this.width, this.height);
				}
				if (Mem.isInDisplayMemory(dstWord)) {
					System.out.printf("++ dst :: x: %d , y: %d , width: %d , height: %d , tmp = 0x%04X\n", 
							dstX, dstY,
							this.width, this.height, tmp);
				} else {
					System.out.printf("++ width: %d , height: %d , tmp = 0x%04X\n", this.width, this.height, tmp);
				}
				System.out.println("++   direction = " + this.direction);
				System.out.println("++     srcType = " + this.srcType);
				System.out.println("++     dstType = " + this.dstType);
				System.out.println("++     pattern = " + this.pattern);
				System.out.println("++     srcFunc = " + this.srcFunc);
				System.out.println("++     dstFunc = " + this.dstFunc);
				if (this.pattern) {
					System.out.println("++     patReserved       = " + this.patReserved);
					System.out.println("++     patUnpacked       = " + this.patUnpacked);
					System.out.println("++     patYOffset        = " + this.patYOffset);
					System.out.println("++     patWidthMinusOne  = " + this.patWidthMinusOne);
					System.out.println("++     patHeightMinusOne = " + this.patHeightMinusOne);
				}
				System.out.printf("++ colorMapping[0] = %d , colorMapping[1] = %d\n\n", this.colorMapping[0], this.colorMapping[1]);
			}
			
			this.setupWorkers();
			
			return this;
		}
		
		// initialize for BITBLT: load 12 words from *pointer 
		public BitBltArgs loadFromBitBltArgs(short pointer) {
			this.dstWord = Mem.readMDSDblWord(pointer);
			this.dstPixel = Mem.readMDSWord(pointer, 2);
			
			this.dstPpl = Mem.readMDSWord(pointer, 3);
			
			this.srcWord = Mem.readMDSDblWord(pointer, 4);
			this.srcPixel = Mem.readMDSWord(pointer, 6) & 0xFFFF;
			
			this.srcPpl = Mem.readMDSWord(pointer, 7);
			this.patReserved = 1;
			this.patUnpacked = false;
			this.patYOffset = (this.srcPpl >> 8) & 0x000F;
			this.patWidthMinusOne = (this.srcPpl >> 4) & 0x000F;
			this.patHeightMinusOne = this.srcPpl & 0x000F;
			
			this.width = Mem.readMDSWord(pointer, 8) & 0xFFFF;
			this.height = Mem.readMDSWord(pointer, 9) & 0xFFFF;
			
			short tmp = Mem.readMDSWord(pointer, 10);
			this.direction = ((tmp & 0x8000) == 0) ? Direction.forward : Direction.backward;
			
			int displayStart = Mem.getDisplayVirtualPage() * PrincOpsDefs.WORDS_PER_PAGE;
			int displayEnd = displayStart + (Mem.getDisplayPageSize() * PrincOpsDefs.WORDS_PER_PAGE);
			this.srcType = (displayStart <= this.srcWord && this.srcWord < displayEnd) ? PixelType.display : PixelType.bit;
			this.dstType = (displayStart <= this.dstWord && this.dstWord < displayEnd) ? PixelType.display : PixelType.bit;
			
			this.pattern = (tmp & 0x1000) != 0;
			
			this.srcFunc = ((tmp & 0x0800) == 0) ? SrcFunc.fnull : SrcFunc.fcomplement;
			this.dstFunc = DSTFUNC_MAP_BITBLT[((tmp & 0x0600) >>> 9)];

			this.flgReserved = (short)(tmp & 0x01FF);
			
			this.colorMapping[0] = 0;
			this.colorMapping[1] = 1;
			
			if (Config.LOG_BITBLT_INSNS && Config.dynLogBitblts) {
				Cpu.logf("##\n## ESC x2B .. BITBLT at 0x%08X+0x%04X [insn# %d]\n##\n", Cpu.CB, Cpu.savedPC, Cpu.insns);
				Cpu.logf("\n++ loadFromBitBltArgs( 0x%04X )\n", pointer);
				Cpu.logf("++ dstWord: 0x%08X , dstPixel: 0x%04X, dstPpl: %d\n", this.dstWord, this.dstPixel, this.dstPpl);
				Cpu.logf("++ srcWord: 0x%08X , srcPixel: 0x%04X, srcPpl: %d\n", this.srcWord, this.srcPixel, this.srcPpl);
				Cpu.logf("++ width: %d , height: %d , tmp = 0x%04X\n", this.width, this.height, tmp);
				Cpu.logf("++   direction = " + this.direction);
				Cpu.logf("++     srcType = " + this.srcType);
				Cpu.logf("++     dstType = " + this.dstType);
				Cpu.logf("++     pattern = " + this.pattern);
				Cpu.logf("++     srcFunc = " + this.srcFunc);
				Cpu.logf("++     dstFunc = " + this.dstFunc);
				Cpu.logf("++ colorMapping[0] = %d , colorMapping[1] = %d\n\n", this.colorMapping[0], this.colorMapping[1]);
			}
			
			this.setupWorkers();
			
			return this;
		}
		
		// initialize for BITBLTX: get 11 words from the stack as parameters
		public BitBltArgs loadFromBitBltXArgs() {
			short tmp = Cpu.pop();
			this.height = Cpu.pop() & 0xFFFF;
			this.width = Cpu.pop() & 0xFFFF;
			this.srcPpl = Cpu.pop();
			this.srcPixel = Cpu.pop() & 0xFFFF;
			this.srcWord = Cpu.popLong();
			this.dstPpl = Cpu.pop();
			this.dstPixel = Cpu.pop() & 0xFFFF;
			this.dstWord = Cpu.popLong();
			
			// interpret alternative meaning of srcPpl
			this.patReserved = this.srcPpl >>> 12;
			this.patUnpacked = (this.patReserved != 0);
			this.patYOffset = (this.srcPpl >> 8) & 0x000F;
			this.patWidthMinusOne = (this.srcPpl >> 4) & 0x000F;
			this.patHeightMinusOne = this.srcPpl & 0x000F;
			
			// interpret tmp
			this.direction = ((tmp & 0x8000) == 0) ? Direction.forward : Direction.backward;
			this.srcType = ((tmp & 0x4000) == 0) ? PixelType.bit : PixelType.display;
			this.dstType = ((tmp & 0x2000) == 0) ? PixelType.bit : PixelType.display;
			this.pattern = (tmp & 0x1000) != 0;
			this.srcFunc = ((tmp & 0x0800) == 0) ? SrcFunc.fnull : SrcFunc.fcomplement;
			this.dstFunc = DSTFUNC_MAP_COLORBLT[((tmp & 0x0700) >>> 8)];
			this.flgReserved = (short)(tmp & 0x00FF);
			
			System.out.printf("\n++ loadFromBitBltXArgs()\n");
			System.out.printf("++ dstWord: 0x%08X , dstPixel: 0x%04X, dstPpl: %d\n", this.dstWord, this.dstPixel, this.dstPpl);
			System.out.printf("++ srcWord: 0x%08X , srcPixel: 0x%04X, srcPpl: %d\n", this.srcWord, this.srcPixel, this.srcPpl);
			System.out.printf("++ width: %d , height: %d , tmp = 0x%04X\n", this.width, this.height, tmp);
			System.out.printf("++ colorMapping[0] = %d , colorMapping[1] = %d\n\n", this.colorMapping[0], this.colorMapping[1]);
			
			this.setupWorkers();
			
			return this;
		}
		
		// create pixel source and sink as well as the pixel combiner based on the instruction parameters.
		private void setupWorkers() {
			boolean isBackward = (this.direction == Direction.backward);
			
			if (this.pattern) {
				boolean onePixel = (this.patWidthMinusOne == 0 && this.patHeightMinusOne == 0);
				short w = Mem.readWord(this.srcWord);
				if (onePixel && this.patUnpacked) {
					if (this.srcType == PixelType.bit || Mem.getDisplayType() == DisplayType.monochrome) {
						// monochrome source pattern
						this.pixelSource = new UnipixelPatternSource((w == 0) ? 0 : 1);
					} else {
						// color source pattern
						this.pixelSource = new UnipixelPatternSource(w);
					}
				} else if (onePixel && !this.patUnpacked && w == 0) {
					this.pixelSource = new UnipixelPatternSource(0);
				} else if (onePixel && !this.patUnpacked && w == (short)0xFFFF) {
					this.pixelSource = new UnipixelPatternSource(1);
				} else if (this.patUnpacked) {
					this.pixelSource = new UnpackedPatternPixelSource(
							this.srcWord,            // argSrcWord
							this.srcPixel,           // argSrcBit
							this.patYOffset,         // yOffset
							this.patWidthMinusOne,   // widthMinusOne
							this.patHeightMinusOne,  // heightMinusOne
							this.srcType == PixelType.bit || Mem.getDisplayType() == DisplayType.monochrome // monochrome?
							);
				} else {
					this.pixelSource = new MonochromePackedPatternPixelSource(
							this.srcWord,            // argSrcWord
							this.srcPixel,           // argSrcBit
							this.patYOffset,         // yOffset
							this.patWidthMinusOne,   // widthMinusOne
							this.patHeightMinusOne); // heightMinusOne
				}
			} else {
				this.pixelSource = new PixmapForwardPixelSource(
						this.srcWord,   // lpLineStart
						this.srcPixel,  // pixelOffset,
						this.srcPpl,    // pixelsPerLine
						this.width,     // transferWidth
						(this.srcType == PixelType.bit) ? 1 : Mem.getDisplayType().getBitDepth(), // bitsPerPixel
						isBackward);    // backward
			}
			
			this.pixelSink = new PixmapForwardPixelSink(
					this.dstWord,   // lpLineStart
					this.dstPixel,  // pixelOffset,
					this.dstPpl,    // pixelsPerLine
					this.width,     // transferWidth
					(this.dstType == PixelType.bit) ? 1 : Mem.getDisplayType().getBitDepth(), // bitsPerPixel
					isBackward);    // backward
			
			this.combiner = getCombiner(this.srcFunc, this.dstFunc);
			
			this.remainingLines = this.height;
		}
		
		// check if this operation does effectively nothing.
		public boolean isNoOp() {
			return this.width == 0;
		}
		
		// transfer the pixels line per line, caching complete source and destination lines,
		// with honoring pending interrupts between pixel lines.
		public void execute() {
			
			// check if colortable mapping is needed for source and destination oixels
			boolean mapSrcPixel = Mem.getDisplayType() != DisplayType.monochrome && this.srcType == PixelType.bit && this.srcFunc == SrcFunc.fnull;
			boolean mapDstPixel = Mem.getDisplayType() != DisplayType.monochrome && this.dstType == PixelType.bit;
			
			while(this.remainingLines > 0) {
				// prepare processing of this line (this may cause memory faults)
				this.pixelSource.loadLineCache();
				this.pixelSink.loadLineCache();
				
				// process pixels in the line
				for (int i = 0; i < this.width; i++) {
					
					int srcPixel = (mapSrcPixel) 
							? this.colorMapping[this.pixelSource.getCurrPixel()]
							: this.pixelSource.getCurrPixel();
					int oldDstPixel = (mapDstPixel) 
							? this.colorMapping[this.pixelSink.getCurrPixel()]
							: this.pixelSink.getCurrPixel();
					
					int newDstPixel = this.combiner.combine(srcPixel, oldDstPixel);
					this.pixelSink.setCurrPixel(newDstPixel);
					
					this.pixelSource.moveToNextPixel();
					this.pixelSink.moveToNextPixel();
				}
				this.pixelSink.flush();
				
				// this line is done
				if (this.remainingLines-- > 1) {
					this.pixelSource.moveToNextLine();
					this.pixelSink.moveToNextLine();
					if (Processes.interruptPending()) {
						// the restart state is already pushed on the stack
						Cpu.PC = Cpu.savedPC;
						return;
					}
				}
			}
			
			// IMPORTANT: when done => clear stack to remove instruction restart info
			Cpu.SP = 0;
			Cpu.savedSP = 0;
			
			// IMPORTANT: when done => remove this from pendingBitBlts using key 'id'
			pendingBitBlts.remove(this.id);
		}
		
	}
	
	// the currently active but interrupted operations
	// -> key: the value pushed on the stack as restart info
	// -> value: the instruction data waiting to proceed executing
	private final static Map<Integer,BitBltArgs> pendingBitBlts = new HashMap<>();
	
	@FunctionalInterface
	private interface BitBltArgsLoader {
		BitBltArgs get();
	}
	
	// common instruction implementation for loading the original or restart
	// instruction parameters and (re-)start instruction execution
	private static void executeBitBlt(int initialStackDepth, BitBltArgsLoader initializer) {
		if (Cpu.SP == initialStackDepth) {
			// initial instruction start: get opcode-specific bitblt parameters
			BitBltArgs bitBltOp = initializer.get();
			
			// ignore non-sense calls
			if (bitBltOp.isNoOp()) {
				return;
			}
			
			// push the restart info for the case of an interruption
			int id = bitBltOp.getId();
			pendingBitBlts.put(id, bitBltOp);
			Cpu.pushLong(id);
			Cpu.savedSP = Cpu.SP;
			
			// start processing
			bitBltOp.execute();
		} else if (Cpu.SP == 2) {
			// restart: get id and restore stack for the case of another interruption
//			if (Config.LOG_BITBLT_INSNS && Config.dynLogBitblts) {
//				System.out.println("## restarted ...\n##");
//			}
			int id = Cpu.popLong();
			Cpu.SP = 2;
			
			BitBltArgs bitBltOp = pendingBitBlts.get(id);
			if (bitBltOp != null) {
				// valid restart info: continue processing
				bitBltOp.execute();
			} else {
				// instruction restart but no restart info, so stop it...
				Cpu.SP = 0;
				Cpu.savedSP = 0;
			}
		} else {
			// what?
			Cpu.stackError();
		}
	}
	
	// BITBLT - Bit Block Transfer
	public static final OpImpl ESC_x2B_BITBLT = () -> {
		executeBitBlt(1, () -> new BitBltArgs().loadFromBitBltArgs(Cpu.pop()));
	};
	
	// BITBLTX - Bit Block Transfer X
	public static final OpImpl ESC_xC2_BITBLTX = () -> {
		System.out.printf("##\n## ESC xC2 .. BITBLTX at 0x%08X+0x%04X [insn# %d]\n##\n", Cpu.CB, Cpu.savedPC, Cpu.insns);
		executeBitBlt(11, () -> new BitBltArgs().loadFromBitBltXArgs());
	};
	
	// COLORBLT - Color Block Transfer
	public static final OpImpl ESC_xC0_COLORBLT = () -> {
		String logMsg = Config.LOG_BITBLT_INSNS 
				? String.format("##\n## ESC xC0 .. COLORBLT at 0x%08X+0x%04X [insn# %d]\n##\n", Cpu.CB, Cpu.savedPC, Cpu.insns)
				: null;
		executeBitBlt(1, () -> new BitBltArgs().loadFromColorBltArgs(Cpu.pop(), logMsg));
	};
	

	/*
	 * 8.4.3.4 Text Block Transfers / TxtBlt Routines
	 */
	
	// TXTBLT - Text Block Transfer - delegated to software implementation
	// (by explicitly calling Cpu.thrower.signalEscOpcodeTrap() to suppress the "unimplemented" log line)
	public static final OpImpl ESC_x2C_TXTBLT = () -> {
		Cpu.thrower.signalEscOpcodeTrap(0x2C);
	};
}