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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import dev.hawala.dmachine.engine.Config;
import dev.hawala.dmachine.engine.eLevelVKey;
import dev.hawala.dmachine.engine.iUiDataConsumer;

/**
 * Mapper from Java keystrokes to mesa engine keys.
 * <p>
 * The keyboard-mapping to be used can be configured by calling the
 * {@code map}, {@code mapCtrl} and {@code unmap} methods. These methods
 * are intended for defining keyboard-mappings through configuration
 * files. 
 * <br> 
 * A hard-coded mapping for a german keyboard can be installed with the method
 * {@code mapDefaults_de_DE}.
 * </p>
 * <p>
 * The only unchangeable key assignment is the Control-key (left and right),
 * which acts as modifier for creating the special keys of a Xerox keyboard like
 * Copy, Mode, Props etc..
 * </p>
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class KeyboardMapper {
	
	// Java keycode of the Ctrl key
	private final int CTL_KEY;

	// more or less configurable mapping from Java-Key => MesaKey, without and with Ctrl-Key pressed
	private final Map<Integer,eLevelVKey> normalKeyMapping = new HashMap<>();
	private final Map<Integer,eLevelVKey> ctlKeyMapping = new HashMap<>();
	
	// is the Ctrl key currently pressed?
	private boolean isCtlPressed = false;
	
	// list of key pressed without resp. before the Ctrl key was pressed resp. after Ctrl was released
	private final Map<Integer,eLevelVKey> normalPressed = new HashMap<>();
	
	// list of keys pressed after the Ctrl key was pressed
	private final Map<Integer,eLevelVKey> ctlPressed = new HashMap<>();
	
	// the consumer to the keyboard events, i.e. the mesa side
	private final iUiDataConsumer uiDataConsumer;
	
	private final boolean logKeypressed;
	
	/**
	 * Constructor.
	 * @param consumer the consumer at the mesa side where to forward keystrokes to
	 * @param xeroxControlKeyCode the nimeric keycode for the special Xerox Control Key
	 * @param logKeypressed should all key pressed events be logged to stdout?
	 */
	public KeyboardMapper(iUiDataConsumer consumer, int xeroxControlKeyCode, boolean logKeypressed) {
		this.uiDataConsumer = consumer;
		this.logKeypressed = logKeypressed;
		this.CTL_KEY = xeroxControlKeyCode;
	}
	
	private boolean isPressed(Integer javaKey) {
		return this.normalPressed.containsKey(javaKey) || this.ctlPressed.containsKey(javaKey);
	}
	
	/**
	 * Notify another key pressed on the Java side.
	 * @param key the (Java) keycode for the key that went down
	 */
	public void pressed(int key) {
		
		if (Config.LOG_BITBLT_INSNS && key == 0x000070) {
			// special case F1 for development: enable logging of BITBLT/COLORBLT instructions
			Config.dynLogBitblts = true;
			return;
		}
		
		// when Ctrl goes down: simply remember it, this will influence the next keystrokes  
		if (key == CTL_KEY) {
			this.isCtlPressed = true;
			return;
		}
		
		if (this.logKeypressed) {
			this.logKey(key);
		}
		
		Integer javaKey = Integer.valueOf(key);
		
		if (this.isPressed(javaKey)) { return; } // ignore auto-repeats from the underlying os / ui-toolkit
		
		// map the java key to mesa
		eLevelVKey mesaKey = null;
		Map<Integer,eLevelVKey> keyStates = null;
		if (this.isCtlPressed) {
			mesaKey = this.ctlKeyMapping.get(javaKey);
			keyStates = this.ctlPressed;
		} else {
			mesaKey = this.normalKeyMapping.get(javaKey);
			keyStates = this.normalPressed;
		}
		if (mesaKey != null) {
			keyStates.put(javaKey, mesaKey);
			this.uiDataConsumer.acceptKeyboardKey(mesaKey, true);
		}
	}
	
	/**
	 * Notify another key de-pressed on the Java side.
	 * @param key the (Java) keycode for the key that went up
	 */
	public void released(int key) {
		
		if (key == 0x000070) {
			// special case F1 for development: disable logging of BITBLT/COLORBLT instructions
			Config.dynLogBitblts = false;
			return;
		}

		// if Ctrl goes up: let all Xerox special keys go up
		if (key == CTL_KEY) {			
			// release all keys pressed on the mesa side while the Ctrl key was down
			// but continue waiting the these keys to be released on the java side
			for(Entry<Integer, eLevelVKey> e : this.ctlPressed.entrySet()) {
				this.uiDataConsumer.acceptKeyboardKey(e.getValue(), false);
				this.normalPressed.put(e.getKey(), null);
			}

			// remember that the Ctrl key was released
			this.isCtlPressed = false;
			this.ctlPressed.clear();
			
			// done
			return;
		}
		
		// map the Java key to mesa
		Integer javaKey = Integer.valueOf(key);
		this.release(javaKey,this.normalPressed);
		this.release(javaKey,this.ctlPressed);
	}
	
	// handle releasing a single java key
	private void release(Integer javaKey, Map<Integer,eLevelVKey> keyStates) {
		if (!keyStates.containsKey(javaKey)) { return; }
		eLevelVKey mesaKey = keyStates.get(javaKey);
		if (mesaKey != null) { this.uiDataConsumer.acceptKeyboardKey(mesaKey,  false); }
		keyStates.remove(javaKey);
	}
	
	/*
	 * definition of the mapping
	 */
	
	/**
	 * Add single mapping of a Java key to Xerox keys, both as plain and as special
	 * key when Ctrl is pressed.
	 * 
	 * @param key the Java keycode to map from
	 * @param normalKey the Xerox key to map to when Ctrl is not pressed
	 * @param ctlKey the Xerox key to map to when Ctrl is also pressed
	 * @return this mapper instance (for fluid api)
	 */
	public KeyboardMapper map(int key, eLevelVKey normalKey, eLevelVKey ctlKey) {
		Integer javaKey = Integer.valueOf(key);
		if (normalKey != null) { this.normalKeyMapping.put(javaKey, normalKey); }
		if (ctlKey != null) { this.ctlKeyMapping.put(javaKey, ctlKey); }
		return this;
	}
	
	/**
	 * Add single mapping of a Java key to a Xerox key as plain key.
	 * 
	 * @param key the Java keycode to map from
	 * @param normalKey the Xerox key to map to when Ctrl is not pressed
	 * @return this mapper instance (for fluid api)
	 */
	public KeyboardMapper map(int key, eLevelVKey normalKey) {
		return this.map(key, normalKey, null);
	}
	
	/**
	 * Add single mapping of a Java key to a Xerox key as special key
	 * with Ctrl pressed.
	 * 
	 * @param key the Java keycode to map from
	 * @param ctlKey the Xerox key to map to when Ctrl is also pressed
	 * @return this mapper instance (for fluid api)
	 */
	public KeyboardMapper mapCtrl(int key, eLevelVKey ctlKey) {
		return this.map(key, null, ctlKey);
	}
	
	/**
	 * Remove all mappings for the given Java keycode.
	 * 
	 * @param key the Java keycoce to unnmap
	 * @return this mapper instance (for fluid api)
	 */
	public KeyboardMapper unmap(int key) {
		Integer javaKey = Integer.valueOf(key);
		if (this.normalKeyMapping.containsKey(javaKey)) { this.normalKeyMapping.remove(javaKey); }
		if (this.ctlKeyMapping.containsKey(javaKey)) { this.ctlKeyMapping.remove(javaKey); }
		return this;
	}
	
	/**
	 * Create the default mapping for a german keyboard (somehow minimal, sorry about this).
	 * This mapping is used if no mapping file is specified in the machines configuration
	 * or it is not accessible/readable. 
	 * 
	 * @return this mapper instance (for fluid api)
	 */
	public KeyboardMapper mapDefaults_de_DE() {
		return this
			// first row: digits etc.
			.map(0x000031, eLevelVKey.One)
			// ^ = 'becomes' and ° = 'dereference' ::  .map(0x00000208, eLevelVKey.?)
			.map(0x00000082, eLevelVKey.Bullet)
			.map(0x000032, eLevelVKey.Two)
			.map(0x000033, eLevelVKey.Three)
			.map(0x000034, eLevelVKey.Four)
			.map(0x000035, eLevelVKey.Five)
			.map(0x000036, eLevelVKey.Six)
			.map(0x000037, eLevelVKey.Seven)
			.map(0x000038, eLevelVKey.Eight)
			.map(0x000039, eLevelVKey.Nine)
			.map(0x000030, eLevelVKey.Zero)
			.map(0x010000DF, eLevelVKey.Dash)
			.map(0x000080, eLevelVKey.Equal) // `
			.map(0x000081, eLevelVKey.Equal) // '
			.map(0x0000007F, eLevelVKey.Delete)
			
			// second row: qwertz etc.
			.map(0x000051, eLevelVKey.Q)
			.map(0x000057, eLevelVKey.W)
			.map(0x000045, eLevelVKey.E)
			.map(0x000052, eLevelVKey.R)
			.map(0x000054, eLevelVKey.T)
			.map(0x00005A, eLevelVKey.Z)
			.map(0x000055, eLevelVKey.U)
			.map(0x000049, eLevelVKey.I)
			.map(0x00004F, eLevelVKey.O)
			.map(0x000050, eLevelVKey.P)
			.map(0x010000FC, eLevelVKey.LeftBracket)
			.map(0x00000209, eLevelVKey.RightBracket)
			
			// third row: asdf etc.
			.map(0x000041, eLevelVKey.A)
			.map(0x000053, eLevelVKey.S)
			.map(0x000044, eLevelVKey.D)
			.map(0x000046, eLevelVKey.F)
			.map(0x000047, eLevelVKey.G)
			.map(0x000048, eLevelVKey.H)
			.map(0x00004A, eLevelVKey.J)
			.map(0x00004B, eLevelVKey.K)
			.map(0x00004C, eLevelVKey.L)
			.map(0x010000D6, eLevelVKey.SemiColon)
			.map(0x010000C4, eLevelVKey.Quote)
			.map(0x00000208, eLevelVKey.DoubleQuote)
			
			// fourth row: <yxcv etc.
			// < .map(0x00000099, eLevelVKey.?)
			// | .map(0x0100007C, eLevelVKey.?)
			.map(0x000059, eLevelVKey.Y)
			.map(0x000058, eLevelVKey.X)
			.map(0x000043, eLevelVKey.C)
			.map(0x000056, eLevelVKey.V)
			.map(0x000042, eLevelVKey.B)
			.map(0x00004E, eLevelVKey.N)
			.map(0x00004D, eLevelVKey.M)
			.map(0x0000002C, eLevelVKey.Comma)
			.map(0x0000002E, eLevelVKey.Period)
			.map(0x0000002D, eLevelVKey.Slash)
			
			// others
			.map(0x000020, eLevelVKey.Space)
			.map(0x000010, eLevelVKey.LeftShift)
//			.map(0x000010, eLevelVKey.RightShift)
			.map(0x000009, eLevelVKey.ParaTab)
			.map(0x00000A, eLevelVKey.NewPara)
			.map(0x000008, eLevelVKey.BS)
			.map(0x000014, eLevelVKey.Lock)
			
			// function keys
//			F1 .map(0x000070, eLevelVKey.Y)
//			F2 .map(0x000071, eLevelVKey.Y)
//			F3 .map(0x000072, eLevelVKey.Y)
//			F4 .map(0x000073, eLevelVKey.Y)
//			F5 .map(0x000074, eLevelVKey.Y)
//			F6 .map(0x000075, eLevelVKey.Y)
//			F7 .map(0x000076, eLevelVKey.Y)
//			F8 .map(0x000077, eLevelVKey.Y)
//			F9 .map(0x000078, eLevelVKey.Y)
//			F10 .map(0x000079, eLevelVKey.Y)
//			F11 .map(0x00007A, eLevelVKey.Y)
//			F12 .map(0x00007B, eLevelVKey.Y)
			
			// xerox special keys
			.mapCtrl(0x00001B, eLevelVKey.Stop) // ESC
			.mapCtrl(0x00004D, eLevelVKey.Move) // Ctrl-M
			.mapCtrl(0x000043, eLevelVKey.Copy) // Ctrl-C
			.mapCtrl(0x000053, eLevelVKey.Same) // Ctrl-S
			.mapCtrl(0x00004F, eLevelVKey.Open) // Ctrl-O
			.mapCtrl(0x000050, eLevelVKey.Props)// Ctrl-P
			.mapCtrl(0x000046, eLevelVKey.Find) // Ctrl-F
			.mapCtrl(0x000048, eLevelVKey.Help) // Ctrl-H
			.mapCtrl(0x000055, eLevelVKey.Undo) // Ctrl-U
			.mapCtrl(0x000041, eLevelVKey.Again)// Ctrl-A
			.mapCtrl(0x00004E, eLevelVKey.Next) // Ctrl-N
			
			;
	}
	
	private void logKey(int javaCode) {
		eKeyEventCode kec = eKeyEventCode.get(javaCode);
		System.out.printf("key: x%08x%s%s\n", javaCode, kec != null ? " = " : "", kec != null ? kec.toString() : "");
	}
	
	private final String CTRL_MARK = "Ctrl!";
	
	/**
	 * Load the keyboard mapping from the specified file.
	 * 
	 * @param filename the filename for the keyboard mapping file to load.
	 */
	public void loadConfigFile(String filename) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e1) {
			System.out.printf("Unable to access keyboard map file '%s'\n", filename);
			this.mapDefaults_de_DE();
			return;
			
		}
		try (BufferedReader br = reader) {
			int lineNo = 0;
			String l;
			while((l = br.readLine()) != null) {
				lineNo++;
				String line = l.trim();
				if (line.length() == 0) { continue; }
				if (line.startsWith("#")) { continue; }
				
				String[] mapping = line.split(":");
				if (mapping.length != 2) {
					System.out.printf("Invalid keyboard mapping line[%d]: %s\n", lineNo, l);
					continue;
				}
				
				String xeroxSel = mapping[1].trim();
				eLevelVKey xeroxKey = this.getLevelVKey(xeroxSel);
				if (xeroxKey == null) {
					System.out.printf("Invalid Xerox-key in keyboard mapping line[%d]: %s\n", lineNo, l);
					continue;
				}
				
				String javaSel = mapping[0].trim();
				boolean hasCtrlMod = false;
				int javaCode = -1;
				if (javaSel.startsWith(CTRL_MARK)) {
					hasCtrlMod = true;
					javaSel = javaSel.substring(CTRL_MARK.length()).trim();
				}
				if (javaSel.startsWith("x")) {
					try {
						javaCode = Integer.parseInt(javaSel.substring(1), 16);
					} catch (NumberFormatException nfe) {
						System.out.printf("Invalid hex-code for java-key in keyboard mapping line[%d]: %s\n", lineNo, l);
						continue;
					}
				} else {
					eKeyEventCode javaKey = this.getKeyEventCode(javaSel);
					if (javaKey == null) {
						System.out.printf("Invalid key-name for java-key in keyboard mapping line[%d]: %s\n", lineNo, l);
						continue;
					}
					javaCode = javaKey.getCode();
				}
				
				if (hasCtrlMod) {
					this.mapCtrl(javaCode, xeroxKey);
				} else {
					this.map(javaCode, xeroxKey);
				}
			}
		} catch (IOException e) {
			System.out.printf("Error reading keyboard map file '%s': %s\n", filename, e.getMessage());
			this.mapDefaults_de_DE();
		}
	}
	
	private eKeyEventCode getKeyEventCode(String name) {
		try {
			return eKeyEventCode.valueOf(name);
		} catch (Exception e) {
			return null;
		}
	}
	
	private eLevelVKey getLevelVKey(String name) {
		try {
			return eLevelVKey.valueOf(name);
		} catch (Exception e) {
			return null;
		}
	}
}
