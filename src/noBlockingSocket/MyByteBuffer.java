package noBlockingSocket;

public class MyByteBuffer {
	/**
	 * position 写模式下，指向下一个可写的块
	 */
	private int position=0;
	private int limit=0;
	private int capacity=1;
	private byte[] arr;
	
	public MyByteBuffer(int capacity) throws Exception{
		if(capacity <= 0)
			throw new Exception("capacity must be larger than 0!");
		arr = new byte[capacity];
		this.capacity = capacity;
	}
	
	public int write(byte[] writeBuffer){
		limit = capacity;
		for(int i=0;i<writeBuffer.length;i++){
			arr[position+i] = writeBuffer[i];
		}
		//指向下一个可写的块
		position ++;
		return position;
	}
	
	/**
	 * 写模式向读模式切换
	 */
	public void flip(){
		limit = position;
		position = 0;
	}
	
	public int read(byte[] readBuffer){
		for(int i=position;i<limit;i++){
			readBuffer[i] = arr[position+i];
		}
		position = limit-1;
		return position;
	}
	
}
