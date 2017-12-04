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
import dev.hawala.dmachine.engine.Opcodes.OpImpl;
import dev.hawala.dmachine.engine.opcodes.Ch06_Jump_Instructions;

/**
 * Unittests for instructions implemented in class Ch06_Jump_Instructions.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Ch06_JumpInsnsTest extends AbstractInstructionTest {
	
	// constant for the "distance" identifying a non-jump 
	private final int NOJUMP = 0xFFFFFFFF;

	/*
	 * 6.1 Unconditional Jumps ******************************************************************************************* 
	 */

	
	/*
	 * Jn
	 */
	
	private void innerTestJn(OpImpl op, int distance) {
		Cpu.savedPC = 33;
		op.execute();
		assertEquals("PC <- savedPC after J" + distance, Cpu.savedPC + distance, Cpu.PC);
	}
	
	@Test
	public void test_J2() {
		innerTestJn(Ch06_Jump_Instructions.OPC_x81_J2, 2);
	}
	
	@Test
	public void test_J3() {
		innerTestJn(Ch06_Jump_Instructions.OPC_x82_J3, 3);
	}
	
	@Test
	public void test_J4() {
		innerTestJn(Ch06_Jump_Instructions.OPC_x83_J4, 4);
	}
	
	@Test
	public void test_J5() {
		innerTestJn(Ch06_Jump_Instructions.OPC_x84_J5, 5);
	}
	
	@Test
	public void test_J6() {
		innerTestJn(Ch06_Jump_Instructions.OPC_x85_J6,6);
	}
	
	@Test
	public void test_J7() {
		innerTestJn(Ch06_Jump_Instructions.OPC_x86_J7, 7);
	}
	
	@Test
	public void test_J8() {
		innerTestJn(Ch06_Jump_Instructions.OPC_x87_J8, 8);
	}
	
	/*
	 * JB
	 */
	
	private void innerTestJB(int distance, boolean atEvenPc) {
		int distanceByte = distance & 0xFF;
		if (atEvenPc) {
			mkCode(10, 10, savedPC, 0x88, PC, distanceByte, 55, 66, 33);
		} else {
			mkCode(10, 10, 10, savedPC, 0x88, PC, distanceByte, 55, 66, 33);
		}
		Ch06_Jump_Instructions.OPC_x88_JB_salpha.execute();
		assertEquals("PC <- savedPC after JB[" + distance + "]", (Cpu.savedPC + distance) & 0x0000FFFF, Cpu.PC);
	}
	
	public void test_JB_plus127_atEven() {
		innerTestJB(+127, true);
	}
	
	public void test_JB_plus12_atOdd() {
		innerTestJB(+127, false);
	}
	
	public void test_JB_minus128_atEven() {
		innerTestJB(-128, true);
	}
	
	public void test_JB_minus128_atOdd() {
		innerTestJB(-128, false);
	}
	
	public void test_JB_zero_atEven() {
		innerTestJB(0, true);
	}
	
	public void test_JB_zero_atOdd() {
		innerTestJB(0, false);
	}
	
	/*
	 * JW
	 */
	
	private void innerTestJW(int distance, boolean atEvenPc) {
		int distanceByte1 = (distance >>> 8) & 0xFF;
		int distanceByte2 = distance & 0xFF;
		if (atEvenPc) {
			mkCode(10, 10, savedPC, 0x89, PC, distanceByte1, distanceByte2, 55, 66, 33);
		} else {
			mkCode(10, 10, 10, savedPC, 0x89, PC, distanceByte1, distanceByte2, 55, 66, 33);
		}
		Ch06_Jump_Instructions.OPC_x89_JW_sword.execute();
		assertEquals("PC <- savedPC after JW[" + distance + "]", (Cpu.savedPC + distance) & 0x0000FFFF, Cpu.PC);
	}
	
	@Test
	public void test_JW_plus127_atEven() {
		innerTestJW(127, true);
	}
	
	@Test
	public void test_JW_plus128_atEven() {
		innerTestJW(128, true);
	}
	
	@Test
	public void test_JW_plus32000_atEven() {
		innerTestJW(32000, true);
	}
	
	@Test
	public void test_JW_plus127_atOdd() {
		innerTestJW(127, false);
	}
	
	@Test
	public void test_JW_plus128_atOdd() {
		innerTestJW(128, false);
	}
	
	@Test
	public void test_JW_plus32000_atOdd() {
		innerTestJW(32000, false);
	}
	
	@Test
	public void test_JW_zero_atEven() {
		innerTestJW(0, true);
	}
	
	@Test
	public void test_JW_zero_atOdd() {
		innerTestJW(0, false);
	}
	
	@Test
	public void test_JW_minus128_atEven() {
		innerTestJW(-128, true);
	}
	
	@Test
	public void test_JW_minus32000_atEven() {
		innerTestJW(-32000, true);
	}
	
	@Test
	public void test_JW_minus127_atOdd() {
		innerTestJW(-127, false);
	}
	
	@Test
	public void test_JW_minus128_atOdd() {
		innerTestJW(-128, false);
	}
	
	@Test
	public void test_JW_minus32000_atOdd() {
		innerTestJW(-32000, false);
	}
	
	/*
	 * JS
	 */
	
	private void innerTestJS(int newPC) {
		mkStack(33, newPC & 0xFFFF);
		Ch06_Jump_Instructions.ESC_x19_JS.execute();
		assertEquals("PC after JS[" + newPC + "]", newPC & 0xFFFF, Cpu.PC);
	}
	
	@Test
	public void test_JS_0() {
		innerTestJS(0);
	}
	
	@Test
	public void test_JS_33() {
		innerTestJS(33);
	}
	
	@Test
	public void test_JS_0xFFFF() {
		innerTestJS(0xFFFF);
	}
	
	/*
	 * CATCH
	 */
	
	@Test
	public void test_CATCH_1() {
		mkCode(33, 44, savedPC, 0x80, PC, 66, 77);
		int oldPC = Cpu.PC;
		Ch06_Jump_Instructions.OPC_x80_CATCH_alpha.execute();
		assertEquals("PC absolute", oldPC + 1, Cpu.PC);
		assertEquals("savedPC -> PC", Cpu.savedPC + 2, Cpu.PC);
	}
	
	@Test
	public void test_CATCH_2() {
		mkCode(22, 33, 44, savedPC, 0x80, PC, 66, 77);
		int oldPC = Cpu.PC;
		Ch06_Jump_Instructions.OPC_x80_CATCH_alpha.execute();
		assertEquals("PC absolute", oldPC + 1, Cpu.PC);
		assertEquals("savedPC -> PC", Cpu.savedPC + 2, Cpu.PC);
	}
	
	/*
	 * JZn
	 */
	
	@Test
	public void test_JZ3_zero() {
		mkStack(33, 0);
		mkCode(1, 2, 3, savedPC, 0x98, PC, 4, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x98_JZ3.execute();
		assertEquals("PC <- savedPC + 3", Cpu.savedPC + 3, Cpu.PC);
	}
	
	@Test
	public void test_JZ3_nonZero() {
		mkStack(33, 44);
		mkCode(1, 2, 3, savedPC, 0x98, PC, 4, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x98_JZ3.execute();
		assertEquals("PC <- savedPC + 1", Cpu.savedPC + 1, Cpu.PC);
	}
	
	@Test
	public void test_JZ4_zero() {
		mkStack(33, 0);
		mkCode(1, 2, 3, savedPC, 0x99, PC, 4, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x99_JZ4.execute();
		assertEquals("PC <- savedPC + 4", Cpu.savedPC + 4, Cpu.PC);
	}
	
	@Test
	public void test_JZ4_nonZero() {
		mkStack(33, 44);
		mkCode(1, 2, 3, savedPC, 0x99, PC, 4, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x99_JZ4.execute();
		assertEquals("PC <- savedPC + 1", Cpu.savedPC + 1, Cpu.PC);
	}
	
	/*
	 * JNZn
	 */
	
	@Test
	public void test_JNZ3_nonZero() {
		mkStack(33, 22);
		mkCode(1, 2, 3, savedPC, 0x98, PC, 4, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x9B_JNZ3.execute();
		assertEquals("PC <- savedPC + 3", Cpu.savedPC + 3, Cpu.PC);
	}
	
	@Test
	public void test_JNZ3_zero() {
		mkStack(33, 0);
		mkCode(1, 2, 3, savedPC, 0x9B, PC, 4, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x9B_JNZ3.execute();
		assertEquals("PC <- savedPC + 1", Cpu.savedPC + 1, Cpu.PC);
	}
	
	@Test
	public void test_JNZ4_nonZero() {
		mkStack(33, 22);
		mkCode(1, 2, 3, savedPC, 0x9C, PC, 4, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x9C_JNZ4.execute();
		assertEquals("PC <- savedPC + 4", Cpu.savedPC + 4, Cpu.PC);
	}
	
	@Test
	public void test_JNZ4_zero() {
		mkStack(33, 0);
		mkCode(1, 2, 3, savedPC, 0x9C, PC, 4, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x9C_JNZ4.execute();
		assertEquals("PC <- savedPC + 1", Cpu.savedPC + 1, Cpu.PC);
	}
	
	/*
	 * JZB
	 */
	
	private void innerTestJZB(int stacked, int distance) {
		mkStack(33, stacked);
		mkCode(1, 2, 3, savedPC, 0x9A, PC, distance & 0xFF, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x9A_JZB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((stacked == 0) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JZB_zero_plus33() {
		innerTestJZB(0, 33);
	}
	
	@Test
	public void test_JZB_zero_plus127() {
		innerTestJZB(0, 127);
	}
	
	@Test
	public void test_JZB_nonZero_plus127() {
		innerTestJZB(33, 127);
	}
	
	@Test
	public void test_JZB_zero_minus33() {
		innerTestJZB(0, -33);
	}
	
	@Test
	public void test_JZB_zero_minus128() {
		innerTestJZB(0, -128);
	}
	
	@Test
	public void test_JZB_nonZero_minus128() {
		innerTestJZB(33, -128);
	}
	
	/*
	 * JNZB
	 */
	
	private void innerTestJNZB(int stacked, int distance) {
		mkStack(33, stacked);
		mkCode(1, 2, 3, savedPC, 0x9A, PC, distance & 0xFF, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x9D_JNZB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((stacked != 0) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JNZB_nonZero_plus33() {
		innerTestJNZB(55, 33);
	}
	
	@Test
	public void test_JNZB_nonZero_plus127() {
		innerTestJNZB(-1, 127);
	}
	
	@Test
	public void test_JNZB_zero_plus127() {
		innerTestJNZB(0, 127);
	}
	
	@Test
	public void test_JNZB_nonZero_minus33() {
		innerTestJNZB(44, -33);
	}
	
	@Test
	public void test_JNZB_nonZero_minus128() {
		innerTestJNZB(-1, -128);
	}
	
	@Test
	public void test_JNZB_zero_minus128() {
		innerTestJNZB(0, -128);
	}
	
	/*
	 * JEB
	 */
	
	private void innerTestJEB(int stacked1, int stacked2, int distance) {
		mkStack(33, stacked1, stacked2);
		mkCode(1, 2, 3, savedPC, 0x8B, PC, distance & 0xFF, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x8B_JEB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((stacked1 == stacked2) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JEB_1000_1000_plus33() {
		innerTestJEB(1000, 1000, 33);
	}
	
	@Test
	public void test_JEB_999_1000_plus33() {
		innerTestJEB(999, 1000, 33);
	}
	
	@Test
	public void test_JEB_0xFFFE_0xFFFE_plus33() {
		innerTestJEB(0xFFFE, 0xFFFE, -33);
	}
	
	@Test
	public void test_JEB_0xFFFF_0xFFFE_minus33() {
		innerTestJEB(0xFFFF, 0xFFFE, -33);
	}
	
	/*
	 * JNEB
	 */
	
	private void innerTestJNEB(int stacked1, int stacked2, int distance) {
		mkStack(33, stacked1, stacked2);
		mkCode(1, 2, 3, savedPC, 0x8E, PC, distance & 0xFF, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x8E_JNEB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((stacked1 != stacked2) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JNEB_1000_1000_plus33() {
		innerTestJNEB(1000, 1000, 33);
	}
	
	@Test
	public void test_JNEB_999_1000_plus33() {
		innerTestJNEB(999, 1000, 33);
	}
	
	@Test
	public void test_JNEB_0xFFFE_0xFFFE_plus33() {
		innerTestJNEB(0xFFFE, 0xFFFE, -33);
	}
	
	@Test
	public void test_JNEB_0xFFFF_0xFFFE_minus33() {
		innerTestJNEB(0xFFFF, 0xFFFE, -33);
	}
	
	/*
	 * JDEB
	 */
	
	private void innerTestJDEB(int stacked1, int stacked2, int stacked3, int stacked4, int distance) {
		mkStack(33, stacked1, stacked2, stacked3, stacked4);
		mkCode(1, 2, 3, savedPC, 0x9E, PC, distance & 0xFF, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x9E_JDEB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((stacked1 == stacked3 && stacked2 == stacked4) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JDEB_1000_7_1000_7_plus33() {
		innerTestJDEB(1000, 7, 1000, 7, 33);
	}
	
	@Test
	public void test_JDEB_999_9_1000_9_plus33() {
		innerTestJDEB(999, 9, 1000, 9, 33);
	}
	
	@Test
	public void test_JDEB_7_0xFFFE_7_0xFFFE_plus33() {
		innerTestJDEB(7, 0xFFFE, 7, 0xFFFE, -33);
	}
	
	@Test
	public void test_JDEB_4_0xFFFF_4_0xFFFE_minus33() {
		innerTestJDEB(4, 0xFFFF, 4, 0xFFFE, -33);
	}
	
	/*
	 * JDNEB
	 */
	
	private void innerTestJDNEB(int stacked1, int stacked2, int stacked3, int stacked4, int distance) {
		mkStack(33, stacked1, stacked2, stacked3, stacked4);
		mkCode(1, 2, 3, savedPC, 0x9F, PC, distance & 0xFF, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x9F_JDNEB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((stacked1 != stacked3 || stacked2 != stacked4) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JDNEB_1000_7_1000_7_plus33() {
		innerTestJDNEB(1000, 7, 1000, 7, 33);
	}
	
	@Test
	public void test_JDNEB_999_9_1000_9_plus33() {
		innerTestJDNEB(999, 9, 1000, 9, 33);
	}
	
	@Test
	public void test_JDNEB_7_0xFFFE_7_0xFFFE_plus33() {
		innerTestJDNEB(7, 0xFFFE, 7, 0xFFFE, -33);
	}
	
	@Test
	public void test_JDNEB_4_0xFFFF_4_0xFFFE_minus33() {
		innerTestJDNEB(4, 0xFFFF, 4, 0xFFFE, -33);
	}
	
	/*
	 * JEP
	 */
	
	private void innerTestJEP(int data, int pair, int distance) {
		mkStack(33, data & 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x8A, PC, pair & 0xFF, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x8A_JEP_pair.execute();
		int expectedPC = (Cpu.savedPC + ((distance >= 0) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JEP_jump() {
		innerTestJEP(7, 0x75, 9);
	}
	
	@Test
	public void test_JEP_nojump() {
		innerTestJEP(8, 0x75, -1);
	}
	
	/*
	 * JNEP
	 */
	
	private void innerTestJNEP(int data, int pair, int distance) {
		mkStack(33, data & 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x8D, PC, pair & 0xFF, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x8D_JNEP_pair.execute();
		int expectedPC = (Cpu.savedPC + ((distance >= 0) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JNEP_nojump() {
		innerTestJNEP(7, 0x75, -1);
	}
	
	@Test
	public void test_JNEP_jump() {
		innerTestJNEP(8, 0x75, 9);
	}
	
	/*
	 * JEBB
	 */
	
	private void innerTestJEBB(int data, int zeByte, int zeDisp, int distance) {
		mkStack(33, data & 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x8C, PC, zeByte, zeDisp, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x8C_JEBB_alphasbeta.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 3)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JEBB_jump_positiveDisp() {
		innerTestJEBB(7, 7, 5, 5);
	}
	
	@Test
	public void test_JEBB_jump_negativeDisp() {
		innerTestJEBB(7, 7, 0xFE, -2);
	}
	
	@Test
	public void test_JEBB_nojump() {
		innerTestJEBB(8, 7, 5, NOJUMP);
	}
	
	/*
	 * JEBB
	 */
	
	private void innerTestJNEBB(int data, int zeByte, int zeDisp, int distance) {
		mkStack(33, data & 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x8F, PC, zeByte, zeDisp, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x8F_JNEBB_alphasbeta.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 3)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JNEBB_jump_positiveDisp() {
		innerTestJNEBB(8, 7, 5, 5);
	}
	
	@Test
	public void test_JNEBB_jump_negativeDisp() {
		innerTestJNEBB(8, 7, 0xFE, -2);
	}
	
	@Test
	public void test_JNEBB_nojump() {
		innerTestJNEBB(7, 7, 5, NOJUMP);
	}
	
	/*
	 * JLB
	 */
	
	private void innerTestJLB(int j, int k, int zeDisp, int distance) {
		mkStack(33, j, k);
		mkCode(1, 2, 3, savedPC, 0x90, PC, zeDisp, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x90_JLB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JLB_neg_neg_jump_positiveDisp() {
		innerTestJLB(-10000, -9999, 5, 5);
	}
	
	@Test
	public void test_JLB_neg_neg_jump_negativeDisp() {
		innerTestJLB(-10000, -9999, 0xFE, -2);
	}
	
	@Test
	public void test_JLB_neg_neg_nojump() {
		innerTestJLB(-10000, -10000, 0xFE, NOJUMP);
	}
	
	@Test
	public void test_JLB_pos_pos_jump_positiveDisp() {
		innerTestJLB(9999, 10000, 5, 5);
	}
	
	@Test
	public void test_JLB_pos_pos_jump_negativeDisp() {
		innerTestJLB(9999, 10000, 0xFE, -2);
	}
	
	@Test
	public void test_JLB_pos_pos_nojump() {
		innerTestJLB(10000, 10000, 0xFE, NOJUMP);
	}
	
	/*
	 * JLEB
	 */
	
	private void innerTestJLEB(int j, int k, int zeDisp, int distance) {
		mkStack(33, j, k);
		mkCode(1, 2, 3, savedPC, 0x93, PC, zeDisp, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x93_JLEB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JLEB_neg_neg_less_jump_positiveDisp() {
		innerTestJLEB(-10000, -9999, 5, 5);
	}
	
	@Test
	public void test_JLEB_neg_neg_eq_jump_positiveDisp() {
		innerTestJLEB(-10000, -10000, 5, 5);
	}
	
	@Test
	public void test_JLEB_neg_neg_less_jump_negativeDisp() {
		innerTestJLEB(-10000, -9999, 0xFE, -2);
	}
	
	@Test
	public void test_JLEB_neg_neg_eq_jump_negativeDisp() {
		innerTestJLEB(-10000, -10000, 0xFE, -2);
	}
	
	@Test
	public void test_JLEB_neg_neg_nojump() {
		innerTestJLEB(-10000, -10001, 0xFE, NOJUMP);
	}
	
	@Test
	public void test_JLEB_pos_pos_less_jump_positiveDisp() {
		innerTestJLEB(9999, 10000, 5, 5);
	}
	
	@Test
	public void test_JLEB_pos_pos_eq_jump_positiveDisp() {
		innerTestJLEB(10000, 10000, 5, 5);
	}
	
	@Test
	public void test_JLEB_pos_pos_less_jump_negativeDisp() {
		innerTestJLEB(9999, 10000, 0xFE, -2);
	}
	
	@Test
	public void test_JLEB_pos_pos_eq_jump_negativeDisp() {
		innerTestJLEB(10000, 10000, 0xFE, -2);
	}
	
	@Test
	public void test_JLEB_pos_pos_nojump() {
		innerTestJLEB(10001, 10000, 0xFE, NOJUMP);
	}
	
	/*
	 * JGB
	 */
	
	private void innerTestJGB(int j, int k, int zeDisp, int distance) {
		mkStack(33, j, k);
		mkCode(1, 2, 3, savedPC, 0x92, PC, zeDisp, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x92_JGB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JGB_neg_neg_jump_positiveDisp() {
		innerTestJGB(-9999, -10000, 5, 5);
	}
	
	@Test
	public void test_JGB_neg_neg_jump_negativeDisp() {
		innerTestJGB(-9999, -10000, 0xFE, -2);
	}
	
	@Test
	public void test_JGB_neg_neg_nojump() {
		innerTestJGB(-10000, -10000, 0xFE, NOJUMP);
	}
	
	@Test
	public void test_JGB_pos_pos_jump_positiveDisp() {
		innerTestJGB(10000, 9999, 5, 5);
	}
	
	@Test
	public void test_JGB_pos_pos_jump_negativeDisp() {
		innerTestJGB(10000, 9999, 0xFE, -2);
	}
	
	@Test
	public void test_JGB_pos_pos_nojump() {
		innerTestJGB(10000, 10000, 0xFE, NOJUMP);
	}
	
	/*
	 * JGEB
	 */
	
	private void innerTestJGEB(int j, int k, int zeDisp, int distance) {
		mkStack(33, j, k);
		mkCode(1, 2, 3, savedPC, 0x91, PC, zeDisp, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x91_JGEB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JGEB_neg_neg_grtr_jump_positiveDisp() {
		innerTestJGEB(-9999, -10000, 5, 5);
	}
	
	@Test
	public void test_JGEB_neg_neg_eq_jump_positiveDisp() {
		innerTestJGEB(-10000, -10000, 5, 5);
	}
	
	@Test
	public void test_JGEB_neg_neg_grtr_jump_negativeDisp() {
		innerTestJGEB(-9999, -10000, 0xFE, -2);
	}
	
	@Test
	public void test_JGEB_neg_neg_eq_jump_negativeDisp() {
		innerTestJGEB(-10000, -10000, 0xFE, -2);
	}
	
	@Test
	public void test_JGEB_neg_neg_nojump() {
		innerTestJGEB(-10001, -10000, 0xFE, NOJUMP);
	}
	
	@Test
	public void test_JGEB_pos_pos_grtr_jump_positiveDisp() {
		innerTestJGEB(10000, 9999, 5, 5);
	}
	
	@Test
	public void test_JGEB_pos_pos_eq_jump_positiveDisp() {
		innerTestJGEB(10000, 10000, 5, 5);
	}
	
	@Test
	public void test_JGEB_pos_pos_grtr_jump_negativeDisp() {
		innerTestJGEB(10000, 9999, 0xFE, -2);
	}
	
	@Test
	public void test_JGEB_pos_pos_eq_jump_negativeDisp() {
		innerTestJGEB(10000, 10000, 0xFE, -2);
	}
	
	@Test
	public void test_JGEB_pos_pos_nojump() {
		innerTestJGEB(10000, 10001, 0xFE, NOJUMP);
	}
	
	/*
	 * JULB
	 */
	
	private void innerTestJULB(int u, int v, int zeDisp, int distance) {
		mkStack(33, u & 0xFFFF, v & 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x94, PC, zeDisp, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x94_JULB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JULB_small_less_jump_positiveDisp() {
		innerTestJULB(33, 34, 5, 5);
	}
	
	@Test
	public void test_JULB_small_less_jump_negativeDisp() {
		innerTestJULB(33, 34, 0xFE, -2);
	}
	
	@Test
	public void test_JULB_small_eq_nojump() {
		innerTestJULB(34, 34, 0xFE, NOJUMP);
	}
	
	@Test
	public void test_JULB_large_less_jump_positiveDisp() {
		innerTestJULB(0xF000, 0xF001, 5, 5);
	}
	
	@Test
	public void test_JULB_large_less_jump_negativeDisp() {
		innerTestJULB(0xF000, 0xF001, 0xFE, -2);
	}
	
	@Test
	public void test_JULB_large_eq_nojump() {
		innerTestJULB(0xF000, 0xF000, 0xFE, NOJUMP);
	}
	
	/*
	 * JULEB
	 */
	
	private void innerTestJULEB(int u, int v, int zeDisp, int distance) {
		mkStack(33, u & 0xFFFF, v & 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x97, PC, zeDisp, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x97_JULEB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JULEB_small_less_jump_positiveDisp() {
		innerTestJULEB(33, 34, 5, 5);
	}
	
	@Test
	public void test_JULEB_small_less_jump_negativeDisp() {
		innerTestJULEB(33, 34, 0xFE, -2);
	}
	
	@Test
	public void test_JULEB_small_eq_jump_negativeDisp() {
		innerTestJULEB(34, 34, 0xFE, -2);
	}
	
	@Test
	public void test_JULEB_small_grtr_nojump() {
		innerTestJULEB(35, 34, 0xFE, NOJUMP);
	}
	
	@Test
	public void test_JULEB_large_less_jump_positiveDisp() {
		innerTestJULEB(0xF000, 0xF001, 5, 5);
	}
	
	@Test
	public void test_JULEB_large_eq_jump_positiveDisp() {
		innerTestJULEB(0xF000, 0xF000, 5, 5);
	}
	
	@Test
	public void test_JULEB_large_less_nojump() {
		innerTestJULEB(0xF001, 0xF000, 5, NOJUMP);
	}
	
	@Test
	public void test_JULEB_large_less_jump_negativeDisp() {
		innerTestJULEB(0xF000, 0xF001, 0xFE, -2);
	}
	
	@Test
	public void test_JULEB_large_eq_jump_negativeDisp() {
		innerTestJULEB(0xF000, 0xF000, 0xFE, -2);
	}
	
	@Test
	public void test_JULEB_large_lrgr_nojump() {
		innerTestJULEB(0xF001, 0xF000, 0xFE, NOJUMP);
	}
	
	/*
	 * JUGB
	 */
	
	private void innerTestJUGB(int u, int v, int zeDisp, int distance) {
		mkStack(33, u & 0xFFFF, v & 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x96, PC, zeDisp, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x96_JUGB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JUGB_small_less_jump_positiveDisp() {
		innerTestJUGB(34, 33, 5, 5);
	}
	
	@Test
	public void test_JUGB_small_less_jump_negativeDisp() {
		innerTestJUGB(34, 33, 0xFE, -2);
	}
	
	@Test
	public void test_JUGB_small_eq_nojump() {
		innerTestJUGB(34, 34, 0xFE, NOJUMP);
	}
	
	@Test
	public void test_JUGB_large_less_jump_positiveDisp() {
		innerTestJUGB(0xF001, 0xF000, 5, 5);
	}
	
	@Test
	public void test_JUGB_large_less_jump_negativeDisp() {
		innerTestJUGB(0xF001, 0xF000, 0xFE, -2);
	}
	
	@Test
	public void test_JUGB_large_eq_nojump() {
		innerTestJUGB(0xF000, 0xF000, 0xFE, NOJUMP);
	}
	
	/*
	 * JULEB
	 */
	
	private void innerTestJUGEB(int u, int v, int zeDisp, int distance) {
		mkStack(33, u & 0xFFFF, v & 0xFFFF);
		mkCode(1, 2, 3, savedPC, 0x95, PC, zeDisp, 5, 6, 7, 8);
		Ch06_Jump_Instructions.OPC_x95_JUGEB_salpha.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 2)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JUGEB_small_grtr_jump_positiveDisp() {
		innerTestJUGEB(34, 33, 5, 5);
	}
	
	@Test
	public void test_JUGEB_small_grtr_jump_negativeDisp() {
		innerTestJUGEB(34, 33, 0xFE, -2);
	}
	
	@Test
	public void test_JUGEB_small_eq_jump_negativeDisp() {
		innerTestJUGEB(34, 34, 0xFE, -2);
	}
	
	@Test
	public void test_JUGEB_small_grtr_nojump() {
		innerTestJUGEB(34, 35, 0xFE, NOJUMP);
	}
	
	@Test
	public void test_JUGEB_large_grtr_jump_positiveDisp() {
		innerTestJUGEB(0xF001, 0xF000, 5, 5);
	}
	
	@Test
	public void test_JUGEB_large_eq_jump_positiveDisp() {
		innerTestJUGEB(0xF000, 0xF000, 5, 5);
	}
	
	@Test
	public void test_JUGEB_large_less_nojump() {
		innerTestJUGEB(0xF000, 0xF001, 5, NOJUMP);
	}
	
	@Test
	public void test_JUGEB_large_grtr_jump_negativeDisp() {
		innerTestJUGEB(0xF001, 0xF000, 0xFE, -2);
	}
	
	@Test
	public void test_JUGEB_large_eq_jump_negativeDisp() {
		innerTestJUGEB(0xF000, 0xF000, 0xFE, -2);
	}
	
	/*
	 * JIB
	 */
	
	private void innerTestJIB(boolean oddCodeStart, int index, int limit, int distance, int... offsetPairs) {
		final int tableBase = 0x3222;
		mkStack(44, index & 0xFFFF, limit & 0xFFFF);
		if (oddCodeStart) {
			mkCode(0xFF, 1, 2, 3, savedPC, 0xA0, PC, tableBase >>> 8, tableBase & 0x00FF, 4, 5, 6);
		} else  {
			mkCode(1, 2, 3, savedPC, 0xA0, PC, tableBase >>> 8, tableBase & 0x00FF, 4, 5, 6);
		}
		
		int offset = 0;
		for (int pair : offsetPairs) {
			Mem.writeWord(Cpu.CB + tableBase + offset++, (short)(pair & 0xFFFF));
		}
		
		Ch06_Jump_Instructions.OPC_xA0_JIB_word.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 3)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JIB_jump1a() {
		innerTestJIB(false, 3, 12, 0x32, 0x1016, 0x2232, 0x3844, 0x4852, 0x5866);
	}
	
	@Test
	public void test_JIB_jump1b() {
		innerTestJIB(true, 3, 12, 0x32, 0x1016, 0x2232, 0x3844, 0x4852, 0x5866);
	}
	
	@Test
	public void test_JIB_jump2a() {
		innerTestJIB(false, 4, 12, 0x38, 0x1016, 0x2232, 0x3844, 0x4852, 0x5866);
	}
	
	@Test
	public void test_JIB_jump2b() {
		innerTestJIB(true, 4, 12, 0x38, 0x1016, 0x2232, 0x3844, 0x4852, 0x5866);
	}
	
	@Test
	public void test_JIB_nojumpa() {
		innerTestJIB(false, 12, 12, NOJUMP, 0x1016, 0x2232, 0x3844, 0x4852, 0x5866);
	}
	
	@Test
	public void test_JIB_nojumpb() {
		innerTestJIB(true, 12, 12, NOJUMP, 0x1016, 0x2232, 0x3844, 0x4852, 0x5866);
	}
	
	/*
	 * JIW
	 */
	
	private void innerTestJIW(boolean oddCodeStart, int index, int limit, int distance, int... offsets) {
		final int tableBase = 0x3222;
		mkStack(44, index & 0xFFFF, limit & 0xFFFF);
		if (oddCodeStart) {
			mkCode(0xFF, 1, 2, 3, savedPC, 0xA0, PC, tableBase >>> 8, tableBase & 0x00FF, 4, 5, 6);
		} else  {
			mkCode(1, 2, 3, savedPC, 0xA0, PC, tableBase >>> 8, tableBase & 0x00FF, 4, 5, 6);
		}
		
		int offset = 0;
		for (int offs : offsets) {
			Mem.writeWord(Cpu.CB + tableBase + offset++, (short)(offs & 0xFFFF));
		}
		
		Ch06_Jump_Instructions.OPC_xA1_JIW_word.execute();
		int expectedPC = (Cpu.savedPC + ((distance != NOJUMP) ? distance : 3)) & 0xFFFF;
		assertEquals("new PC", expectedPC, Cpu.PC);
	}
	
	@Test
	public void test_JIW_jump1a() {
		innerTestJIW(false, 3, 12, 0x0133, 0x0100, 0x0111, 0x0122, 0x0133, 0x0144, 0x0155, 0x0166, 0x0177);
	}
	
	@Test
	public void test_JIW_jump1b() {
		innerTestJIW(true, 3, 12, 0x0133, 0x0100, 0x0111, 0x0122, 0x0133, 0x0144, 0x0155, 0x0166, 0x0177);
	}
	
	@Test
	public void test_JIW_jump2a() {
		innerTestJIW(false, 6, 12, 0x0166, 0x0100, 0x0111, 0x0122, 0x0133, 0x0144, 0x0155, 0x0166, 0x0177);
	}
	
	@Test
	public void test_JIW_jump2b() {
		innerTestJIW(true, 6, 12, 0x0166, 0x0100, 0x0111, 0x0122, 0x0133, 0x0144, 0x0155, 0x0166, 0x0177);
	}
	
	@Test
	public void test_JIW_nojumpa() {
		innerTestJIW(false, 12, 12, NOJUMP, 0x0100, 0x0111, 0x0122, 0x0133, 0x0144, 0x0155, 0x0166, 0x0177);
	}
	
	@Test
	public void test_JIW_nojumpb() {
		innerTestJIW(true, 12, 12, NOJUMP, 0x0100, 0x0111, 0x0122, 0x0133, 0x0144, 0x0155, 0x0166, 0x0177);
	}
	
}
