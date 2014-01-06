package com.asiacom.mq;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * 与RabbitMQ服务器进行连接的基类
 */
public abstract class RabbitMQ implements MQConnection {
	protected String mServer;
	protected int port = -1;
	protected String userName;
	protected String password;
	/**
	 * Exchange(消息交换机)名称
	 */
	protected String exchangeName;
	/**
	 * 消息队列名称
	 */
	protected String queueName;
	/**
	 * Exchange(消息交换机)类型
	 */
	protected String exchangeType;
	protected ConnectionListener listener;
	protected int connectionTimeout = 5000;

	/**
	 * 默认Exchange(消息交换机)类型
	 */
	public final static String DEFAULT_EXCHANGE_TYPE = "fanout";
	protected Channel mChannel;
	protected Connection mConnection;

	public RabbitMQ(String mServer, int port, String userName, String password,
			String exchangeName, String queueName) {
		super();
		this.mServer = mServer;
		this.port = port;
		this.userName = userName;
		this.password = password;
		this.exchangeName = exchangeName;
		this.queueName = queueName;
	}
	
	public void setExchangeType(ExchangeType type) {
		this.exchangeType = type.value;
	}
	
	@Override
	public void setConnectionListener(final ConnectionListener listener) {
		this.listener = listener;
		mConnection.addShutdownListener(new ShutdownListener() {
			@Override
			public void shutdownCompleted(ShutdownSignalException e) {
				if (listener != null) {
					listener.onConnectionBreak();
				}
			}
		});
	}

	@Override
	public boolean connect() {
		// already declared
		if (mChannel != null && mChannel.isOpen()) {
			return true;
		}
		try {
			ConnectionFactory connectionFactory = new ConnectionFactory();
			// 地址
			connectionFactory.setHost(mServer);
			if(password != null && userName != null){
				// 用户名密码
				connectionFactory.setUsername(password);
				connectionFactory.setPassword(userName);
			}
			if(port > 0){
				// 端口号
				connectionFactory.setPort(port);
			}

			connectionFactory.setConnectionTimeout(connectionTimeout);
			mConnection = connectionFactory.newConnection();
			mChannel = mConnection.createChannel();
			mChannel.exchangeDeclare(exchangeName, DEFAULT_EXCHANGE_TYPE, true);
			// 消息队列
			if(queueName != null){
				mChannel.queueDeclare(queueName, false, false, false, null);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void disconnect() {
		try {
			if (mChannel != null) {
				mChannel.close();
			}
			if (mConnection != null) {
				mConnection.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setConnectionTimeout(int timeout) {
		connectionTimeout = timeout;
	}
	
	public enum ExchangeType{
		Fanout("fanout"),
		Direct("direct"),
		Topic("topic");
		private String value;
		private ExchangeType(String value){
			this.value = value;
		}
	}

}