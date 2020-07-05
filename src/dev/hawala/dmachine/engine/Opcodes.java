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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import dev.hawala.dmachine.engine.opcodes.Ch03_Memory_Organization;
import dev.hawala.dmachine.engine.opcodes.Ch05_Stack_Instructions;
import dev.hawala.dmachine.engine.opcodes.Ch06_Jump_Instructions;
import dev.hawala.dmachine.engine.opcodes.Ch07_Assignment_Instructions;
import dev.hawala.dmachine.engine.opcodes.Ch08_Block_Transfers;
import dev.hawala.dmachine.engine.opcodes.Ch09_Control_Transfers;
import dev.hawala.dmachine.engine.opcodes.Ch10_Processes;
import dev.hawala.dmachine.engine.opcodes.ChXX_Undocumented;

/**
 * Opcode installer and dispatcher.
 * <p>
 * Besides providing the {@code dispatch(int opcode)} method to execute a single
 * instruction, this class performs the installation of opcode-implementations
 * into the dispatch tables. This installation process allows to use either the
 * "old" PrincOps 4.0 instructions (i.e. before the "MDS relieved" era) or the
 * post-4.0 instructions (i.e. starting with the "MDS relieved" era). The 2 PrincOps
 * variants differ in the global frame handling:
 * </p>
 * <ul>
 * <li>the "old" PrincOps has a 16 bit GF register (as POINTER), meaning all global
 * frames reside in the main data space, putting global module data and stack frame
 * data in concurrence for the limited MDS space;</li>
 * <li>the "new" PrincOps has a 32 bit GF register (as LONG POINTER), allowing global
 * frames to reside anywhere and to mix new style (relieving the MDS from global data)
 * and old style compiled code.</li> 
 * </ul>
 * <p>
 * This mesa engine uses therefore 2 global frame registers GF16 (16 bit) and GF32
 * (32 bit), with GF32 always holding GF16+MDS. The new PrincOps additionally defines
 * the global frame table (with register GFT). Most instructions are not affected
 * by this distinction and are independent of the global frame architecture, thus
 * having a single common implementation. Instructions dependent of the global frame
 * architecture and register length (i.e. bothered by the GF register distinction and GFT)
 * exist in 2 implementations, one for each global frame architecture.
 * </p>
 * <p>
 * The opcode installer scans the known set of classes in package <b>{@link dev.hawala.dmachine.engine.opcodes}</b>
 * for static global variables implementing the interface {@code OpImpl}
 * and complying to the following naming convention:
 * </p>
 * <p>
 * &nbsp;&nbsp;&nbsp;<i>optype</i><b>_</b><i>instrcode</i><b>_</b><i>opcode</i>[<b>_</b><i>arglogspec</i>]
 * </p>
 * The <i>optype</i> must be one of:
 * <ul>
 * <li><b>OPC</b> for an GF-architecture-independent regular instruction</li>
 * <li><b>OPCo</b> for an "old-style" (16bit GF) regular instruction</li>
 * <li><b>OPCn</b> for an "new-style" (32bit GF) regular instruction</li>
 * <li><b>ESC</b> for an GF-architecture-independent ESC(L) instruction</li>
 * <li><b>ESCo</b> for an "old-style" (16bit GF) ESC(L) instruction</li>
 * <li><b>ESCn</b> for an "new-style" (32bit GF) ESC(L) instruction</li>
 * </ul>
 * <p>(this mesa engine makes no distinction between ESC (0xF8X) and ESCL
 * (0xF9) type escape instructions, as the total length of the instruction
 * (2 or 3 bytes) is not relevant here)
 * </p>
 * <p>
 * The <i>instrcode</i> specifies the numeric instruction code to use for
 * dispatching, either as 3-digit octal number or as {@code xNN} for the
 * hexadecimal instruction code.    
 * </p>
 * <p>
 * The <i>opcode</i> gives the short name of the instruction like {@code LL0}
 * or {@code DSUB}.
 * </p>
 * <p>
 * If specified, <i>arglogspec</i> defines if and which/how arguments of the
 * instruction are issued when the instruction is logged during execution (see
 * {@code Config.LOG_OPCODES}). <i>arglogspec</i> can be one of:
 * </p>
 * <ul>
 * <li><b>alpha</b> - the argument is an unsigned byte</li>
 * <li><b>salpha</b> - the argument is a signed byte</li>
 * <li><b>word</b> - the argument is an unsigned word</li>
 * <li><b>sword</b> - the argument is a signed word</li>
 * <li><b>pair</b> - the argument is a pair of 4-bit nibbles in a byte</li>
 * <li><b>alphabeta</b> - the arguments are two unsigned bytes</li>
 * <li><b>alphasbeta</b> - the argument are an unsigned byte and a signed byte</li>
 * </ul>
 * <p>Examples for instruction implementation naming:</p>
 * <ul>
 * <li>{@code OPC_xA4_DIS}<br>instruction DIS at regular instruction hex A4
 * which will be logged only with the instruction name</li>
 * <li>{@code OPC_x7C_SHIFTSB_alpha}<br>instruction SHIFTSB at regular instruction
 * hex 7C which will logged with instruction name and the next code byte as
 * argument 'alpha'</li>
 * <li>{@code ESC_x22_XF_alpha}<br>instruction XF at ESC instruction hex 22 which
 * will be logged with the next code byte as argument 'alpha'
 * </li>
 * <li>{@code OPCn_x5D_RGILP_pair}<br>"new-style" (32bit GF) instruction RGILP at
 * regular instruction hex 5D which will be logged with the next code byte
 * as nibble-pair argument
 * </li>
 * <li>{@code OPCo_x5D_RGILP_pair}<br>the corresponding "old-style" instruction
 * implementation for {@code OPCn_x5D_RGILP_pair}
 * </li> 
 * </ul>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Opcodes {
	
	/**
	 * (functional) Interface for instruction implementations. 
	 */
	@FunctionalInterface
	public interface OpImpl {
		public void execute();
	}
    
	// the classes known to contain instruction implementations and that
	// are scanned for OpImpl global variables.
	// remark: the usual way would have been to scan the runtime for classes below a
	//   given package, but this is
	//   a) futile in the first place as the relevant classes are known and fixed
	//   b) unreliable as Java itself does not provide a standardized way (reflection?) to do
	//      the scan and a home-brew scanner must cope with different types of classloaders
	//      (at least "normal" file system for development environments and jar files
	//      when the application is packaged, possibly more for Java9 and Java10 etc.)
	//   A class scanner was in fact used for some time, but see a) ... 
    private static final List<Class<?>> classes = Arrays.asList( // findClass(OPCODE_PACKAGE);
    			Ch03_Memory_Organization.class,
    			Ch05_Stack_Instructions.class,
    			Ch06_Jump_Instructions.class,
    			Ch07_Assignment_Instructions.class,
    			Ch08_Block_Transfers.class,
    			Ch09_Control_Transfers.class,
    			Ch10_Processes.class,
    			ChXX_Undocumented.class
    		);
	
	// the instruction dispatch tables for regular and ESC(L) instructions
	private static final OpImpl[] opcTable = new OpImpl[256];
	private static final OpImpl[] escTable = new OpImpl[256];
	
	// the instruction names for regular and ESC(L) instructions
	public static final String[] opcNames = new String[256];
	public static final String[] escNames = new String[256];
	
	// the regular codes for the ESC(L) sub-dispatchers
	public static final int zESC = 0xF8;
	public static final int zESCL = 0xF9;
	
	/**
	 * Dispatch an instruction code for execution 
	 * @param opcode the instruction code to dispatch
	 */
	public static void dispatch(int opcode) {
		opcTable[opcode].execute();
	}
	
	// the sub-dispatches for ESC(L) instructions
	private static final OpImpl opEscImpl = () -> {
		escTable[Mem.getNextCodeByte()].execute();
	};
	
	// pre-fill all instruction codes in the dispatch tables
	// with the instruction traps and "invalid" names
	private static void prepareOpcodeTables() {
		for (int i = 0; i < 256; i++) {
			final int code = i;
			final String codeName = String.format("INVx%02X", code);
			opcTable[code] = () -> Cpu.opcodeTrap(code);
			escTable[code] = () -> Cpu.escOpcodeTrap(code);
			opcNames[code] = codeName;
			escNames[code] = "ESC." + codeName;
		}
	}
	
	// post-fill the regular dispatch table to ensure that
	// the ESC(L) dispatches are present (and not overwritten
	// by some rogue instruction)
	private static void postpareOpcodeTables() {
		opcTable[zESC] = opEscImpl;
		opcTable[zESCL] = opEscImpl;
	}
	
	/**
	 * Install the instruction implementations for an "old-style"
	 * mesa engine (PrincOps up to version 4.0),
	 */
	public static void initializeInstructionsPrincOps40() {
		prepareOpcodeTables();
		initializeInstructions("OPC");
		initializeInstructions("ESC");
		initializeInstructions("OPCo");
		initializeInstructions("ESCo");
		postpareOpcodeTables();
	}
	
	private static void innerImplant(int opcode, String opname, OpImpl impl, OpImpl[] tblOps, String[] tblNames)  {
		if (opcode < 0 || opcode > 255) {
			System.out.printf("** attempt to implant invalid opcode 0x%04X - %s\n", opcode, opname);
			return;
		}
		tblOps[opcode] = (Config.LOG_OPCODES)
			? () -> { Cpu.logOpcode(opname); impl.execute(); }
			: impl;
		tblNames[opcode] = opname;
	}
	
	/**
	 * Register the implementation for a regular instruction.
	 * 
	 * @param opcode (regular) opcode of the instruction
	 * @param opname name of the instruction for logging
	 * @param impl implementation of the opcode
	 */
	public static void implantOverride(int opcode, String opname, OpImpl impl) {
		innerImplant(opcode, opname, impl, opcTable, opcNames);
	}
	
	/**
	 * Register the implementation for an ESC(L)-instruction.
	 * 
	 * @param opcode ESC(L)-relative opcode of the instruction
	 * @param opname name of the instruction for logging
	 * @param impl implementation of the opcode
	 */
	public static void implantEscOverride(int opcode, String opname, OpImpl impl) {
		innerImplant(opcode, "ESC." + opname, impl, escTable, escNames);
	}
	
	/**
	 * Install the instruction implementations for an "new-style"
	 * mesa engine (PrincOps post version 4.0),
	 */
	public static void initializeInstructionsPrincOpsPost40() {
		prepareOpcodeTables();
		initializeInstructions("OPC");
		initializeInstructions("ESC");
		initializeInstructions("OPCn");
		initializeInstructions("ESCn");
		postpareOpcodeTables();
	}
	
	// scanner and installer for instruction implementations starting
	// with the given 'optype'-prefix.
	private static void initializeInstructions(String prefix) {
		for(Class<?> clazz : classes) {
			for(Field field: clazz.getDeclaredFields()) {
				if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (!field.getType().isAssignableFrom(OpImpl.class)) {
					continue;
				}
				
				String fieldName = field.getName();
				String[] parts = fieldName.split("_");
				if (parts.length >= 3 &&  prefix.equals(parts[0])) {
					try {
						int opcode = parseInstrCode(parts[1]);
						if (opcode < 0) { continue; }
						field.setAccessible(true);
						OpImpl opImpl = (OpImpl)field.get(null);
						if (opImpl == null) { continue; }
						if (prefix.startsWith("OPC")) {
							if (Config.LOG_OPCODE_INSTALLATION) {
								System.out.printf("** normal Opcode %03o (0x%02X) %s\n", opcode, opcode, fieldName);
							}
							if (Config.LOG_OPCODES) {
								if (parts.length > 3 && "alpha".equals(parts[3])) {
									opcTable[opcode] = () -> {
												Cpu.logOpcode_alpha(parts[2]);
												opImpl.execute();
									};
								} else if (parts.length > 3 && "salpha".equals(parts[3])) {
									opcTable[opcode] = () -> {
										Cpu.logOpcode_salpha(parts[2]);
										opImpl.execute();
									};
								} else if (parts.length > 3 && "word".equals(parts[3])) {
									opcTable[opcode] = () -> {
										Cpu.logOpcode_word(parts[2]);
										opImpl.execute();
									};
								} else if (parts.length > 3 && "sword".equals(parts[3])) {
									opcTable[opcode] = () -> {
										Cpu.logOpcode_sword(parts[2]);
										opImpl.execute();
									};
								} else if (parts.length > 3 && "pair".equals(parts[3])) {
									opcTable[opcode] = () -> {
										Cpu.logOpcode_pair(parts[2]);
										opImpl.execute();
									};
								} else if (parts.length > 3 && "alphabeta".equals(parts[3])) {
									opcTable[opcode] = () -> {
										Cpu.logOpcode_alphabeta(parts[2]);
										opImpl.execute();
									};
								} else if (parts.length > 3 && "alphasbeta".equals(parts[3])) {
									opcTable[opcode] = () -> {
										Cpu.logOpcode_alphasbeta(parts[2]);
										opImpl.execute();
									};
								} else {
									if (parts.length > 3) {
										System.err.printf("###### invalid arglogspec '%s' in: %s\n", parts[3], fieldName);
									}
									opcTable[opcode] = () -> {
										Cpu.logOpcode(parts[2]);
										opImpl.execute();
									};
								}
							} else {
								opcTable[opcode] = opImpl;
							}
							opcNames[opcode] = parts[2];
						} else {
							if (Config.LOG_OPCODE_INSTALLATION) {
								System.out.printf("** ESC Opcode %03o (0x%02X) %s\n", opcode, opcode, fieldName);
							}
							if (Config.LOG_OPCODES) {
								if (parts.length > 3 && "alpha".equals(parts[3])) {
									escTable[opcode] = () -> {
												Cpu.logEscOpcode_alpha(parts[2]);
												opImpl.execute();
									};
								} else if (parts.length > 3 && "word".equals(parts[3])) {
									escTable[opcode] = () -> {
										Cpu.logEscOpcode_word(parts[2]);
										opImpl.execute();
									};
								} else {
									if (parts.length > 3) {
										System.err.printf("###### invalid arglogspec '%s' in: %s\n", parts[3], fieldName);
									}
									escTable[opcode] = () -> {
										Cpu.logEscOpcode(parts[2]);
										opImpl.execute();
									};
								}
							} else {
								escTable[opcode] = opImpl;
							}
							escNames[opcode] = "ESC." + parts[2];
						}
					} catch (IllegalArgumentException|IllegalAccessException exc) {
						// ignored
						System.out.printf("** ERROR :: failed to parse Opcode Impl-Name: %s (%s)\n", fieldName, exc.getMessage());
					}
				}
			}
		}
	}
	
	// parse the 'instrcode' part of an instruction variable name
	private static int parseInstrCode(String code) {
		if (code == null || code.length() != 3) { return -1; }
		char c1 = code.charAt(0);
		char c2 = code.charAt(1);
		char c3 = code.charAt(2);
		if (c1 == 'x'
			&& ((c2 >= '0' && c2 <= '9') || (c2 >= 'A' && c2 <= 'F'))
			&& ((c3 >= '0' && c3 <= '9') || (c3 >= 'A' && c3 <= 'F'))) {
			return Integer.parseInt(code.substring(1), 16);
		}
		
		if (c1 < '0' || c1 > '3') { return -1; }
		if (c2 < '0' || c2 > '7') { return -1; }
		if (c3 < '0' || c3 > '7') { return -1; }
		return Integer.parseInt(code, 8);
	}
    
}
