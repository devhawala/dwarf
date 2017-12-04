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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import dev.hawala.dmachine.engine.Cpu;
import dev.hawala.dmachine.engine.Mem;
import dev.hawala.dmachine.engine.opcodes.Ch07_Assignment_Instructions;

/**
 * Unittests for instructions implemented in class Ch07_Assignment_Instructions.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch07_AssignInsnsTest extends AbstractInstructionTest {

	/*
	 * 7.1 Immediate Instructions ******************************************************************************************* 
	 */
	
	/*
	 * LIN1
	 */
	
	@Test
	public void test_LIN1() {
		mkStack(123, 345, SP, 33, 44);
		Ch07_Assignment_Instructions.OPC_xCB_LIN1.execute();
		checkStack(123, 345, -1, SP, 44);
	}
	
	/*
	 * LINI
	 */
	
	@Test
	public void test_LINI() {
		mkStack(123, 345, SP, 33, 44);
		Ch07_Assignment_Instructions.OPC_xCC_LINI.execute();
		checkStack(123, 345, 0xFFFF8000, SP, 44);
	}
	
	/*
	 * LID0
	 */
	
	@Test
	public void test_LID0() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xD1_LID0.execute();
		checkStack(123, 345, 0, 0, SP, 55);
	}
	
	/*
	 * LI0 .. LI10
	 */
	
	@Test
	public void test_LI0() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xC0_LI0.execute();
		checkStack(123, 345, 0, SP, 44, 55);
	}
	
	@Test
	public void test_LI1() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xC1_LI1.execute();
		checkStack(123, 345, 1, SP, 44, 55);
	}
	
	@Test
	public void test_LI2() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xC2_LI2.execute();
		checkStack(123, 345, 2, SP, 44, 55);
	}
	
	@Test
	public void test_LI3() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xC3_LI3.execute();
		checkStack(123, 345, 3, SP, 44, 55);
	}
	
	@Test
	public void test_LI4() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xC4_LI4.execute();
		checkStack(123, 345, 4, SP, 44, 55);
	}
	
	@Test
	public void test_LI5() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xC5_LI5.execute();
		checkStack(123, 345, 5, SP, 44, 55);
	}
	
	@Test
	public void test_LI6() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xC6_LI6.execute();
		checkStack(123, 345, 6, SP, 44, 55);
	}
	
	@Test
	public void test_LI7() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xC7_LI7.execute();
		checkStack(123, 345, 7, SP, 44, 55);
	}
	
	@Test
	public void test_LI8() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xC8_LI8.execute();
		checkStack(123, 345, 8, SP, 44, 55);
	}
	
	@Test
	public void test_LI9() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xC9_LI9.execute();
		checkStack(123, 345, 9, SP, 44, 55);
	}
	
	@Test
	public void test_LI10() {
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xCA_LI10.execute();
		checkStack(123, 345, 10, SP, 44, 55);
	}
	
	/*
	 * LIB
	 */
	
	@Test
	public void test_LIBa() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xCD, PC, 4, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCD_LIB_alpha.execute();
		checkStack(123, 345, 4, SP, 44, 55);
	}
	
	@Test
	public void test_LIBb() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xCD, PC, 200, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCD_LIB_alpha.execute();
		checkStack(123, 345, 200, SP, 44, 55);
	}
	
	@Test
	public void test_LIBc() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xCD, PC, 0, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCD_LIB_alpha.execute();
		checkStack(123, 345, 0, SP, 44, 55);
	}
	
	@Test
	public void test_LIBd() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xCD, PC, 255, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCD_LIB_alpha.execute();
		checkStack(123, 345, 255, SP, 44, 55);
	}
	
	/*
	 * LINB
	 */
	
	@Test
	public void test_LINBa() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xCF, PC, 4, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCF_LINB_alpha.execute();
		checkStack(123, 345, 0xFF04, SP, 44, 55);
	}
	
	@Test
	public void test_LINBb() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xCF, PC, 0xCC, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCF_LINB_alpha.execute();
		checkStack(123, 345, 0xFFCC, SP, 44, 55);
	}
	
	@Test
	public void test_LINBc() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xCF, PC, 0, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCF_LINB_alpha.execute();
		checkStack(123, 345, 0xFF00, SP, 44, 55);
	}
	
	@Test
	public void test_LINBd() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xCF, PC, 255, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCF_LINB_alpha.execute();
		checkStack(123, 345, 0xFFFF, SP, 44, 55);
	}
	
	/*
	 * LIHB
	 */
	
	@Test
	public void test_LIHBa() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xD0, PC, 255, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xD0_LIHB_alpha.execute();
		checkStack(123, 345, 0xFF00, SP, 44, 55);
	}
	
	@Test
	public void test_LIHBb() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xD0, PC, 33, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xD0_LIHB_alpha.execute();
		checkStack(123, 345, 0x2100, SP, 44, 55);
	}
	
	@Test
	public void test_LIHBc() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xD0, PC, 0, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xD0_LIHB_alpha.execute();
		checkStack(123, 345, 0x0000, SP, 44, 55);
	}
	
	/*
	 * LIW
	 */
	
	@Test
	public void test_LIWa() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xCE, PC, 4, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCE_LIW_word.execute();
		checkStack(123, 345, 0x0405, SP, 44, 55);
	}
	
	@Test
	public void test_LIWb() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(0, 1, 2, 3, savedPC, 0xCE, PC, 4, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCE_LIW_word.execute();
		checkStack(123, 345, 0x0405, SP, 44, 55);
	}
	
	@Test
	public void test_LIWc() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xCE, PC, 0xCC, 0xEE, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCE_LIW_word.execute();
		checkStack(123, 345, 0xCCEE, SP, 44, 55);
	}
	
	@Test
	public void test_LIWd() {
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(0, 1, 2, 3, savedPC, 0xCE, PC, 0xCC, 0xEE, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xCE_LIW_word.execute();
		checkStack(123, 345, 0xCCEE, SP, 44, 55);
	}
	
	/*
	 * 7.2.1 Local Frame Access
	 */
	
	/*
	 * LAn
	 */
	
	@Test
	public void test_LA0() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xD2_LA0.execute();
		checkStack(123, 345, Cpu.LF, SP, 44, 55);
	}
	
	@Test
	public void test_LA1() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xD3_LA1.execute();
		checkStack(123, 345, Cpu.LF + 1, SP, 44, 55);
	}
	
	@Test
	public void test_LA2() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xD4_LA2.execute();
		checkStack(123, 345, Cpu.LF + 2, SP, 44, 55);
	}
	
	@Test
	public void test_LA3() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xD5_LA3.execute();
		checkStack(123, 345, Cpu.LF + 3, SP, 44, 55);
	}
	
	@Test
	public void test_LA6() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xD6_LA6.execute();
		checkStack(123, 345, Cpu.LF + 6, SP, 44, 55);
	}
	
	@Test
	public void test_LA8() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		Ch07_Assignment_Instructions.OPC_xD7_LA8.execute();
		checkStack(123, 345, Cpu.LF + 8, SP, 44, 55);
	}
	
	/*
	 * LAB
	 */

	@Test
	public void test_LABa() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xD8, PC, 4, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xD8_LAB_alpha.execute();
		checkStack(123, 345, Cpu.LF + 4, SP, 44, 55);
	}

	@Test
	public void test_LABb() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xD8, PC, 254, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xD8_LAB_alpha.execute();
		checkStack(123, 345, Cpu.LF + 254, SP, 44, 55);
	}
	
	/*
	 * LAW
	 */

	@Test
	public void test_LAWa() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xD9, PC, 2, 250, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xD9_LAW_word.execute();
		checkStack(123, 345, Cpu.LF + 762, SP, 44, 55);
	}

	@Test
	public void test_LAWb() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkCode(0, 1, 2, 3, savedPC, 0xD9, PC, 2, 251, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xD9_LAW_word.execute();
		checkStack(123, 345, Cpu.LF + 763, SP, 44, 55);
	}
	
	/*
	 * 7.2.1.1 Load Local
	 */
	
	/*
	 * LLn
	 */
	
	@Test
	public void test_LL0() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x01_LL0.execute();
		checkStack(123, 345, 0x7101, SP, 44, 55);
	}
	
	@Test
	public void test_LL1() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x02_LL1.execute();
		checkStack(123, 345, 0x7202, SP, 44, 55);
	}
	
	@Test
	public void test_LL2() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x03_LL2.execute();
		checkStack(123, 345, 0x7303, SP, 44, 55);
	}
	
	@Test
	public void test_LL3() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x04_LL3.execute();
		checkStack(123, 345, 0x7404, SP, 44, 55);
	}
	
	@Test
	public void test_LL4() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x05_LL4.execute();
		checkStack(123, 345, 0x7505, SP, 44, 55);
	}
	
	@Test
	public void test_LL5() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x06_LL5.execute();
		checkStack(123, 345, 0x7606, SP, 44, 55);
	}
	
	@Test
	public void test_LL6() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x07_LL6.execute();
		checkStack(123, 345, 0x7707, SP, 44, 55);
	}
	
	@Test
	public void test_LL7() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x08_LL7.execute();
		checkStack(123, 345, 0x7808, SP, 44, 55);
	}
	
	@Test
	public void test_LL8() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x09_LL8.execute();
		checkStack(123, 345, 0x7909, SP, 44, 55);
	}
	
	@Test
	public void test_LL9() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x0A_LL9.execute();
		checkStack(123, 345, 0x7A0A, SP, 44, 55);
	}
	
	@Test
	public void test_LL10() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x0B_LL10.execute();
		checkStack(123, 345, 0x7B0B, SP, 44, 55);
	}
	
	@Test
	public void test_LL11() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x0C_LL11.execute();
		checkStack(123, 345, 0x7C0C, SP, 44, 55);
	}
	
	/*
	 * LLB
	 */
	
	@Test
	public void test_LLB() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0x0D, PC, 14, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x0D_LLB_alpha.execute();
		checkStack(123, 345, 0x7F0F, SP, 44, 55);
	}
	
	/*
	 * LLDn
	 */
	
	@Test
	public void test_LLD0() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x0E_LLD0.execute();
		checkStack(123, 345, 0x7101, 0x7202, SP, 55);
	}
	
	@Test
	public void test_LLD1() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x0F_LLD1.execute();
		checkStack(123, 345, 0x7202, 0x7303, SP, 55);
	}
	
	@Test
	public void test_LLD2() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x10_LLD2.execute();
		checkStack(123, 345, 0x7303, 0x7404, SP, 55);
	}
	
	@Test
	public void test_LLD3() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x11_LLD3.execute();
		checkStack(123, 345, 0x7404, 0x7505, SP, 55);
	}
	
	@Test
	public void test_LLD4() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x12_LLD4.execute();
		checkStack(123, 345, 0x7505, 0x7606, SP, 55);
	}
	
	@Test
	public void test_LLD5() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x13_LLD5.execute();
		checkStack(123, 345, 0x7606, 0x7707, SP, 55);
	}
	
	@Test
	public void test_LLD6() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x14_LLD6.execute();
		checkStack(123, 345, 0x7707, 0x7808, SP, 55);
	}
	
	@Test
	public void test_LLD7() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x15_LLD7.execute();
		checkStack(123, 345, 0x7808, 0x7909, SP, 55);
	}
	
	@Test
	public void test_LLD8() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x16_LLD8.execute();
		checkStack(123, 345, 0x7909, 0x7A0A, SP, 55);
	}
	
	@Test
	public void test_LLD10() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x17_LLD10.execute();
		checkStack(123, 345, 0x7B0B, 0x7C0C, SP, 55);
	}
	
	/*
	 * LLDB
	 */
	
	@Test
	public void test_LLDB() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 33, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0x0D, PC, 13, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x18_LLDB_alpha.execute();
		checkStack(123, 345, 0x7E0E, 0x7F0F, SP, 55);
	}
	
	/*
	 * 7.2.1.2 Store Local
	 */
	
	/*
	 * SLn
	 */
	
	@Test
	public void test_SL0() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x19_SL0.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x5555, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SL1() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x1A_SL1.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x5555, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SL2() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x1B_SL2.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x5555, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SL3() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x1C_SL3.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x5555, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SL4() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x1D_SL4.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x5555, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SL5() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x1E_SL5.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x5555, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SL6() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x1F_SL6.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x5555, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SL7() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x20_SL7.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x5555, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SL8() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x21_SL8.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x5555, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SL9() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x22_SL9.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x5555, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SL10() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x23_SL10.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x5555, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	/*
	 * SLB
	 */
	
	@Test
	public void test_SLB() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0x24, PC, 14, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x24_SLB_alpha.execute();
		checkStack(123, 345, SP, 0x5555, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x5555);
	}
	
	/*
	 * SLDn
	 */
	
	@Test
	public void test_SLD0() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x25_SLD0.execute();
		checkStack(123, 345, SP, 0x5555, 0x6666, 44, 55);
		checkLocalFrame(0x5555, 0x6666, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SLD1() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x26_SLD1.execute();
		checkStack(123, 345, SP, 0x5555, 0x6666, 44, 55);
		checkLocalFrame(0x7101, 0x5555, 0x6666, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SLD2() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x27_SLD2.execute();
		checkStack(123, 345, SP, 0x5555, 0x6666, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x5555, 0x6666, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SLD3() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x28_SLD3.execute();
		checkStack(123, 345, SP, 0x5555, 0x6666, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x5555, 0x6666, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SLD4() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x29_SLD4.execute();
		checkStack(123, 345, SP, 0x5555, 0x6666, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x5555, 0x6666, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SLD5() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x2A_SLD5.execute();
		checkStack(123, 345, SP, 0x5555, 0x6666, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x5555, 0x6666, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SLD6() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x2B_SLD6.execute();
		checkStack(123, 345, SP, 0x5555, 0x6666, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x5555, 0x6666, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_SLD8() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x2C_SLD8.execute();
		checkStack(123, 345, SP, 0x5555, 0x6666, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x5555, 0x6666, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	/*
	 * SLDB
	 */
	
	@Test
	public void test_SLDB() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0x24, PC, 13, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x75_SLDB_alpha.execute();
		checkStack(123, 345, SP, 0x5555, 0x6666, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x5555, 0x6666);
	}
	
	/*
	 * 7.2.1.3 Put Local
	 */
	
	/*
	 * PLn
	 */
	
	@Test
	public void test_PL0() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x2D_PL0.execute();
		checkStack(123, 345, 0x5555, SP, 44, 55);
		checkLocalFrame(0x5555, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_PL1() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x2E_PL1.execute();
		checkStack(123, 345, 0x5555, SP, 44, 55);
		checkLocalFrame(0x7101, 0x5555, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_PL2() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x2F_PL2.execute();
		checkStack(123, 345, 0x5555, SP, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x5555, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	@Test
	public void test_PL3() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x30_PL3.execute();
		checkStack(123, 345, 0x5555, SP, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x5555, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	/*
	 * PLB
	 */
	
	@Test
	public void test_PLB() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0x24, PC, 14, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x31_PLB_alpha.execute();
		checkStack(123, 345, 0x5555, SP, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x5555);
	}
	
	/*
	 * PLD0
	 */
	
	@Test
	public void test_PLD0() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPC_x32_PLD0.execute();
		checkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		checkLocalFrame(0x5555, 0x6666, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
	}
	
	/*
	 * PLDB
	 */
	
	@Test
	public void test_PLDB() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0x24, PC, 13, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x33_PLDB_alpha.execute();
		checkStack(123, 345, 0x5555, 0x6666, SP, 44, 55);
		checkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x5555, 0x6666);
	}
	
	/*
	 * 7.2.1.4 Add Local
	 */
	
	/*
	 * AL0IB
	 */
	
	@Test
	public void test_AL0IB() {
		assertNotEquals("LF", 0, Cpu.LF);
		mkStack(123, 345, SP, 44, 55);
		mkLocalFrame(0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0xBB, PC, 0xF1, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_xBB_AL0IB_alpha.execute();
		checkStack(123, 345, 0x71F2, SP, 55);
	}

	/*
	 * 7.2.2 Global Frame Access
	 */
	
	/*
	 * GAn
	 */
	
	@Test
	public void test_GA0() {
		assertNotEquals("GF32", 0, Cpu.GF32);
		assertNotEquals("GF16", 0, Cpu.GF16);
		mkStack(123, 345, SP, 44, 55);
		Ch07_Assignment_Instructions.OPCo_xDA_GA0.execute();
		checkStack(123, 345, Cpu.GF16, SP, 55);
	}
	
	@Test
	public void test_GA1() {
		assertNotEquals("GF32", 0, Cpu.GF32);
		assertNotEquals("GF16", 0, Cpu.GF16);
		mkStack(123, 345, SP, 44, 55);
		Ch07_Assignment_Instructions.OPCo_xDB_GA1.execute();
		checkStack(123, 345, Cpu.GF16 + 1, SP, 55);
	}
	
	/*
	 * GAB
	 */
	
	@Test
	public void test_GAB() {
		assertNotEquals("GF32", 0, Cpu.GF32);
		assertNotEquals("GF16", 0, Cpu.GF16);
		mkStack(123, 345, SP, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xDC, PC, 0xFF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCo_xDC_GAB_alpha.execute();
		checkStack(123, 345, Cpu.GF16 + 255, SP, 55);
	}
	
	/*
	 * GAW
	 */
	
	@Test
	public void test_GAW() {
		assertNotEquals("GF32", 0, Cpu.GF32);
		assertNotEquals("GF16", 0, Cpu.GF16);
		mkStack(123, 345, SP, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xDC, PC, 0x33, 0x44, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCo_xDD_GAW_word.execute();
		checkStack(123, 345, (Cpu.GF16 + 0x3344) & 0xFFFF, SP, 55);
	}
	
	/*
	 * 7.2.2 Global Frame Access (the changed chapters)
	 */
	
	/*
	 * LGAn
	 */
	
	@Test
	public void test_LGA0() {
		assertNotEquals("GF32", 0, Cpu.GF32);
		assertNotEquals("GF16", 0, Cpu.GF16);
		mkStack(123, 345, SP, 44, 55);
		Ch07_Assignment_Instructions.OPCn_xFA_LGA0.execute();
		checkStack(123, 345, Cpu.GF32 & 0xFFFF, Cpu.GF32 >>> 16, SP);
	}
	
	/*
	 * LGAB
	 */
	
	@Test
	public void test_LGAB() {
		assertNotEquals("GF32", 0, Cpu.GF32);
		assertNotEquals("GF16", 0, Cpu.GF16);
		mkStack(123, 345, SP, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xDC, PC, 0xFF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCn_xFB_LGAB_alpha.execute();
		checkStack(123, 345, (Cpu.GF32 + 255) & 0xFFFF, (Cpu.GF32 + 255) >>> 16, SP);
	}
	
	/*
	 * LGAW
	 */
	
	@Test
	public void test_LGAW() {
		assertNotEquals("GF32", 0, Cpu.GF32);
		assertNotEquals("GF16", 0, Cpu.GF16);
		mkStack(123, 345, SP, 44, 55);
		mkCode(1, 2, 3, savedPC, 0xDC, PC, 0x33, 0x44, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCn_xFC_LGAW_word.execute();
		checkStack(123, 345, (Cpu.GF32 + 0x3344) & 0xFFFF, (Cpu.GF32 + 0x3344) >>> 16, SP);
	}
	
	
	
	/*
	 * 7.2.2.1 Load Global
	 */
	
	/*
	 * LGn
	 */
	
	@Test
	public void test_o_LG0() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPCo_x34_LG0.execute();
		checkStack(123, 345, 0x7000, SP, 55);
	}
	
	@Test
	public void test_n_LG0() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPCn_x34_LG0.execute();
		checkStack(123, 345, 0x7000, SP, 55);
	}
	
	@Test
	public void test_o_LG1() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPCo_x35_LG1.execute();
		checkStack(123, 345, 0x7101, SP, 55);
	}
	
	@Test
	public void test_n_LG1() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPCn_x35_LG1.execute();
		checkStack(123, 345, 0x7101, SP, 55);
	}
	
	@Test
	public void test_o_LG2() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPCo_x36_LG2.execute();
		checkStack(123, 345, 0x7202, SP, 55);
	}
	
	@Test
	public void test_n_LG2() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPCn_x36_LG2.execute();
		checkStack(123, 345, 0x7202, SP, 55);
	}
	
	/*
	 * LGB
	 */
	
	@Test
	public void test_o_LGB() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0xDC, PC, 0x0E, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCo_x37_LGB_alpha.execute();
		checkStack(123, 345, 0x7E0E, SP, 55);
	}
	
	@Test
	public void test_n_LGB() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0xDC, PC, 0x0E, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCn_x37_LGB_alpha.execute();
		checkStack(123, 345, 0x7E0E, SP, 55);
	}
	
	/*
	 * LGDn
	 */
	
	@Test
	public void test_o_LGD0() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPCo_x38_LGD0.execute();
		checkStack(123, 345, 0x7000, 0x7101, SP);
	}
	
	@Test
	public void test_n_LGD0() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPCn_x38_LGD0.execute();
		checkStack(123, 345, 0x7000, 0x7101, SP);
	}
	
	@Test
	public void test_o_LGD2() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPCo_x39_LGD2.execute();
		checkStack(123, 345, 0x7202, 0x7303, SP);
	}
	
	@Test
	public void test_n_LGD2() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		Ch07_Assignment_Instructions.OPCn_x39_LGD2.execute();
		checkStack(123, 345, 0x7202, 0x7303, SP);
	}
	
	/*
	 * LGDB
	 */
	
	@Test
	public void test_o_LGDB() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0xDC, PC, 0x0E, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCo_x3A_LGDB_alpha.execute();
		checkStack(123, 345, 0x7E0E, 0x7F0F, SP);
	}
	
	@Test
	public void test_n_LGDB() {
		mkStack(123, 345, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0xDC, PC, 0x0E, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCn_x3A_LGDB_alpha.execute();
		checkStack(123, 345, 0x7E0E, 0x7F0F, SP);
	}
	
	/*
	 * 7.2.2.2 Store Global
	 */
	
	/*
	 * SGB
	 */
	
	@Test
	public void test_o_SGB() {
		mkStack(123, 345, 0x8765, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0x3B, PC, 0x0E, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCo_x3B_SGB_alpha.execute();
		checkStack(123, 345, SP);
		checkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x8765, 0x7F0F);
	}
	
	@Test
	public void test_n_SGB() {
		mkStack(123, 345, 0x8765, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0x3B, PC, 0x0E, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCn_x3B_SGB_alpha.execute();
		checkStack(123, 345, SP);
		checkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x8765, 0x7F0F);
	}
	
	/*
	 * SGDB
	 */
	
	@Test
	public void test_o_SGDB() {
		mkStack(123, 345, 0x8765, 0x9876, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0x3B, PC, 0x0E, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCo_x76_SGDB_alpha.execute();
		checkStack(123, 345, SP);
		checkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x8765, 0x9876);
	}
	
	@Test
	public void test_n_SGDB() {
		mkStack(123, 345, 0x8765, 0x9876, SP, 44, 55);
		mkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x7E0E, 0x7F0F);
		mkCode(1, 2, 3, savedPC, 0x3B, PC, 0x0E, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCn_x76_SGDB_alpha.execute();
		checkStack(123, 345, SP);
		checkGlobalFrame(0x07000, 0x7101, 0x7202, 0x7303, 0x7404, 0x7505, 0x7606, 0x7707, 0x7808, 0x7909, 0x7A0A, 0x7B0B, 0x7C0C, 0x7D0D, 0x8765, 0x9876);
	}
	
	/*
	 * 7.3.1.1 Read Direct
	 */
	
	/*
	 * Rn
	 */
	
	@Test
	public void test_R0() {
		mkStack(22, 33, testShortMem);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		Ch07_Assignment_Instructions.OPC_x40_R0.execute();
		checkStack(22, 33, 0x1234);
	}
	
	@Test
	public void test_R1() {
		mkStack(22, 33, testShortMem);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		Ch07_Assignment_Instructions.OPC_x41_R1.execute();
		checkStack(22, 33, 0x2345);
	}
	
	/*
	 * RB
	 */
	
	@Test
	public void test_RB() {
		mkStack(22, 33, testShortMem);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		mkCode(1, 2, 3, savedPC, 0x42, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x42_RB_alpha.execute();
		checkStack(22, 33, 0x4567);
	}
	
	/*
	 * RL0
	 */
	
	@Test
	public void test_RL0() {
		mkStack(22, 33, testLongMemLow, testLongMemHigh);
		mkLongMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		Ch07_Assignment_Instructions.OPC_x43_RL0.execute();
		checkStack(22, 33, 0x1234);
	}
	
	/*
	 * RLB
	 */
	
	@Test
	public void test_RLB() {
		mkStack(22, 33, testLongMemLow, testLongMemHigh);
		mkLongMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		mkCode(1, 2, 3, savedPC, 0x42, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x44_RLB_alpha.execute();
		checkStack(22, 33, 0x4567);
	}
	
	/*
	 * RD0
	 */
	
	@Test
	public void test_RD0() {
		mkStack(22, 33, testShortMem);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		Ch07_Assignment_Instructions.OPC_x45_RD0.execute();
		checkStack(22, 33, 0x1234, 0x2345);
	}
	
	/*
	 * RDB
	 */
	
	@Test
	public void test_RDB() {
		mkStack(22, 33, testShortMem);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		mkCode(1, 2, 3, savedPC, 0x42, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x46_RDB_alpha.execute();
		checkStack(22, 33, 0x4567, 0x5678);
	}
	
	/*
	 * RDL0
	 */
	
	@Test
	public void test_RDL0() {
		mkStack(22, 33, testLongMemLow, testLongMemHigh);
		mkLongMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		Ch07_Assignment_Instructions.OPC_x47_RDL0.execute();
		checkStack(22, 33, 0x1234, 0x2345);
	}
	
	/*
	 * RDLB
	 */
	
	@Test
	public void test_RDLB() {
		mkStack(22, 33, testLongMemLow, testLongMemHigh);
		mkLongMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		mkCode(1, 2, 3, savedPC, 0x48, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x48_RDLB_alpha.execute();
		checkStack(22, 33, 0x4567, 0x5678);
	}
	
	/*
	 * RC
	 */
	
	@Test
	public void test_RC() {
		Mem.writeWord(Cpu.CB + 48, (short)0x9283);
		mkStack(22, 33, 44);
		mkCode(1, 2, 3, savedPC, 0x00, 0x48, PC, 0x04, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.ESC_x1B_RC_alpha.execute();
		checkStack(22, 33, 0x9283);
	}
	
	/*
	 * 7.3.1.2 Write Direct
	 */
	
	/*
	 * W0
	 */
	
	@Test
	public void test_W0() {
		mkStack(22, 33, 0x7654, testShortMem);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		Ch07_Assignment_Instructions.OPC_x49_W0.execute();
		checkStack(22, 33);
		checkShortMem(0x7654, 0x2345, 0x3456, 0x4567, 0x5678);
	}
	
	/*
	 * WB
	 */
	
	@Test
	public void test_WB() {
		mkStack(22, 33, 0x7654, testShortMem);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		mkCode(1, 2, 3, savedPC, 0x4A, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x4A_WB_alpha.execute();
		checkStack(22, 33);
		checkShortMem(0x1234, 0x2345, 0x3456, 0x7654, 0x5678);
	}
	
	/*
	 * WLB
	 */
	
	@Test
	public void test_WLB() {
		mkStack(22, 33, 0x7654, testLongMemLow, testLongMemHigh);
		mkLongMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		mkCode(1, 2, 3, savedPC, 0x4C, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x4C_WLB_alpha.execute();
		checkStack(22, 33);
		checkLongMem(0x1234, 0x2345, 0x3456, 0x7654, 0x5678);
	}
	
	/*
	 * WDB
	 */
	
	@Test
	public void test_WDB() {
		mkStack(22, 33, 0x7654, 0xABCD, testShortMem);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789);
		mkCode(1, 2, 3, savedPC, 0x4E, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x4E_WDB_alpha.execute();
		checkStack(22, 33);
		checkShortMem(0x1234, 0x2345, 0x3456, 0x7654, 0xABCD, 0x6789);
	}
	
	/*
	 * WDLB
	 */
	
	@Test
	public void test_WDLB() {
		mkStack(22, 33, 0x7654, 0xABCD, testLongMemLow, testLongMemHigh);
		mkLongMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678, 0x6789);
		mkCode(1, 2, 3, savedPC, 0x51, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x51_WDLB_alpha.execute();
		checkStack(22, 33);
		checkLongMem(0x1234, 0x2345, 0x3456, 0x7654, 0xABCD, 0x6789);
	}
	
	/*
	 * 7.3.1.3 Put Swapped Direct
	 */
	
	/*
	 * PSB
	 */
	
	@Test
	public void test_PSB() {
		mkStack(22, 33, testShortMem, 0x7654);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		mkCode(1, 2, 3, savedPC, 0x4B, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x4B_PSB_alpha.execute();
		checkStack(22, 33, testShortMem);
		checkShortMem(0x1234, 0x2345, 0x3456, 0x7654, 0x5678);
	}
	
	/*
	 * PSD0
	 */
	
	@Test
	public void test_PSD0() {
		mkStack(22, 33, testShortMem, 0x7654, 0xFEDC);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		Ch07_Assignment_Instructions.OPC_x4F_PSD0.execute();
		checkStack(22, 33, testShortMem);
		checkShortMem(0x7654, 0xFEDC, 0x3456, 0x4567, 0x5678);
	}
	
	/*
	 * PSDB
	 */
	
	@Test
	public void test_PSDB() {
		mkStack(22, 33, testShortMem, 0x7654, 0xFEDC);
		mkShortMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		mkCode(1, 2, 3, savedPC, 0x50, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x50_PSDB_alpha.execute();
		checkStack(22, 33, testShortMem);
		checkShortMem(0x1234, 0x2345, 0x3456, 0x7654, 0xFEDC);
	}
	
	/*
	 * PSLB
	 */
	
	@Test
	public void test_PSLB() {
		mkStack(22, 33, testLongMemLow, testLongMemHigh, 0x7654);
		mkLongMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		mkCode(1, 2, 3, savedPC, 0x4B, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x4D_PSLB_alpha.execute();
		checkStack(22, 33, testLongMemLow, testLongMemHigh);
		checkLongMem(0x1234, 0x2345, 0x3456, 0x7654, 0x5678);
	}
	
	/*
	 * PSDLB
	 */
	
	@Test
	public void test_PSDLB() {
		mkStack(22, 33, testLongMemLow, testLongMemHigh, 0x7654, 0xFEDC);
		mkLongMem(0x1234, 0x2345, 0x3456, 0x4567, 0x5678);
		mkCode(1, 2, 3, savedPC, 0x52, PC, 0x03, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x52_PSDLB_alpha.execute();
		checkStack(22, 33, testLongMemLow, testLongMemHigh);
		checkLongMem(0x1234, 0x2345, 0x3456, 0x7654, 0xFEDC);
	}
	
	/*
	 * 7.3.2.1 Read Indirect
	 */
	
	/*
	 * RLI0n
	 */
	
	@Test
	public void test_RLI00() {
		mkLocalFrame(testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933);
		mkStack(22, 33);
		Ch07_Assignment_Instructions.OPC_x53_RLI00.execute();
		checkStack(22, 33, 0x9393);
	}
	
	@Test
	public void test_RLI01() {
		mkLocalFrame(testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933);
		mkStack(22, 33);
		Ch07_Assignment_Instructions.OPC_x54_RLI01.execute();
		checkStack(22, 33, 0x2233);
	}
	
	@Test
	public void test_RLI02() {
		mkLocalFrame(testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933);
		mkStack(22, 33);
		Ch07_Assignment_Instructions.OPC_x55_RLI02.execute();
		checkStack(22, 33, 0x5858);
	}
	
	@Test
	public void test_RLI03() {
		mkLocalFrame(testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933);
		mkStack(22, 33);
		Ch07_Assignment_Instructions.OPC_x56_RLI03.execute();
		checkStack(22, 33, 0x3737);
	}
	
	/*
	 * RLIP
	 */
	
	@Test
	public void test_RLIP_x15() {
		mkLocalFrame(333, testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x57, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x57_RLIP_pair.execute();
		checkStack(22, 33, 0x0505);
	}
	
	@Test
	public void test_RLIP_x9C() {
		mkLocalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x57, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x57_RLIP_pair.execute();
		checkStack(22, 33, 0x4444);
	}
	
	/*
	 * RLILP
	 */
	
	@Test
	public void test_RLILP_x15() {
		mkLocalFrame(333, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x58, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x58_RLILP_pair.execute();
		checkStack(22, 33, 0x0505);
	}
	
	@Test
	public void test_RLILP_x9C() {
		mkLocalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x58, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x58_RLILP_pair.execute();
		checkStack(22, 33, 0x4444);
	}
	
	/*
	 * RGIP
	 */
	
	@Test
	public void test_o_RGIP_x15() {
		mkGlobalFrame(333, testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5C, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCo_x5C_RGIP_pair.execute();
		checkStack(22, 33, 0x0505);
	}
	
	@Test
	public void test_n_RGIP_x15() {
		mkGlobalFrame(333, testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5C, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCn_x5C_RGIP_pair.execute();
		checkStack(22, 33, 0x0505);
	}
	
	@Test
	public void test_o_RGIP_x9C() {
		mkGlobalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5C, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCo_x5C_RGIP_pair.execute();
		checkStack(22, 33, 0x4444);
	}
	
	@Test
	public void test_n_RGIP_x9C() {
		mkGlobalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5C, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCn_x5C_RGIP_pair.execute();
		checkStack(22, 33, 0x4444);
	}
	
	/*
	 * RGILP
	 */
	
	@Test
	public void test_o_RGILP_x15() {
		mkGlobalFrame(333, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5D, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCo_x5D_RGILP_pair.execute();
		checkStack(22, 33, 0x0505);
	}
	
	@Test
	public void test_n_RGILP_x15() {
		mkGlobalFrame(333, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5D, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCn_x5D_RGILP_pair.execute();
		checkStack(22, 33, 0x0505);
	}
	
	@Test
	public void test_o_RGILP_x9C() {
		mkGlobalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5D, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCo_x5D_RGILP_pair.execute();
		checkStack(22, 33, 0x4444);
	}
	
	@Test
	public void test_n_RGILP_x9C() {
		mkGlobalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5D, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPCn_x5D_RGILP_pair.execute();
		checkStack(22, 33, 0x4444);
	}
	
	/*
	 * RLDI00
	 */
	
	@Test
	public void test_RLDI00() {
		mkLocalFrame(testShortMem + 3, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		Ch07_Assignment_Instructions.OPC_x59_RLDI00.execute();
		checkStack(22, 33, 0x3737, 0x1616);
	}
	
	/*
	 * RLDIP
	 */
	
	@Test
	public void test_RLDIP_x15() {
		mkLocalFrame(333, testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5A, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x5A_RLDIP_pair.execute();
		checkStack(22, 33, 0x0505, 0x8181);
	}
	
	@Test
	public void test_RLDIP_x9C() {
		mkLocalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5A, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x5A_RLDIP_pair.execute();
		checkStack(22, 33, 0x4444, 0x5555);
	}
	
	/*
	 * RLDILP
	 */
	
	@Test
	public void test_RLDILP_x15() {
		mkLocalFrame(333, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5B, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x5B_RLDILP_pair.execute();
		checkStack(22, 33, 0x0505, 0x8181);
	}
	
	@Test
	public void test_RLDILP_x9C() {
		mkLocalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33);
		mkCode(1, 2, 3, savedPC, 0x5B, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x5B_RLDILP_pair.execute();
		checkStack(22, 33, 0x4444, 0x5555);
	}
	
	/*
	 * 7.3.2.2 	Write Indirect
	 */
	
	/*
	 * WLIP
	 */
	
	@Test
	public void test_WLIP_x15() {
		mkLocalFrame(333, testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33, 0xFEFE);
		mkCode(1, 2, 3, savedPC, 0x57, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x5E_WLIP_pair.execute();
		checkStack(22, 33);
		checkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0xFEFE, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
	}
	
	@Test
	public void test_WLIP_x9C() {
		mkLocalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testShortMem, 111, 222);
		mkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33, 0xEFEF);
		mkCode(1, 2, 3, savedPC, 0x5E, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x5E_WLIP_pair.execute();
		checkStack(22, 33);
		checkShortMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0xEFEF, 0x5555);
	}
	
	/*
	 * WLILP
	 */
	
	@Test
	public void test_WLILP_x15() {
		mkLocalFrame(333, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33, 0xDEDE);
		mkCode(1, 2, 3, savedPC, 0x5F, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x5F_WLILP_pair.execute();
		checkStack(22, 33);
		checkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0xDEDE, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
	}
	
	@Test
	public void test_WLILP_x9C() {
		mkLocalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33, 0xEDED);
		mkCode(1, 2, 3, savedPC, 0x5F, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x5F_WLILP_pair.execute();
		checkStack(22, 33);
		checkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0xEDED, 0x5555);
	}
	
	/*
	 * WLDILP
	 */
	
	@Test
	public void test_WLDILP_x15() {
		mkLocalFrame(333, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33, 0xBBAA, 0xCCDD);
		mkCode(1, 2, 3, savedPC, 0x60, PC, 0x15, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x60_WLDILP_pair.execute();
		checkStack(22, 33);
		checkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0xBBAA, 0xCCDD, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
	}
	
	@Test
	public void test_WLDILP_x9C() {
		mkLocalFrame(111, 222, 333, 444, 555, 666, 777, 888, 999, testLongMemLow, testLongMemHigh, 111, 222);
		mkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0x4444, 0x5555);
		mkStack(22, 33, 0xFFEE, 0xDDCC);
		mkCode(1, 2, 3, savedPC, 0x60, PC, 0x9C, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x60_WLDILP_pair.execute();
		checkStack(22, 33);
		checkLongMem(0x9393, 0x2233, 0x5858, 0x3737, 0x1616, 0x0505, 0x8181, 0x9933, 0x1234, 0x4321, 0x2222, 0x3333, 0xFFEE, 0xDDCC);
	}
	
	/*
	 * 7.4.1 Read String
	 */
	
	/*
	 * RS
	 */
	
	@Test
	public void test_RS_alpha4_index3() {
		mkShortMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, testShortMem + 1, 3);
		mkCode(1, 2, 3, savedPC, 0x61, PC, 0x04, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x61_RS_alpha.execute();
		checkStack(11, 22, 0x0008);
	}
	
	@Test
	public void test_RS_alpha4_index16() {
		mkShortMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, testShortMem + 1, 16);
		mkCode(1, 2, 3, savedPC, 0x61, PC, 0x04, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x61_RS_alpha.execute();
		checkStack(11, 22, 0x0015);
	}
	
	@Test
	public void test_RS_alpha0_index0() {
		mkShortMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, testShortMem + 1, 0);
		mkCode(1, 2, 3, savedPC, 0x61, PC, 0x00, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x61_RS_alpha.execute();
		checkStack(11, 22, 0x0001);
	}
	
	/*
	 * RLS
	 */
	
	@Test
	public void test_RLS_alpha4_index3() {
		mkLongMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, testLongMemLow + 1, testLongMemHigh, 3);
		mkCode(1, 2, 3, savedPC, 0x61, PC, 0x04, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x62_RLS_alpha.execute();
		checkStack(11, 22, 0x0008);
	}
	
	@Test
	public void test_RLS_alpha4_index3_hbit() {
		mkLongMem(0, 0x8182, 0x8384, 0x8586, 0x8788, 0x898A, 0x8B8C, 0x8D8E, 0x8F90, 0x9192, 0x9394, 0x9596, 0x9798);
		mkStack(11, 22, testLongMemLow + 1, testLongMemHigh, 3);
		mkCode(1, 2, 3, savedPC, 0x61, PC, 0x04, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x62_RLS_alpha.execute();
		checkStack(11, 22, 0x0088);
	}
	
	@Test
	public void test_RLS_alpha5_index3_hbit() {
		mkLongMem(0, 0x8182, 0x8384, 0x8586, 0x8788, 0x898A, 0x8B8C, 0x8D8E, 0x8F90, 0x9192, 0x9394, 0x9596, 0x9798);
		mkStack(11, 22, testLongMemLow + 1, testLongMemHigh, 3);
		mkCode(1, 2, 3, savedPC, 0x61, PC, 0x05, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x62_RLS_alpha.execute();
		checkStack(11, 22, 0x0089);
	}
	
	@Test
	public void test_RLS_alpha4_index16() {
		mkLongMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, testLongMemLow + 1, testLongMemHigh, 16);
		mkCode(1, 2, 3, savedPC, 0x62, PC, 0x04, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x62_RLS_alpha.execute();
		checkStack(11, 22, 0x0015);
	}
	
	@Test
	public void test_RLS_alpha0_index0() {
		mkLongMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, testLongMemLow + 1, testLongMemHigh, 0);
		mkCode(1, 2, 3, savedPC, 0x62, PC, 0x00, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x62_RLS_alpha.execute();
		checkStack(11, 22, 0x0001);
	}
	
	/*
	 * 7.4.2 Write String
	 */
	
	/*
	 * RS
	 */
	
	@Test
	public void test_WS_alpha4_index3() {
		mkShortMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, 0xAA33, testShortMem + 1, 3);
		mkCode(1, 2, 3, savedPC, 0x63, PC, 0x04, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x63_WS_alpha.execute();
		checkShortMem(0, 0x0102, 0x0304, 0x0506, 0x0733, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		checkStack(11, 22);
	}
	
	@Test
	public void test_WS_alpha4_index16() {
		mkShortMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, 0xAA44, testShortMem + 1, 16);
		mkCode(1, 2, 3, savedPC, 0x63, PC, 0x04, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x63_WS_alpha.execute();
		checkShortMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x4416, 0x1718);
		checkStack(11, 22);
	}
	
	@Test
	public void test_WS_alpha0_index0() {
		mkShortMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, 0xAA55, testShortMem + 1, 0);
		mkCode(1, 2, 3, savedPC, 0x63, PC, 0x00, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x63_WS_alpha.execute();
		checkShortMem(0, 0x5502, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		checkStack(11, 22);
	}
	
	/*
	 * RLS
	 */
	
	@Test
	public void test_WLS_alpha4_index3() {
		mkLongMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, 0xAA55, testLongMemLow + 1, testLongMemHigh, 3);
		mkCode(1, 2, 3, savedPC, 0x64, PC, 0x04, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x64_WLS_alpha.execute();
		checkLongMem(0, 0x0102, 0x0304, 0x0506, 0x0755, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		checkStack(11, 22);
	}
	
	@Test
	public void test_WLS_alpha4_index16() {
		mkLongMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, 0xAA66, testLongMemLow + 1, testLongMemHigh, 16);
		mkCode(1, 2, 3, savedPC, 0x64, PC, 0x04, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x64_WLS_alpha.execute();
		checkLongMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x6616, 0x1718);
		checkStack(11, 22);
	}
	
	@Test
	public void test_WLS_alpha0_index0() {
		mkLongMem(0, 0x0102, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		mkStack(11, 22, 0xAA77, testLongMemLow + 1, testLongMemHigh, 0);
		mkCode(1, 2, 3, savedPC, 0x64, PC, 0x00, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x64_WLS_alpha.execute();
		checkLongMem(0, 0x7702, 0x0304, 0x0506, 0x0708, 0x090A, 0x0B0C, 0x0D0E, 0x0F10, 0x1112, 0x1314, 0x1516, 0x1718);
		checkStack(11, 22);
	}
	
	/*
	 * 7.5 Field Instructions
	 */
	
	private int mkFieldSpec(int pos, int width) {
		if (width > 16 || pos > 15) {
			fail("invalid field spec");
		}
		int spec = (pos << 4) | ((width - 1) & 0x0F);
		return spec & 0xFF;
	}
	
	private int mkFieldDesc(int offset, int fieldSpec) {
		return (offset << 8) | fieldSpec;
	}
	
	private int mkFieldDesc(int offset, int pos, int width) {
		return mkFieldDesc(offset, mkFieldSpec(pos, width));
	}
	
	/*
	 * 7.5.1 Read Field
	 */
	
	/*
	 * RF
	 */
	
	@Test
	public void test_RF_a() {
		int desc = mkFieldDesc(2, 2, 4);
		mkShortMem(0x0000, 0x0000, 0b0011110000000000);
		mkStack(11, 22, testShortMem);
		mkCode(1, 2, 3, savedPC, 0x66, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x66_RF_word.execute();
		checkStack(11, 22, 0x000F);
	}
	
	@Test
	public void test_RF_b() {
		int desc = mkFieldDesc(2, 2, 4);
		mkShortMem(0xFFFF, 0xFFFF, 0b1100001111111111, 0xFFFF, 0xFFFF);
		mkStack(11, 22, testShortMem);
		mkCode(1, 2, 3, savedPC, 0x66, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x66_RF_word.execute();
		checkStack(11, 22, 0x0000);
	}
	
	/*
	 * R0F
	 */
	
	@Test
	public void test_R0F_a() {
		int spec = mkFieldSpec(1, 9);
		mkShortMem(0x0000, 0x0000, 0b0111111111000000);
		mkStack(11, 22, testShortMem + 2);
		mkCode(1, 2, 3, savedPC, 0x66, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x65_R0F_alpha.execute();
		checkStack(11, 22, 0x01FF);
	}
	
	@Test
	public void test_R0F_b() {
		int spec = mkFieldSpec(1, 9);
		mkShortMem(0xFFFF, 0xFFFF, 0b1000000000111111, 0xFFFF, 0xFFFF);
		mkStack(11, 22, testShortMem + 2);
		mkCode(1, 2, 3, savedPC, 0x65, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x65_R0F_alpha.execute();
		checkStack(11, 22, 0x0000);
	}
	
	/*
	 * RLF
	 */
	
	@Test
	public void test_RLF_a() {
		int desc = mkFieldDesc(2, 5, 5);
		mkLongMem(0x0000, 0x0000, 0b0000011111000000);
		mkStack(11, 22, testLongMemLow, testLongMemHigh);
		mkCode(1, 2, 3, savedPC, 0x68, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x68_RLF_word.execute();
		checkStack(11, 22, 0x001F);
	}
	
	@Test
	public void test_RLF_b() {
		int desc = mkFieldDesc(2, 5, 5);
		mkLongMem(0xFFFF, 0xFFFF, 0b1111100000111111, 0xFFFF, 0xFFFF);
		mkStack(11, 22, testLongMemLow, testLongMemHigh);
		mkCode(1, 2, 3, savedPC, 0x68, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x68_RLF_word.execute();
		checkStack(11, 22, 0x0000);
	}
	
	/*
	 * RL0F
	 */
	
	@Test
	public void test_RL0F_a() {
		int spec = mkFieldSpec(1, 15);
		mkLongMem(0x0000, 0x0000, 0b0111111111111111);
		mkStack(11, 22, testLongMemLow + 2, testLongMemHigh);
		mkCode(1, 2, 3, savedPC, 0x67, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x67_RL0F_alpha.execute();
		checkStack(11, 22, 0x7FFF);
	}
	
	@Test
	public void test_RL0F_b() {
		int spec = mkFieldSpec(1, 15);
		mkLongMem(0xFFFF, 0xFFFF, 0b1000000000000000, 0xFFFF, 0xFFFF);
		mkStack(11, 22, testLongMemLow + 2, testLongMemHigh);
		mkCode(1, 2, 3, savedPC, 0x67, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x67_RL0F_alpha.execute();
		checkStack(11, 22, 0x0000);
	}
	
	/*
	 * RLFS
	 */
	
	@Test
	public void test_RLFS_a() {
		int desc = mkFieldDesc(2, 0, 15);
		mkLongMem(0x0000, 0x0000, 0b1111111111111110);
		mkStack(11, 22, testLongMemLow, testLongMemHigh, desc);
		mkCode(1, 2, 3, savedPC, 0x67, PC, 0xFFFF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x69_RLFS.execute();
		checkStack(11, 22, 0x7FFF);
	}
	
	@Test
	public void test_RLFS_b() {
		int desc = mkFieldDesc(2, 0, 15);
		mkLongMem(0xFFFF, 0xFFFF, 0b0000000000000001, 0xFFFF, 0xFFFF);
		mkStack(11, 22, testLongMemLow, testLongMemHigh, desc);
		mkCode(1, 2, 3, savedPC, 0x69, PC, 0xFFFF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x69_RLFS.execute();
		checkStack(11, 22, 0x0000);
	}
	
	/*
	 * RCFS
	 */
	
	@Test
	public void test_RCFS_a() {
		int desc = mkFieldDesc(2, 11, 5);
		mkStack(11, 22, 4, desc); // 4 = *word* offset from CB
		mkCode(1, 2, 3, savedPC, 0x69, PC, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0b00000000, 0b00011111, 0, 0);
		Ch07_Assignment_Instructions.ESC_x1A_RCFS.execute();
		checkStack(11, 22, 0x001F);
	}
	
	@Test
	public void test_RCFS_b() {
		int desc = mkFieldDesc(2, 11, 5);
		mkStack(11, 22, 4, desc); // 4 = *word* offset from CB
		mkCode(1, 2, 3, savedPC, 0x69, PC, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xff, 0b11111111, 0b11100000, 0xFF, 0xFF);
		Ch07_Assignment_Instructions.ESC_x1A_RCFS.execute();
		checkStack(11, 22, 0x0000);
	}
	
	/*
	 * RLIPF
	 */
	
	@Test
	public void test_RLIPF_a() {
		int spec = mkFieldSpec(14, 1);
		mkShortMem(0x0000, 0x0000, 0x0000, 0b0000000000000010);
		mkLocalFrame(0x3344, 0x5566, 0x7788, 0x99AA, testShortMem);
		mkStack(11, 22);
		mkCode(1, 2, 3, savedPC, 0x6A, PC, 0x43, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6A_RLIPF_alphabeta.execute();
		checkStack(11, 22, 0x0001);
	}
	
	@Test
	public void test_RLIPF_b() {
		int spec = mkFieldSpec(14, 1);
		mkShortMem(0xFFFF, 0xFFFF, 0xFFFF, 0b1111111111111101, 0xFFFF);
		mkLocalFrame(0x3344, 0x5566, 0x7788, 0x99AA, testShortMem);
		mkStack(11, 22);
		mkCode(1, 2, 3, savedPC, 0x6A, PC, 0x43, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6A_RLIPF_alphabeta.execute();
		checkStack(11, 22, 0x0000);
	}
	
	/*
	 * RLILPF
	 */
	
	@Test
	public void test_RLILPF_a() {
		int spec = mkFieldSpec(0, 1);
		mkLongMem(0x0000, 0x0000, 0x0000, 0b1000000000000000);
		mkLocalFrame(0x3344, 0x5566, 0x7788, 0x99AA, testLongMemLow, testLongMemHigh);
		mkStack(11, 22);
		mkCode(1, 2, 3, savedPC, 0x6B, PC, 0x43, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6B_RLILPF_alphabeta.execute();
		checkStack(11, 22, 0x0001);
	}
	
	@Test
	public void test_RLILPF_b() {
		int spec = mkFieldSpec(0, 1);
		mkLongMem(0xFFFF, 0xFFFF, 0xFFFF, 0b0111111111111111, 0xFFFF);
		mkLocalFrame(0x3344, 0x5566, 0x7788, 0x99AA, testLongMemLow, testLongMemHigh);
		mkStack(11, 22);
		mkCode(1, 2, 3, savedPC, 0x6B, PC, 0x43, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6B_RLILPF_alphabeta.execute();
		checkStack(11, 22, 0x0000);
	}
	
	/*
	 * 7.5.2 Write Field
	 */
	
	/*
	 * WF
	 */
	
	@Test
	public void test_WF_a() {
		int desc = mkFieldDesc(2, 2, 4);
		mkShortMem(0x0000, 0x0000, 0b0000000000000000, 0x0000);
		mkStack(11, 22, 0xFFFF, testShortMem);
		mkCode(1, 2, 3, savedPC, 0x6D, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6D_WF_word.execute();
		checkStack(11, 22);
		checkShortMem(0x0000, 0x0000, 0b0011110000000000, 0x0000);
	}
	
	@Test
	public void test_WF_b() {
		int desc = mkFieldDesc(2, 2, 4);
		mkShortMem(0xFFFF, 0xFFFF, 0b1111111111111111, 0xFFFF);
		mkStack(11, 22, 0xFFE0, testShortMem);
		mkCode(1, 2, 3, savedPC, 0x6D, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6D_WF_word.execute();
		checkStack(11, 22);
		checkShortMem(0xFFFF, 0xFFFF, 0b1100001111111111, 0xFFFF);
	}
	
	/*
	 * W0F
	 */
	
	@Test
	public void test_W0F_a() {
		int spec = mkFieldSpec(1, 9);
		mkShortMem(0x0000, 0x0000, 0b0000000000000000, 0x0000);
		mkStack(11, 22, 0xFFFF, testShortMem + 2);
		mkCode(1, 2, 3, savedPC, 0x6C, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6C_W0F_alpha.execute();
		checkStack(11, 22);
		checkShortMem(0x0000, 0x0000, 0b0111111111000000, 0x0000);
	}
	
	@Test
	public void test_W0F_b() {
		int spec = mkFieldSpec(1, 9);
		mkShortMem(0xFFFF, 0xFFFF, 0b1111111111111111, 0xFFFF);
		mkStack(11, 22, 0xFC00, testShortMem + 2);
		mkCode(1, 2, 3, savedPC, 0x6C, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6C_W0F_alpha.execute();
		checkStack(11, 22);
		checkShortMem(0xFFFF, 0xFFFF, 0b1000000000111111, 0xFFFF);
	}
	
	/*
	 * WLF
	 */
	
	@Test
	public void test_WLF_a() {
		int desc = mkFieldDesc(2, 13, 3);
		mkLongMem(0x0000, 0x0000, 0b0000000000000000, 0x0000);
		mkStack(11, 22, 0xFFFF, testLongMemLow, testLongMemHigh);
		mkCode(1, 2, 3, savedPC, 0x72, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x72_WLF_word.execute();
		checkStack(11, 22);
		checkLongMem(0x0000, 0x0000, 0b0000000000000111, 0x0000);
	}
	
	@Test
	public void test_WLF_b() {
		int desc = mkFieldDesc(2, 13, 3);
		mkLongMem(0xFFFF, 0xFFFF, 0b1111111111111111, 0xFFFF);
		mkStack(11, 22, 0xFFF0, testLongMemLow, testLongMemHigh);
		mkCode(1, 2, 3, savedPC, 0x72, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x72_WLF_word.execute();
		checkStack(11, 22);
		checkLongMem(0xFFFF, 0xFFFF, 0b1111111111111000, 0xFFFF);
	}
	
	/*
	 * WL0F
	 */
	
	@Test
	public void test_WL0F_a() {
		int spec = mkFieldSpec(0, 2);
		mkLongMem(0x0000, 0x0000, 0b0000000000000000, 0x0000);
		mkStack(11, 22, 0xFFFF, testLongMemLow + 2, testLongMemHigh);
		mkCode(1, 2, 3, savedPC, 0x71, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x71_WL0F_alpha.execute();
		checkStack(11, 22);
		checkLongMem(0x0000, 0x0000, 0b1100000000000000, 0x0000);
	}
	
	@Test
	public void test_WL0F_b() {
		int spec = mkFieldSpec(0, 2);
		mkLongMem(0xFFFF, 0xFFFF, 0b1111111111111111, 0xFFFF);
		mkStack(11, 22, 0xFFF8, testLongMemLow + 2, testLongMemHigh);
		mkCode(1, 2, 3, savedPC, 0x71, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x71_WL0F_alpha.execute();
		checkStack(11, 22);
		checkLongMem(0xFFFF, 0xFFFF, 0b0011111111111111, 0xFFFF);
	}
	
	/*
	 * WLFS
	 */
	
	@Test
	public void test_WLFS_a() {
		int desc = mkFieldDesc(2, 13, 3);
		mkLongMem(0x0000, 0x0000, 0b0000000000000000, 0x0000);
		mkStack(11, 22, 0xFFFF, testLongMemLow, testLongMemHigh, desc);
		mkCode(1, 2, 3, savedPC, 0x74, PC, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x74_WLFS.execute();
		checkStack(11, 22);
		checkLongMem(0x0000, 0x0000, 0b0000000000000111, 0x0000);
	}
	
	@Test
	public void test_WLFS_b() {
		int desc = mkFieldDesc(2, 13, 3);
		mkLongMem(0xFFFF, 0xFFFF, 0b1111111111111111, 0xFFFF);
		mkStack(11, 22, 0xFFF0, testLongMemLow, testLongMemHigh, desc);
		mkCode(1, 2, 3, savedPC, 0x74, PC, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x74_WLFS.execute();
		checkStack(11, 22);
		checkLongMem(0xFFFF, 0xFFFF, 0b1111111111111000, 0xFFFF);
	}
	
	/*
	 * WS0F
	 */
	
	@Test
	public void test_WS0F_a() {
		int spec = mkFieldSpec(4, 9);
		mkShortMem(0x0000, 0x0000, 0b0000000000000000, 0x0000);
		mkStack(11, 22, testShortMem + 2, 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x70, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x70_WS0F_alpha.execute();
		checkStack(11, 22);
		checkShortMem(0x0000, 0x0000, 0b0000111111111000, 0x0000);
	}
	
	@Test
	public void test_WS0F_b() {
		int spec = mkFieldSpec(4, 9);
		mkShortMem(0xFFFF, 0xFFFF, 0b1111111111111111, 0xFFFF);
		mkStack(11, 22, testShortMem + 2, 0xFC00);
		mkCode(1, 2, 3, savedPC, 0x70, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x70_WS0F_alpha.execute();
		checkStack(11, 22);
		checkShortMem(0xFFFF, 0xFFFF, 0b1111000000000111, 0xFFFF);
	}
	
	/*
	 * 7.5.3 Put Swapped Field
	 */
	
	/*
	 * PS0F
	 */
	
	@Test
	public void test_PS0F_a() {
		int spec = mkFieldSpec(4, 9);
		mkShortMem(0x0000, 0x0000, 0b0000000000000000, 0x0000);
		mkStack(11, 22, testShortMem + 2, 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x6F, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6F_PS0F.execute();
		checkStack(11, 22, testShortMem + 2);
		checkShortMem(0x0000, 0x0000, 0b0000111111111000, 0x0000);
	}
	
	@Test
	public void test_PS0F_b() {
		int spec = mkFieldSpec(4, 9);
		mkShortMem(0xFFFF, 0xFFFF, 0b1111111111111111, 0xFFFF);
		mkStack(11, 22, testShortMem + 2, 0xFC00);
		mkCode(1, 2, 3, savedPC, 0x6F, PC, spec, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6F_PS0F.execute();
		checkStack(11, 22, testShortMem + 2);
		checkShortMem(0xFFFF, 0xFFFF, 0b1111000000000111, 0xFFFF);
	}
	
	/*
	 * PSF
	 */
	
	@Test
	public void test_PSF_a() {
		int desc = mkFieldDesc(2, 2, 4);
		mkShortMem(0x0000, 0x0000, 0b0000000000000000, 0x0000);
		mkStack(11, 22, testShortMem, 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x6E, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6E_PSF_word.execute();
		checkStack(11, 22, testShortMem);
		checkShortMem(0x0000, 0x0000, 0b0011110000000000, 0x0000);
	}
	
	@Test
	public void test_PSF_b() {
		int desc = mkFieldDesc(2, 2, 4);
		mkShortMem(0xFFFF, 0xFFFF, 0b1111111111111111, 0xFFFF);
		mkStack(11, 22, testShortMem, 0xFFE0);
		mkCode(1, 2, 3, savedPC, 0x6E, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x6E_PSF_word.execute();
		checkStack(11, 22, testShortMem);
		checkShortMem(0xFFFF, 0xFFFF, 0b1100001111111111, 0xFFFF);
	}
	
	/*
	 * PSLF
	 */
	
	@Test
	public void test_PSLF_a() {
		int desc = mkFieldDesc(2, 12, 3);
		mkLongMem(0x0000, 0x0000, 0b0000000000000000, 0x0000);
		mkStack(11, 22, testLongMemLow, testLongMemHigh, 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x73, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x73_PSLF_word.execute();
		checkStack(11, 22, testLongMemLow, testLongMemHigh);
		checkLongMem(0x0000, 0x0000, 0b0000000000001110, 0x0000);
	}
	
	@Test
	public void test_WPSLF_b() {
		int desc = mkFieldDesc(2, 12, 3);
		mkLongMem(0xFFFF, 0xFFFF, 0b1111111111111111, 0xFFFF);
		mkStack(11, 22, testLongMemLow, testLongMemHigh, 0xFFF0);
		mkCode(1, 2, 3, savedPC, 0x73, PC, desc >>> 8, desc & 0x00FF, 5, 6, 7, 8);
		Ch07_Assignment_Instructions.OPC_x73_PSLF_word.execute();
		checkStack(11, 22, testLongMemLow, testLongMemHigh);
		checkLongMem(0xFFFF, 0xFFFF, 0b1111111111110001, 0xFFFF);
	}
	
}
