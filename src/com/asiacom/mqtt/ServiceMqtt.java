package com.asiacom.mqtt;

import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asiacom.mq.R;
import com.asiacom.mqtt.Constants.State;

import de.greenrobot.event.EventBus;


//Mqtt服务（连接服务器相关操作）
public class ServiceMqtt extends ServiceBindable implements MqttCallback {
	private final String TAG = "ServiceMqtt";

	private static Constants.State.ServiceMqtt state = Constants.State.ServiceMqtt.INITIAL;//获取自定义状态

	private short keepAliveSeconds;//低耗网络，但是又需要及时获取数据，心跳30s
	private int timeout;//超时时间设定
	private MqttClient mqttClient;
	private SharedPreferences sharedPreferences;
	private static ServiceMqtt instance;//单例
	private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
	private Thread workerThread;
	private LinkedList<DeferredPublishable> deferredPublishables;
	private Exception error;
	private HandlerThread pubThread;
	private Handler pubHandler;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		workerThread = null;
		error = null;
		changeState(Constants.State.ServiceMqtt.INITIAL);
		keepAliveSeconds = 30;
		timeout = 20;//超时时间
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		deferredPublishables = new LinkedList<DeferredPublishable>();
		EventBus.getDefault().register(this);//注册EventBus

		pubThread = new HandlerThread("MQTTPUBTHREAD");
		pubThread.start();
		pubHandler = new Handler(pubThread.getLooper());

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		doStart(intent, startId);
		return super.onStartCommand(intent, flags, startId);
	}

	//启动连接
	private void doStart(final Intent intent, final int startId) {
		// init();

		Thread thread1 = new Thread() {
			@Override
			public void run() {
				handleStart(intent, startId);
				if (this == workerThread) // Clean up worker thread
					workerThread = null;
			}

			@Override
			public void interrupt() {
				if (this == workerThread) // Clean up worker thread
					workerThread = null;
				super.interrupt();
			}
		};
		thread1.start();
	}

	//开始连接服务器
	void handleStart(Intent intent, int startId) {
		Log.v(TAG, "开始处理");

		// Respect user's wish to stay disconnected. Overwrite with startId ==
		// -1 to reconnect manually afterwards
		if ((state == State.ServiceMqtt.DISCONNECTED_USERDISCONNECT)
				&& startId != -1) {
			Log.d(TAG, "用户断开连接");
			return;
		}

		if (isConnecting()) {
			Log.d(TAG, "正在连接服务器");
			return;
		}

//		// Respect user's wish to not use data
//		if (!isBackgroundDataEnabled()) {
//			Log.e(TAG, "数据不可用");
//			changeState(State.ServiceMqtt.DISCONNECTED_DATADISABLED);
//			return;
//		}

		// Don't do anything unless we're disconnected
		if (isDisconnected()) {
			Log.v(TAG, "是否断开连接");
			// 检查是否有数据连接
			if (isOnline(true)) {
				if (connect()) {
					Log.v(TAG, "连接服务器成功");
					onConnect();
				}
			} else {
				Log.e(TAG, "handleStart: !isOnline");
				changeState(State.ServiceMqtt.DISCONNECTED_WAITINGFORINTERNET);
			}
		} else {
			Log.d(TAG, "已经连接");

		}
	}

	//是否断开连接
	private boolean isDisconnected() {
		Log.v(TAG, "断开连接检查: " + state);
		return state == State.ServiceMqtt.INITIAL
				|| state == State.ServiceMqtt.DISCONNECTED
				|| state == State.ServiceMqtt.DISCONNECTED_USERDISCONNECT
				|| state == State.ServiceMqtt.DISCONNECTED_WAITINGFORINTERNET
				|| state == State.ServiceMqtt.DISCONNECTED_ERROR;
	}

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

//	private int getBrokerSecurityMode() {
//		return sharedPreferences.getInt(Defaults.SETTINGS_KEY_BROKER_SECURITY,
//				Defaults.VALUE_BROKER_SECURITY_NONE);
//	}

	//SSL
//	private javax.net.ssl.SSLSocketFactory getSSLSocketFactory()
//			throws CertificateException, KeyStoreException,
//			NoSuchAlgorithmException, IOException, KeyManagementException {
//		CertificateFactory cf = CertificateFactory.getInstance("X.509");
//		// From https://www.washington.edu/itconnect/security/ca/load-der.crt
//		InputStream caInput = new BufferedInputStream(new FileInputStream(
//				sharedPreferences.getString(
//						Defaults.SETTINGS_KEY_BROKER_SECURITY_SSL_CA_PATH, "")));
//		java.security.cert.Certificate ca;
//		try {
//			ca = cf.generateCertificate(caInput);
//		} finally {
//			caInput.close();
//		}
//
//		// Create a KeyStore containing our trusted CAs
//		String keyStoreType = KeyStore.getDefaultType();
//		KeyStore keyStore = KeyStore.getInstance(keyStoreType);
//		keyStore.load(null, null);
//		keyStore.setCertificateEntry("ca", ca);
//
//		// Create a TrustManager that trusts the CAs in our KeyStore
//		String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
//		TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
//		tmf.init(keyStore);
//
//		// Create an SSLContext that uses our TrustManager
//		SSLContext context = SSLContext.getInstance("TLS");
//		context.init(null, tmf.getTrustManagers(), null);
//
//		return context.getSocketFactory();
//	}

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

//	private void setWill(MqttConnectOptions m) {
//		StringBuffer payload = new StringBuffer();
//		payload.append("{");
//		payload.append("\"type\": ").append("\"").append("_lwt").append("\"");
//		payload.append(", \"tst\": ").append("\"")
//				.append((int) (new Date().getTime() / 1000)).append("\"");
//		payload.append("}");

//		m.setWill(mqttClient.getTopic(sharedPreferences.getString(
//				Defaults.SETTINGS_KEY_TOPIC, Defaults.VALUE_TOPIC)), payload
//				.toString().getBytes(), 0, false);
//	}

	private void onConnect() {
		if (!isConnected())
			Log.e(TAG, "onConnect: !isConnected");
	}

	//断开与服务器的连接
	public void disconnect(boolean fromUser) {
		Log.v(TAG, "断开服务器连接");

		if (isConnecting()) // throws
							// MqttException.REASON_CODE_CONNECT_IN_PROGRESS
							// when disconnecting while connect is in progress.
			return;

		if (fromUser)
			changeState(State.ServiceMqtt.DISCONNECTED_USERDISCONNECT);

		try {
			if (isConnected())
				mqttClient.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mqttClient = null;

			if (workerThread != null) {
				workerThread.interrupt();
			}

		}
	}

	@SuppressLint("Wakelock")
	// Lint check derps with the wl.release() call.
	@Override
	public void connectionLost(Throwable t) {
		Log.e(TAG, "error: " + t.toString());
		// 设置保持手机处于唤醒状态，以保证MQTT连接完成
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
		wl.acquire();//设置保持唤醒

		if (!isOnline(true)) {
			changeState(State.ServiceMqtt.DISCONNECTED_WAITINGFORINTERNET);
		} else {
			changeState(State.ServiceMqtt.DISCONNECTED);
		}
		wl.release();//解除保持唤醒
	}

	public void reconnect() {
		disconnect(true);
		doStart(null, -1);
	}

	public void messageArrived(MqttTopic topic, MqttMessage message)
			throws MqttException {

	}

	public void onEvent(Events.StateChanged.ServiceMqtt event) {
		if (event.getState() == State.ServiceMqtt.CONNECTED)
			publishDeferrables();
	}

	//修改状态
	private void changeState(Exception e) {
		error = e;
		changeState(Constants.State.ServiceMqtt.DISCONNECTED_ERROR, e);
	}

	private void changeState(Constants.State.ServiceMqtt newState) {
		changeState(newState, null);
	}

	private void changeState(Constants.State.ServiceMqtt newState, Exception e) {
		Log.d(TAG, "ServiceMqtt 状态改为: " + newState);
		state = newState;
		EventBus.getDefault().postSticky(
				new Events.StateChanged.ServiceMqtt(newState, e));
	}

	//是否联网
	private boolean isOnline(boolean shouldCheckIfOnWifi) {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();

		return netInfo != null && netInfo.isAvailable()
				&& netInfo.isConnected();
	}

	public boolean isConnected() {
		return ((mqttClient != null) && (mqttClient.isConnected() == true));
	}

	public static boolean isErrorState(State.ServiceMqtt state) {
		return state == State.ServiceMqtt.DISCONNECTED_ERROR;
	}

	public boolean hasError() {
		return error != null;
	}

	//正在连接
	public boolean isConnecting() {
		return (mqttClient != null)
				&& state == State.ServiceMqtt.CONNECTING;
	}

	//数据是否启用
	private boolean isBackgroundDataEnabled() {
		return isOnline(false);
	}

	/**
	 * @category MISC
	 */
	public static ServiceMqtt getInstance() {
		return instance;
	}

	/**
	 * 
	 * @Title: getDefaultClientId 
	 * @Description:获取clientID
	 * @param @return
	 * @return:String
	 * @exception:
	 */
	public static String getDefaultClientId() {
		String mqttClientId = Constants.mDeviceID;

		// MQTT规范不允许客户端ID长于23个字符
		if (mqttClientId.length() > 22){
			Log.d("ServiceMqtt1111", "clientId:" + mqttClientId);
			mqttClientId = mqttClientId.substring(0, 22);
		}
		Log.d("ServiceMqtt", "clientId:" + mqttClientId);
		return mqttClientId;
	}

	@Override
	public void onDestroy() {
		// disconnect immediately
		disconnect(false);

		changeState(State.ServiceMqtt.DISCONNECTED);

		sharedPreferences
				.unregisterOnSharedPreferenceChangeListener(preferencesChangedListener);

		super.onDestroy();
	}

	public static State.ServiceMqtt getState() {
		return state;
	}

	//获取错误信息
	public static String getErrorMessage() {
		Exception e = getInstance().error;

		if (getInstance() != null && getInstance().hasError()
				&& e.getCause() != null)
			return "Error: " + e.getCause().getLocalizedMessage();
		else
			return "Error: " + getInstance().getString(R.string.na);

	}

	public static String getStateAsString() {
		return State.toString(state);
	}

	public static String stateAsString(State.ServiceLocator state) {
		return State.toString(state);
	}

	private void deferPublish(final DeferredPublishable p) {
		p.wait(deferredPublishables, new Runnable() {

			@Override
			public void run() {
				deferredPublishables.remove(p);
				if (!p.isPublishing())// might happen that the publish is in
										// progress while the timeout occurs.
					p.publishFailed();
			}
		});
	}

	public void publish(String topic, String payload) {
		publish(topic, payload, false, 0, 0, null, null);
	}

	public void publish(String topic, String payload, boolean retained) {
		publish(topic, payload, retained, 0, 0, null, null);
	}

	public void publish(final String topic, final String payload,
			final boolean retained, final int qos, final int timeout,
			final MqttPublish callback, final Object extra) {

		publish(new DeferredPublishable(topic, payload, retained, qos, timeout,
				callback, extra));

	}

	private void publish(final DeferredPublishable p) {

		pubHandler.post(new Runnable() {

			@Override
			public void run() {

				if (Looper.getMainLooper().getThread() == Thread
						.currentThread()) {
					Log.e(TAG, "PUB ON MAIN THREAD");
				}

				if (!isOnline(false) || !isConnected()) {
					Log.d(TAG, "pub deferred");

					deferPublish(p);
					doStart(null, 1);
					return;
				}

				try {
					p.publishing();
					mqttClient.getTopic(p.getTopic()).publish(p);
					p.publishSuccessfull();
				} catch (MqttException e) {
					Log.e(TAG, e.getMessage());
					e.printStackTrace();
					p.cancelWait();
					p.publishFailed();
				}
			}
		});

	}

	private void publishDeferrables() {
		for (Iterator<DeferredPublishable> iter = deferredPublishables
				.iterator(); iter.hasNext();) {
			DeferredPublishable p = iter.next();
			iter.remove();
			publish(p);
		}
	}

	//发布消息类
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

	@Override
	protected void onStartOnce() {
	}

	@Override
	public void deliveryComplete(MqttDeliveryToken arg0) {
		// TODO Auto-generated method stub

	}

}
