/* ------------------------------------------------------------------
* Copyright (C) 2020 ewan xu<ewan_xu@outlook.com>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied.
* See the License for the specific language governing permissions
* and limitations under the License.
* -------------------------------------------------------------------
*/

#ifndef LIBROSA_H_
#define LIBROSA_H_

#include "Eigen/Core"
#include "unsupported/Eigen/FFT"

#include <vector>
#include <complex>
#include <iostream>

///
/// \brief c++ implemention of librosa
///
namespace librosa{

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif // !M_PI

typedef Eigen::Matrix<float, 1, Eigen::Dynamic, Eigen::RowMajor> Vectorf;
typedef Eigen::Matrix<std::complex<float>, 1, Eigen::Dynamic, Eigen::RowMajor> Vectorcf;
typedef Eigen::Matrix<float, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor> Matrixf;
typedef Eigen::Matrix<std::complex<float>, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor> Matrixcf;

namespace internal{

static Vectorf pad(Vectorf &x, int left, int right, const std::string &mode, float value){
  Vectorf x_paded = Vectorf::Constant(left+x.size()+right, value);
  x_paded.segment(left, x.size()) = x;

  if (mode.compare("reflect") == 0){
    for (int i = 0; i < left; ++i){
      x_paded[i] = x[left-i];
    }
    for (int i = left; i < left+right; ++i){
      x_paded[i+x.size()] = x[x.size()-2-i+left];
    }
  }

  if (mode.compare("symmetric") == 0){
    for (int i = 0; i < left; ++i){
      x_paded[i] = x[left-i-1];
    }
    for (int i = left; i < left+right; ++i){
      x_paded[i+x.size()] = x[x.size()-1-i+left];
    }
  }

  if (mode.compare("edge") == 0){
    for (int i = 0; i < left; ++i){
      x_paded[i] = x[0];
    }
    for (int i = left; i < left+right; ++i){
      x_paded[i+x.size()] = x[x.size()-1];
    }
  }
  return x_paded;
}

static Matrixcf stft(Vectorf &x, int n_fft, int n_hop, const std::string &win, bool center, const std::string &mode){
  // hanning
  Vectorf window = 0.5*(1.f-(Vectorf::LinSpaced(n_fft, 0.f, static_cast<float>(n_fft-1))*2.f*M_PI/n_fft).array().cos());

  int pad_len = center ? n_fft / 2 : 0;
  Vectorf x_paded = pad(x, pad_len, pad_len, mode, 0.f);

  int n_f = n_fft/2+1;
  int n_frames = 1+(x_paded.size()-n_fft) / n_hop;
  Matrixcf X(n_frames, n_fft);
  Eigen::FFT<float> fft;

  for (int i = 0; i < n_frames; ++i){
    Vectorf x_frame = window.array()*x_paded.segment(i*n_hop, n_fft).array();
    X.row(i) = fft.fwd(x_frame);
  }
  return X.leftCols(n_f);
}

static Matrixf spectrogram(Matrixcf &X, float power = 1.f){
  return X.cwiseAbs().array().pow(power);
}

static Matrixf melfilter(int sr, int n_fft, int n_mels, int fmin, int fmax){
  int n_f = n_fft/2+1;
  Vectorf fft_freqs = (Vectorf::LinSpaced(n_f, 0.f, static_cast<float>(n_f-1))*sr)/n_fft;

  float f_min = 0.f;
  float f_sp = 200.f/3.f;
  float min_log_hz = 1000.f;
  float min_log_mel = (min_log_hz-f_min)/f_sp;
  float logstep = logf(6.4f)/27.f;

  auto hz_to_mel = [=](int hz, bool htk = false) -> float {
    if (htk){
      return 2595.0f*log10f(1.0f+hz/700.0f);
    }
    float mel = (hz-f_min)/f_sp;
    if (hz >= min_log_hz){
      mel = min_log_mel+logf(hz/min_log_hz)/logstep;
    }
    return mel;
  };
  auto mel_to_hz = [=](Vectorf &mels, bool htk = false) -> Vectorf {
    if (htk){
      return 700.0f*(Vectorf::Constant(n_mels+2, 10.f).array().pow(mels.array()/2595.0f)-1.0f);
    }
    return (mels.array()>min_log_mel).select(((mels.array()-min_log_mel)*logstep).exp()*min_log_hz, (mels*f_sp).array()+f_min);
  };

  float min_mel = hz_to_mel(fmin);
  float max_mel = hz_to_mel(fmax);
  Vectorf mels = Vectorf::LinSpaced(n_mels+2, min_mel, max_mel);
  Vectorf mel_f = mel_to_hz(mels);
  Vectorf fdiff = mel_f.segment(1, mel_f.size() - 1) - mel_f.segment(0, mel_f.size() - 1);
  Matrixf ramps = mel_f.replicate(n_f, 1).transpose().array() - fft_freqs.replicate(n_mels + 2, 1).array();

  Matrixf lower = -ramps.topRows(n_mels).array()/fdiff.segment(0, n_mels).transpose().replicate(1, n_f).array();
  Matrixf upper = ramps.bottomRows(n_mels).array()/fdiff.segment(1, n_mels).transpose().replicate(1, n_f).array();
  Matrixf weights = (lower.array()<upper.array()).select(lower, upper).cwiseMax(0);

  auto enorm = (2.0/(mel_f.segment(2, n_mels)-mel_f.segment(0, n_mels)).array()).transpose().replicate(1, n_f);
  weights = weights.array()*enorm;

  return weights;
}

static Matrixf melspectrogram(Vectorf &x, int sr, int n_fft, int n_hop,
                        const std::string &win, bool center,
                        const std::string &mode, float power,
                        int n_mels, int fmin, int fmax){
  Matrixcf X = stft(x, n_fft, n_hop, win, center, mode);
  Matrixf mel_basis = melfilter(sr, n_fft, n_mels, fmin, fmax);
  Matrixf sp = spectrogram(X, power);
  Matrixf mel = mel_basis*sp.transpose();
  return mel;
}

static Matrixf power2db(Matrixf& x) {
  auto log_sp = 10.0f*x.array().max(1e-10).log10();
  return log_sp.cwiseMax(log_sp.maxCoeff() - 80.0f);
}

static Matrixf dct(Matrixf& x, bool norm, int type) {
  int N = x.cols();
  Matrixf xi = Matrixf::Zero(N, N);
  xi.rowwise() += Vectorf::LinSpaced(N, 0.f, static_cast<float>(N-1));
  // type 2
  Matrixf coeff = 2*(M_PI*xi.transpose().array()/N*(xi.array()+0.5)).cos();
  Matrixf dct = x*coeff.transpose();
  // ortho
  if (norm) {
    Vectorf ortho = Vectorf::Constant(N, std::sqrtf(0.5f/N));
    ortho[0] = std::sqrtf(0.25f/N);
    dct = dct*ortho.asDiagonal();
  }
  return dct;
}

} // namespace internal

class Feature
{
public:
  /// \brief      short-time fourier transform similar with librosa.feature.stft
  /// \param      x             input audio signal
  /// \param      n_fft         length of the FFT size
  /// \param      n_hop         number of samples between successive frames
  /// \param      win           window function. currently only supports 'hann'
  /// \param      center        same as librosa
  /// \param      mode          pad mode. support "reflect","symmetric","edge"
  /// \return     complex-valued matrix of short-time fourier transform coefficients.
  static std::vector<std::vector<std::complex<float>>> stft(std::vector<float> &x,
                                                            int n_fft, int n_hop,
                                                            const std::string &win, bool center,
                                                            const std::string &mode){
    Vectorf map_x = Eigen::Map<Vectorf>(x.data(), x.size());
    Matrixcf X = internal::stft(map_x, n_fft, n_hop, win, center, mode);
    std::vector<std::vector<std::complex<float>>> X_vector(X.rows(), std::vector<std::complex<float>>(X.cols(), 0));
    for (int i = 0; i < X.rows(); ++i){
      auto &row = X_vector[i];
      Eigen::Map<Vectorcf>(row.data(), row.size()) = X.row(i);
    }
    return X_vector;
  }

  /// \brief      compute mel spectrogram similar with librosa.feature.melspectrogram
  /// \param      x             input audio signal
  /// \param      sr            sample rate of 'x'
  /// \param      n_fft         length of the FFT size
  /// \param      n_hop         number of samples between successive frames
  /// \param      win           window function. currently only supports 'hann'
  /// \param      center        same as librosa
  /// \param      mode          pad mode. support "reflect","symmetric","edge"
  /// \param      power         exponent for the magnitude melspectrogram
  /// \param      n_mels        number of mel bands
  /// \param      f_min         lowest frequency (in Hz)
  /// \param      f_max         highest frequency (in Hz)
  /// \return     mel spectrogram matrix
  static std::vector<std::vector<float>> melspectrogram(std::vector<float> &x, int sr, 
                                                        int n_fft, int n_hop, const std::string &win, bool center, const std::string &mode,
                                                        float power, int n_mels, int fmin, int fmax){
    Vectorf map_x = Eigen::Map<Vectorf>(x.data(), x.size());
    Matrixf mel = internal::melspectrogram(map_x, sr, n_fft, n_hop, win, center, mode, power, n_mels, fmin, fmax).transpose();
    std::vector<std::vector<float>> mel_vector(mel.rows(), std::vector<float>(mel.cols(), 0.f));
    for (int i = 0; i < mel.rows(); ++i){
      auto &row = mel_vector[i];
      Eigen::Map<Vectorf>(row.data(), row.size()) = mel.row(i);
    }
    return mel_vector;
  }

  /// \brief      compute mfcc similar with librosa.feature.mfcc
  /// \param      x             input audio signal
  /// \param      sr            sample rate of 'x'
  /// \param      n_fft         length of the FFT size
  /// \param      n_hop         number of samples between successive frames
  /// \param      win           window function. currently only supports 'hann'
  /// \param      center        same as librosa
  /// \param      mode          pad mode. support "reflect","symmetric","edge"
  /// \param      power         exponent for the magnitude melspectrogram
  /// \param      n_mels        number of mel bands
  /// \param      f_min         lowest frequency (in Hz)
  /// \param      f_max         highest frequency (in Hz)
  /// \param      n_mfcc        number of mfccs
  /// \param      norm          ortho-normal dct basis
  /// \param      type          dct type. currently only supports 'type-II'
  /// \return     mfcc matrix
  static std::vector<std::vector<float>> mfcc(std::vector<float> &x, int sr,
                                              int n_fft, int n_hop, const std::string &win, bool center, const std::string &mode,
                                              float power, int n_mels, int fmin, int fmax,
                                              int n_mfcc, bool norm, int type) {
    Vectorf map_x = Eigen::Map<Vectorf>(x.data(), x.size());
    Matrixf mel = internal::melspectrogram(map_x, sr, n_fft, n_hop, win, center, mode, power, n_mels, fmin, fmax).transpose();
    Matrixf mel_db = internal::power2db(mel);
    Matrixf dct = internal::dct(mel_db, norm, type).leftCols(n_mfcc);
    std::vector<std::vector<float>> mfcc_vector(dct.rows(), std::vector<float>(dct.cols(), 0.f));
    for (int i = 0; i < dct.rows(); ++i) {
      auto &row = mfcc_vector[i];
      Eigen::Map<Vectorf>(row.data(), row.size()) = dct.row(i);
    }
    return mfcc_vector;
  }
};

} // namespace librosa

#endif
