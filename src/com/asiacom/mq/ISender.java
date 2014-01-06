package com.asiacom.mq;



public interface ISender extends MQConnection{
	/**
	 * 发送消息
	 * @param context 发送的内容
	 */
	public void send(String content);
}
