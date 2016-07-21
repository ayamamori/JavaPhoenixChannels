package jp.ne.donuts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.javatuples.Pair;
import org.phoenixframework.channels.Socket;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class ServerBenchMark {


    static final int numTasks = 1000;
    static final String host = "";
    //static final String host = "localhost";
    static final String port = "4000";

    static ArrayBlockingQueue<Pair<String,Long>> execTimes = new ArrayBlockingQueue<>(numTasks);
    public static void main(String[] args) throws IllegalStateException, IOException, InterruptedException {
        List<Pair<String,Socket>> sockets = IntStream.range(0,numTasks)
            .mapToObj(String::valueOf)
            .map(str -> Pair.with(str,createSocket(str))).collect(Collectors.toList());

        sockets.parallelStream()
            .map(pair -> {
                try {
                    String port = pair.getValue0();
                    Socket socket = pair.getValue1();
                    long startTime = System.nanoTime();
                    return connect(socket)
                            .chan("rooms:lobby", JsonNodeFactory.instance.nullNode())
                            .join()
                            .receive("ok", (msg) -> disconnect(socket,startTime,port));
                } catch (IllegalStateException | IOException e) {
                    throw new RuntimeException(e);
                }
            }).count();

        while(execTimes.size()<numTasks){
            Thread.sleep(1000L);
        }

        try(BufferedWriter bw = new BufferedWriter(new FileWriter("bench.csv"))){
            execTimes.stream().map(i -> i.getValue0()+","+i.getValue1()+",\n").forEach(t -> 
                {try{bw.append(t);}catch(IOException e){}}
            );
        }
        System.out.println(execTimes.stream().map(p -> p.getValue1()).collect(Collectors.summarizingDouble(i->i)));
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
    
    private static Socket disconnect(Socket socket, long startTime, String port){
        try {
            execTimes.put(Pair.with(port,(System.nanoTime()-startTime)/1_000_000));
            socket.disconnect();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return socket;
    }
    
    

}
