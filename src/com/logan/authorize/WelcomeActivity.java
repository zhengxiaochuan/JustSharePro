package com.logan.authorize;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;

import com.logan.R;
import com.logan.weibo.bean.BaseActivity;


public class WelcomeActivity extends BaseActivity {
	public static final int SETTING_WIFI = 0;
	long startTime = System.currentTimeMillis();
	long endTime;
	private Boolean isDBAvaliable = false;
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				// NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
				// boolean available = networkInfo.isAvailable();
				State state = connManager.getNetworkInfo(
						ConnectivityManager.TYPE_WIFI).getState();//TYPE_WIFI
				if (state != State.CONNECTED) {
					new AlertDialog.Builder(WelcomeActivity.this)
							.setTitle("无连接")
							.setMessage("请使用WIFI连接")
							.setPositiveButton("设置",
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(DialogInterface dialog,int which) {
											startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS), SETTING_WIFI);
											 finish();
										}
									})
							.setNegativeButton("取消",
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(DialogInterface dialog, int which) {
											 finish();
										}

									}).create().show();

					return;
				}

				if (isDBAvaliable) {
					Intent intent = new Intent();
					intent.setClass(WelcomeActivity.this, AccountActivity.class);
					startActivity(intent);
					finish();
				} else {
					Intent intent = new Intent();
					intent.setClass(WelcomeActivity.this, AuthorizeActivity.class);
					startActivity(intent);
					finish();

				}

				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SETTING_WIFI) {  
//            if (resultCode == UPDATE_ADAPTER_OK) {  
//                //do something  
//            }  
        }  
		
		super.onActivityResult(requestCode, resultCode, data);
	}


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(getLayout());
		if (mDBManager.getAccounts().size() != 0)
			isDBAvaliable = true;

		new Thread(new Runnable() {
			@Override
			public void run() {
				endTime = startTime;
				while (endTime - startTime < 2000) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					endTime = System.currentTimeMillis();
				}
				handler.sendEmptyMessage(0);
			}
		}).start();

	}


	@Override
	public int getLayout() {
		return R.layout.welcome_activity;
	}

}