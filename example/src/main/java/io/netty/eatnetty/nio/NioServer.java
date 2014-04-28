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
		
		// ���������socket channel��
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		
		// ���÷����socket, �󶨶˿�
		ServerSocket socket = serverChannel.socket();
		socket.bind(new InetSocketAddress(9998));
		
		// ��serverChannelע�ᵽselector�ϡ�
		// ��ʱ��select�Ϳ�ʼ����channel�ˡ�����ACCEPT�¼�����֪ͨchannel��
		serverChannel.register(select, SelectionKey.OP_ACCEPT);
		
		while(true){
			// blocking ...
			// ����ServerSocketChannel�����channel���ܵĸ���Ȥ���¼���ACCEPT.
			int num = select.select();
			 
			Set<SelectionKey> keySets = select.selectedKeys();
			Iterator<SelectionKey> iterKeys = keySets.iterator();
			
			if(num > 0){
				System.out.println("��" + num + "������Ȥ�����鷢����, keySets-size:" + keySets.size());
			}
			
			while(iterKeys.hasNext()){
				SelectionKey key = iterKeys.next();
				
				if((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT){
					// ����ĵ���acceptҲ����Ҫ��
					// ����serverChannel�Ѿ������˸��¼���select�ٴ�ѭ����ʱ��Ͳ���ɸѡ�����¼��ˡ�����������޵�ѭ����
					SocketChannel clientSocket = serverChannel.accept();
					
					clientSocket.configureBlocking(false);
					clientSocket.register(select, SelectionKey.OP_READ);
				} else if((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ){
				    // System.out.println("f"); 
					// ���ע�͵��±�if�����������е����ݣ�ֻ�����ϱ���һ������Ļ��ֻ���ֿͻ��˴�һ���֣���������ͣ
					// ����ַ�f��������������Ϊ��������������������������selector�ͻ�һֱ����ɸѡ���������Ȥ
					// ���¼���Ȼ��������ѭ����
					SocketChannel channel = (SocketChannel) key.channel();
					
					ByteBuffer buf = ByteBuffer.allocate(1024);
					channel.read(buf);
					
					System.out.println(new String(buf.array()));
				} else {
					System.out.println("fuck, no accept.");
				}
				
				// ���ԣ����������Ļ��ǲ���~~ TODO
				iterKeys.remove();
			}
		}
	}
}







