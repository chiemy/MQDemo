package com.asiacom.mqtt;

import com.asiacom.mq.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MqttActivity extends Activity implements MqttPublish,OnClickListener{
	private static final String TAG = "MqttActivity";
	private EditText editText;
	private Button uploadBtn;
	private String uploadContent = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mqtt);
		editText = (EditText)findViewById(R.id.content_et);
		uploadBtn = (Button)findViewById(R.id.upload_btn);
		uploadBtn.setOnClickListener(this);
	}

	@Override
	public void publishSuccessfull(Object extra) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publishFailed(Object extra) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publishing(Object extra) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publishWaiting(Object extra) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.upload_btn:
			uploadContent = editText.getText().toString();
			Log.d(TAG, "内容：" + uploadContent+"----"+uploadContent.length());
			if(!"".equals(uploadContent.trim())&& uploadContent != null&&uploadContent.length()>0){
				StringBuilder payload = new StringBuilder();// 上传参数
				String topic = Constants.UPLOAD_LOCATION_MQTT;
				payload.append("{");
				payload.append("\"content\": ").append(uploadContent);
				payload.append("}");
				Log.d(TAG, "上传的参数：" + payload.toString());
				//利用MQTT上传，此处上传服务器地址为空，所以会报错
				ServiceMqtt.getInstance().publish(topic, payload.toString(), false, 0,
						20, this, uploadContent);
			}else{
				Toast.makeText(this, "请输入要上传的内容", Toast.LENGTH_SHORT).show();
			}
			break;

		default:
			break;
		}
	}

	
	

}
