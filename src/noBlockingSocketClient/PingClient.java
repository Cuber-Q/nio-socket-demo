package noBlockingSocketClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * ping多个网站，并打印各自的耗时
 * 
 * 主线程从控制台读取网址，放进连接队列
 * Connector线程向从连接读到的网址发起连接，连接成功，放进完成队列
 * Printer线程从完成队列读取，打印
 * 三个线程，两个公共资源，全部是异步操作
 * @author Cuber_Q
 *
 */
public class PingClient {
	private LinkedList toConnect;
	private LinkedList toPrint;
	private Selector selector;
	private Object lock = new Object();
	
	class Target {
		public String hostname;
		public int port;
		public SocketChannel sc;
		public boolean connected = false;
		public boolean print = false;
		public long start = 0;
		public int cost = 0;
	}
	
	class Connector implements Runnable{
		@Override
		public void run() {
			
		}
		public Connector(){}
		public Connector(Target target) throws IOException{
			target.sc = SocketChannel.open();
			target.sc.connect(new InetSocketAddress(target.hostname, target.port));
			target.sc.configureBlocking(false);
			target.start = System.currentTimeMillis();
			synchronized(lock){
				selector.wakeup();
				target.sc.register(selector, SelectionKey.OP_CONNECT,target.start);
			}
		}
		//pick up host from toConnect
		
		//connect to the host
				
		//add into toPrint when the host has been connected successfully
	}
	class Printer implements Runnable{
		private String host;
		private String time;
		@Override
		public void run() {
			
		}
		//pick up from toPrint
		
		//print relative info, such as host name and cost time
	}

	public static void main(String[] args){
		//read hosts from console
		
		//add host into toConnect
	}
}
