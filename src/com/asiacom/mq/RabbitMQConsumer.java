package com.asiacom.mq;

import java.io.IOException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * RabbitMQ消息接收
 */
public class RabbitMQConsumer extends RabbitMQ implements IReceiver {
	protected boolean running;
	private QueueingConsumer mySubscription;
	private String queueName;
	
	private OnReceiveMessageHandler mOnReceiveMessageHandler;
	private static final int START_CONSUME = 1;
	private static final int RECEIVE = 2;
	private static final int CONNECTION_FAILED = 3;

	/**
	 * @param mServer 服务器
	 * @param exchangeName 交换机名称
	 */
	public RabbitMQConsumer(String mServer, String exchangeName) {
		super(mServer, 0, null, null, exchangeName, null);
		exchangeType = DEFAULT_EXCHANGE_TYPE;
	}

	
	@Override
	public void startReceive(OnReceiveMessageHandler mOnReceiveMessageHandler) {
		this.mOnReceiveMessageHandler = mOnReceiveMessageHandler;
		running = false;
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (connect()) {
					try {
						queueName = mChannel.queueDeclare().getQueue();
						mySubscription = new QueueingConsumer(mChannel);
						mChannel.basicConsume(queueName, false, mySubscription);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					if (exchangeType == "fanout") {
						addBinding("");// fanout has default binding
					}
					
					running = true;
					Message msg = handler.obtainMessage();
					msg.what = START_CONSUME;
					handler.sendMessage(msg);
				}else{
					Message msg = handler.obtainMessage();
					msg.what = CONNECTION_FAILED;
					handler.sendMessage(msg);
				}
			}
		}).start();
		
	}
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what){
			case RECEIVE: //接收到消息
				if(mOnReceiveMessageHandler != null){
					mOnReceiveMessageHandler.onReceiveMessage((byte[]) msg.obj);
				}
				break;
			case START_CONSUME: //开始接收
				if(listener != null){
					listener.onConnectionResult(true);
				}
				consume();
				break;
			case CONNECTION_FAILED: //连接服务器失败
				if(listener != null){
					listener.onConnectionResult(false);
				}
				break;
			}
		}
	};


	private void consume() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				while (running) {
					QueueingConsumer.Delivery delivery;
					try {
						Log.d("RabbitMQConsumer", "准备接收消息");
						delivery = mySubscription.nextDelivery();
						Log.d("RabbitMQConsumer", "已接收到");
						byte[] date = delivery.getBody();
						Message msg = handler.obtainMessage();
						msg.what = RECEIVE;
						msg.obj = date;
						handler.sendMessage(msg);
						try {
							mChannel.basicAck(delivery.getEnvelope()
									.getDeliveryTag(), false);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} catch (InterruptedException ie) {
						ie.printStackTrace();
					} catch(Exception e){
						
					}
				}
			}
		};
		thread.start();
	}
	
	
	@Override
	public void disconnect() {
		running = false;
		super.disconnect();
	}
	
	
	/**
	 * 使用路由关键字将Exchange与Queue进行绑定，如果Exchange类型为Fanout,则不用指定routingKey
	 * @param routingKey
	 */
	public void addBinding(String routingKey) {
		try {
			mChannel.queueBind(queueName, exchangeName, routingKey);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 通过关键字，移除消费端Queue和Exchange的绑定
	 * @param routingKey 路由关键字
	 */
	public void removeBinding(String routingKey) {
		try {
			mChannel.queueUnbind(queueName, exchangeName, routingKey);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
