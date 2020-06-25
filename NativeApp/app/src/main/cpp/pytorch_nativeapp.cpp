#include <android/log.h>
#include <cassert>
#include <cmath>
#include <pthread.h>
#include <unistd.h>
#include <vector>
#define ALOGI(...)                                                             \
  __android_log_print(ANDROID_LOG_INFO, "PyTorchNativeApp", __VA_ARGS__)
#define ALOGE(...)                                                             \
  __android_log_print(ANDROID_LOG_ERROR, "PyTorchNativeApp", __VA_ARGS__)

#include "jni.h"

#include <opencv2/opencv.hpp>
#include <torch/script.h>

namespace pytorch_nativeapp {
namespace {
torch::Tensor warp_perspective(torch::Tensor image, torch::Tensor warp) {
  cv::Mat image_mat(/*rows=*/image.size(0),
                    /*cols=*/image.size(1),
                    /*type=*/CV_32FC1,
                    /*data=*/image.data_ptr<float>());
  cv::Mat warp_mat(/*rows=*/warp.size(0),
                   /*cols=*/warp.size(1),
                   /*type=*/CV_32FC1,
                   /*data=*/warp.data_ptr<float>());

  cv::Mat output_mat;
  cv::warpPerspective(image_mat, output_mat, warp_mat, /*dsize=*/{8, 8});

  torch::Tensor output =
      torch::from_blob(output_mat.ptr<float>(), /*sizes=*/{8, 8});
  return output.clone();
}

static auto registry =
    torch::RegisterOperators("my_ops::warp_perspective", &warp_perspective);

template <typename T> void log(const char *m, T t) {
  std::ostringstream os;
  os << t << std::endl;
  ALOGI("%s %s", m, os.str().c_str());
}

struct JITCallGuard {
  torch::autograd::AutoGradMode no_autograd_guard{false};
  torch::AutoNonVariableTypeMode non_var_guard{true};
  torch::jit::GraphOptimizerEnabledGuard no_optimizer_guard{false};
};
} // namespace

static void loadAndForwardModel(JNIEnv *env, jclass, jstring jModelPath) {
  const char *modelPath = env->GetStringUTFChars(jModelPath, 0);
  assert(modelPath);

  // To load torchscript model for mobile we need set these guards,
  // because mobile build doesn't support features like autograd for smaller
  // build size which is placed in `struct JITCallGuard` in this example. It may
  // change in future, you can track the latest changes keeping an eye in
  // android/pytorch_android/src/main/cpp/pytorch_jni_jit.cpp
  JITCallGuard guard;
  torch::jit::Module module = torch::jit::load(modelPath);
  module.eval();
  torch::Tensor x = torch::randn({4, 8});
  torch::Tensor y = torch::randn({8, 5});
  log("x:", x);
  log("y:", y);
  c10::IValue t_out = module.forward({x, y});
  log("result:", t_out);
  env->ReleaseStringUTFChars(jModelPath, modelPath);
}
} // namespace pytorch_nativeapp

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *) {
  JNIEnv *env;
  if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }

  jclass c = env->FindClass("org/pytorch/nativeapp/NativeClient$NativePeer");
  if (c == nullptr) {
    return JNI_ERR;
  }

  static const JNINativeMethod methods[] = {
      {"loadAndForwardModel", "(Ljava/lang/String;)V",
       (void *)pytorch_nativeapp::loadAndForwardModel},
  };
  int rc = env->RegisterNatives(c, methods,
                                sizeof(methods) / sizeof(JNINativeMethod));

  if (rc != JNI_OK) {
    return rc;
  }

  return JNI_VERSION_1_6;
}
