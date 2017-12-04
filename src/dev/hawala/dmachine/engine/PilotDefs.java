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
 * Constants specific to Pilot.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class PilotDefs {
	
	/*
	 * AV / fsi configuration
	 */

	public static final int AVHeapSize = 32; // 00B
	public static final int LastAVHeapSlot = AVHeapSize - 2; // ==> the last entry is unused??
	
	/** the local frame sizes used by Pilot (data area only) at the fsi slots */
	public static final int FRAME_SIZE_MAP[] = {
			   8,   12,   16,   20,   24,
			  28,   32,   40,   48,   56,
			  68,   80,   96,  112,  128,
			 148,  168,  192,  224,  252,
			 508,  764, 1020, 1276, 1532,
			1788, 2044, 2556, 3068, 3580, 4092
		};

	/** number of pre-allocated local frames for each slot */
	public static final int FRAME_WEIGHT_MAP[] = {
			20, 26, 15, 16, 16,
			12,  8,  8,  5,  5,
			 7,  2,  2,  1,  1,
			 1,  1,  1,  1,  0,
			 0,  0,  0,  0,  0,
			 0,  0,  0,  0,  0, 0
		};

	/*
	 * devices
	 */
	
	public enum DisplayType {
		monochrome(0, 1),
		fourBitPlaneColor(1, 4),
		byteColor(2, 8); 
	
		private final int type;
		private final int bitDepth;
		
		private DisplayType(int type, int depth) {
			this.type = type;
			this.bitDepth = depth;
		}
		
		public int getType() {
			return this.type;
		}
		
		public int getBitDepth() {
			return this.bitDepth;
		}
	}
	
	public static final int Device_anyFloppy = 17;
	public static final int Device_microFloppy = 23; // 1.44MB, 3 1/2" disks
	
	public static final int Device_anyPilotDisk = 64;
}
