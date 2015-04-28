package ru.spb.mobisoft.kry127.ttt;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;

import ru.spb.mobisoft.kry127.ttt.R;

public class BgSoundService extends Service {

	MediaPlayer p;
	
	public BgSoundService() {};
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		
		p = MediaPlayer.create(this, R.raw.rm);
		p.setLooping(true);
		p.setAudioStreamType(AudioManager.STREAM_MUSIC);
		p.start();
		
		return 1;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		p.stop();
		p.release();
	}

}
