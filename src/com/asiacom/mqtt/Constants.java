package com.asiacom.mqtt;

import android.content.Context;

import com.asiacom.mq.R;
import com.asiacom.mq.app.BaseApp;

//自定义常量类
public class Constants {
	public static final String TAG = "Constant";
	public static final String URL_MQTT_HOST = "";// 服务器地址  例 tcp://116.55.244.124
	public static final String URL_MQTT_PORT = "1883";// 端口号
	public static final String UPLOAD_LOCATION_MQTT = "topic/gps";// 上传地址  ,即MQTT上传主题
	public static String mDeviceID;

	// 定义MQTT服务的状态
	public static class State {
		public static enum ServiceMqtt {
			INITIAL, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED_WAITINGFORINTERNET, DISCONNECTED, DISCONNECTED_USERDISCONNECT, DISCONNECTED_DATADISABLED, DISCONNECTED_ERROR
		}

		public static String toString(ServiceMqtt state) {
			int id;
			switch (state) {
			case CONNECTED:
				id = R.string.connectivityConnected;
				break;
			case CONNECTING:
				id = R.string.connectivityConnecting;
				break;
			case DISCONNECTING:
				id = R.string.connectivityDisconnecting;
				break;
			case DISCONNECTED_USERDISCONNECT:
				id = R.string.connectivityDisconnectedUserDisconnect;
				break;
			case DISCONNECTED_DATADISABLED:
				id = R.string.connectivityDisconnectedDataDisabled;
				break;
			case DISCONNECTED_ERROR:
				id = R.string.error;
				break;
			default:
				id = R.string.connectivityDisconnected;

			}
			return BaseApp.mApp.getString(id);
		}

		public static enum ServiceLocator {
			INITIAL, PUBLISHING, PUBLISHING_WAITING, PUBLISHING_TIMEOUT, NOTOPIC, NOLOCATION
		}

		public static String toString(ServiceLocator state) {
			int id;
			switch (state) {
			case PUBLISHING:
				id = R.string.statePublishing;
				break;
			case PUBLISHING_WAITING:
				id = R.string.stateWaiting;
				break;
			case PUBLISHING_TIMEOUT:
				id = R.string.statePublishTimeout;
				break;
			case NOTOPIC:
				id = R.string.stateNotopic;
				break;
			case NOLOCATION:
				id = R.string.stateLocatingFail;
				break;
			default:
				id = R.string.stateIdle;
			}

			return BaseApp.mApp.getString(id);
		};

	}

}
