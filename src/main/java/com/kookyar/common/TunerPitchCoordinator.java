package com.kookyar.common;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Coordinates throttled note-trigger actions and delayed pitch-needle resets
 * so rapid string/fret selection does not flood the audio patch or leave stale UI callbacks.
 */
public final class TunerPitchCoordinator {
  public static final long TRIGGER_DEBOUNCE_MS = 80;
  public static final long MIN_TRIGGER_GAP_MS = 250;

  private static final Object PD_SEND_LOCK = new Object();

  private final Handler triggerHandler = new Handler(Looper.getMainLooper());
  private final Handler pitchHandler = new Handler(Looper.getMainLooper());
  private Runnable pendingDispatch;
  private Runnable latestTriggerAction;
  private int triggerGeneration;
  private long lastTriggerExecutedAt;
  private Runnable pitchResetRunnable;
  private int pitchResetToken;
  private AudioStarter audioStarter;

  public interface AudioStarter {
    void ensureRunning();
  }

  public void setAudioStarter(AudioStarter starter) {
    audioStarter = starter;
  }

  public static void runPdSend(Runnable action) {
    synchronized (PD_SEND_LOCK) {
      action.run();
    }
  }

  public void invalidatePitchReset() {
    pitchResetToken++;
    if (pitchResetRunnable != null) {
      pitchHandler.removeCallbacks(pitchResetRunnable);
      pitchResetRunnable = null;
    }
  }

  public void cancelTriggers() {
    triggerGeneration++;
    latestTriggerAction = null;
    if (pendingDispatch != null) {
      triggerHandler.removeCallbacks(pendingDispatch);
      pendingDispatch = null;
    }
  }

  /**
   * Coalesces rapid selections: the debounce timer is not restarted on every tap,
   * so continuous clicking still delivers the latest note at a bounded rate.
   */
  public void scheduleTrigger(Runnable triggerAction) {
    latestTriggerAction = triggerAction;
    if (pendingDispatch != null) {
      return;
    }

    triggerGeneration++;
    final int generation = triggerGeneration;
    long now = SystemClock.uptimeMillis();
    long sinceLast = now - lastTriggerExecutedAt;
    long delay = Math.max(TRIGGER_DEBOUNCE_MS, MIN_TRIGGER_GAP_MS - sinceLast);

    pendingDispatch = () -> dispatchTrigger(generation);
    triggerHandler.postDelayed(pendingDispatch, delay);
  }

  private void dispatchTrigger(int generation) {
    pendingDispatch = null;
    if (generation != triggerGeneration) {
      return;
    }

    Runnable triggerAction = latestTriggerAction;
    if (triggerAction == null) {
      return;
    }

    long now = SystemClock.uptimeMillis();
    long wait = MIN_TRIGGER_GAP_MS - (now - lastTriggerExecutedAt);
    if (wait > 0) {
      pendingDispatch = () -> dispatchTrigger(generation);
      triggerHandler.postDelayed(pendingDispatch, wait);
      return;
    }

    lastTriggerExecutedAt = now;
    if (audioStarter != null) {
      audioStarter.ensureRunning();
    }
    runPdSend(triggerAction);
  }

  public void onPitchInput(Runnable updatePitchDisplay, Runnable resetPitchDisplay) {
    pitchResetToken++;
    final int resetToken = pitchResetToken;
    if (pitchResetRunnable != null) {
      pitchHandler.removeCallbacks(pitchResetRunnable);
    }
    updatePitchDisplay.run();
    pitchResetRunnable = () -> {
      if (resetToken == pitchResetToken) {
        resetPitchDisplay.run();
      }
    };
    pitchHandler.postDelayed(pitchResetRunnable, PITCH_RESET_DELAY_MS);
  }

  public void destroy() {
    triggerGeneration++;
    pitchResetToken++;
    triggerHandler.removeCallbacksAndMessages(null);
    pitchHandler.removeCallbacksAndMessages(null);
    pendingDispatch = null;
    latestTriggerAction = null;
    pitchResetRunnable = null;
    audioStarter = null;
  }

  public static final long PITCH_RESET_DELAY_MS = 2500;
}
