package noBlockingSocketClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;

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
	private LinkedList<Target> toConnect = new LinkedList<Target>();
	private LinkedList<Target> toPrint = new LinkedList<Target>();
	private Selector selector;
	private Object lock = new Object();
	
	class Target {
		public String hostname;
		public int port;
		public InetSocketAddress addr;
		public SocketChannel sc;
		public boolean connected = false;
		public boolean print = false;
		public long start = 0;
		public long cost = 0;
		public Target(String hostname){
			this(hostname,80);
		}
		public Target(String hostname,int port){
			this.hostname = hostname;
			this.port = port;
			addr = new InetSocketAddress(hostname, port);
			start = System.currentTimeMillis();
			
			try {
				sc = SocketChannel.open();
				sc.configureBlocking(false);
				sc.connect(addr);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		@Override
		public String toString(){
			return "Taregt="+"["+hostname+":"+port+"]";
		}
	}
	
	class Connector extends Thread{
		@Override
		public void run() {
			registerTargets();
			processConnectedTargets();
		}
		public Connector(){}
//		public Connector(Target target) throws IOException{
//			target.sc = SocketChannel.open();
//			target.sc.connect(new InetSocketAddress(target.hostname, target.port));
//			target.sc.configureBlocking(false);
//			target.start = System.currentTimeMillis();
//			synchronized(lock){
//				selector.wakeup();
//				target.sc.register(selector, SelectionKey.OP_CONNECT,target.start);
//			}
//			
//			
//		}
		//pick up host from toConnect
		
		//connect to the host
				
		//add into toPrint when the host has been connected successfully
	}
	//pickup all target from the toConn and regist them
	public void registerTargets(){
		synchronized (toConnect) {
			while(toConnect.size() > 0){
				Target target = toConnect.removeFirst();
				try {
					selector.wakeup();
					target.sc.register(selector, SelectionKey.OP_CONNECT,target);
					System.out.println("registerTargets done. "+target);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		}
	}
	public void processConnectedTargets(){
		try {
			while(selector.select()>0){
				for(Iterator<SelectionKey> it=selector.selectedKeys().iterator();
					it.hasNext();){
					SelectionKey key = it.next();
					if(key.isConnectable()){
						//handle connected target
						Target target = (Target)key.attachment();
						target.sc.close();
						addToPrint(target);
						System.out.println("processConnectedTargets done. "+target);
					}
					it.remove();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//put connected target into toPrint list
	public void addToPrint(Target target){
		synchronized (toPrint) {
			toPrint.notify();
			target.cost = System.currentTimeMillis() - target.start;
			toPrint.add(target);
		}
	}
	class Printer extends Thread{
		@Override
		public void run() {
			print();
		}
		//pick up from toPrint
		
		//print relative info, such as host name and cost time
	}
	public void print(){
		try {
			while(true){
				Target target=null;
				synchronized (toPrint) {
					while(toPrint.size() == 0){
						toPrint.wait();
					}
					target = toPrint.removeFirst();
				}
				show(target);
			}
		}catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void show(Target target){
		System.out.println(target+" finished, cost="+target.cost+"ms");
	}
	

	public static void main(String[] args){
		//read hosts from console
		new PingClient();
		//add host into toConnect
	}
	public PingClient(){
		try {
			selector = Selector.open();
			Connector connector = new Connector();
			Printer printer = new Printer();
			connector.start();
			printer.start();
			//要放在最后，不然前两个线程没机会启动起
			reciveInput();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void reciveInput(){
		Scanner scanner = new Scanner(System.in);
		String hostname = "";
		int port = 80;
		System.out.println("ready to recive...");
		while(scanner.hasNextLine()){
			String[] input = scanner.nextLine().split("\\:");
			if(input[0] != ""){
				hostname = input[0];
			}
			if(input[1] != ""){
				port = Integer.valueOf(input[1]);
			}
			Target target = new Target(hostname,port);
			addToConnect(target);
		}
	}
	public void addToConnect(Target target){
		synchronized (toConnect) {
			toConnect.add(target);
			System.out.println("addToConnect done. "+target);
		}
		selector.wakeup();
	}
}
