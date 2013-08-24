package org.lh.action.chapter03;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class ViewStatisticsTest {
	private static final String host = "192.168.137.77";
	private static final int port = 5672;
	private static final String vhost = "/";
	private static final String user = "guest";
	private static final String pass = "guest";
	private static ConnectionFactory factory = new ConnectionFactory();
	Connection conn = null;
	Channel channel = null;
	@Before
	public void init() throws IOException{
		factory.setHost(host);
		factory.setPort(port);
		factory.setVirtualHost(vhost);
		factory.setUsername(user);
		factory.setPassword(pass);
		conn = factory.newConnection();
		channel = conn.createChannel();
	}
	
	@Test
	public void testProduce() throws IOException{
		channel.exchangeDeclare("logs-exchange", "topic", true, false, false, null);
		channel.queueDeclare("msg-inbox-errors", true, false, false, null);
		channel.queueDeclare("msg-inbox-logs", true, false, false, null);
		channel.queueDeclare("all-logs", true, false, false, null);
		
		channel.queueBind("msg-inbox-errors", "logs-exchange", "error.msg-inbox");
		channel.queueBind("msg-inbox-logs", "logs-exchange", "*.msg-inbox");
	}
	
	@Test
	public void testMonitorServerLogs() throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException{
		String exchangeName = "amq.rabbitmq.log";
		String errorsQueue = channel.queueDeclare().getQueue();
		String warningsQueue = channel.queueDeclare().getQueue();
		String infoQueue = channel.queueDeclare().getQueue();
		
		channel.queueBind(errorsQueue, exchangeName, "error");
		channel.queueBind(warningsQueue, exchangeName, "warning");
		channel.queueBind(infoQueue, exchangeName, "info");
		
		QueueingConsumer errorsConsumer = new QueueingConsumer(channel);
		QueueingConsumer warningsConsumer = new QueueingConsumer(channel);
		QueueingConsumer infoConsumer = new QueueingConsumer(channel);
		
		channel.basicConsume(errorsQueue, true, errorsConsumer);
		channel.basicConsume(warningsQueue, true, warningsConsumer);
		channel.basicConsume(infoQueue, true, infoConsumer);
		
		new Thread(new LogPrinter(infoConsumer, "info")).start();
		new Thread(new LogPrinter(warningsConsumer, "warning")).start();
		new Thread(new LogPrinter(errorsConsumer, "error")).start();
	}
	
	/**
	 * a thread to print server logs
	 */
	private class LogPrinter implements Runnable{
		QueueingConsumer consumber = null;
		String level = "";
		public LogPrinter(QueueingConsumer consumber, String level){
			this.consumber = consumber;
			this.level = level;
		}
		
		@Override
		public void run() {
			try {
				while(true){
					System.out.println("监听["+this.level+"]级别日志");
					QueueingConsumer.Delivery delivery  = this.consumber.nextDelivery();
					String msg = new String(delivery.getBody());
					System.out.println("["+this.level+"]: "+ msg);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
