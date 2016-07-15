package jp.ne.donuts;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.phoenixframework.channels.Channel;
import org.phoenixframework.channels.Socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class PhoenixClient {
    static String test = "test1";
    static ObjectMapper mapper = new ObjectMapper();
    public static void main(String[] args) throws IOException {
        Socket socket = initSocket("wss://localhost:4001/socket/websocket?user_name="+test);
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
    private static Socket initSocket(String url){
        try {
            Socket socket = new Socket(url);
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null,
                            new X509TrustManager[] { new LooseTrustManager() },
                            new SecureRandom());
            socket.setSSLSocketFactory(sslContext.getSocketFactory());
            socket.setHostnameVerifier(new LooseHostnameVerifier());    
            return socket;
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

}

class LooseTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }
 
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }
 
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}

class LooseHostnameVerifier implements HostnameVerifier {
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }
}