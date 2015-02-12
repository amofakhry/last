package com.authentication.activity;

import java.io.IOException;
import java.security.InvalidParameterException;

import com.authentication.asynctask.AsyncM1Card;
import com.authentication.asynctask.AsyncM1Card.OnReadAtPositionListener;
import com.authentication.asynctask.AsyncM1Card.OnReadCardNumListener;
import com.authentication.asynctask.AsyncM1Card.OnReadDataListener;
import com.authentication.asynctask.AsyncM1Card.OnWriteAtPositionListener;
import com.authentication.asynctask.AsyncM1Card.OnWriteDataListener;
import com.authentication.utils.ToastUtil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import android_serialport_api.M1CardAPI;
import android_serialport_api.SerialPortManager;

public class RFIDActivity extends Activity implements OnClickListener {
	private ProgressDialog progressDialog;

	private MyApplication application;

	private AsyncM1Card reader;

	private TextView tv;
	private TextView readTextView;

	private Button readCardNum;
	private Button write_button;
	private Button read_button;
	private Button writePositionButton;
	private Button readPositionButton;
	private Button changeButton;
	
	private Button openButton;
	private Button closeButton;

	private EditText writeEditText;

	private EditText writePositionEditText;
	private EditText writePositionDataEditText;
	private EditText writePasswordEditText;

	private EditText readPositionEditText;
	private TextView readPositionText;
	private EditText readPasswordEditText;

	private Spinner spinner1;
	private ArrayAdapter<String> adapter1;

	private Spinner spinner2;
	private ArrayAdapter<String> adapter2;

	private static final String[] m = { "KEYA", "KEYB" };
	private static final int[] keyType = { M1CardAPI.KEY_A, M1CardAPI.KEY_B };
	private int writeKeyType;
	private int readKeyType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rfid_activity);
		initView();
		initData();
	}

	private void initView() {
		tv = (TextView) findViewById(R.id.card_num_textview);
		readCardNum = (Button) findViewById(R.id.read_num_button);
		write_button = (Button) findViewById(R.id.write_button);
		read_button = (Button) findViewById(R.id.read_button);
		writeEditText = (EditText) findViewById(R.id.write_edittext);
		readTextView = (TextView) findViewById(R.id.read_textview);
		
		openButton = (Button) findViewById(R.id.open_button);
		closeButton = (Button) findViewById(R.id.close_button);
		openButton.setOnClickListener(this);
		closeButton.setOnClickListener(this);

		writePositionButton = (Button) findViewById(R.id.write_position_button);
		readPositionButton = (Button) findViewById(R.id.read_position_button);
		changeButton = (Button) findViewById(R.id.change_button);

		writePositionEditText = (EditText) findViewById(R.id.write_position);
		writePositionDataEditText = (EditText) findViewById(R.id.write_position_text);
		writePasswordEditText = (EditText) findViewById(R.id.write_password_text);

		readPositionEditText = (EditText) findViewById(R.id.read_position);
		readPositionText = (TextView) findViewById(R.id.read_position_text);
		readPasswordEditText = (EditText) findViewById(R.id.read_password_text);

		spinner1 = (Spinner) findViewById(R.id.Spinner01);
		// ����ѡ������ArrayAdapter��������
		adapter1 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, m);
		// ���������б�ķ��
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// ��adapter ��ӵ�spinner��
		spinner1.setAdapter(adapter1);
		spinner1.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				writeKeyType = keyType[position];
				Log.i("whw", "position=" + position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}

		});

		spinner2 = (Spinner) findViewById(R.id.Spinner02);
		// ����ѡ������ArrayAdapter��������
		adapter2 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, m);
		// ���������б�ķ��
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// ��adapter ��ӵ�spinner��
		spinner2.setAdapter(adapter2);
		spinner2.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				readKeyType = keyType[position];
				Log.i("whw", "position=" + position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}

		});

		readCardNum.setOnClickListener(this);
		write_button.setOnClickListener(this);
		read_button.setOnClickListener(this);

		writePositionButton.setOnClickListener(this);
		readPositionButton.setOnClickListener(this);
		
		changeButton.setOnClickListener(this);
	}

	private void initData() {
		application = (MyApplication) this.getApplicationContext();

		reader = new AsyncM1Card(application.getHandlerThread().getLooper());
		reader.setOnReadCardNumListener(new OnReadCardNumListener() {

			@Override
			public void onReadCardNumSuccess(String num) {
				tv.setText(num);
			}

			@Override
			public void onReadCardNumFail(int confirmationCode) {
				tv.setText("");
				if(confirmationCode == M1CardAPI.Result.FIND_FAIL){
					ToastUtil.showToast(RFIDActivity.this, "δѰ����,�з�������");
				}else if(confirmationCode == M1CardAPI.Result.TIME_OUT){
					ToastUtil.showToast(RFIDActivity.this, "δѰ����,�޷������ݣ���ʱ����");
				}else if(confirmationCode == M1CardAPI.Result.OTHER_EXCEPTION){
					ToastUtil.showToast(RFIDActivity.this, "�����Ǵ��ڴ�ʧ�ܻ������쳣");
				}
			}
		});

		reader.setOnReadDataListener(new OnReadDataListener() {

			@Override
			public void onReadDataSuccess(byte[][] data) {
				cancleProgressDialog();
				if (data != null && data.length != 0) {
					readTextView.setText(new String(data[0]));
				}
			}

			@Override
			public void onReadDataFail(int comfirmationCode) {
				cancleProgressDialog();
				ToastUtil.showToast(RFIDActivity.this, "����ʧ�ܣ�");
			}
		});

		reader.setOnWriteDataListener(new OnWriteDataListener() {

			@Override
			public void onWriteDataSuccess() {
				cancleProgressDialog();
				ToastUtil.showToast(RFIDActivity.this, "д���ɹ���");

			}

			@Override
			public void onWriteDataFail(int confirmationCode) {
				cancleProgressDialog();
				ToastUtil.showToast(RFIDActivity.this, "д��ʧ�ܣ�");
			}
		});

		reader.setOnWriteAtPositionListener(new OnWriteAtPositionListener() {

			@Override
			public void onWriteAtPositionSuccess() {
				cancleProgressDialog();
				ToastUtil.showToast(RFIDActivity.this, "д���ɹ���");
			}

			@Override
			public void onWriteAtPositionFail(int comfirmationCode) {
				cancleProgressDialog();
				ToastUtil.showToast(RFIDActivity.this, "д��ʧ�ܣ�");
			}
		});

		reader.setOnReadAtPositionListener(new OnReadAtPositionListener() {

			@Override
			public void onReadAtPositionSuccess(byte[] num,byte[] data) {
				cancleProgressDialog();
				if (data != null && data.length != 0) {
					tv.setText(new String(num));
					readPositionText.setText(new String(data));
				}

			}

			@Override
			public void onReadAtPositionFail(int comfirmationCode) {
				cancleProgressDialog();
				ToastUtil.showToast(RFIDActivity.this, "����ʧ�ܣ�");
			}
		});
	}

	private void showProgressDialog(String message) {
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(message);
		if (!progressDialog.isShowing()) {
			progressDialog.show();
		}
	}

	private void cancleProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.cancel();
			progressDialog = null;
		}
	}

	@Override
	public void onClick(View v) {
		try {
			if(!SerialPortManager.getInstance().isOpen()&& !SerialPortManager.getInstance().openSerialPort()){
				ToastUtil.showToast(this, "�򿪴���ʧ�ܣ�");
			}
		} catch (InvalidParameterException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(!SerialPortManager.getInstance().isOpen()){
				return;
			}
		}
		int id = v.getId();
		switch (id) {
		case R.id.read_num_button:
			tv.setText("");
			reader.readCardNum();
			break;
		case R.id.write_button:

			String data = writeEditText.getText().toString();
			if (!TextUtils.isEmpty(data)) {
				showProgressDialog("����д��...");
				reader.writeData(data.getBytes());
			} else {
				ToastUtil.showToast(this, "д�����ݲ���Ϊ��");
			}
			break;
		case R.id.read_button:
			showProgressDialog("���ڶ���...");
			reader.readData();
			break;
		case R.id.write_position_button:
			String writePosition = writePositionEditText.getText().toString();
			String writeData = writePositionDataEditText.getText().toString();
			String writePassword = writePasswordEditText.getText().toString();
			if (TextUtils.isEmpty(writePosition)
			 || TextUtils.isEmpty(writeData)
			// || TextUtils.isEmpty(writePassword)
			) {
				ToastUtil.showToast(this, "д��ʱ ��š����ݲ���Ϊ��");
			} else {
				showProgressDialog("����д��...");
				if (TextUtils.isEmpty(writePassword)) {
					reader.write(Integer.parseInt(writePosition), writeKeyType,
							null, writeData.getBytes());
				} else {
					reader.write(Integer.parseInt(writePosition), writeKeyType,
							writePassword.getBytes(), writeData.getBytes());
				}

			}
			break;
		case R.id.read_position_button:
			tv.setText("");
			readPositionText.setText("");
			String readPosition = readPositionEditText.getText().toString();
			String readPassword = readPasswordEditText.getText().toString();
			if (TextUtils.isEmpty(readPosition)) {
				ToastUtil.showToast(this, "���� ʱ��Ų���Ϊ��");
			} else {
				showProgressDialog("���ڶ���...");
				if (TextUtils.isEmpty(readPassword)) {
					reader.read(Integer.parseInt(readPosition), readKeyType,
							null);
				} else {
					reader.read(Integer.parseInt(readPosition), readKeyType,
							readPassword.getBytes());
				}
			}
			break;
		case R.id.change_button:
			 changePassword();
			break;
		case R.id.open_button:
//			initData();
			break;
		case R.id.close_button:
			if(SerialPortManager.getInstance().isOpen()){
				Log.i("whw", "closeSerialPort");
				SerialPortManager.getInstance().closeSerialPort();
			}
			break;
		default:
			break;
		}
	}

	/**
	 * �޸�����������,��Ҫ¼���ţ��������ͣ����������������A������B������������û��
	 * �޸Ĺ�����Ͳ����������룬����޸����밴ť���ɣ���ʵҲ���������ƿ�д��16�ֽڵ�����
	 */
	private void changePassword() {
		byte[] password = new byte[] { 
				//����A(6�ֽ�)
//				'b', 'b', 'b', 'b', 'b', 'b',
				(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,
				//��ȡ����(4�ֽ�),�����޸�
				(byte) 0xff, 0x07, (byte) 0x80, 0x69, 
				//����B(6�ֽ�)
				(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff
//				'a', 'a', 'a', 'a', 'a','a' 
				};
		String writePosition = writePositionEditText.getText().toString();
		String writePassword = writePasswordEditText.getText().toString();
		if (TextUtils.isEmpty(writePosition)
		) {
			ToastUtil.showToast(this, "д��ʱ�����ƿ�Ŀ�Ų���Ϊ�գ�");
		} else {
			showProgressDialog("����д��...");
			if (TextUtils.isEmpty(writePassword)) {
				reader.write(Integer.parseInt(writePosition), writeKeyType,
						null, password);
			} else {
				reader.write(Integer.parseInt(writePosition), writeKeyType,
						writePassword.getBytes(), password);
			}

		}
	}

}
