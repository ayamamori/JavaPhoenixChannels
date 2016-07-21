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


    static final int numTasks = 1000;
    static final String host = "104.155.235.219";
    //static final String host = "localhost";
    static final String port = "4000";

    static ArrayBlockingQueue<Long> execTimes = new ArrayBlockingQueue<>(numTasks);
    public static void main(String[] args) throws IllegalStateException, IOException, InterruptedException {
        List<Socket> sockets = IntStream.range(0,numTasks)
            .mapToObj(String::valueOf)
            .map(str -> createSocket(str)).collect(Collectors.toList());
        /*
        for(Socket s: sockets){
            long startTime = System.nanoTime();
            connect(s).chan("rooms:lobby", JsonNodeFactory.instance.nullNode()).join()
                            .receive("ok", (msg) -> disconnect(s,startTime));
        }
        */
        sockets.parallelStream()
            .map(socket -> {
                try {
                    long startTime = System.nanoTime();
                    return connect(socket)
                            .chan("rooms:lobby", JsonNodeFactory.instance.nullNode())
                            .join()
                            .receive("ok", (msg) -> disconnect(socket,startTime));
                } catch (IllegalStateException | IOException e) {
                    throw new RuntimeException(e);
                }
            }).count();
        while(execTimes.size()<numTasks){
            Thread.sleep(1000L);
        }
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("bench.csv"))){
            execTimes.stream().map(i -> i+",\n").forEach(t -> 
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
    
    private static Socket disconnect(Socket socket, long startTime){
        try {
            //System.out.println(System.currentTimeMillis()-startTime+"[ms]");
            execTimes.put((System.nanoTime()-startTime)/1_000_000);
            socket.disconnect();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return socket;
    }
    
    

}
