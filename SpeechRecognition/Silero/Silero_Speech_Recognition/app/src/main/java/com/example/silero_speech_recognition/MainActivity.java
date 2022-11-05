package com.example.silero_speech_recognition;

import static android.Manifest.permission.RECORD_AUDIO;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pytorch.LiteModuleLoader;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    private Module module;
    private TextView mTextView,timeView;
    private Button mButton;

    private final static int REQUEST_RECORD_AUDIO = 13;
    private final static int AUDIO_LEN_IN_SECOND = 6;
    private final static int SAMPLE_RATE = 16000;
    private final static int RECORDING_LENGTH = SAMPLE_RATE * AUDIO_LEN_IN_SECOND;

    private final static String LOG_TAG = MainActivity.class.getSimpleName();

    private int mStart = 1;
    private HandlerThread mTimerThread;
    private Handler mTimerHandler;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTimerHandler.postDelayed(mRunnable, 1000);

            MainActivity.this.runOnUiThread(
                    () -> {
                        mButton.setText(String.format("Listening - %ds left", AUDIO_LEN_IN_SECOND - mStart));
                        mStart += 1;
                    });
        }
    };

    @Override
    protected void onDestroy() {
        stopTimerThread();
        super.onDestroy();
    }

    protected void stopTimerThread() {
        if (mTimerThread !=null)
        {
            mTimerThread.quitSafely();
        }
        try {
            mTimerThread.join();
            mTimerThread = null;
            mTimerHandler = null;
            mStart = 1;
        } catch (InterruptedException e) {
            Log.e(TAG, "Error on stopping background thread", e);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.start_btn);
        mTextView = findViewById(R.id.result_view);
        timeView = findViewById(R.id.spec_txt_view);

        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mButton.setText(String.format("Listening - %ds left", AUDIO_LEN_IN_SECOND));
                mButton.setEnabled(false);

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                        if (ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                        {
                            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                                    bufferSize);

                            if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                                Log.e(LOG_TAG, "Audio Record can't initialize!");
                                return;
                            }
                            record.startRecording();

                            long shortsRead = 0;
                            int recordingOffset = 0;
                            short[] audioBuffer = new short[bufferSize / 2];
                            short[] recordingBuffer = new short[RECORDING_LENGTH];

                            while (shortsRead < RECORDING_LENGTH) {
                                int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                                shortsRead += numberOfShort;
                                System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, numberOfShort);
                                recordingOffset += numberOfShort;
                            }

                            record.stop();
                            record.release();
                            stopTimerThread();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mButton.setText("Recognizing...");
                                }
                            });

                            float[] floatInputBuffer = new float[RECORDING_LENGTH];

                            // feed in float values between -1.0f and 1.0f by dividing the signed 16-bit inputs.
                            for (int i = 0; i < RECORDING_LENGTH; ++i) {
                                floatInputBuffer[i] = recordingBuffer[i] / (float) Short.MAX_VALUE;
                            }

                            long s = System.currentTimeMillis();
                            final String result = recognize(floatInputBuffer);
                            long e = System.currentTimeMillis();
                            NumberFormat formatter = new DecimalFormat("#0.00000");
                            String runTime = " " +formatter.format((e - s) / 1000d) + " seconds";

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    timeView.setText(runTime);
                                    showTranslationResult(result);
                                    mButton.setEnabled(true);
                                    mButton.setText("Start");
                                } });


                        }
                    }
                });
                thread.start();

                mTimerThread = new HandlerThread("Timer");
                mTimerThread.start();
                mTimerHandler = new Handler(mTimerThread.getLooper());
                mTimerHandler.postDelayed(mRunnable, 1000);

            }
        });
        requestMicrophonePermission();
    }

    private void requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    private String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, assetName + ": " + e.getLocalizedMessage());
        }
        return null;
    }

    private void showTranslationResult(String result) {
        mTextView.setText(result);
    }



    private String recognize(float[] floatInputBuffer) {
        if (module == null) {
            module = LiteModuleLoader.load(assetFilePath(getApplicationContext(), "silero.ptl"));
        }

        double modelInput[] = new double[RECORDING_LENGTH];
        for (int n = 0; n < RECORDING_LENGTH; n++)
            modelInput[n] = floatInputBuffer[n];

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(RECORDING_LENGTH);
        for (double val : modelInput)
            inTensorBuffer.put((float)val);

        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, RECORDING_LENGTH});
        Tensor outputTensor = module.forward(IValue.from(inTensor)).toTensor();

        String result = SileroDecoder(outputTensor);

        return result;
    }
    private int[] argmaxOneD(float[]arr,int dim1,int dim2)
    {
        int ans[] = new int[dim1];
        int k=0;
        for(int i=0;i<dim1;i++)
        {
            float max=Integer.MIN_VALUE;
            int index=0;
            for(int j=0;j<dim2;j++)
            {
                if(arr[k]>max)
                {
                    max=arr[k];
                    index=j;
                }
                k++;
            }
            ans[i]=index;
        }
        return ans;

    }
    private String SileroDecoder(Tensor opProbs)
    {
        long shape[] = opProbs.shape();
        final int DIM1= (int) shape[1];
        final int DIM2 = (int) shape[2];
        float array[] =  opProbs.getDataAsFloatArray();
        String labels[] = {"_", "th", "the", "in", "an", "re", "er", "on", "at", "ou", "is", "en", "to", "and", "ed", "al", "as", "it", "ing", "or", "of", "es", "ar", "he", "le", "st", "se", "om", "ic", "be", "we", "ly", "that", "no", "wh", "ve", "ha", "you", "ch", "ion", "il", "ent", "ro", "me", "id", "ac", "gh", "for", "was", "lo", "ver", "ut", "li", "ld", "ay", "ad", "so", "ir", "im", "un", "wi", "ter", "are", "with", "ke", "ge", "do", "ur", "all", "ce", "ab", "mo", "go", "pe", "ne", "this", "ri", "ght", "de", "one", "us", "am", "out", "fe", "but", "po", "his", "te", "ho", "ther", "not", "con", "com", "ll", "they", "if", "ould", "su", "have", "ct", "ain", "our", "ation", "fr", "ill", "now", "sa", "had", "tr", "her", "per", "ant", "oun", "my", "ul", "ca", "by", "what", "red", "res", "od", "ome", "ess", "man", "ex", "she", "pl", "co", "wor", "pro", "up", "thing", "there", "ple", "ag", "can", "qu", "art", "ally", "ok", "from", "ust", "very", "sh", "ind", "est", "some", "ate", "wn", "ti", "fo", "ard", "ap", "him", "were", "ich", "here", "bo", "ity", "um", "ive", "ous", "way", "end", "ig", "pr", "which", "ma", "ist", "them", "like", "who", "ers", "when", "act", "use", "about", "ound", "gr", "et", "ide", "ight", "ast", "king", "would", "ci", "their", "other", "see", "ment", "ong", "wo", "ven", "know", "how", "said", "ine", "ure", "more", "der", "sel", "br", "ren", "ack", "ol", "ta", "low", "ough", "then", "peo", "ye", "ace", "people", "ink", "ort", "your", "will", "than", "pp", "any", "ish", "look", "la", "just", "tor", "ice", "itt", "af", "these", "sp", "has", "gre", "been", "ty", "ies", "ie", "get", "able", "day", "could", "bl", "two", "time", "beca", "into", "age", "ans", "mis", "new", "ree", "ble", "ite", "si", "urn", "ass", "cl", "ber", "str", "think", "dis", "mar", "ence", "pt", "self", "ated", "did", "el", "don", "ck", "ph", "ars", "ach", "fore", "its", "part", "ang", "cre", "well", "ions", "where", "ves", "ved", "em", "good", "because", "over", "ud", "ts", "off", "turn", "cr", "right", "ress", "most", "every", "pre", "fa", "fir", "ild", "pos", "down", "work", "ade", "say", "med", "also", "litt", "little", "ance", "come", "ving", "only", "ful", "ought", "want", "going", "spe", "ps", "ater", "first", "after", "ue", "ose", "mu", "iz", "ire", "int", "rest", "ser", "coun", "des", "light", "son", "let", "ical", "ick", "ra", "back", "mon", "ase", "ign", "ep", "world", "may", "read", "form", "much", "even", "should", "again", "make", "long", "sto", "cont", "put", "thr", "under", "cor", "bet", "jo", "car", "ile", "went", "yes", "ually", "row", "hand", "ak", "call", "ary", "ia", "many", "cho", "things", "try", "gl", "ens", "really", "happ", "great", "dif", "bu", "hi", "made", "room", "ange", "cent", "ious", "je", "three", "ward", "op", "gen", "those", "life", "tal", "pa", "through", "und", "cess", "before", "du", "side", "need", "less", "inter", "ting", "ry", "ise", "na", "men", "ave", "fl", "ction", "pres", "old", "something", "miss", "never", "got", "feren", "imp", "sy", "ations", "tain", "ning", "ked", "sm", "take", "ten", "ted", "ip", "col", "own", "stand", "add", "min", "wer", "ms", "ces", "gu", "land", "bod", "log", "cour", "ob", "vo", "ition", "hu", "came", "comp", "cur", "being", "comm", "years", "ily", "wom", "cer", "kind", "thought", "such", "tell", "child", "nor", "bro", "ial", "pu", "does", "head", "clo", "ear", "led", "llow", "ste", "ness", "too", "start", "mor", "used", "par", "play", "ents", "tri", "upon", "tim", "num", "ds", "ever", "cle", "ef", "wr", "vis", "ian", "sur", "same", "might", "fin", "differen", "sho", "why", "body", "mat", "beg", "vers", "ouse", "actually", "ft", "ath", "hel", "sha", "ating", "ual", "found", "ways", "must", "four", "gi", "val", "di", "tre", "still", "tory", "ates", "high", "set", "care", "ced", "last", "find", "au", "inte", "ev", "ger", "thank", "ss", "ict", "ton", "cal", "nat", "les", "bed", "away", "place", "house", "che", "ject", "sol", "another", "ited", "att", "face", "show", "ner", "ken", "far", "ys", "lect", "lie", "tem", "ened", "night", "while", "looking", "ah", "wal", "dr", "real", "cha", "exp", "war", "five", "pol", "fri", "wa", "cy", "fect", "xt", "left", "give", "soci", "cond", "char", "bor", "point", "number", "mister", "called", "six", "bre", "vi", "without", "person", "air", "different", "lot", "bit", "pass", "ular", "youn", "won", "main", "cri", "ings", "den", "water", "human", "around", "quest", "ters", "ities", "feel", "each", "friend", "sub", "though", "saw", "ks", "hund", "hundred", "times", "lar", "ff", "amer", "scho", "sci", "ors", "lt", "arch", "fact", "hal", "himself", "gener", "mean", "vol", "school", "ason", "fam", "ult", "mind", "itch", "ped", "home", "young", "took", "big", "love", "reg", "eng", "sure", "vent", "ls", "ot", "ince", "thous", "eight", "thousand", "better", "mom", "appe", "once", "ied", "mus", "stem", "sing", "ident", "als", "uh", "mem", "produ", "stud", "power", "atch", "bas", "father", "av", "nothing", "gir", "pect", "unt", "few", "kes", "eyes", "sk", "always", "ared", "toge", "stru", "together", "ics", "bus", "fort", "ween", "rep", "ically", "small", "ga", "mer", "ned", "ins", "between", "yet", "stre", "hard", "system", "course", "year", "cept", "publ", "sim", "sou", "ready", "follow", "present", "rel", "turned", "sw", "possi", "mother", "io", "bar", "ished", "dec", "ments", "pri", "next", "ross", "both", "ship", "ures", "americ", "eas", "asked", "iness", "serv", "ists", "ash", "uni", "build", "phone", "lau", "ctor", "belie", "change", "interest", "peri", "children", "thir", "lear", "plan", "import", "ational", "har", "ines", "dist", "selves", "city", "sen", "run", "law", "ghter", "proble", "woman", "done", "heart", "book", "aut", "ris", "lim", "looked", "vid", "fu", "bab", "ately", "ord", "ket", "oc", "doing", "area", "tech", "win", "name", "second", "certain", "pat", "lad", "quite", "told", "ific", "ative", "uring", "gg", "half", "reason", "moment", "ility", "ution", "shall", "aur", "enough", "idea", "open", "understand", "vie", "contin", "mal", "hor", "qui", "address", "heard", "help", "inst", "oney", "whole", "gover", "commun", "exam", "near", "didn", "logy", "oh", "tru", "lang", "restaur", "restaurant", "design", "ze", "talk", "leg", "line", "ank", "ond", "country", "ute", "howe", "hold", "live", "comple", "however", "ized", "ush", "seen", "bye", "fer", "ital", "women", "net", "state", "bur", "fac", "whe", "important", "dar", "nine", "sat", "fic", "known", "having", "against", "soon", "ety", "langu", "public", "sil", "best", "az", "knew", "black", "velo", "sort", "seven", "imag", "lead", "cap", "ask", "alth", "ature", "nam", "began", "white", "sent", "sound", "vir", "days", "anything", "yeah", "ub", "blo", "sun", "story", "dire", "money", "trans", "mil", "org", "grow", "cord", "pped", "cus", "spo", "sign", "beaut", "goodbye", "inde", "large", "question", "often", "hour", "que", "pur", "town", "ield", "coming", "door", "lig", "ling", "incl", "partic", "keep", "engl", "move", "later", "ants", "food", "lights", "bal", "words", "list", "aw", "allow", "aren", "pret", "tern", "today", "believe", "almost", "bir", "word", "possible", "ither", "case", "ried", "ural", "round", "twent", "develo", "plain", "ended", "iting", "chang", "sc", "boy", "gy", "since", "ones", "suc", "cas", "national", "plac", "teen", "pose", "started", "mas", "fi", "fif", "afr", "fully", "grou", "wards", "girl", "e", "t", "a", "o", "i", "n", "s", "h", "r", "l", "d", "u", "c", "m", "w", "f", "g", "y", "p", "b", "v", "k", "'", "x", "j", "q", "z", "-", "2", " "};
        int blank_idx =0;
        int space_idx = 998;
        int two_idx = 997;
        int argm[] = argmaxOneD(array,DIM1,DIM2);
        List<Integer> final_labels = new ArrayList<Integer>();

        for(int i=0;i<argm.length;i++)
        {
            if (argm[i] == two_idx )
            {
                if(!final_labels.isEmpty())
                {
                    final_labels.add(final_labels.get(final_labels.size()-1));
                    if(final_labels.get(final_labels.size()-1)!=space_idx)
                    {
                        final_labels.add(space_idx);
                    }
                }
                else
                {
                    final_labels.add(space_idx);
                }
            }

            if(argm[i] != blank_idx)
            {
                if(!final_labels.isEmpty())
                {
                    if(final_labels.get(final_labels.size()-1) != argm[i])
                    {
                        final_labels.add(argm[i]);
                    }
                }
                else
                {
                    final_labels.add(argm[i]);
                }
            }


        }

        String ans = "";
        for(int j=0;j<final_labels.size();j++)
        {
            ans=ans + labels[final_labels.get(j)];
        }
        return ans;
    }

}