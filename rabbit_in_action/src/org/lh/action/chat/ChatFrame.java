package org.lh.action.chat;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * Chat Frame
 * @author hao
 * @since 2013
 */
public class ChatFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	
	//rmq配置
	private static final String host = "192.168.137.77";
	private static final int port = 5672;
	private static final String vhost = "/";
	private static final String user = "guest";
	private static final String pass = "guest";
	private static ConnectionFactory factory = new ConnectionFactory();
	private static Connection conn = null;
	private static Channel channel = null;
	private static String loginer = null;
	private static String MSGS_EXCHANGE = "msgs_exchange";
	
	//好友列表
	private static List<String> persons = Arrays.asList(new String[]{"all", "zhangsan", "lisi", "wangwu"});
	
	public static final int WIDTH = 500;
	public static final int HEIGHT = 400;
	private static final int POS_X = 400;
	private static final int POS_Y = 100;
	private JTextField msgField = new JTextField(25);
	private JButton msgBtn = new JButton("Send");
	private static JTextArea msgArea = new JTextArea();
	private JComboBox friendsCbx = new JComboBox();
	private static Boolean isReceive = true;

	public static void main(String[] args) {
		loginer = args[0];
		new ChatFrame().launch();
		init();
	}
	
	/**
	 * init the msgs consume
	 */
	private static void init() {
		try {
			factory.setHost(host);
			factory.setPort(port);
			factory.setVirtualHost(vhost);
			factory.setUsername(user);
			factory.setPassword(pass);
			conn = factory.newConnection();
			channel = conn.createChannel();
			channel.exchangeDeclare(MSGS_EXCHANGE, "topic");
			//开启消息接收线程
			new ReceiveMsgThread().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * launch the frame
	 */
	private void launch() {
		this.setTitle("I am " + loginer);
		this.setLayout(new BorderLayout());
		this.setSize(WIDTH, HEIGHT);
		this.setLocation(new Point(POS_X, POS_Y));
		initComponents();
		this.setResizable(false);
		this.setVisible(true);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					System.out.println(loginer+"退出，关闭连接..");
					isReceive = false;
					if (channel != null) channel.close(200, "关闭channel");
					if (conn != null) conn.close(200, "关闭conn", 2000);
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally{
					System.exit(1);
				}
			}
		});
	}

	/**
	 * 初始化组件
	 */
	private void initComponents() {
		addComponents();
		addEvents();
	}
	
	/**
	 * add events
	 */
	private void addEvents() {
		msgBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String msg = msgField.getText();
				if (msg != null && !"".equals(msg)){
					String friend = (String)friendsCbx.getSelectedItem();
					sendMsg(friend, msg);
					msgField.setText("");
				}
			}
			
			/**
			 * 发送消息
			 * @param friend 好友名
 			 * @param msg 消息内容
			 */
			private void sendMsg(String friend, String msg) {
				try {
					String routingkey = null;
					String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
					if ("all".equals(friend)){ //群聊
						routingkey = "msg.group";
						msg = loginer+"对大家说: " + msg +"\t"+time;
					} else{ //私聊
						routingkey = "msg." + friend;
						msg = loginer + "对"+ friend +"说:" + msg + "\t"+ time;
					}
					
					channel.basicPublish(MSGS_EXCHANGE, routingkey, null,
							msg.getBytes());
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * 添加事件
	 */
	private void addComponents() {
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(msgField);
		for (String f : persons){
			if (!loginer.equals(f)){
				friendsCbx.addItem(f);
			}
		}
		bottomPanel.add(friendsCbx);
		bottomPanel.add(msgBtn);
		add(bottomPanel, BorderLayout.SOUTH);
		JScrollPane msgPanel = new JScrollPane(msgArea);
		msgArea.setEditable(false);
		add(msgPanel, BorderLayout.CENTER);
	}
	
	/**
	 * ReceiveMsg Thread
	 * @author hao
	 * @since 2013
	 */
	public static class ReceiveMsgThread extends Thread{
		private QueueingConsumer consumer;
		public ReceiveMsgThread() {
			try {
				String queueName = channel.queueDeclare().getQueue();
				//绑定发送给自己的消息
				channel.queueBind(queueName, MSGS_EXCHANGE, "*."+loginer);
				//绑定发送给群的消息
				channel.queueBind(queueName, MSGS_EXCHANGE, "*.group");
				consumer = new QueueingConsumer(channel);
				channel.basicConsume(queueName, true, consumer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			try {
				while(isReceive){
					QueueingConsumer.Delivery delivery  = this.consumer.nextDelivery();
					String receivedMsg = new String(delivery.getBody());
					msgArea.setText(msgArea.getText()+receivedMsg+"\n");
				}
			} catch (Exception e) {
				//e.printStackTrace();
			} 
		}
	}
}
