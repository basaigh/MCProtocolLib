package ch.spacebase.mcprotocol.standard.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.spacebase.mcprotocol.net.Client;
import ch.spacebase.mcprotocol.net.ServerConnection;
import ch.spacebase.mcprotocol.packet.Packet;

public class PacketTimeUpdate extends Packet {

	public long age;
	public long time;
	
	public PacketTimeUpdate() {
	}
	
	public PacketTimeUpdate(long age, long time) {
		this.age = age;
		this.time = time;
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		this.age = in.readLong();
		this.time = in.readLong();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(this.age);
		out.writeLong(this.time);
	}

	@Override
	public void handleClient(Client conn) {
	}
	
	@Override
	public void handleServer(ServerConnection conn) {
	}
	
	@Override
	public int getId() {
		return 4;
	}
	
}
