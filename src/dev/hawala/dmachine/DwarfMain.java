/*
Copyright (c) 2020, Dr. Hans-Walter Latz
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main program for the Dwarf Mesa emulator family, dispatching to
 * either the Guam machine emulation (Duchess) or the 6085/daybreak
 * emulation (Draco), depending on the machine type selection argument
 * and passing the remaining command line arguments to the invoked
 * emulation program.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2020)
 */
public class DwarfMain {
	
	private static void usage() {
		System.out.println("Usage: Dwarf -duchess|-draco <machine-specific-args...>");
		System.exit(0);
	}
	
	public static void main(String[] args) throws IOException {
		int argCount = args.length;
		if (argCount < 1) {
			usage();
		}
		
		List<String> newArgs = new ArrayList<>();
		
		boolean isDuchess = false;
		boolean isDraco = false;
		for (String arg : args) {
			String lcArg = arg.toLowerCase();
			if ("-duchess".equals(lcArg)) {
				isDuchess = true;
			} else if ("-draco".equals(lcArg)) {
				isDraco = true;
			} else {
				newArgs.add(arg);
			}
		}
		
		if (isDuchess == isDraco) {
			usage();
		}
		
		if (isDuchess) {
			Duchess.main(newArgs.toArray(new String[newArgs.size()]));
		} else {
			Draco.main(newArgs.toArray(new String[newArgs.size()]));
		}
	}

}
