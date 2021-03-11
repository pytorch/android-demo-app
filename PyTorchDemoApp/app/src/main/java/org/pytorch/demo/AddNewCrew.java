package org.pytorch.demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.util.Util;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.pytorch.demo.Utils.rotateImage;

public class AddNewCrew extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_crew);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        init_button();


    }
    private String crewID;
    private Bitmap crewFace;

    private final String TAG = getClass().getSimpleName();
    private static final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
    private static final int REQUEST_PERMISSION_CODE = 267;
    private static final int TAKE_PHOTO = 189;
    private static final int CHOOSE_PHOTO = 385;
    private static final String FILE_PROVIDER_AUTHORITY = Utils.FILE_PROVIDER_AUTHORITY;
    private Uri mImageUri, mImageUriFromFile;
    private File imageFile;

    private void init_button()
    {
        findViewById(R.id.btn_set_face).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(intent, CODE_SYSTEM_CAMERA);
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(PERMISSIONS, REQUEST_PERMISSION_CODE);
                }else if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(PERMISSIONS, REQUEST_PERMISSION_CODE);
                }else
                    takePhoto();
            }
        });

        findViewById(R.id.btn_select_face).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAlbum();
            }
        });

        findViewById(R.id.btn_return).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btn_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AppCompatEditText textInputEditText = findViewById(R.id.input_id);
                crewID = textInputEditText.getText().toString();
                if(crewFace == null || crewID.equals("")){
                    Toast.makeText(AddNewCrew.this,"没有设置人脸或id"+crewID, Toast.LENGTH_SHORT).show();
                }
                else{
                    //TODO 1. read model 2. calculate encoding of picture 3.add face encoding and id to a new json

                    // 1. read model
                    Module mModule = null;
                    Module encoder = null;
                    if (mModule == null) {
                        try {
                            final String moduleFileAbsoluteFilePath = new File(
                                    Utils.assetFilePath(AddNewCrew.this, "mobile_model2.pt")).getAbsolutePath();
                            mModule = Module.load(moduleFileAbsoluteFilePath);
                            final String encoderFileAbsoluteFilePath = new File(
                                    Utils.assetFilePath(AddNewCrew.this, "encoder1.pt")).getAbsolutePath();
                            encoder = Module.load(encoderFileAbsoluteFilePath);
                        }catch (Exception e)
                        {e.printStackTrace();}
                    }

                    // 2. extract face from picture
                    Bitmap bitmap = crewFace;
                    float [] mean = new float[] {0.49804f, 0.49804f, 0.49804f};
                    float [] std = new float[] {0.501960f, 0.501960f, 0.501960f};
                    float nms_threshold = 0.4f;
                    int w1 = bitmap.getWidth();
                    int h1 = bitmap.getHeight();

                    Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap,480,360,true);

                    Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap1, mean, std);
                    System.out.println(Arrays.toString(inputTensor.shape()));
                    // running the model
                    final IValue[] output = mModule.forward(IValue.from(inputTensor)).toTuple();
                    Tensor scores = output[0].toTensor();
                    Tensor boxes = output[1].toTensor();
                    float threshold = 0.8f;

                    ArrayList<Float> possible_indexes = possible_score(scores, boxes, threshold);
                    System.out.println("in onCreate len possible_indexes " + possible_indexes.size());

                    ArrayList<float[]> nms_boxes = nms(boxes, scores, possible_indexes, nms_threshold);
                    float ratio = 0.1f;
                    nms_boxes = expand_box(nms_boxes, ratio);

                    if (nms_boxes.size() > 0){

                        if (nms_boxes.size() > 1){
                            Toast.makeText(AddNewCrew.this, "图片中检测到过多的人脸信息", Toast.LENGTH_LONG).show();
                            return;
                        }

                        float[] box_c = nms_boxes.get(0);
                        Bitmap bitmap_c = cropBitmap(bitmap, box_c);

                        File bitmap_file = new File(new Util().project_path, "tmp.jpg");
                        if (!bitmap_file.exists()) {
                            try {
                                bitmap_file.createNewFile();
                                FileOutputStream fileOutputStream = new FileOutputStream(bitmap_file);
                                bitmap_c.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            } catch (IOException exception) {
                                exception.printStackTrace();
                            }
                        }



                        Utils.NamedEmbedding unnamed = encode_face(bitmap_c, encoder);
                        unnamed.id = crewID;

                        // 3. save embedding and id to local file
                        boolean result = new Util().save_embedding_local_file(unnamed);
                        if (result){
                            Toast.makeText(AddNewCrew.this,"已添加到本地库中"+crewID, Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(AddNewCrew.this,"添加到本地库失败"+crewID, Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        Toast.makeText(AddNewCrew.this, "图片中没有检测到人脸信息", Toast.LENGTH_LONG).show();
                        return;
                    }


                }

            }
        });
    }


    private Bitmap cropBitmap(Bitmap bitmap, float[] rect) {
        try{
            int x,y,w,h;
            x = (int) (bitmap.getWidth() * rect[0]);
            y = (int) (bitmap.getHeight() * rect[1]);

            w = (int) (bitmap.getWidth() * (rect[2]-rect[0]));
            h = (int) (bitmap.getHeight() * (rect[3]-rect[1]));

            return Bitmap.createBitmap(bitmap, x, y, w, h,null, false);
        }catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }
        return null;

    }

    private Utils.NamedEmbedding encode_face(Bitmap bitmap_c, Module encoder){
        final long startTime = SystemClock.elapsedRealtime();

        float [] mean = new float[] {0.49804f, 0.49804f, 0.49804f};
        float [] std = new float[] {0.501960f, 0.501960f, 0.501960f};

        Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap_c,160,160,true);
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap1, mean, std);
        System.out.println(Arrays.toString(inputTensor.shape()));
        final long moduleForwardStartTime = SystemClock.elapsedRealtime();
        final Tensor outputTensor = encoder.forward(IValue.from(inputTensor)).toTensor();
//        final IValue[] output = encoder.forward(IValue.from(inputTensor)).toTuple();
        final long moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;
        System.out.println("output tensor shape is "+ Arrays.toString(outputTensor.shape()));
        Utils.NamedEmbedding unNamedEmbedding = new Utils.NamedEmbedding();

        // put encoded face into unNamedEmbedding
        float[] tensorDataAsFloatArray = outputTensor.getDataAsFloatArray();
        for (int i = 0; i< 512; i++)
            unNamedEmbedding.embedding[i] = tensorDataAsFloatArray[i];
        return unNamedEmbedding;

    }

    public ArrayList<Float> possible_score(Tensor scores, Tensor boxes, float threshold)
    {
        ArrayList<Float> list_index_prob = new ArrayList<>();
        float[] floatArray = scores.getDataAsFloatArray();
        int len = floatArray.length;
        long num = scores.shape()[2];
        len = (int)(len / num);
        System.out.println(len);


        for (int i = 0; i < len; i++)
        {
            for (int j = 1; j < num; j++)
                if (floatArray[(int) (i * num + j)] > threshold)
                {
                    list_index_prob.add((float)i);
                    list_index_prob.add(floatArray[(int) (i * num + j)]);
                    System.out.println("porb is " + floatArray[(int) (i * num)] + " and " + floatArray[(int) (i * num + 1)]);
                }

        }

        return list_index_prob;
    }

    private ArrayList<float[]> expand_box(ArrayList<float[]> boxes, float ratio)
    {
        for (int i = 0; i < boxes.size(); i++)
        {
            float w = boxes.get(i)[2] - boxes.get(i)[0];
            float h = boxes.get(i)[3] - boxes.get(i)[1];

            if (boxes.get(i)[0] - w * ratio > 0)
                boxes.get(i)[0] -= w*ratio;
            if (boxes.get(i)[1] - h * ratio > 0)
                boxes.get(i)[1] -= h*ratio;
            if (boxes.get(i)[2] + w * ratio < 1)
                boxes.get(i)[2] += w*ratio;
            if (boxes.get(i)[3] + w * ratio < 1)
                boxes.get(i)[3] += w*ratio;
        }
        return boxes;
    }

    float IoU(float[] rec1, float[] rec2)
    {

        float S_rec1 = (rec1[2] - rec1[0]) * (rec1[3] - rec1[1]);
        float S_rec2 = (rec2[2] - rec2[0]) * (rec2[3] - rec2[1]);

//    computing the sum_area
        float sum_area = S_rec1 + S_rec2;

//     find the each edge of intersect rectangle
        float left_line = max(rec1[1], rec2[1]);
        float right_line = min(rec1[3], rec2[3]);
        float top_line = max(rec1[0], rec2[0]);
        float bottom_line = min(rec1[2], rec2[2]);

//     judge if there is an intersect
        float intersect = 0;
        if (left_line >= right_line || top_line >= bottom_line)
            return 0;
        else
            intersect = (right_line - left_line) * (bottom_line - top_line);
        return (intersect / (sum_area - intersect));
    }

    ArrayList<float[]> nms(Tensor boxes, Tensor scores, ArrayList<Float> possible_indexes, float nms_threshold)
    {
//        float[] sfloatArray = scores.getDataAsFloatArray();
//        int slen = sfloatArray.length;
//        long snum = scores.shape()[2];
//        slen = (int)(slen / snum);

        ArrayList<float[]> nms_boxes = new ArrayList<>();
        float[] bfloatArray = boxes.getDataAsFloatArray();
        int blen = bfloatArray.length;
        int bnum = (int) boxes.shape()[2];
        blen = (blen / bnum);


        float[] box2 = {1,1,1,1,1};
        for(int i = 0; i < possible_indexes.size() / 2; i++)
        {
            float[] box1 = {0,0,0,0,0};
            int index = (int) (float) possible_indexes.get(i * 2);
            for(int j = 0; j < bnum; j++)
            {
                box1[j] = bfloatArray[index * bnum + j];
            }
            box1[bnum] = possible_indexes.get(i * 2 + 1);
            boolean flag = true;
            for(int j = 0; j < nms_boxes.size(); j++)
            {
                box2 = nms_boxes.get(j);
                if(IoU(box1, box2) > nms_threshold) {
                    if (box2[bnum] > box1[bnum]) { //prob of box2 > box1
                        nms_boxes.remove(j);
                        nms_boxes.add(box1);
                        flag = false;
                    } else {
                        flag = false;
                        break;
                    }
                }
            }
            if (flag)
                nms_boxes.add(box1);


        }
        return nms_boxes;
    }
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == CODE_SYSTEM_CAMERA && resultCode == RESULT_OK) {
//            /*缩略图信息是储存在返回的intent中的Bundle中的，
//             * 对应Bundle中的键为data，因此从Intent中取出
//             * Bundle再根据data取出来Bitmap即可*/
//            Bundle extras = data.getExtras();
//            crewFace = (Bitmap) extras.get("data");
//            ImageView imageView = findViewById(R.id.input_image);
//            imageView.setImageBitmap(crewFace);
//        }
//    }

    /**
     * 打开相册
     */
    private void openAlbum() {
        Intent openAlbumIntent = new Intent(Intent.ACTION_GET_CONTENT);
        openAlbumIntent.setType("image/*");
        startActivityForResult(openAlbumIntent, CHOOSE_PHOTO);//打开相册
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//打开相机的Intent
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {//这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
            imageFile = createImageFile();//创建用来保存照片的文件
            mImageUriFromFile = Uri.fromFile(imageFile);
            Log.i(TAG, "takePhoto: uriFromFile " + mImageUriFromFile);
            if (imageFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    /*7.0以上要通过FileProvider将File转化为Uri*/
                    mImageUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, imageFile);
                } else {
                    /*7.0以下则直接使用Uri的fromFile方法将File转化为Uri*/
                    mImageUri = Uri.fromFile(imageFile);
                }
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);//将用于输出的文件Uri传递给相机
                startActivityForResult(takePhotoIntent, TAKE_PHOTO);//打开相机
            }
        }
    }

    /**
     * 创建用来存储图片的文件，以时间来命名就不会产生命名冲突
     *
     * @return 创建的图片文件
     */
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = null;
        try {
            imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageFile;
    }

    /*申请权限的回调*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "onRequestPermissionsResult: permission granted");
            takePhoto();
        } else {
            Log.i(TAG, "onRequestPermissionsResult: permission denied");
            Toast.makeText(this, "You Denied Permission", Toast.LENGTH_SHORT).show();
        }
    }

    /*相机或者相册返回来的数据*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        /*如果拍照成功，将Uri用BitmapFactory的decodeStream方法转为Bitmap*/
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mImageUri));
                        ExifInterface ei = new ExifInterface(imageFile.getAbsolutePath());
                        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED);

                        Bitmap rotatedBitmap = null;
                        switch(orientation) {

                            case ExifInterface.ORIENTATION_ROTATE_90:
                                rotatedBitmap = rotateImage(bitmap, 90);
                                break;

                            case ExifInterface.ORIENTATION_ROTATE_180:
                                rotatedBitmap = rotateImage(bitmap, 180);
                                break;

                            case ExifInterface.ORIENTATION_ROTATE_270:
                                rotatedBitmap = rotateImage(bitmap, 270);
                                break;

                            case ExifInterface.ORIENTATION_NORMAL:
                            default:
                                rotatedBitmap = bitmap;
                        }
                        Log.i(TAG, "onActivityResult: imageUri " + mImageUri);
                        galleryAddPic(mImageUriFromFile);
                        ImageView imageView = findViewById(R.id.input_image);
                        imageView.setImageBitmap(rotatedBitmap);//显示到ImageView上
                        crewFace = bitmap;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if (data == null) {//如果没有选取照片，则直接返回
                    return;
                }
                Log.i(TAG, "onActivityResult: ImageUriFromAlbum: " + data.getData());
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        handleImageOnKitKat(data);//4.4之后图片解析
                    } else {
                        handleImageBeforeKitKat(data);//4.4之前图片解析
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 4.4版本以下对返回的图片Uri的处理：
     * 就是从返回的Intent中取出图片Uri，直接显示就好
     * @param data 调用系统相册之后返回的Uri
     */
    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    /**
     * 4.4版本以上对返回的图片Uri的处理：
     * 返回的Uri是经过封装的，要进行处理才能得到真实路径
     * @param data 调用系统相册之后返回的Uri
     */
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是document类型的Uri，则提供document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];//解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型的uri，则进行普通处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是file类型的uri，则直接获取路径
            imagePath = uri.getPath();
        }
        displayImage(imagePath);
    }

    /**
     * 将imagePath指定的图片显示到ImageView上
     */
    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            ImageView imageView = findViewById(R.id.input_image);
            crewFace = bitmap;
            imageView.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将Uri转化为路径
     * @param uri 要转化的Uri
     * @param selection 4.4之后需要解析Uri，因此需要该参数
     * @return 转化之后的路径
     */
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    /**
     * 将拍的照片添加到相册
     *
     * @param uri 拍的照片的Uri
     */
    private void galleryAddPic(Uri uri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uri);
        sendBroadcast(mediaScanIntent);
    }

}