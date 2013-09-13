package org.lh.action.chapter04;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * Rpc Client
 * @author Hao Lin
 * @since 2013
 */
public class RpcClient {
	private static String SERVER_IP = null;
	private static Integer SERVER_PORT = null;
	private static String USER = null;
	private static String PASS = null;
	private static String VHOST = null;
	private static String EXCHANGE_NAME = "rpc_ex";
	private static String RPC_ROUTE_KEY = "rpc_route_key";
	private static ConnectionFactory fac = null;
	private static Connection conn = null;
	static{
		try {
			Properties props = new Properties();
			props.load(
					new FileInputStream(new File(AlertComsumer.class.getResource("rabbitmq.properties").getPath())));
			//load config
			SERVER_IP = (String)props.get("server.ip");
			SERVER_PORT = Integer.parseInt((String)props.get("server.port"));
			USER = (String)props.get("server.user");
			PASS = (String)props.get("server.pass");
			VHOST = (String)props.get("server.vhost");
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
	
	private static class ReplyConsumeThread extends Thread{
		private QueueingConsumer consumer;
		private Channel channel;
		private Boolean isReceive = true;
		private String corrId;
		
		public ReplyConsumeThread(QueueingConsumer consumer, Channel channel, String corrId){
			this.consumer = consumer;
			this.channel = channel;
			this.corrId = corrId;
		}
		
		@Override
		public void run() {
			try {
				while(isReceive){
					//收听消息
					QueueingConsumer.Delivery delivery = consumer.nextDelivery();
					//System.out.println("客户端收到RPC回复...");
					if (delivery.getProperties().getCorrelationId().equals(this.corrId)){
						System.out.println("服务器已经处理完请求["+this.corrId+"]");
					}
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
	
	public static void main(String[] args) throws IOException {
		Channel channel = conn.createChannel();
		//该队列用于客户端监听RPCServer执行完RPC请求后返回的消息
		String replyQueueName = channel.queueDeclare().getQueue();
		
		//开始监听RPCServer回复队列
		//生产随机数发送给RPCServer， RPCServer返回结果时需要带回确认
		String corrId = UUID.randomUUID().toString();
		channel.queueBind(replyQueueName, EXCHANGE_NAME, replyQueueName);
		QueueingConsumer replyConsumer = new QueueingConsumer(channel);
		channel.basicConsume(replyQueueName, true, replyConsumer);
		new ReplyConsumeThread(replyConsumer, channel, corrId).startConsume();
		
		//发起RPC请求
		BasicProperties props = new BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
		channel.basicPublish(EXCHANGE_NAME, RPC_ROUTE_KEY, props, "我是RpcClient.".getBytes("UTF-8"));
	}
}
