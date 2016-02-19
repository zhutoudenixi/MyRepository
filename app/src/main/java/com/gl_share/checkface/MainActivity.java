package com.gl_share.checkface;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@ContentView(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {
    @ViewInject(R.id.main_et)
    private EditText editText;
    @ViewInject(R.id.main_taktPhoto_btn)
    private Button takePhotoButton;
    @ViewInject(R.id.main_start_btn)
    private Button startButton;
    @ViewInject(R.id.main_tv)
    private TextView textView;
    @ViewInject(R.id.main_iv)
    private ImageView imageView;
    @ViewInject(R.id.takePhoto_sv)
    private SurfaceView surfaceView;
    @ViewInject(R.id.takePhoto_btn)
    private Button btn;
    @ViewInject(R.id.main_rl)
    private RelativeLayout relativeLayout;

    private Camera camera;
    private String strName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewUtils.inject(this);

        surfaceView.getHolder().addCallback(new MyCallback());
    }

    /**
     * SurfaceView回调方法
     */
    class MyCallback implements SurfaceHolder.Callback{

        void setCamera(){
            Camera.Parameters parameters = camera.getParameters();

            List<Camera.Size> list = parameters.getSupportedPictureSizes();

            parameters.setPictureSize(list.get(0).width, list.get(0).height);

            parameters.setPictureFormat(ImageFormat.JPEG);

            camera.setParameters(parameters);
        }
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            camera = Camera.open();

            try {
                camera.setPreviewDisplay(holder);

                setCamera();

                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            camera.autoFocus(null);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if(camera != null){
                camera.release();
                camera = null;
            }
        }
    }

    /**
     * 上传按钮的点击事件
     * @param view
     */
    @OnClick(R.id.main_taktPhoto_btn)
    public void upLoad(View view){
        relativeLayout.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        startButton.setVisibility(View.GONE);
    }

    /**
     * 拍照按钮的点击事件
     * @param view
     */
    @OnClick(R.id.takePhoto_btn)
    public void takePhoto(View view){
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                //TODO  旋转图片
                Bitmap bitmapRet = rotateBitmap(bitmap);

                //TODO 保存图片
                String bitName = saveBitmap(bitmapRet);

                relativeLayout.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                textView.setVisibility(View.VISIBLE);
                startButton.setVisibility(View.VISIBLE);

                editText.setText(bitName);
                imageView.setImageBitmap(bitmapRet);
            }
        });
    }

    /**
     * 保存图片
      * @param bitmap   经过处理的图片
     * @return  图片名
     */
    protected String saveBitmap(Bitmap bitmap){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
        String strData = sdf.format(new Date());
        strName = "/sdcard/" + strData + ".jpg";

        File file = new File(strName);

        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

            bitmap.compress(Bitmap.CompressFormat.JPEG,
                    100,
                    bos);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return strName;
    }

    /**
     * 旋转图片
     * @param bitmap    拍下的图片
     * @return  旋转后的图片
     */
    protected Bitmap rotateBitmap(Bitmap bitmap){
        Bitmap bitmapRet = null;

        Matrix matrix = new Matrix();
        bitmapRet = Bitmap.createBitmap(bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true);

        return bitmapRet;
    }

    /**
     * 开始识别按钮的点击事件
     * @param view
     */
    @OnClick(R.id.main_start_btn)
    public void startCheck(View view){
        String strUrl = "http://api.avatardata.cn/Face/CheckOne";
        String strKey = "";
        String strBase64 = "";
        String strImgUrl = "";
        String strFileContent = "";

        try {
            FileInputStream fis = new FileInputStream(new File(strName));
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            strBase64 = new String(Base64.encode(bytes, Base64.DEFAULT));
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestParams requestParams = new RequestParams();
        requestParams.addBodyParameter("key", strKey);
        requestParams.addBodyParameter("strIMGURL", strImgUrl);
        requestParams.addBodyParameter("Base64", strBase64);
        requestParams.addBodyParameter("fileContent", strFileContent);

        MyApplication.httpUtils.send(HttpRequest.HttpMethod.POST,
                strUrl,
                requestParams,
                new RequestCallBack<String>() {
                    @Override
                    public void onSuccess(ResponseInfo<String> responseInfo) {

                    }

                    @Override
                    public void onFailure(HttpException error, String msg) {

                    }
                });
    }
}
