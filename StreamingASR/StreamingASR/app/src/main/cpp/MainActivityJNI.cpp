#include <jni.h>
#include <string>
#include <Eigen/Dense>
#include "librosa/librosa.h"
#include <iostream>
#include <vector>
#include <chrono>
#include <numeric>
#include <algorithm>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

using namespace std;

using Eigen::MatrixXd;

extern "C" JNIEXPORT jobject JNICALL
Java_org_pytorch_demo_streamingasr_MainActivity_melSpectrogram(JNIEnv* env, jobject obj,
                                                                    jdoubleArray data) {
    int len = env -> GetArrayLength(data);
    std::vector<float> x;
    jdouble *element = env->GetDoubleArrayElements(data, 0);
    for(int i=0; i<len; ++i) {
        x.push_back((float)element[i]);
    }

    int n_fft = 400;
    int n_hop = 160;
    int n_mel = 80;
    int fmin = 0;
    int fmax = 8000;
    int sr = 16000;

    std::vector<std::vector<std::complex<float>>> X = librosa::Feature::stft(x, n_fft, n_hop, "hann", true, "reflect");

    std::vector<std::vector<float>> mels = librosa::Feature::melspectrogram(x, sr, n_fft, n_hop, "hann", true, "reflect", 2.f, n_mel, fmin, fmax);

    jclass vectorClass = env->FindClass("java/util/Vector");
    if(vectorClass == NULL) {
        return NULL;
    }

    jclass floatClass = env->FindClass("java/lang/Float");
    if(floatClass == NULL) {
        return NULL;
    }

    jmethodID vectorConstructorID = env->GetMethodID(
            vectorClass, "<init>", "()V");
    if(vectorConstructorID == NULL) {
        return NULL;
    }

    jmethodID addMethodID = env->GetMethodID(
            vectorClass, "add", "(Ljava/lang/Object;)Z" );
    if(addMethodID == NULL) {
        return NULL;
    }

    jmethodID floatConstructorID = env->GetMethodID(floatClass, "<init>", "(F)V");
    if(floatConstructorID == NULL) {
        return NULL;
    }

    jobject outerVector = env->NewObject(vectorClass, vectorConstructorID);
    if(outerVector == NULL) {
        return NULL;
    }

    for(vector<float> i : mels) {
        jobject innerVector = env->NewObject(vectorClass, vectorConstructorID);

        for(float f : i) {
            jobject floatValue = env->NewObject(floatClass, floatConstructorID, f);
            if(floatValue == NULL) {
                return NULL;
            }

            env->CallBooleanMethod(innerVector, addMethodID, floatValue);
        }

        env->CallBooleanMethod(outerVector, addMethodID, innerVector);
    }

    env->DeleteLocalRef(vectorClass);
    env->DeleteLocalRef(floatClass);

    return outerVector;
}
