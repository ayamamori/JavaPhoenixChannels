package jp.ne.donuts;

import java.io.IOException;

import org.phoenixframework.channels.Socket;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class PhoenixClient {
	public static void main(String[] args) throws IOException {
		Socket socket = new Socket("ws://localhost:4000/socket/websocket");
		socket
			.onOpen(() -> {
				try {
					socket.chan("rooms:lobby", JsonNodeFactory.instance.nullNode())
					.join()
					.receive("ok", msg -> System.out.println("join msg: "+msg));
				} catch (IllegalStateException | IOException e) {
					e.printStackTrace();
				}
			})
			.onClose(() ->System.out.println("close"))
			.onMessage(envelope -> {if(envelope.getRef()==null)System.out.println(envelope);})
			.onError(reason -> System.out.println(reason));
		socket.connect();
		
	}

}
