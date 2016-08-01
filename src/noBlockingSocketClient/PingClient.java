package noBlockingSocketClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
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
	
	//a connection task
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
			
			try {
				sc = SocketChannel.open();
				sc.configureBlocking(false);
				sc.connect(addr);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println(hostname+" exxception, e="+e);
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
			while(true){
				registerTargets();
				processConnectedTargets();
			}
		}
	}
	//pickup all target from the toConn and regist them
	public void registerTargets(){
		while(toConnect.size() > 0){
			synchronized (toConnect) {
				Target target = toConnect.removeFirst();
				try {
					selector.wakeup();
					target.start = System.currentTimeMillis();
					target.sc.register(selector, SelectionKey.OP_CONNECT,target);
					System.out.println("registerTargets done. "+target);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		}
	}
	//add into toPrint when the target has been finished successfully
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
						target.cost = System.currentTimeMillis() - target.start;
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
//			toPrint.notify();
			toPrint.add(target);
		}
	}
	class Printer extends Thread{
		@Override
		public void run() {
			print(System.currentTimeMillis());
		}
		
	}
	//pick up from toPrint
	//print relative info, such as host name and cost time
	public void print(long current){
		try {
			while(true){
				Target target=null;
				synchronized (toPrint) {
					while((System.currentTimeMillis() - current < 5*1000)
							&& toPrint.size() > 0){
						target = toPrint.removeFirst();
						current = System.currentTimeMillis();
					}
					if((System.currentTimeMillis() - current > 5*1000)){
						System.out.println("shutdown while no more data more than 5s");
						System.exit(0);
					}
				}
				show(target);
			}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void show(Target target){
		if(null == target) return;
		System.out.println("["+target+" finished, cost="+target.cost+"ms]");
		System.out.println();
	}
	
	public static void main(String[] args){
		new PingClient();
	}
	
	public PingClient(){
		try {
			selector = Selector.open();
			Connector connector = new Connector();
			Printer printer = new Printer();
			connector.start();
			printer.start();
			//要放在最后，不然前两个线程没机会启动起
//			reciveInputFromConsole();
			autoReciveInput();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//read hosts and port from console
	public void reciveInputFromConsole(){
		Scanner scanner = new Scanner(System.in);
		System.out.println("ready to recive from console...");
		while(scanner.hasNextLine()){
			String[] input = scanner.nextLine().split("\\:");
			createTarget(input);
		}
	}
	public void autoReciveInput(){
		System.out.println("ready to auto recive...");
		ArrayList<String> inputs = new ArrayList<String>();
		inputs.add("www.baidu.com:80");
		inputs.add("www.douban.com:80");
		inputs.add("www.1931.com:80");
//		inputs.add("www.yy.com:80");
		inputs.add("carrot.yy.com:80");
		inputs.add("livemgr.yy.com:80");
		inputs.add("tmall.com:80");
		inputs.add("jd.com:80");
		for(String str : inputs){
			createTarget(str.split("\\:"));
		}
	}
	
	public void createTarget(String[] input){
		String hostname = "";
		int port = 80;
		if(input.length == 1){
			hostname = input[0];
		}else{
			if(input[0] != ""){
				hostname = input[0];
			}
			if(input[1] != ""){
				port = Integer.valueOf(input[1]);
			}
		}
		Target target = new Target(hostname,port);
		addToConnect(target);
	}
	public void addToConnect(Target target){
		synchronized (toConnect) {
			toConnect.add(target);
			System.out.println("addToConnect done. "+target);
			selector.wakeup();
		}
	}
}
