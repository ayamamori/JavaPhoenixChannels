package jp.ne.donuts;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.phoenixframework.channels.Socket;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class ServerBenchMark {

	static long startTime;
	public static void main(String[] args) throws IllegalStateException, IOException {
		List<Socket> sockets = IntStream.range(0,3000)
			.mapToObj(String::valueOf)
			.map(str -> createSocket(str)).collect(Collectors.toList());
		startTime = System.currentTimeMillis();
		for(Socket s: sockets){
			connect(s).chan("rooms:lobby", JsonNodeFactory.instance.nullNode()).join()
							.receive("ok", (msg) -> disconnect(s));
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
	
	private static Socket disconnect(Socket socket){
		try {
			System.out.println(System.currentTimeMillis()-startTime+"[ms]");
			socket.disconnect();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return socket;
	}
	
	

}
