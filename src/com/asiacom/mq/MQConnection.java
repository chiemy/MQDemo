package com.asiacom.mq;


public interface MQConnection {
	/**
	 * 连接消息服务器，需要在子线程中执行
	 * @return
	 */
	public boolean connect();
	/**
	 * 与消息服务器断开
	 */
	public void disconnect();
	/**
	 * 设置连接超时时间
	 * @param timeout
	 */
	public void setConnectionTimeout(int timeout);
	/**
	 * 设置连接监听
	 * @param listener
	 */
	public void setConnectionListener(ConnectionListener listener);
	public interface ConnectionListener{
		/**
		 * 连接断开
		 */
		public void onConnectionBreak();
		/**
		 * 是否连接成功
		 * @param 
		 */
		public void onConnectionResult(boolean success);
	};
}
