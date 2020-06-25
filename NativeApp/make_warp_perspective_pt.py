import torch
import torch.utils.cpp_extension

print(torch.version.__version__)
op_source = """
#include <opencv2/opencv.hpp>
#include <torch/script.h>

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
  cv::warpPerspective(image_mat, output_mat, warp_mat, /*dsize=*/{64, 64});

  torch::Tensor output =
    torch::from_blob(output_mat.ptr<float>(), /*sizes=*/{64, 64});
  return output.clone();
}

static auto registry =
  torch::RegisterOperators("my_ops::warp_perspective", &warp_perspective);
"""

torch.utils.cpp_extension.load_inline(
    name="warp_perspective",
    cpp_sources=op_source,
    extra_ldflags=["-lopencv_core", "-lopencv_imgproc"],
    is_python_module=False,
    verbose=True,
)

print(torch.ops.my_ops.warp_perspective)


@torch.jit.script
def compute(x, y):
    if bool(x[0][0] == 42):
        z = 5
    else:
        z = 10
    x = torch.ops.my_ops.warp_perspective(x, torch.eye(3))
    return x.matmul(y) + z


compute.save("app/src/main/assets/compute.pt")
