package org.lh.action.chapter04.alert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Alert模型生产者
 * 
 * @author Hao Lin
 * @since 2013
 * @location symantec
 */
public class AlertProducer {
	private static String SERVER_IP = null;
	private static Integer SERVER_PORT = null;
	private static String USER = null;
	private static String PASS = null;
	private static String VHOST = null;
	private static String EXCHANGE_NAME = null;
	private static ConnectionFactory fac = null;
	private static Connection conn = null;
	static {
		try {
			Properties props = new Properties();
			props.load(new FileInputStream(new File(AlertComsumer.class
					.getResource("rabbitmq.properties").getPath())));
			// load config
			SERVER_IP = (String) props.get("server.ip");
			SERVER_PORT = (Integer) props.get("server.port");
			USER = (String) props.get("server.user");
			PASS = (String) props.get("server.pass");
			VHOST = (String) props.get("server.vhost");
			EXCHANGE_NAME = (String) props.get("server.exchange");
			// set config
			fac = new ConnectionFactory();
			fac.setHost(SERVER_IP);
			fac.setPort(SERVER_PORT);
			fac.setVirtualHost(VHOST);
			fac.setUsername(USER);
			fac.setPassword(PASS);
			conn = fac.newConnection();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException {
		Channel channel = conn.createChannel();
		channel.basicPublish(EXCHANGE_NAME, "critical.rate_limit", true, null, "the msg will be received critical and rate_limit.".getBytes("UTF-8"));
		channel.basicPublish(EXCHANGE_NAME, "critical.xxx", true, null, "the msg will be received critical.".getBytes("UTF-8"));
		channel.basicPublish(EXCHANGE_NAME, "yyy.rate_limit", true, null, "the msg will be received rate_limit.".getBytes("UTF-8"));
	}
}
