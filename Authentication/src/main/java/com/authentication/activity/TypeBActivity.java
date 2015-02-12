package com.authentication.activity;

import java.io.IOException;
import java.security.InvalidParameterException;

import com.authentication.utils.ToastUtil;

import android.app.Activity;
import android.app.SearchManager.OnCancelListener;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android_serialport_api.SerialPortManager;
import android_serialport_api.TypeBCardAPI;

public class TypeBActivity extends Activity implements OnClickListener {
	private Button release;
	private Button use;

	private EditText cid;
	private EditText identify;
	private EditText key;
	private EditText writeData;

	TypeBCardAPI api;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.type_b_activity);
		initView();
		initData();
	}

	private void initView() {
		release = (Button) findViewById(R.id.release);
		use = (Button) findViewById(R.id.use);

		cid = (EditText) findViewById(R.id.cid);
		identify = (EditText) findViewById(R.id.identify);
		key = (EditText) findViewById(R.id.key);
		writeData = (EditText) findViewById(R.id.writeData);

		release.setOnClickListener(this);
		use.setOnClickListener(this);
	}

	private void initData() {
		api = new TypeBCardAPI();
	}

	@Override
	public void onClick(View view) {
		try {
			if (!SerialPortManager.getInstance().isOpen()
					&& !SerialPortManager.getInstance().openSerialPort()) {
				ToastUtil.showToast(this, "´ò¿ª´®¿ÚÊ§°Ü£¡");
			}
		} catch (InvalidParameterException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (!SerialPortManager.getInstance().isOpen()) {
				return;
			}
		}
		int id = view.getId();
		switch (id) {
		case R.id.release:
			api.release('1');
			break;
		case R.id.use:
			api.comsume('2');
			break;

		default:
			break;
		}

	}

}
