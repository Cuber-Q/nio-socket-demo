package noBlockingSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
/**
 * 非阻塞与多线程的混合使用
 * 
 * 用新线程进行accept()，
 * 用非阻塞进行recive()和send()
 * @author Cuber_Q
 *
 */
public class NoBlockingMultiThreadServer {
	private ServerSocketChannel serverChannel;
	private Selector selector;
	private Charset charSet = Charset.forName("GBK");
	private Object lock = new Object();
	
	public NoBlockingMultiThreadServer(){
		System.out.println("init...");
		try {
			//创建selector对象
			selector = Selector.open();
			serverChannel = ServerSocketChannel.open();
			serverChannel.socket().setReuseAddress(true);
			//让accept()成为阻塞式
//			serverChannel.configureBlocking(false);
			serverChannel.bind(new InetSocketAddress(8081));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("server start...");
	}
	public void service() throws IOException{
		while(true){
			/**
			 * 服务一启动，accept线程先启动，接着service()启动
			 * selecte()要一直询问同步资源allkeys，
			 * 当有客户端连接时，accept()方法试图去操作 allkeys(注册)，但allkeys
			 * 一直被selecte()占用，导致阻塞，所以客户端连接不上。
			 * 
			 * 所以当有新客户端连进来，要注册时
			 * 必须暂停select()方法，释放出allkeys
			 */
			synchronized (lock) {
				
			}
			if(selector.select() == 0)continue;
			//有多个通道准备就绪
			Set<SelectionKey> keySet = selector.selectedKeys();
			Iterator<SelectionKey> it = keySet.iterator();
			SelectionKey key = null;
			while(it.hasNext()){
				key = it.next();
				it.remove();
//					if(key.isAcceptable()){
//						accept();
//					}
				if(key.isReadable()){
					recive(key);
				}
				if(key.isWritable()){
					send(key);
				}
					
			}
			
		} 
	}
	public String decode(ByteBuffer buffer){
		CharBuffer charBuffer = charSet.decode(buffer);
		return charBuffer.toString();
	}
	public ByteBuffer encode(String str){
		return charSet.encode(str);
	}
	public void accept(){
		while(true){
			try {
				SocketChannel sc = serverChannel.accept();
				System.out.println("key.isAcceptable(): "+sc.socket().getPort());
				sc.configureBlocking(false);
				ByteBuffer dst = ByteBuffer.allocate(4096);
				synchronized (lock) {
					/**
					 * 如果正好在执行select()，且处于阻塞，
					 * 则唤醒它，立即退出select()
					 */
					selector.wakeup();
					sc.register(selector, 
							SelectionKey.OP_READ|
							SelectionKey.OP_WRITE,dst);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void recive(SelectionKey key){
		System.out.println("key.isReadable()");
		SocketChannel sc = (SocketChannel)key.channel();
		ByteBuffer buffer = (ByteBuffer)key.attachment();
		
		ByteBuffer readBuffer = ByteBuffer.allocate(32);
		try {
			sc.read(readBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		readBuffer.flip();
		
		buffer.limit(buffer.capacity());
		buffer.put(readBuffer);
	}
	
	public void send(SelectionKey key){
//		System.out.println("key.isWritable()");
		SocketChannel sc = (SocketChannel)key.channel();
		ByteBuffer buffer = (ByteBuffer)key.attachment();
		buffer.flip();
//		System.out.println("buffer.limit="+buffer.limit()+", pos="+buffer.position());
		//把buffer中的所有字节转换为字符串
		String data = decode(buffer);
		if(data.indexOf("\r\n") < 0)return;
		//截取一行数据
		String outputData = data.substring(0, data.indexOf("\n")+1);
		System.out.println(sc.socket().getPort()+":"+outputData);
		try {
			//单线程，会阻塞
			Thread.currentThread().sleep(5000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ByteBuffer outputBuffer = encode(sc.socket().getPort()+"@get: " + outputData);
		//输出oututBuffer中的所有字节
		while(outputBuffer.hasRemaining()){
			try {
				sc.write(outputBuffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//把outData字符串按GBK编码，转换为字节，放在tempBuffer中
		ByteBuffer tempBuffer = charSet.encode(outputData);
		
		int limit = tempBuffer.limit();
//		System.out.println("tempBuffer.limit="+limit+", pos="+tempBuffer.position()
//			+", capacity="+tempBuffer.capacity());
		//把buffer的位置设置为tempBuffer的极限
		buffer.position(limit);
		//删除旧的字节
		buffer.compact();
	}
	public static void main(String[] args) throws IOException{
		NoBlockingMultiThreadServer server = 
				new NoBlockingMultiThreadServer();
		new Thread("accept"){
			@Override
			public void run(){
				server.accept();
			}
		}.start();;
		server.service();
	}
}
