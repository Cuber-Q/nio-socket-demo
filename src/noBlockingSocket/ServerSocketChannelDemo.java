package noBlockingSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
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
public class ServerSocketChannelDemo {
	private ServerSocketChannel ssc = null;
	private Selector selector = null;
	private Charset charSet = Charset.forName("utf-8");   
	public ServerSocketChannelDemo(){
		try {
			ssc = ServerSocketChannel.open();
			ssc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8099));
			ssc.configureBlocking(false);
			selector = Selector.open();
			System.out.println("init...");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void service(){
		try {
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("registed...");
			while(selector.select() > 0){
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator(); 
				while(it.hasNext()){
					SelectionKey key = it.next();
					if(key.isAcceptable()){
						accept(key);
					}
					if(key.isReadable()){
						read(key);
					}
					if(key.isWritable()){
						write(key);
					}
					it.remove();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void accept(SelectionKey key){
		System.out.println("accept...");
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		ByteBuffer bb = ByteBuffer.allocate(1024);
		try {
			SocketChannel sc = ssc.accept();
			sc.configureBlocking(false);
			sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE,bb);
		
			Socket socket = sc.socket();
			System.out.println("socket.getLocalPort()="+socket.getLocalPort());
			System.out.println("socket.getInetAddress()="+socket.getInetAddress());
			System.out.println("socket.getPort()="+socket.getPort());
			System.out.println("socket.getRemoteSocketAddress()="+socket.getRemoteSocketAddress());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void read(SelectionKey key){
		SocketChannel sc = (SocketChannel) key.channel();
		ByteBuffer bb = (ByteBuffer) key.attachment();
		
//		Socket socket = sc.socket();
		ByteBuffer dst = ByteBuffer.allocate(32);
		try {
			System.out.println("read...");
			sc.read(dst);
			dst.flip();
//			bb.position(0);
			bb.limit(bb.capacity());
			bb.put(dst);
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
		bb.flip();
		String in = decode(bb);
		String aLine = "";
		try {
			if(in.indexOf("\r\n") == -1){
				return;
			}
			aLine = in.substring(0,in.indexOf("\n")+1);
			System.out.println("@get::"+aLine);
			String out = "got:::" + aLine ;
			sc.write(encode(out));
			bb.compact();
			if(aLine.indexOf("bye") != -1){
				sc.close();
				ssc.close();
				System.out.println("stoped...");
				System.exit(0);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private ByteBuffer encode(String str){
		return charSet.encode(str);
	}
	private String decode(ByteBuffer bb){
		return new String(charSet.decode(bb).array());
	}
	
	public static void main(String[] args){
		new ServerSocketChannelDemo().service();
	}
}
