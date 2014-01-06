package com.asiacom.mq;

import android.os.AsyncTask;
import android.util.Log;


public class RabbitMQSender extends RabbitMQ implements ISender {

	private ConnectionListener listener;
	private SendTask task;
	private boolean exit = false;

	/**
	 * @param mServer
	 *            服务器
	 * @param port
	 *            端口号
	 * @param userName
	 *            用户名
	 * @param password
	 *            密码
	 * @param exchangeName
	 *            交换机名称
	 * @param queueName
	 *            消息队列名称
	 */
	public RabbitMQSender(String mServer, int port, String userName,
			String password, String exchangeName, String queueName) {
		super(mServer, port, userName, password, exchangeName, queueName);
	}

	@Override
	public void send(String content) {
		task = new SendTask();
		task.execute(content);
	}

	class SendTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			boolean flag = false;
			if (exit) {
				return flag;
			}
			if (connect()) {
				flag = true;
				StringBuffer tempstr = new StringBuffer();
				for (int i = 0; i < params.length; i++) {
					tempstr.append(params[i]);
				}
				try {
					// 发送
					RabbitMQSender.this.mChannel.basicPublish(
							RabbitMQSender.this.exchangeName,
							RabbitMQSender.this.queueName, null, tempstr
									.toString().getBytes());
				} catch (Exception e) {
					Log.d("SendTask", "send failed");
				}
			} else {
				Log.d("SendTask", "login failed");
			}
			return flag;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (listener != null) {
				listener.onConnectionResult(result);
			}
		}
	}

	@Override
	public void disconnect() {
		super.disconnect();
		exit = true;
	}

}
