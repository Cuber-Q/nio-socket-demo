package noBlockingSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
/**
 * 单线程非阻塞服务
 * 使用selector，监听注册、读、写事件
 * @author Cuber_Q
 *
 */
public class NoBlockingSingleThreadServer {
	private ServerSocketChannel serverChannel;
	private Selector selector;
	private Charset charSet = Charset.forName("GBK");
	
	public NoBlockingSingleThreadServer(){
		System.out.println("init...");
		try {
			//创建selector对象
			selector = Selector.open();
			serverChannel = ServerSocketChannel.open();
			serverChannel.socket().setReuseAddress(true);
			serverChannel.configureBlocking(false);
			serverChannel.bind(new InetSocketAddress(8081));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("server start...");
	}
	public void service(){
		try {
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			while(selector.select() > 0){
				Set<SelectionKey> keySet = selector.selectedKeys();
				Iterator<SelectionKey> it = keySet.iterator();
				while(it.hasNext()){
					SelectionKey key = it.next();
					it.remove();
					if(key.isAcceptable()){
						accept(key);
					}
					if(key.isReadable()){
						recive(key);
					}
					if(key.isWritable()){
						send(key);
					}
					
				}
			}
			
		} catch (ClosedChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public String decode(ByteBuffer buffer){
		CharBuffer charBuffer = charSet.decode(buffer);
		return charBuffer.toString();
	}
	public ByteBuffer encode(String str){
		return charSet.encode(str);
	}
	public void accept(SelectionKey key){
		System.out.println("key.isAcceptable()");
		ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
		try {
			SocketChannel sc = ssc.accept();
			sc.configureBlocking(false);
			ByteBuffer dst = ByteBuffer.allocate(4096);
			sc.register(selector, 
				SelectionKey.OP_READ|
				SelectionKey.OP_WRITE,dst);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	public static void main(String[] args){
		new NoBlockingSingleThreadServer().service();
	}
}
