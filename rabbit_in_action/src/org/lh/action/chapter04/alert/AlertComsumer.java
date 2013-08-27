package org.lh.action.chapter04.alert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * Alert模型消费者
 * @author Hao Lin
 * @since 2013
 * @location symantec
 */
public class AlertComsumer {
	private static String SERVER_IP = null;
	private static Integer SERVER_PORT = null;
	private static String USER = null;
	private static String PASS = null;
	private static String VHOST = null;
	private static String EXCHANGE_NAME = null;
	private static ConnectionFactory fac = null;
	private static Connection conn = null;
	static{
		try {
			Properties props = new Properties();
			props.load(
					new FileInputStream(new File(AlertComsumer.class.getResource("rabbitmq.properties").getPath())));
			//load config
			SERVER_IP = (String)props.get("server.ip");
			SERVER_PORT = (Integer)props.get("server.port");
			USER = (String)props.get("server.user");
			PASS = (String)props.get("server.pass");
			VHOST = (String)props.get("server.vhost");
			EXCHANGE_NAME = (String)props.get("server.exchange");
			//set config
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
		channel.exchangeDeclare(EXCHANGE_NAME, "topic", false, false, null);
		
		//critical queue
		String criticalQueueName = channel.queueDeclare().getQueue();
		channel.queueBind(criticalQueueName, EXCHANGE_NAME, "critical.*");
		
		//rate_limit queue
		String rateLimitQueueName = channel.queueDeclare().getQueue();
		channel.queueBind(rateLimitQueueName, EXCHANGE_NAME, "*.rate_limit");
		
		//start to consume critical
		QueueingConsumer criticalQueueingConsumer = new QueueingConsumer(channel);
		channel.basicConsume(criticalQueueName, false, "critical", criticalQueueingConsumer);
		new ConsumeThread(criticalQueueingConsumer,channel).startConsume();
		
		//start to consume rate_limit
		QueueingConsumer rateLimitQueueingConsumer = new QueueingConsumer(channel);
		channel.basicConsume(rateLimitQueueName, false, "rate_limit", rateLimitQueueingConsumer);
		new ConsumeThread(rateLimitQueueingConsumer, channel).startConsume();
	}
	
	/**
	 * The thread of consumer to consume msgs
	 * @author Hao Lin
	 * @since 2013
	 * @location symantec
	 */
	private static class ConsumeThread extends Thread{
		private QueueingConsumer consumer;
		private Channel channel;
		private Boolean isReceive = true;
		public ConsumeThread(QueueingConsumer consumer, Channel channel){
			this.consumer = consumer;
			this.channel = channel;
		}
		
		@Override
		public void run() {
			try {
				while(isReceive){
					//收听消息
					QueueingConsumer.Delivery delivery = consumer.nextDelivery();
					String msg = new String(delivery.getBody(), "UTF-8");
					System.out.println(this.consumer.getConsumerTag()+" 收到消息: "+msg);
					//here, you can do sth. to alert, for example send email...
					//mailTo();
					//确认收到消息(to servermaster)
					channel.basicAck(delivery.getEnvelope().getDeliveryTag(), true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * start to consume
		 */
		public void startConsume(){
			this.start();
		}
		
		/**
		 * stop to consume
		 */
		@SuppressWarnings("unused")
		public void stopConsume(){
			try {
				this.channel.basicCancel(this.consumer.getConsumerTag());
				this.isReceive = false;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
