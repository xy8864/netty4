package io.netty.eatnetty.nio;
 
import java.net.InetSocketAddress;
import java.net.ServerSocket; 
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioServer {
	public static void main(String[] args) throws Exception {
		System.out.println("GO!");
		Selector select = Selector.open();
		
		// 开启服务端socket channel。
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		
		// 配置服务端socket, 绑定端口
		ServerSocket socket = serverChannel.socket();
		socket.bind(new InetSocketAddress(9998));
		
		// 把serverChannel注册到selector上。
		// 这时候select就开始管理channel了。当有ACCEPT事件来，通知channel。
		serverChannel.register(select, SelectionKey.OP_ACCEPT);
		
		while(true){
			// blocking ...
			// 管理ServerSocketChannel。这个channel接受的感兴趣的事件是ACCEPT.
			int num = select.select();
			 
			Set<SelectionKey> keySets = select.selectedKeys();
			Iterator<SelectionKey> iterKeys = keySets.iterator();
			
			if(num > 0){
				System.out.println("有" + num + "个感兴趣的事情发生了, keySets-size:" + keySets.size());
			}
			
			while(iterKeys.hasNext()){
				SelectionKey key = iterKeys.next();
				
				if((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT){
					// 这里的调用accept也很重要。
					// 表明serverChannel已经处理了该事件。select再次循环的时候就不会筛选到该事件了。否则会有无限的循环。
					SocketChannel clientSocket = serverChannel.accept();
					
					clientSocket.configureBlocking(false);
					clientSocket.register(select, SelectionKey.OP_READ);
				} else if((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ){
				    // System.out.println("f"); 
					// 如果注释掉下边if代码块里边所有的内容，只留下上边这一句输出的话又会出现客户端打一个字，服务器不停
					// 输出字符f的情况，这就是因为，如果服务器不处理过来的请求，selector就会一直可以筛选出这个感兴趣
					// 的事件。然后做无限循环。
					SocketChannel channel = (SocketChannel) key.channel();
					
					ByteBuffer buf = ByteBuffer.allocate(1024);
					channel.read(buf);
					
					System.out.println(new String(buf.array()));
				} else {
					System.out.println("fuck, no accept.");
				}
				
				// 不对，这里我理解的还是不对~~ TODO
				iterKeys.remove();
			}
		}
	}
}







