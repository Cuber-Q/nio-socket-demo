package blockingSocket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockingMultiThreadsServer {
	ServerSocketChannel serverSocketChannel = null;
	ExecutorService executorService = null;
	BufferedReader br = null;
	PrintWriter pw = null;
	
	public BlockingMultiThreadsServer(){
		try {
			//打开连接
			serverSocketChannel = ServerSocketChannel.open();
			//绑定本地ip和端口
			serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 8099));
			//初始化线程池
			executorService = Executors.newFixedThreadPool(4*Runtime.getRuntime().availableProcessors());
			System.out.println("server started...");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void service(){
		while(true){
			SocketChannel socketChannel = null;
			try {
				socketChannel = serverSocketChannel.accept();
				executorService.execute(new Handler(socketChannel.socket()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	public static void main(String[] args){
		try {
			new BlockingMultiThreadsServer().service();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}

class Handler implements Runnable{
	private Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;

	public Handler(Socket socket){
		this.socket = socket;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		String remoteAddr = socket.getRemoteSocketAddress().toString();
		reader = getReader(socket);
		writer = getWriter(socket);
		String msg = "";
		try {
			while((msg = reader.readLine()) != null){
				System.out.println(msg);
				writer.println(remoteAddr +" [" + msg + "] got");
				writer.flush();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println();
	}
	
	public BufferedReader getReader(Socket socket) {
		try {
			return new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public PrintWriter getWriter(Socket socket){
		try {
			return new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
