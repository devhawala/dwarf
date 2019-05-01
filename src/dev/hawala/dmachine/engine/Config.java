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
 * Configuration constants (with one exception) controlling the logging
 * and debugging behavior of the mesa engine,
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class Config {
	
	/**
	 * log recognized opcode implementations as they are scanned and added to dispatch tables?
	 */
	public static final boolean LOG_OPCODE_INSTALLATION = false;
	
	/**
	 * log opcodes and their locations as they are executed?
	 */
	public static final boolean LOG_OPCODES = false;
	
	/**
	 * log as flight recorder?
	 * (i.e.: collect but do not write the data to stdout, speeding up
	 * things, but having them at hand if needed, e.g. if a stack trap happens)
	 * (slows down things, but not to the point as writing to stdout)
	 */
	public static final boolean LOG_OPCODES_AS_FLIGHTRECORDER = false;
	
	/**
	 * prepend the stack data before the logged instruction in flight recorder mode?
	 */
	public static final boolean FLIGHTRECORDER_WITH_STACK = false;
	
	/**
	 * use interactive utility for debugging opcode execution?
	 */
	public static final boolean USE_DEBUG_INTERPRETER = false;
	
	/**
	 * log BITBLT/BITBLTX/COLORBLT arguments and the like
	 */
	public static final boolean LOG_BITBLT_INSNS = false;
	
	/**
	 * If LOG_BITBLT_INSNS is true, logging of BITBLT and friends will
	 * only occur if they are executed while this flag is true.
	 * <br>
	 * At runtime, pressing a specific key (normally F1, see class
	 * KeyboardMapper) will set this variable to true, allowing to
	 * restrict logging to specific situations in the UI.
	 */
	public static volatile boolean dynLogBitblts = false;
	
	
	/*
	 * logging in agents
	 */
	
	public static final boolean AGENTS_LOG_DISPLAY = false;
	
	public static final boolean AGENTS_LOG_MOUSE = false;
	
	public static final boolean AGENTS_LOG_KEYBOARD = false;
	
	public static final boolean AGENTS_LOG_DISK = false;
	
	public static final boolean AGENTS_LOG_FLOPPY = false;
	
	public static final boolean AGENTS_LOG_NETWORK = false;

}
