package com.asiacom.mq;

import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.asiacom.mq.IReceiver.OnReceiveMessageHandler;
import com.asiacom.mqtt.MqttActivity;

public class SampleActivity extends Activity implements OnReceiveMessageHandler{
	private RabbitMQConsumer receiver;
	private RabbitMQSender sender;
	private static final String SERVER = "192.168.0.209";
	private static final String EXCHANGE_NAME = "test";
	private TextView receiveMsgTv;
	private EditText inputEt;
	private Button sendBtn,mqttBtn;
	//Sourcetree test
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		receiver = new RabbitMQConsumer(SERVER, EXCHANGE_NAME);
		receiver.startReceive(this);
		sender = new RabbitMQSender(SERVER, 5672, "guest", "guest", EXCHANGE_NAME, "mq");
	}
	private void initView(){
		receiveMsgTv = (TextView) this.findViewById(R.id.msg_tv);
		inputEt = (EditText) this.findViewById(R.id.input_et);
		sendBtn = (Button) this.findViewById(R.id.send_btn);
		sendBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sender.send(inputEt.getText().toString());
				inputEt.setText("");
			}
		});
		mqttBtn = (Button)findViewById(R.id.mqtt_btn);
		mqttBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(SampleActivity.this,MqttActivity.class);
				startActivity(intent);
			}
		});
	}
	@Override
	public void onReceiveMessage(byte[] message) {
		try {
			receiveMsgTv.append(new String(message,"utf-8") + "\n");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		receiver.disconnect();
		sender.disconnect();
	}
}
