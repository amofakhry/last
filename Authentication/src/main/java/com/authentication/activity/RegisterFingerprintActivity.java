package com.authentication.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;

import com.authentication.asynctask.AsyncFingerprint;
import com.authentication.asynctask.AsyncFingerprint.OnDeleteCharListener;
import com.authentication.asynctask.AsyncFingerprint.OnDownCharListener;
import com.authentication.asynctask.AsyncFingerprint.OnEmptyListener;
import com.authentication.asynctask.AsyncFingerprint.OnEnrollListener;
import com.authentication.asynctask.AsyncFingerprint.OnGenCharListener;
import com.authentication.asynctask.AsyncFingerprint.OnGetImageListener;
import com.authentication.asynctask.AsyncFingerprint.OnIdentifyListener;
import com.authentication.asynctask.AsyncFingerprint.OnLoadCharListener;
import com.authentication.asynctask.AsyncFingerprint.OnMatchListener;
import com.authentication.asynctask.AsyncFingerprint.OnRegModelListener;
import com.authentication.asynctask.AsyncFingerprint.OnSearchListener;
import com.authentication.asynctask.AsyncFingerprint.OnStoreCharListener;
import com.authentication.asynctask.AsyncFingerprint.OnUpCharListener;
import com.authentication.asynctask.AsyncFingerprint.OnUpImageListener;
import com.authentication.utils.ToastUtil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import android_serialport_api.SerialPortManager;

public class RegisterFingerprintActivity extends Activity implements
		OnClickListener {
	private MyApplication application;

	private AsyncFingerprint registerFingerprint;
	private AsyncFingerprint validateFingerprint;
	private Button start;

	private Button validate;

	private Button back;

	private ImageView fingerprintImage;


	private ProgressDialog progressDialog;

	private boolean isValidate;

	private byte[] model;


	private int count;
	
	private String path = Environment.getExternalStorageDirectory()+File.separator+"fingerprint_image";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register_zhiwen);
		initView();
		initViewListener();
		initData();
	}

	private void initView() {
		start = (Button) findViewById(R.id.start);
		validate = (Button) findViewById(R.id.validateZhiwen);
		back = (Button) findViewById(R.id.backRegister);
		fingerprintImage = (ImageView) findViewById(R.id.fingerprintImage);
	}
	
	private String rootPath = Environment.getExternalStorageDirectory()
			.getAbsolutePath();
	private void writeToFile(byte[] data) {
		String dir = rootPath + File.separator + "fingerprint_image";
		File dirPath = new File(dir);
		if(!dirPath.exists()){
			dirPath.mkdir();
		}
		
		String filePath =dir+"/"+System.currentTimeMillis()+".bmp";
		File file = new File(filePath);
		if (file.exists()) {
			file.delete();
		}
		FileOutputStream fos = null;
		try {
			file.createNewFile();
			fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void initData() {
		application = (MyApplication) this.getApplicationContext();

		registerFingerprint = new AsyncFingerprint(application.getHandlerThread().getLooper());
		validateFingerprint = new AsyncFingerprint(application.getHandlerThread().getLooper());
		registerFingerprint.setOnGetImageListener(new OnGetImageListener() {
			@Override
			public void onGetImageSuccess() {
				cancleProgressDialog();
				registerFingerprint.PS_UpImage();
				showProgressDialog("���ڴ���...");

			}

			@Override
			public void onGetImageFail() {
				registerFingerprint.PS_GetImage();
			}
		});
		registerFingerprint.setOnUpImageListener(new OnUpImageListener() {
			@Override
			public void onUpImageSuccess(byte[] data) {
				Log.i("whw", "up image data.length="+data.length);
				saveImage(data);
				Bitmap image = BitmapFactory.decodeByteArray(data, 0,
						data.length);
				fingerprintImage
						.setBackgroundDrawable(new BitmapDrawable(image));
				registerFingerprint.PS_GenChar(count);
			}

			@Override
			public void onUpImageFail() {
				Log.i("whw", "up image fail");
			}
		});

		registerFingerprint.setOnGenCharListener(new OnGenCharListener() {
			@Override
			public void onGenCharSuccess(int bufferId) {
				if (bufferId == 1) {
					cancleProgressDialog();
						showProgressDialog("���ٰ�һ��ָ�ƣ�");
						registerFingerprint.PS_GetImage();
						count++;
				} else if (bufferId == 2) {
					registerFingerprint.PS_RegModel();
				}
			}

			@Override
			public void onGenCharFail() {
				cancleProgressDialog();
				ToastUtil.showToast(RegisterFingerprintActivity.this,
						"��������ֵʧ�ܣ�");
			}
		});

		registerFingerprint.setOnRegModelListener(new OnRegModelListener() {

			@Override
			public void onRegModelSuccess() {
				cancleProgressDialog();
				// showProgressDialog("�����ϴ�ģ�壡");
				registerFingerprint.PS_UpChar();
				ToastUtil
						.showToast(RegisterFingerprintActivity.this, "�ϳ�ģ��ɹ���");
			}

			@Override
			public void onRegModelFail() {
				cancleProgressDialog();
				ToastUtil
						.showToast(RegisterFingerprintActivity.this, "�ϳ�ģ��ʧ�ܣ�");
			}
		});

		registerFingerprint.setOnUpCharListener(new OnUpCharListener() {

			@Override
			public void onUpCharSuccess(byte[] model) {
				cancleProgressDialog();
				Log.i("whw", "#################model.length="+model.length);
				RegisterFingerprintActivity.this.model = model;
				ToastUtil.showToast(RegisterFingerprintActivity.this, "ע��ɹ���");

				// ///test
//				 fingerprint.PS_StoreChar(2, 1000);

			}

			@Override
			public void onUpCharFail() {
				cancleProgressDialog();
				ToastUtil.showToast(RegisterFingerprintActivity.this, "ע��ʧ�ܣ�");
			}
		});
		
		validateFingerprint.setOnGetImageListener(new OnGetImageListener() {
			@Override
			public void onGetImageSuccess() {
				cancleProgressDialog();
				validateFingerprint.PS_GenChar(1);
				showProgressDialog("���ڴ���...");

			}

			@Override
			public void onGetImageFail() {
				validateFingerprint.PS_GetImage();
			}
		});
		
		validateFingerprint.setOnGenCharListener(new OnGenCharListener() {
			@Override
			public void onGenCharSuccess(int bufferId) {
				validateFingerprint.PS_DownChar(2,model);
//				validateFingerprint.PS_Match();
				Log.i("whw", "validateFingerprint onGenCharSuccess bufferId="+bufferId);
			}

			@Override
			public void onGenCharFail() {
				cancleProgressDialog();
				ToastUtil.showToast(RegisterFingerprintActivity.this,
						"��������ֵʧ�ܣ�");
				Log.i("whw", "validateFingerprint onGenCharFail");
			}
		});

		validateFingerprint.setOnDownCharListener(new OnDownCharListener() {

			@Override
			public void onDownCharSuccess() {
				cancleProgressDialog();
				validateFingerprint.PS_Match();
//				showProgressDialog("�밴ָ�ƣ�");
//				validateFingerprint.PS_GetImage();
				Log.i("whw", "validateFingerprint onDownCharSuccess");
			}

			@Override
			public void onDownCharFail() {
				cancleProgressDialog();
				ToastUtil
						.showToast(RegisterFingerprintActivity.this, "����ģ��ʧ�ܣ�");
				Log.i("whw", "validateFingerprint onDownCharFail");
			}
		});

		validateFingerprint.setOnMatchListener(new OnMatchListener() {
			@Override
			public void onMatchSuccess() {
				cancleProgressDialog();
				ToastUtil.showToast(RegisterFingerprintActivity.this, "��֤ͨ����");
				Log.i("whw", "validateFingerprint onMatchSuccess");
			}

			@Override
			public void onMatchFail() {
				cancleProgressDialog();
				ToastUtil.showToast(RegisterFingerprintActivity.this, "��֤ʧ�ܣ�");
				Log.i("whw", "validateFingerprint onMatchFail");

			}
		});

		// //////////////////////////////////////////////
		registerFingerprint.setOnStoreCharListener(new OnStoreCharListener() {

			@Override
			public void onStoreCharSuccess() {
				ToastUtil
						.showToast(RegisterFingerprintActivity.this, "����ģ��ɹ���");
			}

			@Override
			public void onStoreCharFail() {
				ToastUtil
						.showToast(RegisterFingerprintActivity.this, "����ģ��ʧ�ܣ�");
			}
		});

		registerFingerprint.setOnLoadCharListener(new OnLoadCharListener() {

			@Override
			public void onLoadCharSuccess() {
				ToastUtil
						.showToast(RegisterFingerprintActivity.this, "����ģ��ɹ���");

			}

			@Override
			public void onLoadCharFail() {
				ToastUtil
						.showToast(RegisterFingerprintActivity.this, "����ģ��ʧ�ܣ�");

			}
		});

		registerFingerprint.setOnSearchListener(new OnSearchListener() {

			@Override
			public void onSearchSuccess(int pageId, int matchScore) {
				ToastUtil.showToast(RegisterFingerprintActivity.this, "�����ɹ���");
				Log.i("whw", "pageId=" + pageId + "    matchScore="
						+ matchScore);
			}

			@Override
			public void onSearchFail() {
				ToastUtil.showToast(RegisterFingerprintActivity.this, "����ʧ�ܣ�");
			}
		});

		registerFingerprint.setOnDeleteCharListener(new OnDeleteCharListener() {

			@Override
			public void onDeleteCharSuccess() {
				Log.i("whw", "ɾ���ɹ���");
			}

			@Override
			public void onDeleteCharFail() {
				Log.i("whw", "ɾ��ʧ�ܣ�");
			}
		});

		registerFingerprint.setOnEmptyListener(new OnEmptyListener() {

			@Override
			public void onEmptySuccess() {
				Log.i("whw", "��ճɹ���");

			}

			@Override
			public void onEmptyFail() {
				Log.i("whw", "���ʧ�ܣ�");

			}
		});

		registerFingerprint.setOnEnrollListener(new OnEnrollListener() {

			@Override
			public void onEnrollSuccess(int pageId) {
				Log.i("whw", "�Զ�ע��ɹ���pageid=" + pageId);
			}

			@Override
			public void onEnrollFail() {
				Log.i("whw", "�Զ�ע��ʧ�ܣ�");
			}
		});

		registerFingerprint.setOnIdentifyListener(new OnIdentifyListener() {

			@Override
			public void onIdentifySuccess(int pageId, int matchScore) {
				Log.i("whw", "�Զ���ָ֤�Ƴɹ���pageid=" + pageId + "  matchScore="
						+ matchScore);
			}

			@Override
			public void onIdentifyFail() {
				Log.i("whw", "�Զ���ָ֤��ʧ��");
			}
		});
	}

	private void initViewListener() {
		start.setOnClickListener(this);
		validate.setOnClickListener(this);
		back.setOnClickListener(this);
	}

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
		switch (v.getId()) {
		case R.id.start:
			count = 1;
			showProgressDialog("�밴ָ�ƣ�");
			registerFingerprint.PS_GetImage();
			model = null;
			Log.i("whw", "send end");
			break;
		case R.id.validateZhiwen:
			if (model != null) {
				showProgressDialog("�밴ָ�ƣ�");
				validateFingerprint.PS_GetImage();
			} else {
				ToastUtil
						.showToast(RegisterFingerprintActivity.this, "����ע��ָ�ƣ�");
			}
			break;
		case R.id.backRegister:
			finish();
			break;
		default:
			break;
		}
	}
	
	private void saveImage(byte[] data){
		File filePath = new File(path);
		if(!filePath.exists()){
			filePath.mkdir();
		}
		
		File file = new File(path+File.separator+System.currentTimeMillis()+".bmp");
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(fos != null){
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		
	}


	private void showProgressDialog(String message) {
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(message);
		progressDialog.setCanceledOnTouchOutside(false);
		if (!progressDialog.isShowing()) {
			progressDialog.show();
		}
	}

	private void showProgressDialog(int resId) {
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(getResources().getString(resId));
		progressDialog.show();
	}

	private void cancleProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.cancel();
			progressDialog = null;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode) {
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onStop() {
		cancleProgressDialog();
		super.onStop();
	}

}
