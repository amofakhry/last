package android_serialport_api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.authentication.utils.DataUtils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android_serialport_api.SerialPortManager;

public class M1CardAPI {
	public static final int KEY_A = 1;
	public static final int KEY_B = 2;

	private static final byte[] SWITCH_COMMAND = "D&C00040104".getBytes();
	// 发送数据包的前缀
	private static final String DATA_PREFIX = "c050605";
	private static final String FIND_CARD_ORDER = "01";// 寻卡指令
	private static final String PASSWORD_SEND_ORDER = "02";// 密码下发指令
	private static final String PASSWORD_VALIDATE_ORDER = "03";// 密码认证命令
	private static final String READ_DATA_ORDER = "04";// 读指令
	private static final String WRITE_DATA_ORDER = "05";// 写指令
	private static final String ENTER = "\r\n";// 换行符

	private static final String TURN_OFF = "c050602\r\n";// 关闭天线厂

	// 寻卡的指令包
	private static final String FIND_CARD = DATA_PREFIX + FIND_CARD_ORDER
			+ ENTER;

	// 下发密码指令包(A，B段密码各12个’f‘)
	private static final String SEND_PASSWORD = DATA_PREFIX
			+ PASSWORD_SEND_ORDER + "ffffffffffffffffffffffff" + ENTER;

	// //密码认证指令包
	// private static final static PASSWORD_VALIDATE =

	// private static final String FIND_SUCCESS = "c05060501" + ENTER + "0x00,";
	private static final String FIND_SUCCESS = "0x00,";

	private static final String WRITE_SUCCESS = " Write Success!" + ENTER;

	private byte[] buffer = new byte[100];

	public static final int WRITE = 1;
	public static final int READ_INFO = 3;

	public int inputLength = 0;
	public int switchLength = 0;

	public M1CardAPI() {
	}

	/**
	 * 读取M1卡卡号
	 * 
	 * @return
	 */
	public Result readCard() {
		Log.i("whw", "!!!!!!!!!!!!readCard");
		Result result = new Result();
		byte[] command = FIND_CARD.getBytes();
		int length = receive(command, buffer);
		if (length == 0) {
			result.confirmationCode = Result.TIME_OUT;
			return result;
		}
		inputLength = length;
		String msg = "";
		msg = new String(buffer, 0, length);
		Log.i("whw", "msg hex=" + msg);
		turnOff();
		if (msg.startsWith(FIND_SUCCESS)) {
			result.confirmationCode = Result.SUCCESS;
			result.resultInfo = msg.substring(FIND_SUCCESS.length());
		} else {
			result.confirmationCode = Result.FIND_FAIL;
		}
		return result;
	}

	/**
	 * 切换成读取RFID
	 * 
	 * @return
	 */
	private boolean switchStatus() {
		sendCommand(SWITCH_COMMAND);
		Log.i("whw", "SWITCH_COMMAND hex=" + new String(SWITCH_COMMAND));
		SystemClock.sleep(500);
		SerialPortManager.switchRFID = true;
		return true;
	}

	private int receive(byte[] command, byte[] buffer) {
		int length = -1;
		if (!SerialPortManager.switchRFID) {
			switchStatus();
		}
		sendCommand(command);

		length = SerialPortManager.getInstance().read(buffer, 3000, 50);
		return length;
	}

	private void sendCommand(byte[] command) {
		SerialPortManager.getInstance().write(command);
	}

	/**
	 * 寻卡
	 * 
	 * @return
	 */
	private boolean findCard() {
		Result resultInfo = readCard();
		if (resultInfo.confirmationCode == Result.SUCCESS) {
			return true;
		}
		return false;
	}

	private String read(int position) {
		byte[] command = { 'c', '0', '5', '0', '6', '0', '5', '0', '4', '0',
				'0', '\r', '\n' };
		char[] c = getZoneId(position).toCharArray();
		command[9] = (byte) c[0];
		command[10] = (byte) c[1];
		int length = receive(command, buffer);
		String data = new String(buffer, 0, length);
		Log.i("whw", "read data=" + data);
		String[] split = data.split(";");
		String msg = "";
		if (split.length == 2) {
			int index = split[1].indexOf("\r\n");
			if (index != -1) {
				msg = split[1].substring(0, index);
			}

			Log.i("whw", "split msg=" + msg + "  msg length=" + msg.length());
		}
		return msg;
	}

	/**
	 * 
	 * @return
	 */
	private boolean validatePassword(int position) {
		// 下发认证命令
		byte[] command1 = SEND_PASSWORD.getBytes();
		int tempLength = receive(command1, buffer);
		String verifyStr = new String(buffer, 0, tempLength);
		Log.i("whw", "validatePassword verifyStr=" + verifyStr);
		// 验证密码
		byte[] command2 = (DATA_PREFIX + PASSWORD_VALIDATE_ORDER + "60"
				+ getZoneId(position) + ENTER).getBytes();

		int length = receive(command2, buffer);
		String msg = new String(buffer, 0, length);
		Log.i("whw", "validatePassword msg=" + msg);
		String prefix = "0x00,\r\n";
		if (msg.startsWith(prefix)) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @return
	 */
	private boolean validatePassword(int position, int keyType, byte[] password) {
		Log.i("whw", "!!!!!!!!!!!!!!keyType=" + keyType);
		byte[] command1 = null;
		if (password == null) {
			// 下发认证命令
			command1 = SEND_PASSWORD.getBytes();
		} else {
			String passwordHexStr = DataUtils.toHexString(password);
			String completePassword = getCompletePassword(keyType,
					passwordHexStr);
			command1 = (DATA_PREFIX + PASSWORD_SEND_ORDER + completePassword + ENTER)
					.getBytes();
		}

		int tempLength = receive(command1, buffer);
		String verifyStr = new String(buffer, 0, tempLength);
		Log.i("whw", "validatePassword verifyStr=" + verifyStr);
		// 验证密码
		byte[] command2 = (DATA_PREFIX + PASSWORD_VALIDATE_ORDER
				+ getKeyTypeStr(keyType) + getZoneId(position) + ENTER)
				.getBytes();

		int length = receive(command2, buffer);
		String msg = new String(buffer, 0, length);
		Log.i("whw", "validatePassword msg=" + msg);
		String prefix = "0x00,\r\n";
		if (msg.startsWith(prefix)) {
			return true;
		}
		return false;
	}

	private static final String DEFAULT_PASSWORD = "ffffffffffff";

	private String getCompletePassword(int keyType, String passwordHexStr) {
		// A.B端密码长度各为6字节，换算成16进制字符串，密码长度就为12个字符长度
		StringBuffer passwordBuffer = new StringBuffer();
		passwordBuffer.append(passwordHexStr);
		if (passwordHexStr != null && passwordHexStr.length() < 12) {
			int length = 12 - passwordHexStr.length();
			for (int i = 0; i < length; i++) {
				passwordBuffer.append('0');
			}
		}
		passwordHexStr = passwordBuffer.toString();
		String completePasswordHexStr = "";
		switch (keyType) {
		case KEY_A:
			completePasswordHexStr = passwordHexStr + DEFAULT_PASSWORD;
			break;
		case KEY_B:
			completePasswordHexStr = DEFAULT_PASSWORD + passwordHexStr;
			break;

		default:
			break;
		}
		return completePasswordHexStr;
	}

	private String getKeyTypeStr(int keyType) {
		String keyTypeStr = null;
		switch (keyType) {
		case KEY_A:
			keyTypeStr = "60";
			break;
		case KEY_B:
			keyTypeStr = "61";
			break;
		default:
			keyTypeStr = "60";
			break;
		}
		return keyTypeStr;
	}

	// 转换扇区里块的地址为两位
	private String getZoneId(int position) {
		return DataUtils.byte2Hexstr((byte) position);
	}

	// 关闭天线厂
	private String turnOff() {
		// byte[] command = TURN_OFF.getBytes();
		// int length = receive(command, buffer);
		// String str = "";
		// if (length > 0) {
		// str = new String(buffer, 0, length);
		// }
		// return str;
		return "";
	}

	private int write(String hexStr, int position) {
		byte[] command = (DATA_PREFIX + WRITE_DATA_ORDER + getZoneId(position)
				+ hexStr + ENTER).getBytes();
		Log.i("whw", "***write hexStr=" + hexStr);

		int length = receive(command, buffer);
		return length;
	}

	/********************************************************************/

	/**
	 * 把需要写入的数据编码成字节数组
	 * 
	 * @param data
	 */
	public Result write(byte[][] data) {
		Result result = new Result();
		// 字段数
		short fieldNum = (short) data.length;
		// 获取每个字段里数据的大小
		short[] fieldSize = new short[fieldNum];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// 每个字段占2个字节，把各个字段大小拼成一个字节数组
		byte[] fieldSizeData = null;
		try {
			for (int i = 0; i < fieldNum; i++) {
				fieldSize[i] = (short) data[i].length;
				baos.write(DataUtils.short2byte(fieldSize[i]));
			}
			baos.flush();
			fieldSizeData = baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				baos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// 把各个字段数据拼成一个字节数组
		byte[] fieldData = null;
		baos = new ByteArrayOutputStream();
		try {
			for (int i = 0; i < fieldNum; i++) {
				baos.write(data[i]);
			}
			baos.flush();
			fieldData = baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				baos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// 一个块可以写入16个字节
		int fieldSizeDataSector = fieldSizeData.length % 16 == 0 ? fieldSizeData.length / 16
				: fieldSizeData.length / 16 + 1;
		int fieldDataSector = fieldData.length % 16 == 0 ? fieldData.length / 16
				: fieldData.length / 16 + 1;
		// 所有数据写入需要多少个块，字段数单独占一个块
		int piece = 1 + fieldSizeDataSector + fieldDataSector;
		byte[][] byteData = new byte[piece][];
		// 总字段数从short转为byte[]类型
		byte[] fieldNumByte = DataUtils.short2byte(fieldNum);
		// 这个flag跟在fieldNum后面作为正确的写入方式
		byte[] flag = new byte[] { 0x01, 0x02, 0x03, 0x04 };
		byteData[0] = new byte[fieldNumByte.length + flag.length];
		System.arraycopy(fieldNumByte, 0, byteData[0], 0, fieldNumByte.length);
		System.arraycopy(flag, 0, byteData[0], fieldNumByte.length, flag.length);

		// 各个字段大小拼起来的字节数组按每16个字节分为一组,即写入一个块最多只能写16字节
		for (int i = 0; i < fieldSizeDataSector; i++) {
			if (fieldSizeDataSector - 1 == i) {
				byteData[1 + i] = new byte[fieldSizeData.length - i * 16];
			} else {
				byteData[1 + i] = new byte[16];
			}
			System.arraycopy(fieldSizeData, i * 16, byteData[1 + i], 0,
					byteData[1 + i].length);
		}

		// 各个字段数据拼起来的字节数组按每16个字节分为一组
		for (int i = 0; i < fieldDataSector; i++) {
			if (fieldDataSector - 1 == i) {
				byteData[1 + fieldSizeDataSector + i] = new byte[fieldData.length
						- i * 16];
			} else {
				byteData[1 + fieldSizeDataSector + i] = new byte[16];
			}
			System.arraycopy(fieldData, i * 16, byteData[1
					+ fieldSizeDataSector + i], 0, byteData[1
					+ fieldSizeDataSector + i].length);
		}
		int position = 1;
		int tempPiece = piece;
		while (--tempPiece >= 0) {
			if (position == 1 || position % 4 == 3) {
				// 每个扇区共四个块，第四个块不可以写如数据，故需要跳过此块
				if (position % 4 == 3) {
					++position;
				}
				int time = 0;
				while (!findCard()) {
					time++;
					if (time == 3) {
						result.confirmationCode = Result.FIND_FAIL;
						return result;
					}
					SystemClock.sleep(100);
					Log.i("whw", "findCard fail");
				}
				Log.i("whw", "findCard success");
				time = 0;
				while (!validatePassword(position)) {
					time++;
					if (time == 3) {
						result.confirmationCode = Result.VALIDATE_FAIL;
						return result;
					}
					SystemClock.sleep(100);
					Log.i("whw", "validatePassword fail");
				}
				Log.i("whw", "validatePassword success");
			}
			String hexStr = DataUtils.toHexString(byteData[piece - tempPiece
					- 1]);
			int receiveLength = write(hexStr, position);
			String writeResult = new String(buffer, 0, receiveLength);
			if (WRITE_SUCCESS.equals(writeResult)) {
				result.confirmationCode = Result.SUCCESS;
			} else {
				result.confirmationCode = Result.WRITE_FAIL;
			}
			if (position % 4 == 2) {
				turnOff();
			}
			position++;
		}
		turnOff();
		return result;

	}

	/**
	 * 把卡片里的数据读出来，以字节数组的方式返回
	 * 
	 * @return
	 */
	public Result read() {
		Result result = new Result();
		int position = 1;
		int piece = 1;
		short fieldNum = 0;
		int fieldSizeDataSector = 0;
		int sizeData = 0;
		short[] fieldSize = null;
		// 各个字段的实际数据占用了多少个块
		int fieldDataSector;
		// 所有字段数据大小共占用多少空间
		int sumSize = 0;
		// data存放各个字段的数据
		byte[][] data;
		StringBuffer strBuffer1 = new StringBuffer();
		StringBuffer strBuffer2 = new StringBuffer();
		for (int i = 0; i < piece; i++) {
			if (i == 0 || position % 4 == 3) {
				if (position % 4 == 3) {
					++position;
				}
				int time = 0;
				while (!findCard()) {
					time++;
					if (time == 3) {
						result.confirmationCode = Result.FIND_FAIL;
						return result;
					}
					SystemClock.sleep(100);
					Log.i("whw", "findCard fail");
				}
				Log.i("whw", "findCard success");
				time = 0;
				while (!validatePassword(position)) {
					time++;
					if (time == 3) {
						result.confirmationCode = Result.VALIDATE_FAIL;
						return result;
					}
					SystemClock.sleep(100);
					Log.i("whw", "validatePassword fail");
				}
				Log.i("whw", "validatePassword success");
			}
			String hexStr = read(position);
			if (i == 0) {
				// 获取总字段数,总字段数占两个字节，后面跟着校验位：0x01,0x02,0x03,0x04,如果没有说明此卡没有用户想要的数据
				byte[] tempData = DataUtils.hexStringTobyte(hexStr);
				if (tempData.length > 6 && tempData[2] == 0x01
						&& tempData[3] == 0x02 && tempData[4] == 0x03
						&& tempData[5] == 0x04) {
					byte[] temp = new byte[2];
					System.arraycopy(tempData, 0, temp, 0, temp.length);
					fieldNum = DataUtils.getShort(temp[0], temp[1]);
					if (fieldNum <= 0) {
						turnOff();
						Log.i("whw", "turnOff()  111111111");
						result.confirmationCode = Result.OTHER_EXCEPTION;
						return result;
					} else {
						// 每个字段占2个字节，共占用fieldNum*2个字节
						sizeData = fieldNum * 2;
						fieldSizeDataSector = sizeData % 16 == 0 ? sizeData / 16
								: sizeData / 16 + 1;
						piece += fieldSizeDataSector;
						Log.i("whw", "!!!!!!!!!!!!!piece=" + piece);
					}
				} else {
					turnOff();
					Log.i("whw", "turnOff()  222222222");
					result.confirmationCode = Result.OTHER_EXCEPTION;
					return result;
				}
			}

			// 获取各个字段所占用的空间
			if (i >= 1 && i < fieldSizeDataSector + 1) {
				strBuffer1.append(hexStr);
				if (i == fieldSizeDataSector) {
					byte[] temp = DataUtils.hexStringTobyte(strBuffer1
							.toString());
					if (sizeData <= temp.length) {
						fieldSize = new short[fieldNum];
						for (int j = 0; j < fieldNum; j++) {
							byte[] byteTemp = new byte[2];
							System.arraycopy(temp, j * 2, byteTemp, 0, 2);
							fieldSize[j] = DataUtils.getShort(byteTemp[0],
									byteTemp[1]);
						}
						for (int k = 0; k < fieldSize.length; k++) {
							sumSize += fieldSize[k];
						}
						if (sumSize <= 0) {
							turnOff();
							Log.i("whw", "turnOff()  33333333333333");
							result.confirmationCode = Result.OTHER_EXCEPTION;
							return result;
						} else {
							fieldDataSector = sumSize % 16 == 0 ? sumSize / 16
									: sumSize / 16 + 1;
							piece += fieldDataSector;
							Log.i("whw", "@@@@@@@@@@@@@piece=" + piece
									+ "  fieldDataSector=" + fieldDataSector);
						}
					} else {
						turnOff();
						Log.i("whw", "turnOff()  44444444444");
						result.confirmationCode = Result.OTHER_EXCEPTION;
						return result;
					}
				}
			}

			// 获取各个字段所有的数据
			if (i >= fieldSizeDataSector + 1 && i < piece) {
				strBuffer2.append(hexStr);
				Log.i("whw", "append hexStr=" + hexStr.toString());
				if (i == piece - 1) {
					byte[] temp = DataUtils.hexStringTobyte(strBuffer2
							.toString());
					Log.i("whw", "strBuffer2 hexStr=" + strBuffer2.toString());
					if (temp.length >= sumSize) {
						data = new byte[fieldNum][];
						if (fieldSize != null) {
							int startPosition = 0;
							for (int j = 0; j < fieldSize.length; j++) {
								data[j] = new byte[fieldSize[j]];
								System.arraycopy(temp, startPosition, data[j],
										0, data[j].length);
								startPosition += fieldSize[j];
							}
							turnOff();
							Log.i("whw", "turnOff()  55555555555555555");
							result.confirmationCode = Result.SUCCESS;
							result.resultInfo = data;
							return result;
						}

					} else {
						turnOff();
						Log.i("whw", "turnOff()  66666666666666");
						result.confirmationCode = Result.OTHER_EXCEPTION;
						return result;
					}
				}
			}
			if (position % 4 == 2) {
				// 每个扇区的块3不可以读写数据
				// turnOff();
				Log.i("whw", "turnOff()  777777777777777");
			}
			position++;
		}
		turnOff();
		return result;
	}

	/**
	 * 在指定块号写入数据
	 * 
	 * @param position
	 *            写入数据的块号
	 * @param keyType
	 *            密码类型：密码A或密码B
	 * @param password
	 *            密码为空时，会使用默认的密码即24个“f”
	 * @param data
	 *            写入的数据不能为空，data的长度必须小于等于16字节，因一个块只能存放16字节的数据,建议不足16字节 用0补齐
	 * @return  写成功，Result.resultInfo存有M1卡号
	 */
	public Result writeAtPosition(int position, int keyType, byte[] password,
			byte[] data) {
		Result result = new Result();
		int time = 0;
//		while (!findCard()) {
//			time++;
//			if (time == 3) {
//				result.confirmationCode = Result.FIND_FAIL;
//				return result;
//			}
//			SystemClock.sleep(100);
//			Log.i("whw", "findCard fail");
//		}
		Result findCardResultInfo = readCard();
		if(findCardResultInfo.confirmationCode == Result.FIND_FAIL){
			result.confirmationCode = Result.FIND_FAIL;
			return result;
		}

		time = 0;
		while (!validatePassword(position, keyType, password)) {
			time++;
			if (time == 3) {
				result.confirmationCode = Result.VALIDATE_FAIL;
				return result;
			}
			SystemClock.sleep(100);
			Log.i("whw", "validatePassword fail");
		}

		String hexStr = DataUtils.toHexString(data);
		int receiveLength = write(hexStr, position);
		String writeResult = new String(buffer, 0, receiveLength);
		Log.i("whw", "write result=" + writeResult);
		turnOff();
		if (WRITE_SUCCESS.equals(writeResult)) {
			result.confirmationCode = Result.SUCCESS;
			result.resultInfo = (String)findCardResultInfo.resultInfo;
		} else {
			result.confirmationCode = Result.WRITE_FAIL;
		}
		return result;
	}

	/**
	 * 在指定块写入数据，每个块最多只能写入16字节数据，不足可以补0.
	 * 
	 * @param position
	 *            块号
	 * @param keyType
	 *            密码类型：密码A或密码B
	 * @param password
	 *            密码为空时，会使用默认的密码即24个“f”
	 * @return 读成功，Result.resultInfo为 二维数组  array[0]：卡号，array[1]：读取的信息
	 */
	public Result readAtPosition(int position, int keyType, byte[] password) {
		Result result = new Result();
		int time = 0;
//		while (!findCard()) {
//			time++;
//			if (time == 3) {
//				result.confirmationCode = Result.FIND_FAIL;
//				return result;
//			}
//			SystemClock.sleep(100);
//			Log.i("whw", "findCard fail");
//		}
		
		Result findCardResultInfo = readCard();
		if(findCardResultInfo.confirmationCode == Result.FIND_FAIL){
			result.confirmationCode = Result.FIND_FAIL;
			return result;
		}

		time = 0;
		while (!validatePassword(position, keyType, password)) {
			time++;
			if (time == 3) {
				result.confirmationCode = Result.VALIDATE_FAIL;
				return result;
			}
			SystemClock.sleep(100);
			Log.i("whw", "validatePassword fail");
		}

		String hexStr = read(position);
		byte[] data = DataUtils.hexStringTobyte(hexStr);
		turnOff();
		result.confirmationCode = Result.SUCCESS;
		byte[][] obj = new byte[2][];
		obj[0] = ((String)findCardResultInfo.resultInfo).getBytes();
		obj[1] = data;
		result.resultInfo = obj;
		return result;
	}

	public class Result {
		/**
		 * 成功
		 */
		public static final int SUCCESS = 1;
		/**
		 * 寻卡失败
		 */
		public static final int FIND_FAIL = 2;
		/**
		 * 验证失败
		 */
		public static final int VALIDATE_FAIL = 3;
		/**
		 * 写卡失败
		 */
		public static final int WRITE_FAIL = 4;
		/**
		 * 超时
		 */
		public static final int TIME_OUT = 5;
		/**
		 * 其它异常
		 */
		public static final int OTHER_EXCEPTION = 6;

		/**
		 * 确认码 1: 成功 2：寻卡失败 3：验证失败 4:写卡失败 5：超时 6：其它异常
		 */
		public int confirmationCode;

		/**
		 * 结果集:当确认码为1时，再判断是否有结果
		 */
		public Object resultInfo;
	}

}
