/*
Copyright (c) 2019, Dr. Hans-Walter Latz
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

package dev.hawala.dmachine;

import java.io.File;

import dev.hawala.dmachine.dwarf.eKeyEventCode;

/**
 * Utilities for Dwarf main programs.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2019,2020)
 */
public class Utils {

	
	// check if the filename identifies an readably file
	public static boolean isFileOk(String kind, String filename) {
		if (filename == null) {
			System.err.printf("Error: no filename given for %s\n", kind);
			return false;
		}
		File f = new File(filename);
		if (!f.canRead()) {
			System.err.printf("Error: file '%s' given for %s does not exist or is not readable\n", filename, kind);
			return false;
		}
		return true;
	}
	
	// parse the mac (machine address, processor id) from the given string
	public static String parseMac(String mac, int[] macBytes, int[] macWords) {
		String[] submacs = mac.split("-");
		if (submacs.length != macBytes.length) {
			System.err.printf("Error: invalid processor id format (not XX-XX-XX-XX-XX-XX): %s\n", mac);
			return "";
		}
		
		for (int i = 0; i < macBytes.length; i++) {
			try {
				macBytes[i] = Integer.parseInt(submacs[i], 16) & 0xFF;
			} catch (Exception e) {
				System.err.printf("Error: invalid processor id format (not XX-XX-XX-XX-XX-XX): %s\n", mac); 
				return "";
			}
		}
		
		String recognizedMacId = String.format(
				"%02X-%02X-%02X-%02X-%02X-%02X", 
				macBytes[0], macBytes[1], macBytes[2], macBytes[3], macBytes[4], macBytes[5]);
		macWords[0] = (macBytes[0] << 8) | macBytes[1];
		macWords[1] = (macBytes[2] << 8) | macBytes[3];
		macWords[2] = (macBytes[4] << 8) | macBytes[5];
		return recognizedMacId;
	}
	
	// parse the keycode either as 0x-hexcode or as VK_-name of the key
	public static int parseKeycode(String keycode) {
		if (keycode.startsWith("x")) {
			try {
				return Integer.parseInt(keycode.substring(1), 16);
			} catch (NumberFormatException nfe) {
				System.out.printf("Invalid hex-code for keycode '%s', using Ctrl-Key instead\n", keycode);
				return eKeyEventCode.VK_CONTROL.getCode();
			}
		} else {
			eKeyEventCode javaKey = null;
			try {
				javaKey = eKeyEventCode.valueOf(keycode);
			} catch (Exception e) {
				// ignored
			}
			if (javaKey == null) {
				System.out.printf("Invalid key-name '%s' for keycode, using Ctrl-Key instead\n", keycode);
				return eKeyEventCode.VK_CONTROL.getCode();
			}
			return javaKey.getCode();
		}
	}

}