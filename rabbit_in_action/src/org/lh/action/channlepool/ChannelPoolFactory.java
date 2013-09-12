package org.lh.action.channlepool;

import org.apache.commons.pool.PoolableObjectFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

/**
 * channel池
 * @author Hao Lin
 * @since 2013
 * @location symantec
 */
public class ChannelPoolFactory implements PoolableObjectFactory<Channel> {
	private Connection conn = null;
	
	public ChannelPoolFactory(Connection conn){
		this.conn = conn;
	}
	
	@Override
	public void activateObject(Channel channel) throws Exception {
		System.out.println("Active Channel");
	}

	@Override
	public void destroyObject(Channel channel) throws Exception {
		System.out.println("Destroy Channel");
		channel.close();
	}

	@Override
	public Channel makeObject() throws Exception {
		System.out.println("Instance Channel");
		return conn.createChannel();
	}

	@Override
	public void passivateObject(Channel channel) throws Exception {
		
	}

	@Override
	public boolean validateObject(Channel channel) {
		return true;
	}
}
