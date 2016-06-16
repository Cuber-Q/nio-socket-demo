package noBlockingSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class NoBlockingMultiThreadServer {
	private ServerSocketChannel serverChannel;
	private Selector selector;
	public NoBlockingMultiThreadServer(){
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
						ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
						SocketChannel sc = ssc.accept();
						sc.configureBlocking(false);
						ByteBuffer dst = ByteBuffer.allocate(1024);
						sc.register(selector, 
								SelectionKey.OP_READ|
								SelectionKey.OP_WRITE,dst);
						if(sc.read(dst) != -1 ){
							System.out.println(new String(dst.toString()));
						}
						
					}
					if(key.isReadable()){
						SocketChannel sc = (SocketChannel)key.channel();
						ByteBuffer buffer = (ByteBuffer)key.attachment();
						
						ByteBuffer readBuffer = ByteBuffer.allocate(32);
						sc.read(readBuffer);
						readBuffer.flip();
						
						buffer.limit(buffer.capacity());
						buffer.put(readBuffer);
						
					}
					if(key.isWritable()){
						SocketChannel sc = (SocketChannel)key.channel();
						ByteBuffer buffer = (ByteBuffer)key.attachment();
						buffer.flip();

						String str = new String(buffer.array(),Charset.forName("GBK"))+",recived";
						ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
						writeBuffer.put(str.getBytes());

						while(writeBuffer.remaining() > 0){
							sc.write(writeBuffer);
						}
						
						ByteBuffer tempBuffer = ByteBuffer.allocate(1024);
						tempBuffer.put(str.getBytes());
						//删除旧的字节
						buffer.position(tempBuffer.limit());
						buffer.compact();
						
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
}
