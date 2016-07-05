package noBlockingSocketClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * 负责以NIO方式发起连接
 * @author QuPeng
 *
 */
public class Connector {
	private ServerSocketChannel ssc = null;
	private Selector selector = null;
	private Charset charSet = Charset.forName("utf-8");   
	public Connector(){
		try {
			ssc = ServerSocketChannel.open();
			ssc.bind(new InetSocketAddress(InetAddress.getLocalHost(), 8099));
			ssc.configureBlocking(false);
			
			selector = Selector.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void service(){
		try {
			ssc.register(selector, SelectionKey.OP_ACCEPT 
									| SelectionKey.OP_READ 
									| SelectionKey.OP_WRITE);
			while(selector.select() > 0){
				Set<SelectionKey> keys = selector.keys();
				Iterator<SelectionKey> it = keys.iterator(); 
				while(it.hasNext()){
					SelectionKey key = it.next();
					it.remove();
					if(key.isAcceptable()){
						accept(key);
					}
					if(key.isReadable()){
						read(key);
					}
					if(key.isWritable()){
						write(key);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void accept(SelectionKey key){
		SocketChannel sc = (SocketChannel) key.channel();
		
		
		Socket socket = sc.socket();
		socket.getLocalPort();
		socket.getInetAddress();
		socket.getPort();
		socket.getRemoteSocketAddress();
	}
	
	private void read(SelectionKey key){
		SocketChannel sc = (SocketChannel) key.channel();
		ByteBuffer bb = (ByteBuffer) key.attachment();
		
//		Socket socket = sc.socket();
		ByteBuffer dst = ByteBuffer.allocate(32);
		try {
			sc.read(dst);
			dst.flip();
			bb.put(dst);
			bb.limit(dst.capacity());
			bb.position(0);
			key.attach(bb);
			dst.compact();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void write(SelectionKey key){
		SocketChannel sc = (SocketChannel) key.channel();
		ByteBuffer bb = (ByteBuffer) key.attachment();
		
		String in = decode(bb);
		String aLine = "";
		if(in.indexOf("\r\n") != -1){
			aLine = in.substring(0,in.indexOf("\n")+1);
		}
		System.out.println("@get::"+aLine);
		String out = aLine + " ,got";
		
	}
	
	private ByteBuffer encode(String str){
		return charSet.encode(str);
	}
	private String decode(ByteBuffer bb){
		return new String(charSet.decode(bb).array());
	}
}
