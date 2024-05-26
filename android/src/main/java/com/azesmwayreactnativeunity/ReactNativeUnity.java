package com.azesmwayreactnativeunity;

import android.app.Activity;
import android.content.ContextWrapper;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.unity3d.player.IUnityPlayerSupport;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.IUnityPlayerLifecycleEvents;
import com.unity3d.player.UnityPlayerForActivityOrService;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
public class ReactNativeUnity {
  private static UnityPlayer unityPlayer;
  public static boolean _isUnityReady;
  public static boolean _isUnityPaused;
  public static boolean _fullScreen;

  public static UnityPlayer getPlayer() {
    if (!_isUnityReady) {
      return null;
    }
    return unityPlayer;
  }

  public static boolean isUnityReady() {
    return _isUnityReady;
  }

  public static boolean isUnityPaused() {
    return _isUnityPaused;
  }

  public static void createPlayer(final Activity activity, final UnityPlayerCallback callback) {
    if (unityPlayer != null) {
      callback.onReady();
      return;
    }
    if (activity != null) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          activity.getWindow().setFormat(PixelFormat.RGBA_8888);
          int flag = activity.getWindow().getAttributes().flags;
          boolean fullScreen = false;
          if ((flag & WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN) {
            fullScreen = true;
          }

          unityPlayer = new UnityPlayerForActivityOrService(activity, new IUnityPlayerLifecycleEvents() {

            @Override
            public void onUnityPlayerUnloaded() {
              callback.onUnload();
            }

            @Override
            public void onUnityPlayerQuitted() {
              callback.onQuit();
            }
          });

          FrameLayout unityPlayerView = unityPlayer.getFrameLayout();

          try {
            // wait a moment. fix unity cannot start when startup.
            Thread.sleep(1000);
          } catch (Exception e) {
          }

          // start unity
          addUnityViewToBackground();
          unityPlayer.windowFocusChanged(true);
          unityPlayerView.requestFocus();
          unityPlayer.resume();

          // restore window layout
          if (!fullScreen) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
          }
          _isUnityReady = true;
          callback.onReady();
        }
      });
    }
  }

  public static void pause() {
    if (unityPlayer != null) {
      unityPlayer.pause();
      _isUnityPaused = true;
    }
  }

  public static void resume() {
    if (unityPlayer != null) {
      unityPlayer.resume();
      _isUnityPaused = false;
    }
  }

  public static void unload() {
    if (unityPlayer != null) {
      unityPlayer.unload();
      _isUnityPaused = false;
    }
  }

  public static void addUnityViewToBackground() {
    if (unityPlayer == null) {
      return;
    }
    FrameLayout unityPlayerView = unityPlayer.getFrameLayout();
    if (unityPlayerView.getParent() != null) {
      // NOTE: If we're being detached as part of the transition, make sure
      // to explicitly finish the transition first, as it might still keep
      // the view's parent around despite calling `removeView()` here. This
      // prevents a crash on an `addContentView()` later on.
      // Otherwise, if there's no transition, it's a no-op.
      // See https://stackoverflow.com/a/58247331
      ((ViewGroup) unityPlayerView.getParent()).endViewTransition(unityPlayerView);
      ((ViewGroup) unityPlayerView.getParent()).removeView(unityPlayerView);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      unityPlayerView.setZ(-1f);
    }
    final Activity activity = ((Activity) unityPlayerView.getContext());
    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(1, 1);
    activity.addContentView(unityPlayerView, layoutParams);
  }

  public static void addUnityViewToGroup(ViewGroup group) {
    if (unityPlayer == null) {
      return;
    }
    FrameLayout unityPlayerView = unityPlayer.getFrameLayout();
    if (unityPlayerView.getParent() != null) {
      ((ViewGroup) unityPlayerView.getParent()).removeView(unityPlayerView);
    }
    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
    group.addView(unityPlayerView, 0, layoutParams);
    unityPlayer.windowFocusChanged(true);
    unityPlayerView.requestFocus();
    unityPlayer.resume();
  }

  public interface UnityPlayerCallback {
    void onReady();

    void onUnload();

    void onQuit();
  }
}