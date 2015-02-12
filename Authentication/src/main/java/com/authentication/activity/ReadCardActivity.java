package com.authentication.activity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.authentication.asynctask.AsyncM1Card;
import com.authentication.asynctask.AsyncParseSFZ;
import com.authentication.asynctask.AsyncM1Card.OnReadAtPositionListener;
import com.authentication.asynctask.AsyncM1Card.OnReadCardNumListener;
import com.authentication.asynctask.AsyncParseSFZ.OnReadModuleListener;
import com.authentication.asynctask.AsyncParseSFZ.OnReadSFZListener;
import com.authentication.utils.ToastUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android_serialport_api.M1CardAPI;
import android_serialport_api.ParseSFZAPI;
import android_serialport_api.ParseSFZAPI.People;

import android_serialport_api.SerialPortManager;

@SuppressLint("NewApi")
public class ReadCardActivity extends Activity implements OnClickListener {
	private TextView sfz_name;
	private TextView sfz_sex;
	private TextView sfz_nation;
	private TextView sfz_year;
	private TextView sfz_mouth;
	private TextView sfz_day;
	private TextView sfz_address;
	private TextView sfz_id;
	private ImageView sfz_photo;

	private Button read_button;
	private Button stop_button;
	private Button back_button;

	private TextView resultInfo;
	private TextView rfidNum;

	private ProgressDialog progressDialog;

	private MyApplication application;

	private AsyncParseSFZ asyncParseSFZ;

	private AsyncM1Card reader;

	private static final int READ_SFZ = 1;
	private static final int READ_CARD_NUM = 2;

	private boolean isStop;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case READ_SFZ:
				clear();
				readSFZTime++;
				asyncParseSFZ.readSFZ(ParseSFZAPI.SECOND_GENERATION_CARD);
				break;
			case READ_CARD_NUM:
				clear();
				readRFIDTime++;
				reader.readCardNum();
//				reader.read(4, M1CardAPI.KEY_A, null);
				break;
			default:
				break;
			}
		}

	};

	private int readSFZTime = 0;
	private int readSFZFailTime = 0;
	private int readSFZSuccessTime = 0;

	private int readRFIDTime = 0;
	private int readRFIDFailTime = 0;
	private int readRFIDSuccessTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.read_card_activity);
		initView();
		initData();
	}

	private void initView() {
		sfz_name = ((TextView) findViewById(R.id.sfz_name));
		sfz_nation = ((TextView) findViewById(R.id.sfz_nation));
		sfz_sex = ((TextView) findViewById(R.id.sfz_sex));
		sfz_year = ((TextView) findViewById(R.id.sfz_year));
		sfz_mouth = ((TextView) findViewById(R.id.sfz_mouth));
		sfz_day = ((TextView) findViewById(R.id.sfz_day));
		sfz_address = ((TextView) findViewById(R.id.sfz_address));
		sfz_id = ((TextView) findViewById(R.id.sfz_id));
		sfz_photo = ((ImageView) findViewById(R.id.sfz_photo));

		read_button = ((Button) findViewById(R.id.readCardButton));
		stop_button = ((Button) findViewById(R.id.stopReadCardButton));
		back_button = ((Button) findViewById(R.id.backButton));
		rfidNum = ((TextView) findViewById(R.id.rfidNum));
		resultInfo = ((TextView) findViewById(R.id.resultInfo2));

		read_button.setOnClickListener(this);
		stop_button.setOnClickListener(this);
		back_button.setOnClickListener(this);
	}

	private void initData() {
		application = (MyApplication) this.getApplicationContext();

		asyncParseSFZ = new AsyncParseSFZ(application.getHandlerThread().getLooper(),application.getRootPath());
		reader = new AsyncM1Card(application.getHandlerThread().getLooper());
		asyncParseSFZ.setOnReadSFZListener(new OnReadSFZListener() {
			@Override
			public void onReadSuccess(People people) {
				cancleProgressDialog();
				updateInfo(people);
				readSFZSuccessTime++;
				refresh();
				if (!isStop) {
					// showProgressDialog("正在读取数据...");
					// reader.readCardNum();
					// asyncParseSFZ.readSFZ();
					mHandler.sendEmptyMessageDelayed(READ_CARD_NUM, 2000);
				}
			}

			@Override
			public void onReadFail(int result) {
				cancleProgressDialog();
				if (result == ParseSFZAPI.Result.FIND_FAIL) {
					ToastUtil.showToast(ReadCardActivity.this,
							"未寻到卡,有返回数据    ");
				} else if (result == ParseSFZAPI.Result.TIME_OUT) {
					ToastUtil.showToast(ReadCardActivity.this,
							"未寻到卡,无返回数据，超时！！length=");
				} else if (result == ParseSFZAPI.Result.OTHER_EXCEPTION) {
					ToastUtil.showToast(ReadCardActivity.this,
							"可能是串口打开失败或其他异常  length=");
				}
				// ToastUtil.showToast(ReadCardActivity.this, "未寻身份证！length="
				// + asyncParseSFZ.intputLength);
				readSFZFailTime++;
				refresh();
				if (!isStop) {
					// showProgressDialog("正在读取数据...");
					// reader.readCardNum();
					// asyncParseSFZ.readSFZ();
					mHandler.sendEmptyMessageDelayed(READ_CARD_NUM, 2000);
				}
			}
		});

		reader.setOnReadCardNumListener(new OnReadCardNumListener() {

			@Override
			public void onReadCardNumSuccess(String num) {
				cancleProgressDialog();
				rfidNum.setText(num);
				readRFIDSuccessTime++;
				refresh();
				if (!isStop) {
					// showProgressDialog("正在读取数据...");
					// asyncParseSFZ.readSFZ();
					mHandler.sendEmptyMessageDelayed(READ_SFZ, 2000);
				}
			}

			@Override
			public void onReadCardNumFail(int confirmationCode) {
				cancleProgressDialog();
				rfidNum.setText("");
				if (confirmationCode == M1CardAPI.Result.FIND_FAIL) {
					ToastUtil.showToast(ReadCardActivity.this,
							"未寻到卡,有返回数据length=");
				} else if (confirmationCode == M1CardAPI.Result.TIME_OUT) {
					ToastUtil.showToast(ReadCardActivity.this,
							"未寻到卡,无返回数据，超时！！length=");
				} else if (confirmationCode == M1CardAPI.Result.OTHER_EXCEPTION) {
					ToastUtil.showToast(ReadCardActivity.this,
							"可能是串口打开失败或其他异常length=");
				}
				// ToastUtil.showToast(ReadCardActivity.this, "switchLength="
				// + reader.switchLength + "未寻到RFID！length="
				// + reader.inputLength);
				readRFIDFailTime++;
				refresh();
				if (!isStop) {
					// showProgressDialog("正在读取数据...");
					// asyncParseSFZ.readSFZ();
					mHandler.sendEmptyMessageDelayed(READ_SFZ, 2000);
				}
			}
		});
		
		reader.setOnReadAtPositionListener(new OnReadAtPositionListener() {

			@Override
			public void onReadAtPositionSuccess(byte[] num,byte[] data) {
//				cancleProgressDialog();
//				if (data != null && data.length != 0) {
//					tv.setText(new String(num));
//					readPositionText.setText(new String(data));
//				}
				
				cancleProgressDialog();
				rfidNum.setText("卡号："+new String(num)+"\n数据："+new String(data));
				readRFIDSuccessTime++;
				refresh();
				if (!isStop) {
					// showProgressDialog("正在读取数据...");
					// asyncParseSFZ.readSFZ();
					mHandler.sendEmptyMessageDelayed(READ_SFZ, 2000);
				}

			}

			@Override
			public void onReadAtPositionFail(int comfirmationCode) {
				cancleProgressDialog();
				rfidNum.setText("");
				if (comfirmationCode == M1CardAPI.Result.FIND_FAIL) {
					ToastUtil.showToast(ReadCardActivity.this,
							"未寻到卡,有返回数据length=");
				} else if (comfirmationCode == M1CardAPI.Result.TIME_OUT) {
					ToastUtil.showToast(ReadCardActivity.this,
							"未寻到卡,无返回数据，超时！！length=");
				} else if (comfirmationCode == M1CardAPI.Result.OTHER_EXCEPTION) {
					ToastUtil.showToast(ReadCardActivity.this,
							"可能是串口打开失败或其他异常length=");
				}
				// ToastUtil.showToast(ReadCardActivity.this, "switchLength="
				// + reader.switchLength + "未寻到RFID！length="
				// + reader.inputLength);
				readRFIDFailTime++;
				refresh();
				if (!isStop) {
					// showProgressDialog("正在读取数据...");
					// asyncParseSFZ.readSFZ();
					mHandler.sendEmptyMessageDelayed(READ_SFZ, 2000);
				}
			}
		});
	}

	@Override
	public void onClick(View v) {
		try {
			if(!SerialPortManager.getInstance().isOpen()&& !SerialPortManager.getInstance().openSerialPort()){
				ToastUtil.showToast(this, "打开串口失败！");
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
		case R.id.readCardButton:
			isStop = false;
			// showProgressDialog("正在读取数据...");
			// asyncParseSFZ.readSFZ();
			readRFIDTime++;
			reader.readCardNum();
//			reader.read(4, M1CardAPI.KEY_A, null);
			// reader.readCardNum();
			Log.i("whw", "read_sfz");
			break;
		case R.id.stopReadCardButton:
			stopRead();
			break;
		case R.id.backButton:
			finish();
			break;
		default:
			break;
		}

	}

	private void refresh() {
		String result = "身份证总共：" + readSFZTime + "  成功：" + readSFZSuccessTime
				+ "  失败：" + readSFZFailTime + "\n" + "RFID总共：" + readRFIDTime
				+ "  成功：" + readRFIDSuccessTime + "  失败：" + readRFIDFailTime;
		Log.i("whw", "result=" + result);
		resultInfo.setText(result);
	}

	private void stopRead() {
		cancleProgressDialog();
		isStop = true;
		mHandler.removeMessages(READ_CARD_NUM);
		mHandler.removeMessages(READ_SFZ);
	}

	@Override
	protected void onStop() {
		stopRead();
		super.onStop();
	}

	private void updateInfo(People people) {
		sfz_address.setText(people.getPeopleAddress());
		sfz_day.setText(people.getPeopleBirthday().substring(6));
		sfz_id.setText(people.getPeopleIDCode());
		sfz_mouth.setText(people.getPeopleBirthday().substring(4, 6));
		sfz_name.setText(people.getPeopleName());
		sfz_nation.setText(people.getPeopleNation());
		sfz_sex.setText(people.getPeopleSex());
		sfz_year.setText(people.getPeopleBirthday().substring(0, 4));
		Bitmap photo = BitmapFactory.decodeByteArray(people.getPhoto(), 0,
				people.getPhoto().length);
		sfz_photo.setBackgroundDrawable(new BitmapDrawable(photo));
	}

	private void clear() {
		sfz_address.setText("");
		sfz_day.setText("");
		sfz_id.setText("");
		sfz_mouth.setText("");
		sfz_name.setText("");
		sfz_nation.setText("");
		sfz_sex.setText("");
		sfz_year.setText("");
		sfz_photo.setBackgroundColor(0);

		rfidNum.setText("");
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

}
