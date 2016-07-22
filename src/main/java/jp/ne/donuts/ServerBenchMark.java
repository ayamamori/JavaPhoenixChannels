package jp.ne.donuts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.management.RuntimeErrorException;

import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.javatuples.Unit;
import org.phoenixframework.channels.Channel;
import org.phoenixframework.channels.IMessageCallback;
import org.phoenixframework.channels.Socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class ServerBenchMark {


    static final int numTasks = 10;
    static final String host = "";
    //static final String host = "localhost";
    static final String port = "4000";

    static ArrayBlockingQueue<Pair<Socket,Channel>> connectedSocket = new ArrayBlockingQueue<>(numTasks);
    static ArrayBlockingQueue<Long> execTimes = new ArrayBlockingQueue<>(numTasks);

    static final IMessageCallback callback = (msg) -> {
            long time = System.nanoTime(); 
            try {
                execTimes.put((time-msg.getPayload().get("content").asLong())/1_000_000);
            } catch (InterruptedException e) { throw new RuntimeException(e); }
        };

    public static void main(String[] args) throws IllegalStateException, IOException, InterruptedException {
        
        //Make Socket instances
        List<Pair<String,Socket>> sockets = IntStream.range(0,numTasks)
            .mapToObj(String::valueOf)
            .map(str -> Pair.with(str,createSocket(str))).collect(Collectors.toList());

        //Connect to channel
        List<Triplet<String, Socket, Channel>> channels = sockets.parallelStream()
            .map(pair -> {
                try {
                    Socket socket = pair.getValue1();
                    Channel ch = connect(socket)
                            .chan("rooms:lobby", JsonNodeFactory.instance.nullNode());
                    ch.on("new_msg", callback);
                    ch.join().receive("ok", (msg) -> onConnected(socket, ch));
                    return pair.add(Unit.with(ch));
                } catch (IllegalStateException | IOException e) { throw new RuntimeException(e); }
            }).collect(Collectors.toList());

        //guard until finish connecting 
        while(connectedSocket.size()<numTasks){
            Thread.sleep(1000L);
        }
        
        //Push new_msg from one client
        Triplet<String,Socket,Channel> tri = channels.get(0);
        tri.getValue2().push("new_msg", new ObjectMapper().readTree("{\"content\":\""+System.nanoTime()+"\"}"));

        //Guard until finish receiving the message
        while(execTimes.size()<numTasks){
            Thread.sleep(1000L);
        }

        //Disconnect from the server
        connectedSocket.forEach(pair -> disconnect(pair.getValue0()));
        
        //Summarize
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("bench.csv"))){
            execTimes.stream().map(i -> i+"\n").forEach(t -> 
                {try{bw.append(t);}catch(IOException e){}}
            );
        }
        System.out.println(execTimes.stream().collect(Collectors.summarizingDouble(i->i)));
        System.exit(0);

    }
    
    private static Socket createSocket(String userName){
        try {
            return new Socket("ws://"+host+":"+port+"/socket/websocket?user_name="+userName);
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
    
    private static void onConnected(Socket sock, Channel ch){
        connectedSocket.add(Pair.with(sock, ch));
    }
    
    private static Socket disconnect(Socket socket){
        try {
            socket.disconnect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return socket;
    }
    
    

}
