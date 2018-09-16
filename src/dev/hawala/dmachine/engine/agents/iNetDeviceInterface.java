/*
Copyright (c) 2018, Dr. Hans-Walter Latz
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

package dev.hawala.dmachine.engine.agents;

/**
 * Interface between the NetworkAgent and the implementation of
 * the network device.
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2018)
 */
public interface iNetDeviceInterface {

	/**
	 * Callback for signaling that a new packet arrived.
	 */
	@FunctionalInterface
	public interface PacketActor {
		void handleSinglePacket() throws InterruptedException;
	}

	/**
	 * Stop sending/receiving ("unplug the network cable").
	 */
	void shutdown();

	/**
	 * Register the callback to be called when a new packet arrived.
	 * 
	 * @param notifier the callback to use.
	 */
	void setNewPacketNotifier(PacketActor notifier);

	/**
	 * Put a raw packet in the send queue for transmission
	 * to the hub.
	 * 
	 * @param srcBuffer buffer holding the packet raw content
	 * @param byteCount number of bytes to send from {@code byteCount},
	 *   this should be between 14 (ethernet header length) and 766
	 *   (max. packet length supported here),
	 * @param feedback is the packet also to be sent to the originator?
	 * @return the number of bytes enqueued for this packet or 0 if
	 *   the packet was not accepted (send queue full, too small, ...)
	 */
	int enqueuePacket(byte[] srcBuffer, int byteCount, boolean feedback);

	/**
	 * Attempt to retrieve a raw packet from the receive queue.
	 * 
	 * @param trgBuffer buffer where
	 *  to put the packet raw content.
	 * @param maxLength the maximum length allowed to use in {@code trgBuffer}
	 * @return the number of bytes received or 0 if the receive queue
	 *   is empty.
	 */
	int dequeuePacket(byte[] trgBuffer, int maxLength);

}