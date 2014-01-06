package com.asiacom.mq.app;

import android.app.Application;

public class BaseApp extends Application{
	public static BaseApp mApp;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mApp = this;
	}
	
}
