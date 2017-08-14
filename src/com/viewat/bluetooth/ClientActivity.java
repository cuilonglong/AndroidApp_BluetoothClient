package com.viewat.bluetooth;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.viewat.utils.BluetoothClientUtils;
import com.viewat.utils.BluetoothClientUtils.OnBluetoothClientReceiveListener;

public class ClientActivity extends Activity{

	EditText editText;
	TextView tv_message;
	
	BluetoothClientUtils bluetoothClientUtils;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client);
		
		editText = (EditText) findViewById(R.id.editText);
		tv_message = (TextView) findViewById(R.id.tv_message);
	}
	
	public void onClick_open(View view)
	{
		bluetoothClientUtils = new BluetoothClientUtils(this,MainActivity.BlueToothAddress);
		bluetoothClientUtils.setOnBluetoothClientReceiveListener(new OnBluetoothClientReceiveListener() {
			
			@Override
			public void onReceive(byte[] buf) {
				
			}
			
			@Override
			public void onReceive(String info) {
				showText("接收:"+info);
			}
		});
		
		bluetoothClientUtils.open();
	}
	
	public void onClick_send(View view)
	{
		String info = editText.getText().toString().trim();
		bluetoothClientUtils.send(info);
		showText("发送:"+info);
	}
	
	public void onClick_close(View view)
	{
		if(bluetoothClientUtils == null)
			return;
		bluetoothClientUtils.close();
	}

	void showText(String msg)
	{
		tv_message.append(msg+"\n");
	}
	
}
