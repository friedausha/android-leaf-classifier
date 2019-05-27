package com.example.leaf;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.leaf.Response.ResponseApi;
import com.example.leaf.Retrofit.ServerBank.ApiClientAttendance;
import com.example.leaf.Retrofit.ServerBank.Server;
import com.example.leaf.Utility.Constans;
import com.example.leaf.Utility.PermissionsDelegate;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.fotoapparat.Fotoapparat;
import io.fotoapparat.error.CameraErrorListener;
import io.fotoapparat.exception.camera.CameraException;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.result.BitmapPhoto;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.result.WhenDoneListener;
import io.fotoapparat.view.CameraView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static io.fotoapparat.log.LoggersKt.fileLogger;
import static io.fotoapparat.log.LoggersKt.logcat;
import static io.fotoapparat.log.LoggersKt.loggers;
import static io.fotoapparat.result.transformer.ResolutionTransformersKt.scaled;
import static io.fotoapparat.selector.LensPositionSelectorsKt.back;

public class PredictActivity extends AppCompatActivity {
    private static final String LOGGING_TAG = "New Upload Activity";

    private final PermissionsDelegate permissionsDelegate = new PermissionsDelegate(this);
    private boolean hasCameraPermission;
    private CameraView cameraView;
    private Button capture;
    private Fotoapparat fotoapparat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);

        cameraView = findViewById(R.id.camera_view);
        capture = findViewById(R.id.btn_capture);
        hasCameraPermission = permissionsDelegate.hasCameraPermission();

        if (hasCameraPermission) {
            cameraView.setVisibility(View.VISIBLE);
        } else {
            permissionsDelegate.requestCameraPermission();
        }

        fotoapparat = createFotoapparat();
        takePictureOnClick();
    }
    private Fotoapparat createFotoapparat() {
        return Fotoapparat
                .with(this)
                .into(cameraView)
                .previewScaleType(ScaleType.CenterCrop)
                .lensPosition(back())
                .logger(loggers(
                        logcat(),
                        fileLogger(this)
                ))
                .cameraErrorCallback(new CameraErrorListener() {
                    @Override
                    public void onError(@NotNull CameraException e) {
                        Toast.makeText(PredictActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                    }
                })
                .build();
    }

    private void takePictureOnClick() {
        capture.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.FROYO)
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    private void takePicture() {
        PhotoResult photoResult = fotoapparat.takePicture();

        photoResult.saveToFile(new File(
                getExternalFilesDir("photos"),
                "photo.jpg"
        ));

        photoResult
                .toBitmap(scaled(0.25f))
                .whenDone(new WhenDoneListener<BitmapPhoto>() {
                    @Override
                    public void whenDone(@Nullable BitmapPhoto bitmapPhoto) {
                        if (bitmapPhoto == null) {
                            Log.e(LOGGING_TAG, "Couldn't capture photo.");
                            return;
                        }
                        Log.d("cek", "whenDone: "+bitmapPhoto);
                        ImageView imageView = findViewById(R.id.gambar);
                        Bitmap bitmap1 = RotateBitmap(bitmapPhoto.bitmap,-bitmapPhoto.rotationDegrees+180);
                        bitmap1 = flip(bitmap1);
                        send(bitmap1);
                    }
                });
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public Bitmap flip(Bitmap bitmap) {
        Bitmap bOutput;
        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        bOutput = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bOutput;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasCameraPermission) {
            fotoapparat.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (hasCameraPermission) {
            fotoapparat.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsDelegate.resultGranted(requestCode, permissions, grantResults)) {
            hasCameraPermission = true;
            fotoapparat.start();
            cameraView.setVisibility(View.VISIBLE);
        }
    }

    public void send(Bitmap bitmap) {
        Bitmap result = Bitmap.createScaledBitmap(bitmap, 96,96, true);
        String myBase64Image = Constans.encodeToBase64(result, Bitmap.CompressFormat.JPEG, 100);

        Call<ResponseApi> predict;
        ApiClientAttendance api = Server.builder().create(ApiClientAttendance.class);
        predict = api.predict("data:image/jpeg;base64,"+myBase64Image);

        final SweetAlertDialog pDialog = new SweetAlertDialog(PredictActivity.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setTitleText("Loading");
        pDialog.setCancelable(false);
        pDialog.show();

        predict.enqueue(new Callback<ResponseApi>() {
            @Override
            public void onResponse(Call<ResponseApi> call, Response<ResponseApi> response) {
                if (response.code() == 200)
                {
                    pDialog.dismiss();

                    String r = response.body().getMsg();
                    String[] result = r.trim().toString().split(",");

                    if (result[0].equals("ACCEPTED")) {
                        new SweetAlertDialog(PredictActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("Hasil")
                                .setContentText(response.body().getMsg())
                                .setConfirmText("OK")
                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sDialog) {
                                        sDialog.dismiss();
                                    }
                                }).show();
                    } else {
                        new SweetAlertDialog(PredictActivity.this, SweetAlertDialog.WARNING_TYPE)
                                .setTitleText("Error")
                                .setContentText(response.body().getMsg())
                                .setConfirmText("OK")
                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sDialog) {
                                        sDialog.dismiss();
                                    }
                                }).show();
                    }
                }
                else
                {
                    pDialog.dismiss();
                    new SweetAlertDialog(PredictActivity.this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Error")
                            .setContentText("Terjadi kesalahan, mohon ulangi kembali.")
                            .setConfirmText("OK")
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    sDialog.dismiss();
                                }
                            }).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseApi> call, Throwable t) {
                pDialog.dismiss();
                new SweetAlertDialog(PredictActivity.this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText("Terdapat masalah dengan koneksi anda, mohon ulangi kembali.")
                        .setConfirmText("OK")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.dismissWithAnimation();
                            }
                        }).show();
            }
        });
    }
}
