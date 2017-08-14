package com.viewat.bluetooth;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends Activity {

	private TextView tv_message;
	private EditText editText;
	
	// 蓝牙服务端socket
    private BluetoothServerSocket mServerSocket;
    // 蓝牙客户端socket
    private BluetoothSocket mSocket;
    // 设备
    private BluetoothDevice mDevice;
    private BluetoothAdapter mBluetoothAdapter;
	
    // --线程类-----------------
    private ServerThread mServerThread;
    private ClientThread mClientThread;
    private ReadThread mReadThread;
    
    boolean isOpen = false;//表示是否建立了链接
    int mType = 0;//表示当前是客户端还是服务端
    
    public static final int NONE = 0;
    public static final int CLIENT = 10;
    public static final int SERVER = 100;
    
    
    private static final int STATUS_CONNECT = 0x11;
    public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
    public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
    public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		mType = CLIENT;
		
		initDatas();
		initView();
	}
	
	private void initDatas() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
	
	void initView()
	{
		tv_message = (TextView) findViewById(R.id.tv_message);
		editText = (EditText) findViewById(R.id.editText);
	}
	
	public void onClick_send(View view)
	{
		String text = editText.getText().toString();
		if (!TextUtils.isEmpty(text)) {
            // 发送信息
            sendMessageHandle(text);

            editText.setText("");
            editText.clearFocus();
		}
	}
	
	public void onClick_end(View view)
	{
		if (mType == CLIENT) {
            shutdownClient();
        } else if (mType == SERVER) {
            shutdownServer();
        }
        isOpen = false;
        mType = NONE;
        Toast.makeText(ChatActivity.this, "已断开连接！", Toast.LENGTH_SHORT).show();
	}
	
	@Override
    public void onResume() {
        super.onResume();
        
        if (isOpen) {
            Toast.makeText(this, "连接已经打开，可以通信。如果要再建立连接，请先断开", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (mType == CLIENT) {
            String address = MainActivity.BlueToothAddress;
            if (!"".equals(address)) {
                mDevice = mBluetoothAdapter.getRemoteDevice(address);
                mClientThread = new ClientThread();
                mClientThread.start();
                isOpen = true;
            } else {
                Toast.makeText(this, "address is null !", Toast.LENGTH_SHORT).show();
            }
        } else if (mType == SERVER) {
            mServerThread = new ServerThread();
            mServerThread.start();
            isOpen = true;
        }
    }
	
	 
	// 开启服务器
    private class ServerThread extends Thread {
        public void run() {
            try {
                // 创建一个蓝牙服务器 参数分别：服务器名称、UUID
                mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM,
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                Message msg = new Message();
                msg.obj = "请稍候，正在等待客户端的连接...";
                msg.what = STATUS_CONNECT;
                mHandler.sendMessage(msg);

                /* 接受客户端的连接请求 */
                mSocket = mServerSocket.accept();

                msg = new Message();
                msg.obj = "客户端已经连接上！可以发送信息。";
                msg.what = STATUS_CONNECT;
                mHandler.sendMessage(msg);
                // 启动接受数据
                mReadThread = new ReadThread();
                mReadThread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }; 
	 
	 
	// 客户端线程
    private class ClientThread extends Thread {
        public void run() {
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                Message msg = new Message();
                msg.obj = "请稍候，正在连接服务器:" + MainActivity.BlueToothAddress;
                msg.what = STATUS_CONNECT;
                mHandler.sendMessage(msg);

                mSocket.connect();

                msg = new Message();
                msg.obj = "已经连接上服务端！可以发送信息。";
                msg.what = STATUS_CONNECT;
                mHandler.sendMessage(msg);
                // 启动接受数据
                mReadThread = new ReadThread();
                mReadThread.start();
            } catch (Exception e) {
                Message msg = new Message();
                msg.obj = "连接服务端异常！请断开连接重新试一试。";
                msg.what = STATUS_CONNECT;
                mHandler.sendMessage(msg);
            }
        }
    };
    
    
    // 发送数据
    private void sendMessageHandle(String msg) {
    	
        if (mSocket == null) {
            Toast.makeText(this, "没有连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            OutputStream os = mSocket.getOutputStream();
            os.write(msg.getBytes());
            
            Message message = new Message();
            message.obj = "发送 : "+msg;
            mHandler.sendMessage(message);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    // 读取数据
    private class ReadThread extends Thread {
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream is = null;
            try {
                is = mSocket.getInputStream();
                while (true) {
                    if ((bytes = is.read(buffer)) > 0) {
                        byte[] buf_data = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];
                        }
                        String s = new String(buf_data);
                        Message msg = new Message();
                        msg.obj = "接收 : "+s;
                        msg.what = 1;
                        mHandler.sendMessage(msg);
                    }
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

        }
    }
    
    /* 停止服务器 */
    private void shutdownServer() {
        new Thread() {
            public void run() {
                if (mServerThread != null) {
                    mServerThread.interrupt();
                    mServerThread = null;
                }
                if (mReadThread != null) {
                    mReadThread.interrupt();
                    mReadThread = null;
                }
                try {
                    if (mSocket != null) {
                        mSocket.close();
                        mSocket = null;
                    }
                    if (mServerSocket != null) {
                        mServerSocket.close();
                        mServerSocket = null;
                    }
                } catch (Exception e) {
                    Log.e("server", "mserverSocket.close()", e);
                }
            };
        }.start();
    }

    /* ͣ停止客户端连接 */
    private void shutdownClient() {
        new Thread() {
            public void run() {
                if (mClientThread != null) {
                    mClientThread.interrupt();
                    mClientThread = null;
                }
                if (mReadThread != null) {
                    mReadThread.interrupt();
                    mReadThread = null;
                }
                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mSocket = null;
                }
            };
        }.start();
    }
    
    
    /**
     * 信息处理
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String info = (String) msg.obj;
            Toast.makeText(ChatActivity.this, info, 0).show();
            tv_message.append(info+"\n");

        }

    };
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mType == CLIENT) {
            shutdownClient();
        } else if (mType == SERVER) {
            shutdownServer();
        }
        isOpen = false;
        mType = NONE;
    }
	
	
}
