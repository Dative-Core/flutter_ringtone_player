package io.inway.ringtone.player;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding;
import io.flutter.plugin.common.PluginRegistry;

/** 
 * FlutterRingtonePlayerPlugin
 * 
 * A Flutter plugin for playing system ringtones on Android.
 * Supports both V1 and V2 Flutter Android embedding.
 */
public class FlutterRingtonePlayerPlugin implements FlutterPlugin, MethodCallHandler {
    private static final String CHANNEL_NAME = "flutter_ringtone_player";
    /// The MethodChannel that will the communication between Flutter and native Android
    private MethodChannel channel;
    
    /// Application context
    private Context context;
    
    /// Ringtone manager for handling system sounds
    private RingtoneManager ringtoneManager;
    
    /// Current playing ringtone instance
    private Ringtone ringtone;

    /** Plugin registration for V1 embedding. */
    @SuppressWarnings("deprecation")
    public static void registerWith(PluginRegistry.Registrar registrar) {
        FlutterRingtonePlayerPlugin instance = new FlutterRingtonePlayerPlugin();
        instance.onAttachedToEngine(registrar.context(), registrar.messenger());
    }

    /** Plugin registration for V2 embedding. */
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    /** Common initialization code for V1 and V2 embedding. */
    private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        this.context = applicationContext;
        this.ringtoneManager = new RingtoneManager(context);
        this.ringtoneManager.setStopPreviousRingtone(true);

        channel = new MethodChannel(messenger, CHANNEL_NAME);
        channel.setMethodCallHandler(this);
    }

    /** Cleanup when detached from Flutter engine. */
    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (ringtone != null) {
            ringtone.stop();
            ringtone = null;
        }
        channel.setMethodCallHandler(null);
        channel = null;
        context = null;
        ringtoneManager = null;
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        try {
            Uri ringtoneUri = null;
            if (call.method.equals("play")) {
                if (call.hasArgument("uri")) {
                    String uri = call.argument("uri");
                    ringtoneUri = Uri.parse(uri);
                }

                // The androidSound overrides fromAsset if exists
                if (call.hasArgument("android")) {
                    int pref = call.argument("android");
                    switch (pref) {
                        case 1:
                            ringtoneUri = ringtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
                            break;
                        case 2:
                            ringtoneUri = ringtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                            break;
                        case 3:
                            ringtoneUri = ringtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);
                            break;
                        default:
                            result.notImplemented();
                    }

                }
            } else if (call.method.equals("stop")) {
                if (ringtone != null) {
                    ringtone.stop();
                }

                result.success(null);
            }

            if (ringtoneUri != null) {
                if (ringtone != null) {
                    ringtone.stop();
                }
                ringtone = RingtoneManager.getRingtone(context, ringtoneUri);

                if (call.hasArgument("volume")) {
                    final double volume = call.argument("volume");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.setVolume((float) volume);
                    }
                }

                if (call.hasArgument("looping")) {
                    final boolean looping = call.argument("looping");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.setLooping(looping);
                    }
                }

                if (call.hasArgument("asAlarm")) {
                    final boolean asAlarm = call.argument("asAlarm");
                    if (asAlarm) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            ringtone.setAudioAttributes(new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build());
                        } else {
                            // Use deprecated method for older Android versions
                            @SuppressWarnings("deprecation")
                            int streamType = AudioManager.STREAM_ALARM;
                            ringtone.setStreamType(streamType);
                        }
                    }
                }

                ringtone.play();

                result.success(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.error("Exception", e.getMessage(), null);
        }
    }
}
