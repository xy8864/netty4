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
					System.out.println("accept.");
				} else {
					System.out.println("fuck, no accept.");
				}
				
				// ɾ����ǰ���ڱ�������������Ǳ���ġ������������²�JDK�������ơ�
				// �����ɾ�����ٴ�ѭ����ʱ��select��������������(�走����˼����select()�õ���ʼ�������µĵ�ǰ���͹������¼������չ��ľͲ�����)��
				// ����select.selectedKeys�õ������еĸ���Ȥ���¼���������ǰ�Ѿ��������
				// ��������д��ʱ��һ�㶼�ֹ�remove����Ӧ��key��
				// ���ԣ����������Ļ��ǲ���~~ TODO
				iterKeys.remove();
			}
		}
	}
}







