package org.lh.action.channlepool;

import java.io.IOException;
import org.apache.commons.pool.impl.GenericObjectPool;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Test {
	private static final String host = "192.168.21.144";
	private static final int port = 5672;
	private static final String vhost = "/";
	private static final String user = "guest";
	private static final String pass = "guest";
	private static ConnectionFactory factory = new ConnectionFactory();
	private static Connection conn = null;
	
	static {
		try {
			factory.setHost(host);
			factory.setPort(port);
			factory.setVirtualHost(vhost);
			factory.setUsername(user);
			factory.setPassword(pass);
			conn = factory.newConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		ChannelPoolFactory fac = new ChannelPoolFactory(conn);
		GenericObjectPool<Channel> pool = new GenericObjectPool<Channel>(fac);
		
		Channel c = pool.borrowObject();
		System.out.println(c.getChannelNumber());
		conn.close();
		pool.close();
	}
}
