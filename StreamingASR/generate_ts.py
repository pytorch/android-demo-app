from typing import Dict, List, Optional, Tuple
import json
import math

from fairseq.data import Dictionary
import torch
import torchaudio
from torchaudio.pipelines import EMFORMER_RNNT_BASE_LIBRISPEECH
from torchaudio.models import Hypothesis


def get_hypo_tokens(hypo: Hypothesis) -> List[int]:
    return hypo[0]


def get_hypo_score(hypo: Hypothesis) -> float:
    return hypo[3]


def to_string(input: List[int], tgt_dict: List[str], bos_idx: int = 0, eos_idx: int = 2, separator: str = "",) -> str:
    # torchscript dislikes sets
    extra_symbols_to_ignore: Dict[int, int] = {}
    extra_symbols_to_ignore[eos_idx] = 1
    extra_symbols_to_ignore[bos_idx] = 1

    # it also dislikes comprehensions with conditionals
    filtered_idx: List[int] = []
    for idx in input:
        if idx not in extra_symbols_to_ignore:
            filtered_idx.append(idx)

    return separator.join([tgt_dict[idx] for idx in filtered_idx]).replace("\u2581", " ")


def post_process_hypos(
    hypos: List[Hypothesis], tgt_dict: List[str],
) -> List[Tuple[str, List[float], List[int]]]:
    post_process_remove_list = [
        3,  # unk
        2,  # eos
        1,  # pad
    ]
    hypos_str: List[str] = []
    for h in hypos:
        filtered_tokens: List[int] = []
        for token_index in get_hypo_tokens(h)[1:]:
            if token_index not in post_process_remove_list:
                filtered_tokens.append(token_index)
        string = to_string(filtered_tokens, tgt_dict)
        hypos_str.append(string)

    hypos_ids = [get_hypo_tokens(h)[1:] for h in hypos]
    hypos_score = [[math.exp(get_hypo_score(h))] for h in hypos]

    nbest_batch = list(zip(hypos_str, hypos_score, hypos_ids))

    return nbest_batch


def _piecewise_linear_log(x):
    x[x > math.e] = torch.log(x[x > math.e])
    x[x <= math.e] = x[x <= math.e] / math.e
    return x


class ModelWrapper(torch.nn.Module):
    def __init__(self, tgt_dict: List[str]):
        super().__init__()
        self.transform = torchaudio.transforms.MelSpectrogram(sample_rate=16000, n_fft=400, n_mels=80, hop_length=160)

        self.decoder = EMFORMER_RNNT_BASE_LIBRISPEECH.get_decoder()

        self.tgt_dict = tgt_dict

        with open("global_stats.json") as f:
            blob = json.loads(f.read())

        self.mean = torch.tensor(blob["mean"])
        self.invstddev = torch.tensor(blob["invstddev"])

        self.decibel = 2 * 20 * math.log10(32767)
        self.gain = pow(10, 0.05 * self.decibel)

    def forward(
        self, input: torch.Tensor, prev_hypo: Optional[Hypothesis], prev_state: Optional[List[List[torch.Tensor]]]
    ) -> Tuple[str, Hypothesis, Optional[List[List[torch.Tensor]]]]:
        spectrogram = self.transform(input).transpose(1, 0)
        features = _piecewise_linear_log(spectrogram * self.gain).unsqueeze(0)[:, :-1]
        features = (features - self.mean) * self.invstddev
        length = torch.tensor([features.shape[1]])

        hypotheses, state = self.decoder.infer(features, length, 10, state=prev_state, hypothesis=prev_hypo)
        transcript = post_process_hypos(hypotheses[:1], self.tgt_dict)[0][0]
        return transcript, hypotheses[0], state


tgt_dict = Dictionary.load("spm_bpe_4096_fairseq.dict")
wrapper = ModelWrapper(tgt_dict.symbols)
wrapper = torch.jit.script(wrapper)
wrapper.save("scripted_wrapper_tuple.pt")
