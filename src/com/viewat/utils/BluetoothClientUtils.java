package com.viewat.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class BluetoothClientUtils {

	private Context context;
	private String address;
	
	private OnBluetoothClientReceiveListener onBluetoothClientReceiveListener;
	
	// 蓝牙客户端socket
    private BluetoothSocket mSocket;
    // 设备
    private BluetoothDevice mDevice;
    private BluetoothAdapter mBluetoothAdapter;
    
    // --线程类-----------------
    private ClientThread mClientThread;
    private ReadThread mReadThread;
    
    boolean isOpen = false;//表示是否已经启动了线程监听
    
    private static final int STATUS_CONNECT = 0x11;
    public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
    public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
    public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";
	
	public BluetoothClientUtils(Context context,String address)
	{
		this.context = context;
		this.address = address;
	}
	
	public void open()
	{
		if (isOpen) {
            Toast.makeText(context, "连接已经打开，可以通信。如果要再建立连接，请先断开", Toast.LENGTH_SHORT).show();
            return;
        }
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
        if (!"".equals(address) && !isOpen) {
            mDevice = mBluetoothAdapter.getRemoteDevice(address);
            mClientThread = new ClientThread();
            mClientThread.start();
        }
		
	}
	
	
	// 发送数据
    public void send(String msg) {
    	
        if (mSocket == null) {
            Toast.makeText(context, "没有连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            OutputStream os = mSocket.getOutputStream();
            os.write(msg.getBytes());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    // 发送数据
    public void send(byte [] buf) {
    	
        if (mSocket == null) {
            Toast.makeText(context, "没有连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            OutputStream os = mSocket.getOutputStream();
            os.write(buf);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
	
	public void close()
	{
		shutdownClient();
		isOpen = false;
	}
	
	// 客户端线程
    private class ClientThread extends Thread {
        public void run() {
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                Message msg = new Message();
                msg.obj = "请稍候，正在连接服务器:" + address;
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
                isOpen = true;
            } catch (Exception e) {
                Message msg = new Message();
                msg.obj = "连接服务端异常！请断开连接重新试一试。";
                msg.what = STATUS_CONNECT;
                mHandler.sendMessage(msg);
            }
        }
    };
    
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
                        Bundle bundle = msg.getData();
                        bundle.putString("info", s);
                        bundle.putByteArray("buf", buf_data);
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
        	
        	String info = msg.getData().getString("info","");
        	byte [] buf = msg.getData().getByteArray("buf");
            
            if(onBluetoothClientReceiveListener != null)
            {
            	onBluetoothClientReceiveListener.onReceive(buf);
            	onBluetoothClientReceiveListener.onReceive(info);
            }
            
            String m = (String)msg.obj;
            if(m != null && !m.equals("")){
            	Toast.makeText(context, "提示:"+m, Toast.LENGTH_SHORT).show();
            }
            
        }

    };
    
    void showText(String msg)
	{
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}
	
	public void setOnBluetoothClientReceiveListener(OnBluetoothClientReceiveListener onBluetoothClientReceiveListener)
	{
		this.onBluetoothClientReceiveListener = onBluetoothClientReceiveListener;
	}
	
	public interface OnBluetoothClientReceiveListener{
		
		//接收到数据后的回调
		public void onReceive(String info);
		
		//接收到数据后的回调
		public void onReceive(byte [] buf);
		
	}
	
}
