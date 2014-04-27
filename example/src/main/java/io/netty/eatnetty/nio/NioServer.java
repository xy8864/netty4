package io.netty.eatnetty.nio;
 
import java.net.InetSocketAddress;
import java.net.ServerSocket; 
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
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
					System.out.println("accept.");
				} else {
					System.out.println("fuck, no accept.");
				}
				
				// 删除当前正在遍历的这个对象是必须的。这里真是想吐槽JDK的这个设计。
				// 如果不删除，再次循环的时候，select方法将不会阻塞(妈蛋，意思就是select()拿到的始终是最新的当前发送过来的事件，接收过的就不管了)。
				// 但是select.selectedKeys拿的是所有的感兴趣的事件。包括以前已经处理过。
				// 所以我们写的时候一般都手工remove掉对应的key。
				// 不对，这里我理解的还是不对~~ TODO
				iterKeys.remove();
			}
		}
	}
}







