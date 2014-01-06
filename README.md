MQDemo
==============

此工程主要用于Android设备与MQ消息服务器的交互，可向MQ服务器发送消息，也可以接受来自消息服务器的消息。主要包括以下两种实现方式：

- 基于RabbitMQ的实现
- 基于MQTT的实现


用法
-------

**RabbitMQ**

-------

1.发布消息

	//实例化消息发布对象,具体参数见RabbitMQSender构造函数
    RabbitMQSender sender = new RabbitMQSender(SERVER, 5672, "guest", "guest", EXCHANGE_NAME, "mq");
	
	//发送消息
	sender.send("Hello world!");

2.订阅消息
	
	//实例化消息接收对象，具体参数见RabbitMQConsumer构造
	RabbitMQConsumer receiver = new RabbitMQConsumer(SERVER, EXCHANGE_NAME);

	//接收订阅
	receiver.startReceive(new OnReceiveMessageHandler(){
			@Override
			public void onReceiveMessage(byte[] message) {
				//处理接收的消息message
			}	
		}
	); 

3.服务器连接状态

`RabbitMQSender`每次发送消息时，都会检查与服务器连接状态,可以通过设置监听查看连接状态：

    sender.setConnectionListener(new ConnectionListener() {
			
			@Override
			public void onConnectionResult(boolean success) {
				if(success){
					//连接成功
				}
			}
			
			@Override
			public void onConnectionBreak() {
				//连接断开
			}
	});

`RabbitMQConsumer`只在调用`startReceive()`方法时进行连接，可在监听连接失败后尝试重连
	
	receiver.setConnectionListener(new ConnectionListener() {
			
			@Override
			public void onConnectionResult(boolean success) {
				if(!success){
					//重新接收
				}
			}
			
			@Override
			public void onConnectionBreak() {
				//连接断开
			}
	});

应用举例：见工程中`SampleActivity`

**MQTT**

---
MQTT是一个轻量级的消息发布/订阅协议，它是实现基于手机客户端的消息推送服务器的理想解决方案。

1.需要导入的jar包:

- org.eclipse.paho.client.mqttv3.jar
- eventbus-2.1.0-beta-1.jar

2.自定义ServiceMqtt，定义需要用到的方法。

- 初始化mqtt,设置mqtt 服务器的ip和端口号

		//初始化
		private void init() {
			Log.v(TAG, "初始化");
		
			if (mqttClient != null) {
				return;
			}
		
			try {
				//设置mqtt broker的ip和端口
				String brokerAddress = Constants.URL_MQTT_HOST;
				String brokerPort = Constants.URL_MQTT_PORT;
				String cid = getDefaultClientId();
				mqttClient = new MqttClient(brokerAddress + ":" + brokerPort,
						cid, null);
				mqttClient.setCallback(this);
		
			} catch (MqttException e) {
				// 报错
				mqttClient = null;
				changeState(State.ServiceMqtt.DISCONNECTED);
			}
		}

- 连接服务器：

		//连接服务器
			private boolean connect() {
				workerThread = Thread.currentThread(); 
				Log.v(TAG, "连接服务器方法");
				error = null; // clear previous error on connect
				init();//初始化
			
				try {
					changeState(State.ServiceMqtt.CONNECTING);
					MqttConnectOptions options = new MqttConnectOptions();
			
					// setWill(options);
					options.setKeepAliveInterval(keepAliveSeconds);//设置心跳时间  
					options.setConnectionTimeout(timeout);//设置连接超时时间 30s
			
					mqttClient.connect(options);//连接服务器
			
					Log.d(TAG, "成功连接服务器");
					changeState(State.ServiceMqtt.CONNECTED);
			
					return true;
			
				} catch (Exception e) { // Catch paho and socket factory exceptions
					Log.e(TAG, e.toString());
					changeState(e);
					return false;
				}
			}				

- 自定义发布消息类 publishDeferrables

		private class DeferredPublishable extends MqttMessage {
			private Handler timeoutHandler;
			private MqttPublish callback;
			private String topic;
			private int timeout = 0;
			private boolean isPublishing;
			private Object extra;
	
			public DeferredPublishable(String topic, String payload,
					boolean retained, int qos, int timeout, MqttPublish callback,
					Object extra) {
	
				super(payload.getBytes());
				this.setQos(qos);
				this.setRetained(retained);
				this.extra = extra;
				this.callback = callback;
				this.topic = topic;
				this.timeout = timeout;
			}
	
			public void publishFailed() {
				if (callback != null)
					callback.publishFailed(extra);
			}
	
			public void publishSuccessfull() {
				if (callback != null)
					callback.publishSuccessfull(extra);
				cancelWait();
	
			}
	
			public void publishing() {
				isPublishing = true;
				if (callback != null)
					callback.publishing(extra);
			}
	
			public boolean isPublishing() {
				return isPublishing;
			}
	
			public String getTopic() {
				return topic;
			}
	
			public void cancelWait() {
				if (timeoutHandler != null)
					this.timeoutHandler.removeCallbacksAndMessages(this);
			}
	
			public void wait(LinkedList<DeferredPublishable> queue,
					Runnable onRemove) {
				if (timeoutHandler != null) {
					Log.d(TAG,
							"这个DeferredPublishable已经有一个超时设定");
					return;
				}
	
				//无需等待信号为0超时。该命令将立即失败
				if (callback != null && timeout > 0)
					callback.publishWaiting(extra);
	
				queue.addLast(this);
				this.timeoutHandler = new Handler();
				this.timeoutHandler.postDelayed(onRemove, timeout * 1000);
			}
		}

3.使用MQTT协议上传使用

- 使用的类需要实现自定义的MqttPublish接口中的方法，以判断上传时的状态

- 上传的数据为String格式，代码如下：

		StringBuilder payload = new StringBuilder();// 上传参数
		String topic = Constants.UPLOAD_LOCATION_MQTT;
		payload.append("{");
		payload.append("\"content\": ").append(uploadContent);
		payload.append("}");
		Log.d(TAG, "上传的参数：" + payload.toString());
		//利用MQTT上传，此处上传服务器地址为空，所以会报错
		ServiceMqtt.getInstance().publish(topic, payload.toString(), false, 0,
				20, this, uploadContent);
其中，topic为上传主题，即要上传到服务器的什么地方

应用举例：见工程中`MqttActivity`
