package com.example.ssensor;

import java.util.HashMap;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class PromtVoice {
private SoundPool mSoundPool;
	
	private HashMap<Integer, Integer> soundMap = new HashMap<Integer, Integer>();
	
	private Context context;
	
	public PromtVoice(Context context) {
		this.context = context;
		
		mSoundPool = new SoundPool(27, AudioManager.STREAM_MUSIC, 0);
		
		soundMap.put(1, mSoundPool.load(this.context, R.raw.a1, 1));
		soundMap.put(2, mSoundPool.load(this.context, R.raw.a2, 1));
		soundMap.put(3, mSoundPool.load(this.context, R.raw.a3, 1));
		
	}
	
	public void playSense() {
		mSoundPool.play(soundMap.get(1), 1, 1, 0, 0, 1); 
	}
	
	public void playRecognition() {
		mSoundPool.play(soundMap.get(2), 1, 1, 0, 0, 1); 
	}
	
	public void playComplete ( ) {
		mSoundPool.play(soundMap.get(3), 1, 1, 0, 0, 1); 
	}
}
