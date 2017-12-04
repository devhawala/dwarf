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

import java.util.Properties;

/**
 * Extension of plain Java Properties allowing to access properties
 * with specific data types as well as with default values if no
 * configured value is present.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public class PropertiesExt extends Properties {
	private static final long serialVersionUID = -2593299943684111928L;
	
	/**
	 * Read a string property with a default value.
	 * @param name the name of the property to read
	 * @param defValue default value if no value is present in the properties
	 * @return the string value for the property
	 */
	public String getString(String name, String defValue) {
		if (!this.containsKey(name)) { return defValue; }
		String val = this.getProperty(name);
		if (val == null || val.length() == 0) { return null; }
		return val;
	}
	
	/**
	 * Read a string with the empty string as default value.
	 * @param name the name of the property to read
	 * @return the string value for the property
	 */
	public String getString(String name) {
		return this.getString(name, "");
	}
	
	/**
	 * Read an integer property with as default value.
	 * @param name the name of the property to read
	 * @param defValue the default value to return if the property
	 *   is not configured of the value cannot be converted to an integer
	 * @return the integer value
	 */
	public int getInt(String name, int defValue) {
		if (!this.containsKey(name)) { return defValue; }
		try {
			return Integer.parseInt(this.getProperty(name));
		} catch (NumberFormatException exc) {
			return defValue;
		}
	}
	
	/**
	 * Read an integer property with default value {@code 0}.
	 * @param name the name of the property to read
	 * @return the integer value
	 */
	public int getInt(String name) {
		return this.getInt(name, 0);
	}
	
	/**
	 * Read a boolean property with a default value.
	 * @param name the name of the property to read
	 * @param defValue value to return if the property has no
	 *   value.
	 * @return {@code true} if the text value of the property is
	 * one of 'true', 'yes' or 'y' (resp. the default value)
	 */
	public boolean getBoolean(String name, boolean defValue) {
		if (!this.containsKey(name)) { return defValue; }
		String val = this.getProperty(name);
		if (val == null || val.length() == 0) { return false; }
		val = val.toLowerCase();
		return val.equals("true") || val.equals("yes") || val.equals("y");
	}
	
	/**
	 * Read a boolean property with default value {@code false}.
	 * @param name the name of the property to read
	 * @return {@code true} if the text value of the property is
	 * one of 'true', 'yes' or 'y' or else {@code false}
	 */
	public boolean getBoolean(String name) {
		return this.getBoolean(name, false);
	}
}
