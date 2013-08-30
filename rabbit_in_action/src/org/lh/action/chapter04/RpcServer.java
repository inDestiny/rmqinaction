package org.lh.action.chapter04;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * Rpc Server
 * @author Hao Lin
 * @since 2013
 * @location symantec
 */
public class RpcServer {
	private static String SERVER_IP = null;
	private static Integer SERVER_PORT = null;
	private static String USER = null;
	private static String PASS = null;
	private static String VHOST = null;
	private static String EXCHANGE_NAME = "rpc_ex";
	private static String RPC_QUEUE_NAME = "rpc_queue";
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
	
	public static void main(String[] args) throws IOException {
		Channel channel = conn.createChannel();
		channel.exchangeDeclare(EXCHANGE_NAME, "direct", false, false, null);
		//channel.basicQos(1);
		channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
		channel.queueBind(RPC_QUEUE_NAME, EXCHANGE_NAME, RPC_ROUTE_KEY);
		QueueingConsumer rpcConsumer = new QueueingConsumer(channel);
		channel.basicConsume(RPC_QUEUE_NAME, false, "rpc_tag", rpcConsumer);
		new RpcConsumeThread(rpcConsumer,channel).startConsume();
	}
	
	private static class RpcConsumeThread extends Thread{
		private QueueingConsumer consumer;
		private Channel channel;
		private Boolean isReceive = true;
		
		public RpcConsumeThread(QueueingConsumer consumer, Channel channel){
			this.consumer = consumer;
			this.channel = channel;
		}
		
		@Override
		public void run() {
			try {
				while(isReceive){
					//收听消息
					QueueingConsumer.Delivery delivery = consumer.nextDelivery();
					BasicProperties props = delivery.getProperties();
					//1.根据客户端发来的correlationId创建返回属性对象
					BasicProperties replyProps = new BasicProperties
														.Builder()
														.correlationId(props.getCorrelationId())
														.build();
					String msg = new String(delivery.getBody(), "UTF-8");
					//2.处理RPC请求
					System.out.println("收到客户端RPC请求["+props.getCorrelationId()+"]: "+msg);
					//do something
					//3.回复客户, reply_to是由客户端请求时自带过来的，客户端会收听在以reply_to为routekey的队列上，所以将其作为routekey,发布回复消息 
					//System.out.println("向队列["+props.getReplyTo()+"]回复");
					channel.basicPublish(EXCHANGE_NAME, props.getReplyTo(), replyProps, "RPCServer已经处理完请求.".getBytes("UTF-8"));
					//4.确认服务器已收到RPC请求
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
