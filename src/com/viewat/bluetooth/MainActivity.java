package com.viewat.bluetooth;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.viewat.entity.DeviceBean;

public class MainActivity extends Activity {

	ListView mListView;
    //数据
    private ArrayList<DeviceBean> mDatas;
    private ChatListAdapter mAdapter;
	//蓝牙适配器
    private BluetoothAdapter mBtAdapter;
    
    public static String BlueToothAddress = "1nas812asd812e";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initDatas();
		initView();
		init();
		
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        
        Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivity(discoveryIntent);
	}
	
	private void initDatas() {
        mDatas = new ArrayList<DeviceBean>();
        mAdapter = new ChatListAdapter(this, mDatas);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }
	
	public void initView()
	{
		mListView = (ListView) findViewById(R.id.listView);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(mDeviceClickListener);
	}
	
	public void onClick_search(View view)
	{
		if(mBtAdapter == null)
			return;
		
		//开始搜索设备
		mBtAdapter.startDiscovery();
		
	}
	
	/**
     * 列出所有的蓝牙设备
     */
    private void init() {
        Log.i("tag", "mBtAdapter=="+ mBtAdapter);
        //根据适配器得到所有的设备信息
        Set<BluetoothDevice> deviceSet = mBtAdapter.getBondedDevices();
        if (deviceSet.size() > 0) {
            for (BluetoothDevice device : deviceSet) {
                mDatas.add(new DeviceBean(device.getName() + "\n" + device.getAddress(), true));
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(mDatas.size() - 1);
            }
        } else {
            mDatas.add(new DeviceBean("没有配对的设备", true));
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(mDatas.size() - 1);
        }
    }
	
	
    
    class ChatListAdapter extends BaseAdapter{

    	private Context context;
    	private ArrayList<DeviceBean> mDatas;
    	
    	public ChatListAdapter(Context context,ArrayList<DeviceBean> mDatas)
    	{
    		this.context = context;
    		this.mDatas = mDatas;
    	}
    	
		@Override
		public int getCount() {
			return mDatas.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			TextView tv = new TextView(context);
			tv.setText(mDatas.get(position).getMessage()+"");
			return tv;
		}
    	
    }
    
    /**
     * 点击设备监听
     */
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            DeviceBean bean = mDatas.get(position);
            String info = bean.message;
            String address = info.substring(info.length() - 17);
            BlueToothAddress = address;

            AlertDialog.Builder stopDialog = new AlertDialog.Builder(MainActivity.this);
            stopDialog.setTitle("连接");//标题
            stopDialog.setMessage(bean.message);
            stopDialog.setPositiveButton("连接", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mBtAdapter.cancelDiscovery();
//                    showTxt("停止扫描,进行连接...");

                    startActivity(new Intent(MainActivity.this,ClientActivity.class));
                    dialog.cancel();
                }
            });
            
            stopDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    BlueToothAddress = null;
                    dialog.cancel();
                }
            });
            stopDialog.show();
        }
    };

    /**
     * 发现设备广播
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 获得设备信息
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 如果绑定的状态不一样
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mDatas.add(new DeviceBean(device.getName() + "\n" + device.getAddress(), false));
                    mAdapter.notifyDataSetChanged();
                    mListView.setSelection(mDatas.size() - 1);
                }
                
            // 如果搜索完成了
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                if (mListView.getCount() == 0) {
                    mDatas.add(new DeviceBean("没有发现蓝牙设备", false));
                    mAdapter.notifyDataSetChanged();
                    mListView.setSelection(mDatas.size() - 1);
                }
                showTxt("请重新搜索");
            }
        }
    };
    
    
   
	@Override
    public void onStart() {
        super.onStart();
        
        //判断是否支持蓝牙
  		if(mBtAdapter == null) 
  		{
  			showTxt("设备不支持蓝牙!");
  			return;
  		}
        
        //请求打开蓝牙
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 3);
        }
        
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
        
        mDatas.clear();
    }
	
	void showTxt(String msg)
	{
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

}
