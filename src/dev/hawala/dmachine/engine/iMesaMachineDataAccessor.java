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
 * UI callbacks that can be registered with the mesa engine, allowing the engine
 * to provide data at a suitable moment in the instruction processing (i.e. when
 * checking for interrupts or timeouts), ensuring that no concurrent changes occur
 * while the callback is active.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public interface iMesaMachineDataAccessor {
	
	/**
	 * Callback allowing the UI to transfer the display bits from the display
	 * memory in the real memory space of the mesa engine for updating the
	 * UIs pixelmap.
	 * <br>
	 * Using the {@code pageFlags} and {@code firstPage} values, transferring
	 * the display memory can be restricted to changed pages in the display memory,
	 * as the mesa engine can reset the dirty flag of the virtual display memory pages
	 * after the UI has accessed the display memory.
	 * 
	 * @param realMemory the real memory used by the mesa engine.
	 * @param memOffset the real address of the display memory in {@code realMemory}, starting at a memory page
	 * @param memWords the length in addressable words of the display memory
	 * @param pageFlags the virtual page map used by the mesa engine
	 * @param firstPage index of the virtual page for {@code memOffset} in {@code pageFlags}
	 * @param colorTable mapping of pixel values to colors, irrelevant for B/W display; the {@code int}-values
	 * 			are: {@code 0x00rrggbb}
	 */
	void accessRealMemory(
			short[] realMemory, int memOffset, int memWords,
			short[] pageFlags, int firstPage,
			int[] colorTable);
	
	/**
	 * Callback informing the UI of a value change on the Maintenance Panel.
	 * 
	 * @param mp the new MP code to display.
	 */
	void acceptMP(int mp);
	
	/**
	 * Callback informing about some statistical values from the mesa engine
	 * accumulated since starting the mesa engine.
	 * 
	 * @param counterInstructions number of instructions executed.
	 * @param counterDiskReads number of hard-disk page reads. 
	 * @param counterDiskWrites number of hard-disk page writes.
	 * @param counterFloppyReads number of floppy-disk page reads.
	 * @param counterFloppyWrites number of floppy-disk page writes.
	 * @param counterNetworkPacketsReceived number of network packes received.
	 * @param counterNetworkPacketsSent number of network packets sent.
	 */
	void acceptStatistics(
			long counterInstructions,
			int counterDiskReads,
			int counterDiskWrites,
			int counterFloppyReads,
			int counterFloppyWrites,
			int counterNetworkPacketsReceived,
			int counterNetworkPacketsSent
			);
}