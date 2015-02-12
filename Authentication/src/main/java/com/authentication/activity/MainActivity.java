package com.authentication.activity;

import java.io.IOException;
import java.security.InvalidParameterException;

import com.authentication.utils.ToastUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android_serialport_api.SerialPortManager;

public class MainActivity extends Activity implements OnClickListener {
	private static Activity MainActivity = null;

	private MyApplication application;
	
	private Button pin_button;
	private Button fingerprint_button;
	private Button rfid_button;
	private Button loop_button;
	private Button type_button;
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i("whw", "onCreate  ="+this.toString());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		application = (MyApplication) this.getApplicationContext();
		initView();
		initData();
	}

	private void initView() {
		pin_button = (Button) findViewById(R.id.pin_button);
		fingerprint_button = (Button) findViewById(R.id.fingerprint_button);
		rfid_button = (Button) findViewById(R.id.rfid_button);
		loop_button = (Button) findViewById(R.id.loop_button);
		type_button = (Button) findViewById(R.id.type_button);
		
		pin_button.setOnClickListener(this);
		fingerprint_button.setOnClickListener(this);
		rfid_button.setOnClickListener(this);
		loop_button.setOnClickListener(this);
		type_button.setOnClickListener(this);
	}
	
	private void initData(){

	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.pin_button:
			startActivity(new Intent(this, SFZActivity.class));
			break;
		case R.id.fingerprint_button:
			startActivity(new Intent(this, RegisterFingerprintActivity.class));
			break;
		case R.id.rfid_button:
			startActivity(new Intent(this, RFIDActivity.class));
			break;
		case R.id.loop_button:
			startActivity(new Intent(this, ReadCardActivity.class));
			break;
		case R.id.type_button:
			startActivity(new Intent(this, TypeBActivity.class));
			break;
		default:
			break;
		}

	}
	

	
	private long lastBackTime = 0;
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			long currentBackTime = System.currentTimeMillis();
			if(currentBackTime-lastBackTime>2000){
				ToastUtil.showToast(this, "再按一次退出程序");
				lastBackTime = currentBackTime;
			}else{
				SerialPortManager.getInstance().closeSerialPort();
				android.os.Process.killProcess(android.os.Process.myPid());
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onDestroy() {
		Log.i("whw", "onDestroy  ="+this.toString());
		if(SerialPortManager.getInstance().isOpen()){
			Log.i("whw", "closeSerialPort");
			SerialPortManager.getInstance().closeSerialPort();
		}
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	@Override
	protected void onRestart() {
		Log.i("whw", "onRestart  ="+this.toString());
		super.onRestart();
	}

	@Override
	protected void onResume() {
		Log.i("whw", "onResume  ="+this.toString());
		super.onResume();
	}

	@Override
	protected void onStart() {
		Log.i("whw", "onStart  ="+this.toString());
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.i("whw", "onStop  ="+this.toString());
		super.onStop();
	}
	
}
