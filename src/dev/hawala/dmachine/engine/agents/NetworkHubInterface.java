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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Implementation of the the NetHub interface for the mesa NetworkAgent. 
 * 
 * @author Dr. Hans-Walter Latz / Berlin (2018)
 */
public class NetworkHubInterface implements iNetDeviceInterface {
	
	public static final int MIN_NET_PACKET_LEN = 14; // 2x 48 bit (MAC) + 16 bit (etherType) 
	public static final int MAX_NET_PACKET_LEN = 766; // +2 byte for length ~> 768 bytes
	
	private static final int RETRY_INTERVAL = 2000; // wait 2 seconds before retrying a reconnect 
	
	private static final int MAX_QUEUE_LEN = 64;
	
	private final String hubHost;
	private final int hubSocket;
	
	// these are transient and must be used synchronized!
	private Socket sock = null;
	private InputStream istream = null;
	private OutputStream ostream = null;
	
	// these synchronize by themselves
	private final PacketQueue outgoing = new PacketQueue();
	private final PacketQueue ingoing = new PacketQueue();
	private final IOThread outImpl;
	private final IOThread inImpl;
	private final Thread outThread;
	private final Thread inThread;
	
	/**
	 * Construct the hub interface and start sending/receiving,
	 * more or less simulating a "plug in the connector cable". 
	 * 
	 * @param hubHost network name for the host running the hub
	 * @param hubSocket socket number for the hub
	 */
	public NetworkHubInterface(String hubHost, int hubSocket) {
		this.hubHost = hubHost;
		this.hubSocket = hubSocket;
		
		this.outImpl = new IOThread("NetworkHubInterface-sender", () -> {
			Packet p = this.outgoing.get();
			this.send(p);
			freePacket(p);
		});
		this.outThread = new Thread(this.outImpl);
		this.outThread.setName(this.outImpl.getName());
		
		this.inImpl = new IOThread("NetworkHubInterface-receiver", () -> {
			Packet p = getPacket();
			this.receive(p);
			if (this.ingoing.hasSpace()) {
				this.ingoing.put(p); 
			} else {
				freePacket(p); // packet effectively lost due to "buffer overrun"
			}
		});
		this.inThread = new Thread(this.inImpl);
		this.inThread.setName(this.inImpl.getName());
		
		this.outThread.start();
		this.inThread.start();
	}
	
	/**
	 * This cannot be undone! (reconnecting to the hub
	 * requires a new instance of this class).
	 * @see dev.hawala.dmachine.engine.agents.iNetHubInterface#shutdown()
	 */
	@Override
	public void shutdown() {
		this.outImpl.stop();
		this.outThread.interrupt();
		this.inImpl.stop();
		this.inThread.interrupt();
	}
	
	/* (non-Javadoc)
	 * @see dev.hawala.dmachine.engine.agents.iNetHubInterface#setNewPacketNotifier(dev.hawala.dmachine.engine.agents.NetworkHubInterface.PacketActor)
	 */
	@Override
	public void setNewPacketNotifier(iNetDeviceInterface.PacketActor notifier) {
		this.ingoing.setNewPacketNotifier(notifier);
	}
	
	/* (non-Javadoc)
	 * @see dev.hawala.dmachine.engine.agents.iNetHubInterface#enqueuePacket(byte[], int, boolean)
	 */
	@Override
	public int enqueuePacket(byte[] srcBuffer, int byteCount, boolean feedback) {
		if (!this.outgoing.hasSpace()) { return 0; } // packet cannot be sent (possibly lost) due to "buffer overrun"
		Packet p = getPacket();
		int len = p.copyFrom(srcBuffer, byteCount);
		if (len < 1) {
			freePacket(p);
			return 0;
		}
		this.outgoing.put(p);
		
		if (feedback) {
			Packet fb = getPacket();
			fb.copyFrom(srcBuffer, byteCount);
			this.ingoing.put(fb);
			System.out.println(">>>>> sent Packet fed back!!");
		}
		
		return len;
	}
	
	/* (non-Javadoc)
	 * @see dev.hawala.dmachine.engine.agents.iNetHubInterface#dequeuePacket(byte[], int)
	 */
	@Override
	public int dequeuePacket(byte[] trgBuffer, int maxLength) {
		Packet p = this.ingoing.peek();
		if (p == null) { return 0; }
		int len = p.copyTo(trgBuffer, maxLength);
		freePacket(p);
		return len;
	}
	
	/*
	 * internal packets
	 */
	
	/**
	 * Class for a packet been transmitted.
	 */
	private static class Packet {
		
		// packet data buffer
		private byte[] buffer = new byte[MAX_NET_PACKET_LEN + 2];
		
		// number of valid bytes in the buffer
		private int currLength = 0;
		
		/**
		 * Fill this packet from an external data source. 
		 * 
		 * @param src source buffer
		 * @param count number of significant bytes in {@code ssrc}
		 * @return number of bytes copied.
		 */
		public int copyFrom(byte[] src, int count) {
			count = Math.min(count, src.length);
			int len = Math.min(MAX_NET_PACKET_LEN, Math.max(MIN_NET_PACKET_LEN, count));
			if (len == MIN_NET_PACKET_LEN) { return 0; } // no payload !!!
			
			this.buffer[0] = (byte)((len >> 8) & 0xFF);
			this.buffer[1] = (byte)(len & 0xFF);
			
			System.arraycopy(src, 0, this.buffer, 2, len);
			this.currLength = len + 2;
			return len;
		}
		
		/**
		 * Fill an external data target with this packets content.
		 * 
		 * @param trg target buffer
		 * @param maxLength maximum number of bytes to copy
		 * @return number of bytes copied.
		 */
		public int copyTo(byte[] trg, int maxLength) {
			if (this.currLength < 2) { return 0; } // not even content length?
			int contentLength = ((this.buffer[0] << 8) & 0xFF00) | (this.buffer[1] & 0xFF);
			maxLength = Math.min(maxLength, trg.length);
			int len = Math.min(Math.min(contentLength, this.currLength - 2), Math.max(0, maxLength));
			if (len > 0) {
				System.arraycopy(this.buffer, 2, trg, 0, len);
			}
			return len;
		}
		
		/**
		 * Write the current packet content to the given output stream.
		 * @param stream target stream to write to.
		 * @throws IOException exception thrown by the stream operation.
		 */
		public void write(OutputStream stream) throws IOException {
			stream.write(this.buffer, 0, this.currLength);
		}
		
		/**
		 * Read the current packet content from the given input stream.
		 * @param stream source stream to read from.
		 * @throws IOException exception thrown by the stream operation.
		 */
		public void read(InputStream stream) throws IOException {
			this.currLength = stream.read(this.buffer, 0, this.buffer.length);
		}
		
		/**
		 * Set the packets length to 0.
		 * 
		 * @return the packet for method chaining.
		 */
		public Packet reset() {
			this.currLength = 0;
			return this;
		}
	}
	
	// list of unused packets
	private static final List<Packet> freePackets = new ArrayList<>();
	
	private static Packet getPacket() {
		synchronized(freePackets) {
			if (freePackets.isEmpty()) {
				return new Packet();
			} else {
				return freePackets.remove(freePackets.size() - 1); // removing at tail is said to be faster
			}
		}
	}
	
	private static void freePacket(Packet p) {
		if (p == null) { return; }
		synchronized(freePackets) {
			freePackets.add(p.reset());
		}
	}
	
	/**
	 * Packet queue class for buffering packets to send / received,
	 * supporting the producer-consumer discipline by 2 threads. 
	 */
	private static class PacketQueue {
		
		private final Queue<Packet> q = new LinkedList<>();
		
		private PacketActor notifier = null;
		
		public void setNewPacketNotifier(PacketActor notifier) {
			synchronized(this.q) {
				this.notifier = notifier;
			}
		}
		
		/**
		 * @return {@code false} if the queue already holds
		 *   the nominal maximum packet count, meaning no
		 *   packet <i>should</i> be added currently (this is
		 *   however not a hard limit, only a hint!). 
		 */
		public boolean hasSpace() {
			synchronized(this.q) {
				return this.q.size() < MAX_QUEUE_LEN;
			}
		}
		
		/**
		 * Append a packet to the queue.
		 * @param p the packet to append.
		 */
		public void put(Packet p) {
			if (p == null) { return; }
			synchronized(this.q) {
				this.q.add(p);
				this.q.notifyAll();
				if (this.notifier != null) {
					try {
						this.notifier.handleSinglePacket();
					} catch(InterruptedException e) {
						/* ignored */
					}
 				}
			}
		}
		
		/**
		 * Get the next packet in the queue and wait for a packet
		 * to arrive if the queue is empty.
		 * 
		 * @return the packet received.
		 * @throws InterruptedException if waiting for a new
		 *   packet was externally interrupted.
		 */
		public Packet get() throws InterruptedException {
			synchronized(this.q) {
				while (this.q.isEmpty()) {
					this.q.wait();
				}
				return this.q.poll();
			}
		}
		
		/**
		 * Get the next packet in the queue, returning {@code null}
		 * if the queue is currently empty.
		 * 
		 * @return the next packet from the queue or {@code null} if
		 *   none present.
		 */
		private Packet peek() {
			synchronized(this.q) {
				if (this.q.isEmpty()) {
					return null;
				}
				return this.q.remove();
			}
		}
	}
	
	/*
	 * connection management
	 */

	// this *must* be called in synchronized(this) 
	private void connect() throws InterruptedException {
		while(this.sock == null) {
			try {
				this.sock = new Socket(this.hubHost, this.hubSocket);
				this.sock.setTcpNoDelay(true);
				this.istream = this.sock.getInputStream();
				this.ostream = this.sock.getOutputStream();
			} catch(UnknownHostException uhe) {
				System.err.printf("** Unknown host: '%s', network hub unreachable\n", this.hubHost);
				this.wait(); // wait forever resp. until interrupted
			} catch(IOException ioe) {
				System.err.printf("IOException while connecting: %s\n", ioe.getMessage());
				this.disconnect(); // cleanup the possibly partial connect
				this.wait(RETRY_INTERVAL);
			}
		}
	}
	
	// this *must* be called in synchronized(this) 
	private void disconnect() {
		if (this.istream != null) {
			try {this.istream.close(); } catch(Exception e) { }
			this.istream = null;
		}
		if (this.ostream != null) {
			try {this.ostream.close(); } catch(Exception e) { }
			this.ostream = null;
		}
		if (this.sock != null) {
			try {this.sock.close(); } catch(Exception e) { }
			this.sock = null;
		}
	}
	
	/*
	 * "real" packet transmission with automatic (re)connection
	 */
	
	private void send(Packet p) throws InterruptedException {
		OutputStream stream = null;
		while(true) {
			synchronized(this) {
				if (this.ostream == null || stream == this.ostream) {
					this.disconnect();
					this.connect();
				}
				stream = this.ostream;
			}
			try {
				p.write(stream);
				return;
			} catch (IOException e) {
				System.err.printf("IOException while sending: %s\n", e.getMessage());
				// continue sending after reconnecting to hub
			}
		}
	}
	
	private void receive(Packet p) throws InterruptedException {
		InputStream stream = null;
		while(true) {
			synchronized(this) {
				if (this.istream == null || stream == this.istream) {
					this.disconnect();
					this.connect();
				}
				stream = this.istream;
			}
			try {
				p.read(stream);
				return;
			} catch (IOException e) {
				System.err.printf("IOException while receiving: %s\n", e.getMessage());
				// continue receiving after reconnecting to hub
			}
		}
	}
	
	/*
	 * I/O thread implementation
	 */
	
	private static class IOThread implements Runnable {
		
		private final String name;
		private final PacketActor actor;
		
		private boolean stop = false;
		
		private IOThread(String name, PacketActor actor) {
			this.name = name;
			this.actor = actor;
		}
		
		public String getName() { return this.name; }
		
		public synchronized void stop() {
			this.stop = true;
		}
		
		private synchronized boolean doStop() {
			return this.stop;
		}

		@Override
		public void run() {
			try {
				while(!this.doStop()) {
					this.actor.handleSinglePacket();
				}
			} catch (InterruptedException ie) {
				System.out.printf("Stopping IOThread %s (interrupted))\n", this.name);
			} catch (Exception exc) {
				System.out.printf("Stopping IOThread %s due to %s: %s\n",
						this.name, exc.getClass().getSimpleName(), exc.getMessage());
				exc.printStackTrace();
			}
		}
	}
	
}
