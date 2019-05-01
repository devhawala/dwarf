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

import org.junit.Test;

import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.PrincOpsDefs;
import dev.hawala.dmachine.engine.opcodes.Ch08_Block_Transfers;

/**
 * Unittests for instructions implemented in class Ch08_Block_Transfers.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch08_BlockTrfInsnsTest extends AbstractInstructionTest {
	
	/*
	 * 8.1 Word Boundary Block Transfers
	 */
	
	/*
	 * BLT
	 */
	
	@Test
	public void test_BLT_a() {
		mkShortMem(
			// +0
			0x0000, 0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789, 0x7890,
			// +8
			0x8901, 0x9012, 0xA123, 0xB234, 0xC345, 0xD456, 0xE567, 0x0000,
			// +16
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +32
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(22, 33, testShortMem + 1, 14, testShortMem + 24);
		Ch08_Block_Transfers.OPC_xF3_BLT.execute();
		checkShortMem(
			// +0
			0x0000, 0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789, 0x7890,
			// +8
			0x8901, 0x9012, 0xA123, 0xB234, 0xC345, 0xD456, 0xE567, 0x0000,
			// +16
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789, 0x7890, 0x8901,
			// +32
			0x9012, 0xA123, 0xB234, 0xC345, 0xD456, 0xE567, 0x0000, 0x0000
			);
		checkStack(22, 33);
	}
	
	@Test
	public void test_BLT_b() {
		mkShortMem(
			// +0
			0x0000, 0x1234, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +8
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +16
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +32
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(22, 33, testShortMem + 1, 17, testShortMem + 2);
		Ch08_Block_Transfers.OPC_xF3_BLT.execute();
		checkShortMem(
			// +0
			0x0000, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234,
			// +8
			0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234,
			// +16
			0x1234, 0x1234, 0x1234, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +32
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		checkStack(22, 33);
	}
	
	/*
	 * BLTL
	 */
	
	@Test
	public void test_BLTL_a() {
		mkLongMem(
			// +0
			0x0000, 0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789, 0x7890,
			// +8
			0x8901, 0x9012, 0xA123, 0xB234, 0xC345, 0xD456, 0xE567, 0x0000,
			// +16
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +32
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(22, 33, testLongMemLow + 1, testLongMemHigh, 14, testLongMemLow + 24, testLongMemHigh);
		Ch08_Block_Transfers.OPC_xF4_BLTL.execute();
		checkLongMem(
			// +0
			0x0000, 0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789, 0x7890,
			// +8
			0x8901, 0x9012, 0xA123, 0xB234, 0xC345, 0xD456, 0xE567, 0x0000,
			// +16
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789, 0x7890, 0x8901,
			// +32
			0x9012, 0xA123, 0xB234, 0xC345, 0xD456, 0xE567, 0x0000, 0x0000
			);
		checkStack(22, 33);
	}
	
	@Test
	public void test_BLTL_b() {
		mkLongMem(
			// +0
			0x0000, 0x1234, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +8
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +16
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +32
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(22, 33, testLongMemLow + 1, testLongMemHigh, 17, testLongMemLow + 2, testLongMemHigh);
		Ch08_Block_Transfers.OPC_xF4_BLTL.execute();
		checkLongMem(
			// +0
			0x0000, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234,
			// +8
			0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234,
			// +16
			0x1234, 0x1234, 0x1234, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +32
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		checkStack(22, 33);
	}
	
	/*
	 * BLTLR (corrected version!)
	 */
	
	@Test
	public void test_BLTLR_a() {
		mkLongMem(
			// +0
			0x0123, 0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789, 0x7890,
			// +8
			0x8901, 0x9012, 0xA123, 0xB234, 0xC345, 0xD456, 0xE567, 0xF678,
			// +16
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +32
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(22, 33, testLongMemLow + 1, testLongMemHigh, 14, testLongMemLow + 24, testLongMemHigh);
		Ch08_Block_Transfers.ESC_x27_BLTLR.execute();
		checkLongMem(
			// +0
			0x0123, 0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789, 0x7890,
			// +8
			0x8901, 0x9012, 0xA123, 0xB234, 0xC345, 0xD456, 0xE567, 0xF678,
			// +16
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789, 0x7890, 0x8901,
			// +32
			0x9012, 0xA123, 0xB234, 0xC345, 0xD456, 0xE567, 0x0000, 0x0000
			);
		checkStack(22, 33);
	}
	
	@Test
	public void test_BLTLR_b() {
		mkLongMem(
			// +0
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +8
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +16
			0x0000, 0x1234, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +32
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(22, 33, testLongMemLow + 1, testLongMemHigh, 17, testLongMemLow, testLongMemHigh);
		Ch08_Block_Transfers.ESC_x27_BLTLR.execute();
		checkLongMem(
			// +0
			0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234,
			// +8
			0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234, 0x1234,
			// +16
			0x1234, 0x1234, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +24
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			// +32
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		checkStack(22, 33);
	}
	
	/*
	 * BLTC
	 */
	
	@Test
	public void test_BLTC() {
		mkCode(
			1, 2, 3, savedPC, 0xF5, PC, 4, 5, 6, 7,
			0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0,
			0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88
			);
		mkStack(11, 22, 4, 8, testShortMem + 2);
		Ch08_Block_Transfers.OPC_xF5_BLTC.execute();
		checkStack(11, 22);
		checkShortMem(
			0x0000, 0x0000, 0x1234, 0x5678, 0x9ABC, 0xDEF0, 0x1122, 0x3344,
			0x5566, 0x7788, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	/*
	 * BLTCL
	 */
	
	@Test
	public void test_BLTCL() {
		mkCode(
			1, 2, 3, savedPC, 0xF5, PC, 4, 5, 6, 7,
			0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0,
			0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88
			);
		mkStack(11, 22, 4, 8, testLongMemLow + 2, testLongMemHigh);
		Ch08_Block_Transfers.OPC_xF6_BLTCL.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x0000, 0x1234, 0x5678, 0x9ABC, 0xDEF0, 0x1122, 0x3344,
			0x5566, 0x7788, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	/*
	 * CKSUM
	 */
	
	@Test
	public void test_CKSUM() {
		mkLongMem(
			0x0000, 0x0000,
			
			// ---- begin sample IDP packet (intercepted real XNS packet)
			0x1759, // checksum (not part of the checksum, but counted in length)
			0x0028, // length including checksum) = 40 bytes = 20 words 
			0x0004, // transportControl & packetType
			
			0x0000, 0x0000,         // dst network number
			0xffff, 0xffff, 0xffff, // dst address
			0x0030,                 // dst socket
			
			0x0000, 0x0000,         // src network number
			0x0800, 0x27d2, 0x976e, // src address
			0x0030,                 // src socket
			
			0x9135,	0x0599, 0x0008, 0x0000, 0x0000, // IDP payload
			// ---- end sample IDP packet
			
			0x1234, 0x2345
			);
		mkStack(11, 22, 0, 19, testLongMemLow + 3, testLongMemHigh);
		Ch08_Block_Transfers.ESC_x2A_CKSUM.execute();
		checkStack(11, 22, 0x1759);
	}
	
	/*
	 * BLEL
	 */
	
	@Test
	public void test_BLEL_eq() {
		mkLongMem(
			0x0000, 0x0000, 0x1234, 0x5678, 0x9ABC, 0xDEF0, 0x1122, 0x3344,
			0x5566, 0x7788, 0x99AA, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x1234, 0x5678, 0x9ABC, 0xDEF0, 0x1122, 0x3344, 0x5566,
			0x7788, 0x99AA, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 17, testLongMemHigh, 9, testLongMemLow + 2, testLongMemHigh);
		Ch08_Block_Transfers.ESC_x28_BLEL.execute();
		checkStack(11, 22, 1);
	}
	
	@Test
	public void test_BLEL_neq() {
		mkLongMem(
			0x0000, 0x0000, 0x1234, 0x5678, 0x9ABC, 0xDEF0, 0x1122, 0x3344,
			0x5566, 0x7788, 0x99AA, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x1234, 0x5678, 0x9ABC, 0xDEF0, 0x1122, 0x3344, 0x5566,
			0x7788, 0x990A, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 17, testLongMemHigh, 9, testLongMemLow + 2, testLongMemHigh);
		Ch08_Block_Transfers.ESC_x28_BLEL.execute();
		checkStack(11, 22, 0);
	}
	
	/*
	 * 8.2 Block Comparisions
	 */
	
	/*
	 * BLECL
	 */
	
	@Test
	public void test_BLECL_eq() {
		mkLongMem(
				0x0000, 0x0000, 0x1234, 0x5678, 0x9ABC, 0xDEF0, 0x1122, 0x3344,
				0x5566, 0x7788, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
				);
		mkCode(
			1, 2, 3, savedPC, 0xF5, PC, 4, 5, 6, 7,
			0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0,
			0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88,
			0x99, 0xAA
			);
		mkStack(11, 22, 4, 8, testLongMemLow + 2, testLongMemHigh);
		Ch08_Block_Transfers.ESC_x29_BLECL.execute();
		checkStack(11, 22, 1);
	}
	
	@Test
	public void test_BLECL_neq() {
		mkLongMem(
				0x0000, 0x0000, 0x1234, 0x5678, 0x9ABC, 0xDEF0, 0x1122, 0x3344,
				0x5566, 0x7788, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
				);
		mkCode(
			1, 2, 3, savedPC, 0xF5, PC, 4, 5, 6, 7,
			0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0,
			0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x08,
			0x99, 0xAA
			);
		mkStack(11, 22, 4, 8, testLongMemLow + 2, testLongMemHigh);
		Ch08_Block_Transfers.ESC_x29_BLECL.execute();
		checkStack(11, 22, 0);
	}
	
	/*
	 * 8.3 Byte Boundary Block Transfers
	 */
	
	/*
	 * BYTBLT
	 */
	
	// start odd, end odd, count 18
	@Test
	public void test_BYTBLT_a() {
		mkLongMem(
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5678, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 23, testLongMemHigh, 5, 18, testLongMemLow, testLongMemHigh, 3);
		Ch08_Block_Transfers.ESC_x2D_BYTBLT.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5678, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5600, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	// start odd, end even, count 18
	@Test
	public void test_BYTBLT_b() {
		mkLongMem(
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5678, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 23, testLongMemHigh, 4, 18, testLongMemLow, testLongMemHigh, 3);
		Ch08_Block_Transfers.ESC_x2D_BYTBLT.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5678, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	// start even, end odd, count 18
	@Test
	public void test_BYTBLT_c() {
		mkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 23, testLongMemHigh, 5, 18, testLongMemLow + 1, testLongMemHigh, 0);
		Ch08_Block_Transfers.ESC_x2D_BYTBLT.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5600, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	// start even, end odd, count 19
	@Test
	public void test_BYTBLT_d() {
		mkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 23, testLongMemHigh, 5, 19, testLongMemLow + 1, testLongMemHigh, 0);
		Ch08_Block_Transfers.ESC_x2D_BYTBLT.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5678, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	// start even, end even, count 18
	@Test
	public void test_BYTBLT_e() {
		mkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 23, testLongMemHigh, 4, 18, testLongMemLow + 1, testLongMemHigh, 0);
		Ch08_Block_Transfers.ESC_x2D_BYTBLT.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	/*
	 * BYTBLTR
	 */
	
	// start odd, end odd, count 18
	@Test
	public void test_BYTBLTR_a() {
		mkLongMem(
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5678, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 23, testLongMemHigh, 5, 18, testLongMemLow, testLongMemHigh, 3);
		Ch08_Block_Transfers.ESC_x2E_BYTBLTR.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5678, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5600, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	// start odd, end even, count 18
	@Test
	public void test_BYTBLTR_b() {
		mkLongMem(
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5678, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 23, testLongMemHigh, 4, 18, testLongMemLow, testLongMemHigh, 3);
		Ch08_Block_Transfers.ESC_x2E_BYTBLTR.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5678, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	// start even, end odd, count 18
	@Test
	public void test_BYTBLTR_c() {
		mkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 23, testLongMemHigh, 5, 18, testLongMemLow + 1, testLongMemHigh, 0);
		Ch08_Block_Transfers.ESC_x2E_BYTBLTR.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5600, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	// start even, end odd, count 19
	@Test
	public void test_BYTBLTR_d() {
		mkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 23, testLongMemHigh, 5, 19, testLongMemLow + 1, testLongMemHigh, 0);
		Ch08_Block_Transfers.ESC_x2E_BYTBLTR.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0011, 0x2233, 0x4455, 0x6677, 0x8899, 0xAABB, 0xCCDD,
			0xEEFF, 0x1234, 0x5678, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	// start even, end even, count 18
	@Test
	public void test_BYTBLTR_e() {
		mkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
		mkStack(11, 22, testLongMemLow + 23, testLongMemHigh, 4, 18, testLongMemLow + 1, testLongMemHigh, 0);
		Ch08_Block_Transfers.ESC_x2E_BYTBLTR.execute();
		checkStack(11, 22);
		checkLongMem(
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x7800, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x1122, 0x3344, 0x5566, 0x7788, 0x99AA, 0xBBCC, 0xDDEE,
			0xFF12, 0x3456, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000
			);
	}
	
	/*
	 * Bit Boundary Block Transfer
	 */
	
	private void dumpWord(StringBuilder sb, int w) {
		int mask = 0x8000;
		for (int i = 0; i < 16; i++) {
			if ((w & mask) != 0) {
				sb.append('X');
			} else {
				sb.append(' ');
			}
			mask = mask >> 1;
		}
	}
	
	private StringBuilder expandBitmap(StringBuilder sb, int at, int lineWords, int pixelHeight) {
		if (sb == null) { sb = new StringBuilder(); }
		
		sb.append('+');
		for (int i = 0; i < lineWords; i++) { sb.append("----------------"); }
		sb.append("+\n");
		
		for (int j = 0; j < pixelHeight; j++) {
			sb.append('|');
			for (int i = 0; i < lineWords; i++) { dumpWord(sb, Mem.readWord(at++)); }
			sb.append("|\n");
		}

		sb.append('+');
		for (int i = 0; i < lineWords; i++) { sb.append("----------------"); }
		sb.append("+\n");
		
		return sb;
	}
	
	private void dumpBitmap(String intro, int at, int lineWords, int pixelHeight) {
		StringBuilder sb = expandBitmap(null, at, lineWords, pixelHeight);
		System.out.printf("%s:\n%s\n", intro, sb.toString());
	}
	
	private void setupStartBitmap() {
		Mem.writeWord(testLongMem + 7, (short)0xFFFF); Mem.writeWord(testLongMem + 8, (short)0xFFFF);
		Mem.writeWord(testLongMem + 13, (short)0x8000); Mem.writeWord(testLongMem + 14, (short)0x0001);
		Mem.writeWord(testLongMem + 19, (short)0x8000); Mem.writeWord(testLongMem + 20, (short)0x0001);
		Mem.writeWord(testLongMem + 25, (short)0x8000); Mem.writeWord(testLongMem + 26, (short)0x0001);
		Mem.writeWord(testLongMem + 31, (short)0xFFFF); Mem.writeWord(testLongMem + 32, (short)0xFFFF);
	}
	
	private void setupStartBitmapWithBlackZone() {
		setupStartBitmap();
		
		// fill 12 lines starting at the 8th line 
		int at = testLongMem + 42; // start of 8th line
		for (int i = 0; i < 12 * 6; i++) { Mem.writeWord(at++, (short)0xFFFF); }
	}
	
	private void checkBits(String intro, int at, int... words) {
		for (int i = 0; i < words.length; i++) {
			int w = Mem.readWord(at + i) & 0xFFFF;
			assertEquals(intro + " word # " + i, words[i], w);
		}
	}
	
	private void tellDifferent(String intro, int at, int wordWidth, String... lines) {
		StringBuilder sbExp = new StringBuilder();
		sbExp.append("+");
		for (int i = 0; i < wordWidth; i++) { sbExp.append("----------------"); }
		String hsep = sbExp.append("+").toString();
		
		sbExp.setLength(0);
		sbExp.append(hsep).append("\n");
		for (int i = 0; i < lines.length; i++) { sbExp.append("|").append(lines[i]).append("|\n"); }
		sbExp.append(hsep);
		
		StringBuilder sbAct = expandBitmap(null, at, wordWidth, lines.length);
		
		String expected = sbExp.toString();
		String actual = sbAct.toString();
		
		assertEquals(intro, expected, actual);
	}
	
	private void checkBitmap(String intro, int at, int wordWidth, String... lines) {
		int curr = at;
		for (String line : lines) {
			byte[] bits = line.getBytes();
			int word = 0;
			int cnt = 0;
			for (byte b : bits) {
				word <<= 1;
				if (b != ' ') {
					word |= 1;
				}
				cnt++;
				if (cnt == 16) {
					int actual = Mem.readWord(curr++) & 0xFFFF;
					if (actual != word) {
						tellDifferent(intro, at, wordWidth, lines);
						return;
					}
					cnt = 0;
					word = 0;
				}
			}
			if (cnt != 0) {
				int actual = Mem.readWord(curr++) & 0xFFFF;
				if (actual != word) {
					tellDifferent(intro, at, wordWidth, lines);
					return;
				}
			}
		}
	}
	
	private static final int flg_forward = 0x0000;
	private static final int flg_backward = 0x8000;
	
	private static final int flg_overlap = 0x0000;
	private static final int flg_disjoint = 0x4000;
	
	private static final int flg_overlapItems = 0x0000;
	private static final int flg_disjointItems = 0x2000;
	
	private static final int flg_bitmap = 0x0000;
	private static final int flg_gray = 0x1000;
	
	private static final int flg_srcFuncNull = 0x0000;
	private static final int flg_srcFuncComplement = 0x0800;
	
	private static final int flg_dstFuncNull = 0x0000;
	private static final int flg_dstFuncAnd = 0x0200;
	private static final int flg_dstFuncOr = 0x0400;
	private static final int flg_dstFuncXor = 0x0600;
	
	
	private void mkBitBltArg(int shortTarget,
					int dstWordLp,	// LONG POINTER : first dst bitmap word
					int dstBit,		// 0..15 : bit in first dst word
					int dstBpl,		// dst bits per line
					int srcWordLp,	// LONG POINTER : first src bitmap word
					int srcBit,		// 0..15 : bit in first src word
					int srcBpl,		// src bits per line
					int width,		// pixel width to transfer
					int height,		// pixel height to transfer
					int... flagBits	// BitBltFlags, will be ORed
					) {
		int flags = 0;
		for (int i = 0; i < flagBits.length; i++) {
			flags |= flagBits[i];
		}
		
		Mem.writeMDSWord(shortTarget, 0, dstWordLp & 0xFFFF);
		Mem.writeMDSWord(shortTarget, 1, dstWordLp >>> 16);
		Mem.writeMDSWord(shortTarget, 2, dstBit);
		Mem.writeMDSWord(shortTarget, 3, dstBpl);
		Mem.writeMDSWord(shortTarget, 4, srcWordLp & 0xFFFF);
		Mem.writeMDSWord(shortTarget, 5, srcWordLp >>> 16);
		Mem.writeMDSWord(shortTarget, 6, srcBit);
		Mem.writeMDSWord(shortTarget, 7, srcBpl);
		Mem.writeMDSWord(shortTarget, 8, width);
		Mem.writeMDSWord(shortTarget, 9, height);
		Mem.writeMDSWord(shortTarget, 10, flags);
		Mem.writeMDSWord(shortTarget, 11, 0);
	}
	
	@Test
	public void test_BITBLT_forward_null_null_intoWhite() {
		setupStartBitmap();
		
		mkBitBltArg(testShortMem,
				testLongMem + 48, // dstWord
				2, // dstBit
				6 * 16, // dstBpl
				testLongMem + 7, // srcWord
				0, // srcBit
				6 * 16, // srcBpl
				32, // width
				5, // height
				flg_forward, flg_disjoint, flg_disjointItems, flg_srcFuncNull, flg_dstFuncNull);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
		checkBitmap("BITBLT_forward_null_null_intoWhite", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                              "
			    ,"  X                              X                                                              "
			    ,"  X                              X                                                              "
			    ,"  X                              X                                                              "
			    ,"  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                              "
			    ,"                                                                                                "
			);
	}
	
	@Test
	public void test_BITBLT_forward_null_null_intoBlack() {
		setupStartBitmapWithBlackZone();
		
		mkBitBltArg(testShortMem,
				testLongMem + 42, // dstWord
				1, // dstBit
				6 * 16, // dstBpl
				testLongMem, // srcWord
				15, // srcBit
				6 * 16, // srcBpl
				34, // width
				7, // height
				flg_forward, flg_disjoint, flg_disjointItems, flg_srcFuncNull, flg_dstFuncNull);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
		checkBitmap("BITBLT_forward_null_null_intoBlack", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ,"X                                  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X X                              X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X X                              X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X X                              X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X                                  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			);
	}
	
	@Test
	public void test_BITBLT_forward_null_or() {
		setupStartBitmap();
//		dumpBitmap("Start", testLongMem, 6, 24);
		
		mkBitBltArg(testShortMem,
				testLongMem + 48, // dstWord
				2, // dstBit
				6 * 16, // dstBpl
				testLongMem + 7, // srcWord
				0, // srcBit
				6 * 16, // srcBpl
				32, // width
				5, // height
				flg_forward, flg_disjoint, flg_disjointItems, flg_srcFuncNull, flg_dstFuncOr);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
//		dumpBitmap("after BITBLT( forward, null, or )", testLongMem, 6, 24);
		checkBitmap("BITBLT_forward_null_or", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                              "
			    ,"  X                              X                                                              "
			    ,"  X                              X                                                              "
			    ,"  X                              X                                                              "
			    ,"  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                              "
			    ,"                                                                                                "
			);
		
//		checkBits("Line 0", testLongMem, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 1", testLongMem + 6, 0, 0xFFFF, 0xFFFF, 0, 0, 0);
//		checkBits("Line 2", testLongMem + 12, 0, 0x8000, 0x0001, 0, 0, 0);
//		checkBits("Line 3", testLongMem + 18, 0, 0x8000, 0x0001, 0, 0, 0);
//		checkBits("Line 4", testLongMem + 24, 0, 0x8000, 0x0001, 0, 0, 0);
//		checkBits("Line 5", testLongMem + 30, 0, 0xFFFF, 0xFFFF, 0, 0, 0);
//		checkBits("Line 6", testLongMem + 36, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 7", testLongMem + 42, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 8", testLongMem + 48, 0x3FFF, 0xFFFF, 0xC000, 0, 0, 0);
//		checkBits("Line 9", testLongMem + 54, 0x2000, 0x0000, 0x4000, 0, 0, 0);
//		checkBits("Line 10", testLongMem + 60, 0x2000, 0x0000, 0x4000, 0, 0, 0);
//		checkBits("Line 11", testLongMem + 66, 0x2000, 0x0000, 0x4000, 0, 0, 0);
//		checkBits("Line 12", testLongMem + 72, 0x3FFF, 0xFFFF, 0xC000, 0, 0, 0);
//		checkBits("Line 13", testLongMem + 78, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 14", testLongMem + 84, 0, 0, 0, 0, 0, 0);
	}
	
	@Test
	public void test_BITBLT_forward_null_xor_intoWhite() {
		setupStartBitmap();
//		dumpBitmap("Start", testLongMem, 6, 24);
		
		mkBitBltArg(testShortMem,
				testLongMem + 48, // dstWord
				2, // dstBit
				6 * 16, // dstBpl
				testLongMem + 7, // srcWord
				0, // srcBit
				6 * 16, // srcBpl
				32, // width
				5, // height
				flg_forward, flg_disjoint, flg_disjointItems, flg_srcFuncNull, flg_dstFuncXor);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
//		dumpBitmap("after BITBLT", testLongMem, 6, 24);
		checkBitmap("BITBLT_forward_null_xor_intoWhite", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                              "
			    ,"  X                              X                                                              "
			    ,"  X                              X                                                              "
			    ,"  X                              X                                                              "
			    ,"  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                              "
			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
			);
		
//		checkBits("Line 0", testLongMem, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 1", testLongMem + 6, 0, 0xFFFF, 0xFFFF, 0, 0, 0);
//		checkBits("Line 2", testLongMem + 12, 0, 0x8000, 0x0001, 0, 0, 0);
//		checkBits("Line 3", testLongMem + 18, 0, 0x8000, 0x0001, 0, 0, 0);
//		checkBits("Line 4", testLongMem + 24, 0, 0x8000, 0x0001, 0, 0, 0);
//		checkBits("Line 5", testLongMem + 30, 0, 0xFFFF, 0xFFFF, 0, 0, 0);
//		checkBits("Line 6", testLongMem + 36, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 7", testLongMem + 42, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 8", testLongMem + 48, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 9", testLongMem + 54, 0x1FFF, 0xFFFF, 0x8000, 0, 0, 0);
//		checkBits("Line 10", testLongMem + 60, 0x1FFF, 0xFFFF, 0x8000, 0, 0, 0);
//		checkBits("Line 11", testLongMem + 66, 0x1FFF, 0xFFFF, 0x8000, 0, 0, 0);
//		checkBits("Line 12", testLongMem + 72, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 13", testLongMem + 78, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 14", testLongMem + 84, 0, 0, 0, 0, 0, 0);
	}
	
	@Test
	public void test_BITBLT_forward_null_xor_intoBlack() {
		setupStartBitmapWithBlackZone();
		
		mkBitBltArg(testShortMem,
				testLongMem + 48, // dstWord
				2, // dstBit
				6 * 16, // dstBpl
				testLongMem + 7, // srcWord
				0, // srcBit
				6 * 16, // srcBpl
				32, // width
				5, // height
				flg_forward, flg_disjoint, flg_disjointItems, flg_srcFuncNull, flg_dstFuncXor);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
		checkBitmap("BITBLT_forward_null_xor_intoBlack", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX                                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX                                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			);
	}
	
	@Test
	public void test_BITBLT_forward_complement_or() {
		setupStartBitmap();
//		dumpBitmap("Start", testLongMem, 6, 24);
		
		mkBitBltArg(testShortMem,
				testLongMem + 42, // dstWord
				1, // dstBit
				6 * 16, // dstBpl
				testLongMem, // srcWord
				15, // srcBit
				6 * 16, // srcBpl
				34, // width
				7, // height
				flg_forward, flg_disjoint, flg_disjointItems, flg_srcFuncComplement, flg_dstFuncOr);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
//		dumpBitmap("after BITBLT( forward, complement, or )", testLongMem, 6, 24);
		checkBitmap("BITBLT_forward_complement_or", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ," XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                             "
			    ," X                                X                                                             "
			    ," X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX X                                                             "
			    ," X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX X                                                             "
			    ," X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX X                                                             "
			    ," X                                X                                                             "
			    ," XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                             "
			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
//			    ,"                                                                                                "
			);
		
//		checkBits("Line 0",  testLongMem, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 1",  testLongMem + 6, 0, 0xFFFF, 0xFFFF, 0, 0, 0);
//		checkBits("Line 2",  testLongMem + 12, 0, 0x8000, 0x0001, 0, 0, 0);
//		checkBits("Line 3",  testLongMem + 18, 0, 0x8000, 0x0001, 0, 0, 0);
//		checkBits("Line 4",  testLongMem + 24, 0, 0x8000, 0x0001, 0, 0, 0);
//		checkBits("Line 5",  testLongMem + 30, 0, 0xFFFF, 0xFFFF, 0, 0, 0);
//		checkBits("Line 6",  testLongMem + 36, 0, 0, 0, 0, 0, 0);
//		checkBits("Line 7",  testLongMem + 42, 0x7FFF, 0xFFFF, 0xE000, 0, 0, 0);
//		checkBits("Line 8",  testLongMem + 48, 0x4000, 0x0000, 0x2000, 0, 0, 0);
//		checkBits("Line 9",  testLongMem + 54, 0x5FFF, 0xFFFF, 0xA000, 0, 0, 0);
//		checkBits("Line 10", testLongMem + 60, 0x5FFF, 0xFFFF, 0xA000, 0, 0, 0);
//		checkBits("Line 11", testLongMem + 66, 0x5FFF, 0xFFFF, 0xA000, 0, 0, 0);
//		checkBits("Line 12", testLongMem + 72, 0x4000, 0x0000, 0x2000, 0, 0, 0);
//		checkBits("Line 13", testLongMem + 78, 0x7FFF, 0xFFFF, 0xE000, 0, 0, 0);
//		checkBits("Line 14", testLongMem + 84, 0, 0, 0, 0, 0, 0);
	}
	
	@Test
	public void test_BITBLT_forward_null_and_intoWhite() {
		setupStartBitmap();
//		dumpBitmap("Start", testLongMem, 6, 24);
		
		mkBitBltArg(testShortMem,
				testLongMem + 42, // dstWord
				1, // dstBit
				6 * 16, // dstBpl
				testLongMem, // srcWord
				15, // srcBit
				6 * 16, // srcBpl
				34, // width
				7, // height
				flg_forward, flg_disjoint, flg_disjointItems, flg_srcFuncNull, flg_dstFuncAnd);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
//		dumpBitmap("after BITBLT( forward, complement, or )", testLongMem, 6, 24);
		checkBitmap("BITBLT_forward_null_and_intoWhite", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			);
	}
	
	@Test
	public void test_BITBLT_forward_null_and_intoBlack() {
		setupStartBitmapWithBlackZone();
		
		mkBitBltArg(testShortMem,
				testLongMem + 42, // dstWord
				1, // dstBit
				6 * 16, // dstBpl
				testLongMem, // srcWord
				15, // srcBit
				6 * 16, // srcBpl
				34, // width
				7, // height
				flg_forward, flg_disjoint, flg_disjointItems, flg_srcFuncNull, flg_dstFuncAnd);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
		checkBitmap("BITBLT_forward_null_and_intoBlack", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ,"X                                  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X X                              X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X X                              X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X X                              X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"X                                  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			);
	}
	
	@Test
	public void test_BITBLT_forward_complement_and_intoBlack() {
		setupStartBitmapWithBlackZone();
		
		mkBitBltArg(testShortMem,
				testLongMem + 48, // dstWord
				2, // dstBit
				6 * 16, // dstBpl
				testLongMem + 7, // srcWord
				0, // srcBit
				6 * 16, // srcBpl
				32, // width
				5, // height
				flg_forward, flg_disjoint, flg_disjointItems, flg_srcFuncComplement, flg_dstFuncAnd);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
		checkBitmap("BITBLT_forward_complement_and_intoBlack", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX                                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX                                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			);
	}
	
	private void innertest_BITBLT_backward_null_or_intoWhite(int dstBit) {
		setupStartBitmap();
		
		// If the direction is backward, the source and destination addresses point to the beginning of
		// the last item of the blocks. to be processed, and the source and destination bits per line
		// must be negative.
		
		// where
		// -> "item" is: pixel line (for backward: start of last pixel line)
		// -> and not  : pixel      (for backward: not the last pixel in the last line)
		// !!
		
		mkBitBltArg(testShortMem,
				testLongMem + 72, // dstWord of last item
				dstBit,           // dstBit of last item
				-6 * 16,          // dstBpl (negative because backward) 
				testLongMem + 31, // srcWord of last item
				0,                // srcBit of last item
				-6 * 16,          // srcBpl (negative because backward)
				32,               // width
				5,                // height
				flg_backward, flg_disjoint, flg_disjointItems, flg_srcFuncNull, flg_dstFuncOr);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
		String shift = "                ".substring(0, dstBit);
		
		checkBitmap("BITBLT_backward_null_or_intoWhite, dstBit=" + dstBit, testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    //0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF01234...
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,(shift+"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                                ").substring(0, 96)
			    ,(shift+"X                              X                                                                ").substring(0, 96)
			    ,(shift+"X                              X                                                                ").substring(0, 96)
			    ,(shift+"X                              X                                                                ").substring(0, 96)
			    ,(shift+"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                                ").substring(0, 96)
			           //0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF01234...
			    ,"                                                                                                "
			    ,"                                                                                                "
			);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit00() {
		innertest_BITBLT_backward_null_or_intoWhite(0);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit01() {
		innertest_BITBLT_backward_null_or_intoWhite(1);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit02() {
		innertest_BITBLT_backward_null_or_intoWhite(2);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit03() {
		innertest_BITBLT_backward_null_or_intoWhite(3);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit04() {
		innertest_BITBLT_backward_null_or_intoWhite(4);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit05() {
		innertest_BITBLT_backward_null_or_intoWhite(5);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit06() {
		innertest_BITBLT_backward_null_or_intoWhite(6);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit07() {
		innertest_BITBLT_backward_null_or_intoWhite(7);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit08() {
		innertest_BITBLT_backward_null_or_intoWhite(8);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit09() {
		innertest_BITBLT_backward_null_or_intoWhite(9);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit10() {
		innertest_BITBLT_backward_null_or_intoWhite(10);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit11() {
		innertest_BITBLT_backward_null_or_intoWhite(11);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit12() {
		innertest_BITBLT_backward_null_or_intoWhite(12);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit13() {
		innertest_BITBLT_backward_null_or_intoWhite(13);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit14() {
		innertest_BITBLT_backward_null_or_intoWhite(14);
	}
	
	@Test
	public void test_BITBLT_backward_null_or_intoWhite_dstBit15() {
		innertest_BITBLT_backward_null_or_intoWhite(15);
	}
	
	@Test
	public void test_BITBLT_backward_complement_and_intoBlack() {
		setupStartBitmapWithBlackZone();
		
		// If the direction is backward, the source and destination addresses point to the beginning of
		// the last item of the blocks. to be processed, and the source and destination bits per line
		// must be negative.
		
		// where
		// -> "item" is: pixel line (for backward: start of last pixel line)
		// -> and not  : pixel      (for backward: not the last pixel in the last line)
		// !!
		
		mkBitBltArg(testShortMem,
				testLongMem + 72, // dstWord of last item
				2, // dstBit of last item
				-6 * 16, // dstBpl (negative because backward) 
				testLongMem + 31, // srcWord of last item (6th line => 5*6 + 3rd word => +2 = 32)  
				0, // srcBit of last item
				-6 * 16, // srcBpl (negative because backward)
				32, // width
				5, // height
				flg_backward, flg_disjoint, flg_disjointItems, flg_srcFuncComplement, flg_dstFuncAnd);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
		checkBitmap("BITBLT_backward_complement_and_intoBlack", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    //0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF01234...
			    ,"                                                                                                "
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX                                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XX                                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    //0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF01234...
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
			);
	}
	
	private void mkBrick(int at, int... words) {
		for (int i = 0; i < words.length; i++) {
			Mem.writeWord(at + i, (short)words[i]);
		}
	}
	
	private int mkGrayParm(int yOffset, int widthMinusOne, int heightMinusOne) {
		int parm
				= ((yOffset & 0x0F) << 8)
				| (( widthMinusOne & 0x0F) << 4)
				| (heightMinusOne & 0x0F);
		return parm;
	}
	
	@Test
	public void test_BITBLT_brick_none_none() {
		setupStartBitmap();
		
		int brickAt = testLongMem + 4096;
		mkBrick(brickAt,
				0b0011001100110011,
				0b0011001100110011,
				0b1100110011001100,
				0b1100110011001100);
		
		mkBitBltArg(testShortMem,
				testLongMem + 42, // dstWord
				2, // dstBit
				6 * 16, // dstBpl
				brickAt + 1, // srcWord (+1 because grayParm.yOffset == 1)
				3, // srcBit
				mkGrayParm(1, 0, 3), // srcBpl as grayparm :: yOffset = 1 line, width = 1 word, height 4 lines  
				34, // width
				9, // height
				flg_gray, flg_srcFuncNull, flg_dstFuncNull);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
//		dumpBitmap("after BITBLT( forward, complement, or )", testLongMem, 6, 24);
		checkBitmap("BITBLT_brick_none_none", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ,"  X  XX  XX  XX  XX  XX  XX  XX  XX                                                             "
			    ,"   XX  XX  XX  XX  XX  XX  XX  XX  X                                                            "
			    ,"   XX  XX  XX  XX  XX  XX  XX  XX  X                                                            "
			    ,"  X  XX  XX  XX  XX  XX  XX  XX  XX                                                             "
			    ,"  X  XX  XX  XX  XX  XX  XX  XX  XX                                                             "
			    ,"   XX  XX  XX  XX  XX  XX  XX  XX  X                                                            "
			    ,"   XX  XX  XX  XX  XX  XX  XX  XX  X                                                            "
			    ,"  X  XX  XX  XX  XX  XX  XX  XX  XX                                                             "
			    ,"  X  XX  XX  XX  XX  XX  XX  XX  XX                                                             "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			);
	}
	
	@Test
	public void test_BITBLT_brick_complement_none() {
		setupStartBitmap();
		
		int brickAt = testLongMem + 4096;
		mkBrick(brickAt,
				0b0011001100110011,
				0b0011001100110011,
				0b1100110011001100,
				0b1100110011001100);
		
		mkBitBltArg(testShortMem,
				testLongMem + 42, // dstWord
				2, // dstBit
				6 * 16, // dstBpl
				brickAt + 1, // srcWord (+1 because grayParm.yOffset == 1)
				3, // srcBit
				mkGrayParm(1, 0, 3), // srcBpl as grayparm :: yOffset = 1 line, width = 1 word, height 4 lines  
				34, // width
				9, // height
				flg_gray, flg_srcFuncComplement, flg_dstFuncNull);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
//		dumpBitmap("after BITBLT( forward, complement, or )", testLongMem, 6, 24);
		checkBitmap("BITBLT_brick_complement_none", testLongMem, 6
			    ,"                                                                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                X                              X                                                "
			    ,"                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX                                                "
			    ,"                                                                                                "
			    ,"   XX  XX  XX  XX  XX  XX  XX  XX  X                                                            "
			    ,"  X  XX  XX  XX  XX  XX  XX  XX  XX                                                             "
			    ,"  X  XX  XX  XX  XX  XX  XX  XX  XX                                                             "
			    ,"   XX  XX  XX  XX  XX  XX  XX  XX  X                                                            "
			    ,"   XX  XX  XX  XX  XX  XX  XX  XX  X                                                            "
			    ,"  X  XX  XX  XX  XX  XX  XX  XX  XX                                                             "
			    ,"  X  XX  XX  XX  XX  XX  XX  XX  XX                                                             "
			    ,"   XX  XX  XX  XX  XX  XX  XX  XX  X                                                            "
			    ,"   XX  XX  XX  XX  XX  XX  XX  XX  X                                                            "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			    ,"                                                                                                "
			);
	}
	
	@Test
	public void test_COLORBLT_BW_1024x640_pattern() throws InterruptedException {
		
		/*
		Mem.initializeMemory(23, 22, PilotDefs.DisplayType.monochrome, 1024, 640);
		
		=> mapDisplayMemory( toVirtualPage = 0x00001400 ) :: realDispMem = 0x00004000 , displayPageSize =160
		Unmapped page (davor? und) dahinter
		*/
		
		/*
		++ loadFromColorBltArgs( 0x9230 )
		++ dstWord: 0x00140000 , dstPixel: 0x0000, dstPpl: 1024
		++ srcWord: 0x000308FE , srcPixel: 0x0000, srcPpl: 4096
		++ width: 1024 , height: 640 , tmp = 0x7000
		++ colorMapping[0] = 0 , colorMapping[1] = 0
		
		*srcWord = 0 (1 line height)
		*/
		
		int bitmapStart = testLongMem;
		int bitmapEndPlusOne = bitmapStart + (1024 * 640 / 16);
		
		int bitmapStartPage = bitmapStart >>> 8;
		int bitmapEndPagePlusOne = bitmapEndPlusOne >>> 8;
		
		int rPageStartMinusOne = Mem.getVPageRealPage(bitmapStartPage - 1);
		int rPageEndPlusOne = Mem.getVPageRealPage(bitmapEndPagePlusOne);
		
		System.out.printf("bitmapStart   = 0x%08X => vpage = 0x%06X , rpage = 0x%06X , flags = 0x%04X\n",
				bitmapStart, bitmapStartPage, Mem.getVPageRealPage(bitmapStartPage), Mem.getVPageFlags(bitmapStartPage));
		
		System.out.printf("bitmapPostEnd = 0x%08X => vpage = 0x%06X , rpage = 0x%06X , flags = 0x%04X\n",
				bitmapEndPlusOne, bitmapEndPagePlusOne, rPageEndPlusOne, Mem.getVPageFlags(bitmapEndPagePlusOne));
		
		System.out.printf(" => pageCount = %d\n",  bitmapEndPagePlusOne - bitmapStartPage);
		
		// pattern source bits (one zero would be enough
		mkShortMem(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); // 16x 0 , just to be sure...
		
		// build COLORBLT params
		mkLocalFrame(
			// dummy filler in local frame
			0x1234, 
			// dstWord (LONG POINTER)
			testLongMemLow, testLongMemHigh,
			// dstPixel
			0x0000,
			// dstPpl
			1024,
			// srcWord (LONG POINTER)
			testShortMem, Cpu.MDS >>> 16,
			// srcPixel
			0x0000,
			// srcPpl
			4096,
			// width in pixels
			1024,
			// height in pixels
			640, // 639 works, 640 produces a PageFault when the 1st page behind the target bitmap is unmapped (vacant)...
			// direction, srcType, dstType, pattern, srcFunc, dstFunc, flgReserved
			0x7000,
			// colorMapping[0], colorMapping[1]
			0, 0
			);
			
		// colorblt parameter starts at offset 1 in the local frame 
		mkStack(Cpu.LF + 1);
		
		try {
			// unmap the page before and after the target memory area
			Mem.setMap("test_COLORBLT_BW_1024x640_pattern", bitmapStartPage - 1, 0, PrincOpsDefs.MAPFLAGS_VACANT);
			Mem.setMap("test_COLORBLT_BW_1024x640_pattern", bitmapEndPagePlusOne, 0, PrincOpsDefs.MAPFLAGS_VACANT);
			
//			// exercise the blt engine a bit for JIT 
//			for (int i = 0; i < 10; i++) {
//				mkStack(Cpu.LF + 1);
//				long nanoStart = System.nanoTime();
//				Ch08_Block_Transfers.ESC_xC0_COLORBLT.execute();long nanoLength = System.nanoTime() - nanoStart;
//				System.out.printf("COLORBLT duration in nanoSecs: %d\n", nanoLength);
//				Thread.sleep(3);
//			}
//			Thread.sleep(32);
			
			// colorblt parameter starts at offset 1 in the local frame 
			mkStack(Cpu.LF + 1);
			
			// do the COLORBLT
			long nanoStart = System.nanoTime();
			Ch08_Block_Transfers.ESC_xC0_COLORBLT.execute();
			long nanoLength = System.nanoTime() - nanoStart;
			System.out.printf("COLORBLT duration in nanoSecs: %d\n", nanoLength);
		} finally {
			// re-map the page before and after the target memory area
			Mem.setMap("test_COLORBLT_BW_1024x640_pattern", bitmapStartPage - 1, rPageStartMinusOne, PrincOpsDefs.MAPFLAGS_CLEAR);
			Mem.setMap("test_COLORBLT_BW_1024x640_pattern", bitmapEndPagePlusOne, rPageEndPlusOne, PrincOpsDefs.MAPFLAGS_CLEAR);
		}
	}
	
	@Test
	public void test_BITBLT_brick_none_none_ppl9() {
		setupStartBitmap();
		
		int brickAt = testLongMem + 4096;
		mkBrick(brickAt, 0xFFFF);
		
		mkBitBltArg(testShortMem,
				testLongMem, // dstWord
				0, // dstBit
				9, // dstBpl
				brickAt, // srcWord (+1 because grayParm.yOffset == 0)
				0, // srcBit
				mkGrayParm(0, 0, 0), // srcBpl as grayparm :: yOffset = 0 line, width = 1 word, height 1 lines  
				9, // width
				16, // height
				flg_gray, flg_srcFuncNull, flg_dstFuncNull);
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
		checkBitmap("BITBLT_brick_none_none_ppl9", testLongMem, 1
			    ,"XXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXX"
			    ,"XXXXXXXXXXXXXXXX"
			    ,"                "
			    ,"                "
			);
	}
	
	private int ch(char first, char second) {
		return ((first << 8) & 0xFF00) | (second & 0xFF);
	}
	
	private String getLongMemStringEquiv(int pos, int wordCount) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < wordCount; i++) {
			int w = Mem.readWord(testLongMem + i) & 0xFFFF;
			int c = w >>> 8;
			sb.append( (c >= 0x20 && c < 0x7F) ? (char)c : 'µ');
			c = w & 0xFF;
			sb.append( (c >= 0x20 && c < 0x7F) ? (char)c : 'µ');
		}
		return sb.toString();
	}
	
	@Test
	public void test_BITBLT_as_BYTBLTR_misuse_backwrd() {
		mkLongMem(
			ch('q','w'),ch('e','r'),ch('t','z'),ch('u','i'),ch('o','p'),
			ch('\n','a'),ch('s','d'),ch('f','g'),ch('h','j'),ch('k','l'),
			ch('\n','y'),ch('x','c'),ch('v','b'),ch('n','m'),ch('%','%'),ch('+','+'));
		
		mkBitBltArg(testShortMem,
				testLongMem + 0x0E,  // dstWord
				0x0000,              // dstPixel
				-8,                  // dstPpl
				testLongMem + 0x0D,  // srcWord
				0x0008,              // srcPixel
				-8,                  // srcPpl
				8,                   // width
				23,                  // height
				flg_backward
				);
				
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
		String expected = "qwertzzuiopµasdfghjklµyxcvbnm%++";
		String result = getLongMemStringEquiv(testLongMem, 16);
		
		System.out.printf(
				"expected: %s\n" +
				"actual..: %s\n",
				expected, result);
		
		assertEquals(
				expected,
				result
				);
		
		checkLongMem(
			ch('q','w'),ch('e','r'),ch('t','z'),ch('z','u'),ch('i','o'),ch('p','\n'),
			ch('a','s'),ch('d','f'),ch('g','h'),ch('j','k'),ch('l','\n'),
			ch('y','x'),ch('c','v'),ch('b','n'),ch('m','%'),ch('+','+')
			);
	}
	
	@Test
	public void test_BITBLT_as_BYTBLT_misuse_forward() {
		mkLongMem(
			ch('q','w'),ch('e','r'),ch('t','z'),ch('u','i'),ch('o','p'),
			ch('\n','a'),ch('s','d'),ch('f','g'),ch('h','j'),ch('k','l'),
			ch('\n','y'),ch('x','c'),ch('v','b'),ch('n','m'),ch('%','%'),ch('+','+'));
		
		mkBitBltArg(testShortMem,
				testLongMem + 0x02,  // dstWord
				0x0008,              // dstPixel
				8,                   // dstPpl
				testLongMem + 0x03,  // srcWord
				0x0000,              // srcPixel
				8,                   // srcPpl
				8,                   // width
				22,                  // height
				flg_forward
				);
				
		mkStack(testShortMem);
		Ch08_Block_Transfers.ESC_x2B_BITBLT.execute();
		checkStack();
		
		String expected = "qwertuiopµasdfghjklµyxcvbnmm%%++";
		String result = getLongMemStringEquiv(testLongMem, 16);
		
		System.out.printf(
				"expected: %s\n" +
				"actual..: %s\n",
				expected, result);
		
		assertEquals(
				expected,
				result
				);
		
		checkLongMem(
			ch('q','w'),ch('e','r'),ch('t','u'),ch('i','o'),ch('p','\n'),
			ch('a','s'),ch('d','f'),ch('g','h'),ch('j','k'),ch('l','\n'),
			ch('y','x'),ch('c','v'),ch('b','n'),ch('m','m'),ch('%','%'),ch('+','+')
			);
	}

}
