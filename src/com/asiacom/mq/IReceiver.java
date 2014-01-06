package com.asiacom.mq;


public interface IReceiver extends MQConnection{
	
	
	/**
	 * 接收消息回调接口
	 * @author asiacom104
	 *
	 */
	public interface OnReceiveMessageHandler {
		/**
		 * 当接收到消息时此方法将会被调用
		 * @param message 接收到的数据
		 */
		public void onReceiveMessage(byte[] message);
	};

	/**
	 * 开始接收消息
	 * @param handler 接收消息回调接口
	 */
	public void startReceive(OnReceiveMessageHandler handler);
	
}
