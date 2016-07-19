package jp.ne.donuts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.phoenixframework.channels.Socket;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class ServerBenchMark {


    static int numTasks = 2000;
    static ArrayBlockingQueue<Long> execTimes = new ArrayBlockingQueue<>(numTasks);
    public static void main(String[] args) throws IllegalStateException, IOException, InterruptedException {
        List<Socket> sockets = IntStream.range(0,numTasks)
            .mapToObj(String::valueOf)
            .map(str -> createSocket(str)).collect(Collectors.toList());
        for(Socket s: sockets){
            long startTime = System.currentTimeMillis();
            connect(s).chan("rooms:lobby", JsonNodeFactory.instance.nullNode()).join()
                            .receive("ok", (msg) -> disconnect(s,startTime));
        }
        while(execTimes.size()<numTasks){
            Thread.sleep(1000L);
        }
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("bench.csv"))){
            execTimes.stream().map(i -> i+",\n").forEach(t -> 
                {try{bw.append(t);}catch(IOException e){}}
            );
        }
        System.out.println(execTimes.stream().collect(Collectors.summarizingDouble(i->i)));
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

    }
    
    private static Socket createSocket(String userName){
        try {
            return new Socket("ws://104.155.235.219:4000/socket/websocket?user_name="+userName);
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
            //System.out.println(System.currentTimeMillis()-startTime+"[ms]");
            socket.disconnect();
            execTimes.put(System.currentTimeMillis()-startTime);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return socket;
    }
    
    

}
