package com.audioStreaming;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.audioStreaming.AudioPlayerService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import javax.annotation.Nullable;

public class ReactNativeAudioStreamingModule extends ReactContextBaseJavaModule
        implements ServiceConnection {

  public static final String SHOULD_SHOW_NOTIFICATION = "showInAndroidNotifications";
  public static final String PROGRESS_KEY = "progress";
  private ReactApplicationContext context;

  private Class<?> clsActivity;
  private static Signal signal;
  private static AudioPlayerService audioPlayerService;
  private Intent bindIntent;
  private String streamingURL;
  private boolean shouldShowNotification;

  public ReactNativeAudioStreamingModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.context = reactContext;
  }

  public ReactApplicationContext getReactApplicationContextModule() {
    return this.context;
  }

  public Class<?> getClassActivity() {
    if (this.clsActivity == null) {
      this.clsActivity = getCurrentActivity().getClass();
    }
    return this.clsActivity;
  }

  public void stopOnCall() {
    //this.signal.stop();
    audioPlayerService.stop();
  }

  public Signal getSignal() {
    return signal;
  }

  public void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
    this.context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
  }

  @Override public String getName() {
    return "ReactNativeAudioStreaming";
  }

  @Override public void initialize() {
    super.initialize();

    try {
      bindIntent = new Intent(this.context, AudioPlayerService.class);
      //bindIntent = new Intent(this.context, Signal.class);
      this.context.bindService(bindIntent, this, Context.BIND_AUTO_CREATE);
    } catch (Exception e) {
      Log.e("ERROR", e.getMessage());
    }
  }

  @Override public void onServiceConnected(ComponentName className, IBinder service) {
//    signal = ((Signal.RadioBinder) service).getService();
//    signal.setData(this.context, this);
    audioPlayerService = ((AudioPlayerService.RadioBinder) service).getService();
    audioPlayerService.setData(this.context, this);

    WritableMap params = Arguments.createMap();
    sendEvent(this.getReactApplicationContextModule(), "streamingOpen", params);
  }

  @Override public void onServiceDisconnected(ComponentName className) {
    signal = null;
  }

  @ReactMethod public void play(String streamingURL, ReadableMap options) {

    this.streamingURL = streamingURL;
    this.shouldShowNotification =
        options.hasKey(SHOULD_SHOW_NOTIFICATION) && options.getBoolean(SHOULD_SHOW_NOTIFICATION);
//    signal.setURLStreaming(streamingURL); // URL of MP3 or AAC stream


    audioPlayerService.setTrackURL(streamingURL);
    playInternal();

    if (options.hasKey(PROGRESS_KEY)) {
      audioPlayerService.seekToTime(options.getDouble(PROGRESS_KEY));
    }
  }

  @ReactMethod public void setTrackTitle(String trackTitle) {
    audioPlayerService.setTrackTitle(trackTitle);
  }

  @ReactMethod public void setArtist(String artist) {
    audioPlayerService.setArtist(artist);
  }

  @ReactMethod public void setCoverImageUrl(String coverImageUrl) {
    audioPlayerService.setCoverImageUrl(coverImageUrl);
  }


  private void playInternal() {
    audioPlayerService.play();
  }

  @ReactMethod public void stop() {
    //signal.stop();
    audioPlayerService.stop();
  }

  @ReactMethod public void pause() {
    // Not implemented on aac
    this.stop();
  }

  @ReactMethod public void seekToTime(Double time) {
    audioPlayerService.seekToTime(time);
  }

  @ReactMethod public void resume() {
    // Not implemented on aac
    playInternal();
  }

  @ReactMethod public void destroyNotification() {
    signal.exitNotification();
  }

  @ReactMethod public void getStatus(Callback callback) {
    WritableMap state = Arguments.createMap();
    state.putString("status", signal != null && signal.isPlaying ? Mode.PLAYING : Mode.STOPPED);
    callback.invoke(null, state);
  }
}
