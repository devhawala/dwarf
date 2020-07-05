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

package dev.hawala.dmachine.unittest;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.opcodes.Ch03_Memory_Organization;

/**
 * Unittests for instructions implemented in class Ch03_Memory_Organization.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch03_MemoryOrganizationTest extends AbstractInstructionTest {
	
	private int getRpForVp(int vp) {
		Cpu.pushLong(vp);
		
		Ch03_Memory_Organization.ESC_x09_GMF.execute();
		
		int rp = Cpu.popLong();
		short mf = Cpu.pop();
		
		return rp;
	}
	
	private short getMfForVp(int vp) {
		Cpu.pushLong(vp);
		
		Ch03_Memory_Organization.ESC_x09_GMF.execute();
		
		int rp = Cpu.popLong();
		short mf = Cpu.pop();
		
		return mf;
	}
	
	private void setMfForVp(int vp, short newMf) {
		Cpu.pushLong(vp);
		Cpu.push(newMf);
		
		Ch03_Memory_Organization.ESC_x08_SMF.execute();
		Cpu.popLong(); // drop rp
		Cpu.pop(); // drop mf
	}
	
	private void setMap(int vp, short mf, int rp) {
		Cpu.pushLong(vp);
		Cpu.pushLong(rp);
		Cpu.push(mf);
		
		Ch03_Memory_Organization.ESC_x07_SM.execute();
	}
	
	private static short MF_CLEAN = 0x0000;
	private static short MF_READ = 0x0001;
	private static short MF_WRITTEN = 0x0003;
	private static short MF_VACANT = 0x0006;
	
	@After
	public void postTest() {
		Mem.createInitialPageMappingGuam();
	}

	@Test
	public void testVpToRealMapping() {
		int rp = 0x00000100;
		int vp = 0;
		int lp = 0x00000011;
		short data = 0x1234;
		
		// implant the start value
		Mem.writeWord((rp << 8) | lp, data);
		
		// place the real page in virtual page 0 to start testing
		setMfForVp(rp, MF_VACANT); // unmap the test rp , located in the area where vp == rp
		assertEquals("getMfForVp(rp) after unmapping rp", MF_VACANT, getMfForVp(rp));
		setMap(vp, MF_CLEAN, rp);
		
		while(vp < firstUnmappedPage) {
			// unmap the real page
			setMfForVp(vp, MF_VACANT);
			
			// map the real page to the next virtual page to test
			vp++;
			lp += 256;
			setMap(vp, MF_CLEAN, rp);
			
			// check initial state
			assertEquals("mapFlags for mapped vp", MF_CLEAN, getMfForVp(vp));
			assertEquals("rp for mapped vp", rp, getRpForVp(vp));
			
			// read from the page
			short memValue = Mem.readWord(lp);
			assertEquals("value at mapped virtual page address", data, memValue);
			assertEquals("mapFlags for vp after read", MF_READ, getMfForVp(vp));
			setMfForVp(vp, MF_CLEAN);
			
			// change data and write it to page
			data = (short)((data + 17) & 0xFFFF);
			Mem.writeWord(lp, data);
			assertEquals("mapFlags for vp after read", MF_WRITTEN, getMfForVp(vp));
		}
	}
	
}
