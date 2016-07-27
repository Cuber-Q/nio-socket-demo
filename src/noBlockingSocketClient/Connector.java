package noBlockingSocketClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.Key;
import java.util.Iterator;
import java.util.Set;

public class Connector {
	SocketChannel sc = null;
	String hostName = "";
	Selector selector = null;
	boolean connected = false;
	
	public Connector(String hostname, int port, Selector selector){
		try {
			this.hostName = hostName;
			this.selector = selector;
			sc = SocketChannel.open();
			sc.configureBlocking(false);
			sc.connect(new InetSocketAddress(hostName, port));
			long start = System.currentTimeMillis();
			sc.register(selector, SelectionKey.OP_CONNECT,start);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("error occured when Connector init, hostName="
					+hostName+", port="+port);
			e.printStackTrace();

		}
	}
//	
//	public void connect(Selector selector){
//		try {
//			System.out.println("to connect");
//			sc.configureBlocking(false);
//			sc.register(selector, SelectionKey.OP_CONNECT);
//			sc.finishConnect();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			System.out.println(hostName+" connected failed");
//			e.printStackTrace();
//		}
//	}
	
	public static void main(String[] aegs) throws Exception{
		Selector selector = Selector.open();
		String hostname = "www.baidu.com";
		int port = 80;
//		long start = System.currentTimeMillis();
		Connector connector = new Connector(hostname, port, selector);
		System.out.println("connect ok");
		while(selector.select() > 0){
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> it = keys.iterator();
			while(it.hasNext()){
				SelectionKey key = it.next();
				if(key.isConnectable() && !connector.connected) {
					connector.connected = true;
					Long start = (Long)key.attachment();
					it.remove();
					System.out.println(hostname + " connected, cost time="
							+ (System.currentTimeMillis() - start) + " ms");
				}
			}
		}
	}
}
