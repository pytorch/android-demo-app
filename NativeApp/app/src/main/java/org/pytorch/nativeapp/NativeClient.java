package org.pytorch.nativeapp;

public final class NativeClient {

  public static void loadAndForwardModel(final String modelPath) {
    NativePeer.loadAndForwardModel(modelPath);
  }

  private static class NativePeer {
    static {
      System.loadLibrary("pytorch_nativeapp");
    }

    private static native void loadAndForwardModel(final String modelPath);
  }
}
