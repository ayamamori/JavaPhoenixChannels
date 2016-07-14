package jp.ne.donuts;

import java.io.IOException;

import org.phoenixframework.channels.Channel;
import org.phoenixframework.channels.Socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class PhoenixClient {
	static String test = "test1";
	static ObjectMapper mapper = new ObjectMapper();
	public static void main(String[] args) throws IOException {
		Socket socket = new Socket("ws://localhost:4000/socket/websocket?user_name="+test);
		Channel channel = socket.chan("rooms:lobby", JsonNodeFactory.instance.nullNode());
		socket
			.onOpen(() -> {
				try {
					channel
					.join()
					.receive("ok", msg -> System.out.println("join msg: "+msg));
				} catch (IllegalStateException | IOException e) {
					e.printStackTrace();
				}
			})
			.onClose(() ->System.out.println("close"))
			.onMessage(envelope -> {
				try {
					System.out.println("Received: "+envelope);
					if(envelope.getRef()==null){
						return;
					}	
					if(envelope.getRef().equals("1")){
						/*
						JsonNode node = mapper.readTree("{\"content\":\"hoge\",\"to\":\"test2\"}");
						channel.push("msg_to", node);
						*/
						JsonNode node = mapper.readTree("{\"content\":\""+test+"\"}");
						channel.push("new_msg", node);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			})
			.onError(reason -> System.out.println(reason));
		socket.connect();
		
	}

}
