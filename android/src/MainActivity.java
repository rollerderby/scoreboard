package com.carolinarollergirls.scoreboard.android;

import android.app.*;
import android.content.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;

import java.util.ArrayList;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.R;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		bind();
	}

	public void startServer(View view) {
		android.util.Log.i("CRG ScoreBoard", "startServer");
		sendMessage(ScoreBoardService.CMD_START);
	}

	public void stopServer(View view) {
		android.util.Log.i("CRG ScoreBoard", "stopServer");
		sendMessage(ScoreBoardService.CMD_STOP);
	}

	// RECV MESSAGE
	private BroadcastReceiver mReceiver = null;
	@Override
	protected void onResume() {
		super.onResume();
	
		IntentFilter intentFilter = new IntentFilter(ScoreBoardService.ActivityPath);
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int command = intent.getIntExtra("command", -1);
				switch (command) {
					case ScoreBoardService.CMD_LOG:
						String msg = intent.getStringExtra("msg");
						TextView text = (TextView) findViewById(R.id.log);
						text.setText(text.getText() + "\n" + msg);
						break;
				}
				// String msg_for_me = intent.getStringExtra("some_msg");
			}
		};
		//registering our receiver
		this.registerReceiver(mReceiver, intentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.unregisterReceiver(this.mReceiver);
	}

	// SEND MESSAGE
	Messenger mSender = null;
	boolean isBound;

	private void bind() {
		Intent intent = new Intent(ScoreBoardService.ServicePath);
		bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
	}
	
	private ServiceConnection myConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mSender = new Messenger(service);
			isBound = true;
		}
	
		public void onServiceDisconnected(ComponentName className) {
			mSender = null;
			isBound = false;
		}
	};	

	public void sendMessage(int command) {
		android.util.Log.i("CRG ScoreBoard", "sendMessage: command=" + command + "  isBound: " + (isBound ? "true" : "false"));
		if (!isBound) return;
		
		Message msg = Message.obtain();
		
		Bundle bundle = new Bundle();
		bundle.putInt("command", command);
		
		msg.setData(bundle);
		
		try {
			mSender.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}


}
