package jp.ne.donuts;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.phoenixframework.channels.Socket;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class ServerBenchMark {

	public static void main(String[] args) throws IllegalStateException, IOException {
		List<Socket> sockets = IntStream.range(0,2000)
			.mapToObj(String::valueOf)
			.map(str -> createSocket(str)).collect(Collectors.toList());
		for(Socket s: sockets){
			long startTime = System.currentTimeMillis();
			connect(s).chan("rooms:lobby", JsonNodeFactory.instance.nullNode()).join()
							.receive("ok", (msg) -> disconnect(s,startTime));
		}
		/*
		sockets.map(socket -> connect(socket))
			.map(socket -> {
				try {
					return socket.chan("rooms:lobby", JsonNodeFactory.instance.nullNode()).join()
							.receive("ok", (msg) -> disconnect(socket));
				} catch (IllegalStateException | IOException e) {
					throw new RuntimeException(e);
				}
			}).count();
		*/
		System.out.println(System.currentTimeMillis());


	}
	
	private static Socket createSocket(String userName){
		try {
			return new Socket("ws://localhost:4000/socket/websocket?user_name="+userName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Socket connect(Socket socket){
		try {
			socket.connect();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return socket;
	}
	
	private static Socket disconnect(Socket socket, long startTime){
		try {
			System.out.println(System.currentTimeMillis()-startTime+"[ms]");
			socket.disconnect();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return socket;
	}
	
	

}
