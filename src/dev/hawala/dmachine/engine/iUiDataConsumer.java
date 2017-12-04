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
 * Callbacks provided by the (device-) Agents of the mesa engine to the UI
 * for asynchronously receiving UI events or initiating display updates.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2017)
 */
public interface iUiDataConsumer {
	
	/**
	 * Agent callback to inform the mesa engine that a keyboard key
	 * was pressed or released.
	 * 
	 * @param key the logical mesa engine key that changed its state.
	 * @param isPressed is the button identified by {@code key} currently
	 *   pressed ({@code true}) or released ({@code false})?
	 */
	void acceptKeyboardKey(eLevelVKey key, boolean isPressed);
	
	/**
	 * Agent callback to inform the mesa engine about a change
	 * of the mouse buttons depressions.
	 *  
	 * @param key the mouse key pressed or released, with one of the
	 *   values 1 for the left mouse button, 2 for the middle mouse button
	 *   and 3 for the right mouse button.
	 * @param isPressed if the button identified by {@code key} currently
	 *   pressed ({@code true}) or released ({@code false})?
	 */
	void acceptMouseKey(int key, boolean isPressed);
	
	/**
	 * Agent callback to inform the mesa engine about a new mouse pointer
	 * position.
	 * 
	 * @param x the new x coordinate of the mouse pointer.
	 * @param y the new y coordinate of the mouse pointer.
	 */
	void acceptMousePosition(int x, int y);
	
	/**
	 * Functional interface (lambda) defining the method for setting the cursor bitmap.
	 */
	@FunctionalInterface
	public interface PointerBitmapAcceptor {
		
		/**
		 * Method for setting the cursor bitmap.
		 * 
		 * @param bitmap bits for the cursor, with each entry in the array defining
		 *   a 16 bit line of the cursor, the array should have a length of 16 entries.
		 * @param hotspotX horizontal position of the hotspot in the cursors bitmap.
		 * @param hotspotY vertical position of the hotspot in the cursors bitmap.
		 */
		void setPointerBitmap(short[] bitmap, int hotspotX, int hotspotY);
	}
	
	/**
	 * Register the callback to be used by the mesa engine to set a
	 * new shape for the cursor bitmap. The callback is registered once
	 * at initialization time of the UI.
	 *  
	 * @param acpt the callback for setting a new cursor bitmap.
	 */
	void registerPointerBitmapAcceptor(PointerBitmapAcceptor acpt);
	
	/**
	 * Register the callback to be used by the mesa machine to refresh the visible
	 * UI state, i.e. the display bitmap or the MP code or the statistics.
	 * The callback is registered once at initialization time of the UI.
	 * 
	 * @param refresher the set of callbacks to the UI to used by the mesa engine,
	 */
	void registerUiDataRefresher(iMesaMachineDataAccessor refresher);
	
}
