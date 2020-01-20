package com.by5388.pdf.reader;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {
    public static final String PDF_FILE_TYPE = "application/pdf";
    public static final String TAG_PDF_FRAGMENT = "pdf_fragment";
    public static final String TAG = "MainActivity";
    private static final int ACTION_PICK_PDF = 100;

    public static Intent newIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button_get_pdf_file);
        button.setOnClickListener(v -> getPdfFile());
    }

    private void getPdfFile() {
        try {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(PDF_FILE_TYPE);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "选择要打开的pdf文件"), ACTION_PICK_PDF);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "getPdfFile: ", e);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != StartActivity.RESULT_OK) {
            return;
        }
        if (requestCode == ACTION_PICK_PDF) {
            handleResult(data);
        }
    }

    private void handleResult(Intent result) {
        if (result == null) {
            return;
        }
        Log.d(TAG, "onActivityResult: data = " + result);

        final Uri uri = result.getData();
        final String type = result.getType();
        if (PDF_FILE_TYPE.equals(type)) {
            if (uri == null) {
                Log.e(TAG, "handleResult: uri == null");
                return;
            }


            Log.d(TAG, "onActivityResult: uri = " + uri);
            final String scheme = uri.getScheme();
            if ("content".equals(scheme)) {
                Cursor cursor = this.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    try {
                        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        // 取出文件路径
                        String filePath = cursor.getString(column_index);
                        Log.d(TAG, "onActivityResult: filePath = " + filePath);

                        final File file = new File(filePath);
                        if (filePath.endsWith(".pdf") && file.exists() && file.isFile()) {
                            if (true) {
                                showPDF2(uri);
                            } else {
                                showPDF(filePath);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } finally {
                        cursor.close();
                    }
                }
            }
            final String authority = uri.getAuthority();
            final String lastPathSegment = uri.getLastPathSegment();
            Log.d(TAG, "onActivityResult: authority = " + authority);
            Log.d(TAG, "onActivityResult: scheme = " + scheme);
            Log.d(TAG, "onActivityResult: lastPathSegment = " + lastPathSegment);
        } else {
            Log.d(TAG, "onActivityResult: error file type");
            Toast.makeText(this, "选择的文件不是PDF，请重新选择", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPDF(String filePath) {

        final FragmentManager supportFragmentManager = getSupportFragmentManager();
        final Fragment fragment = supportFragmentManager.findFragmentByTag(TAG_PDF_FRAGMENT);
        final PdfFragment newFragment = PdfFragment.newInstance(filePath);
        if (fragment == null) {
            supportFragmentManager.beginTransaction().add(R.id.container, newFragment, TAG_PDF_FRAGMENT).commit();
        } else {
            supportFragmentManager.beginTransaction().replace(R.id.container, newFragment, TAG_PDF_FRAGMENT).commit();
        }

    }

    private void showPDF2(final Uri uri) {

        final FragmentManager supportFragmentManager = getSupportFragmentManager();
        final Fragment fragment = supportFragmentManager.findFragmentByTag(TAG_PDF_FRAGMENT);
        final PdfFragment2 newFragment = PdfFragment2.newInstance(uri);
        if (fragment == null) {
            supportFragmentManager.beginTransaction().add(R.id.container, newFragment, TAG_PDF_FRAGMENT).commit();
        } else {
            supportFragmentManager.beginTransaction().replace(R.id.container, newFragment, TAG_PDF_FRAGMENT).commit();
        }

    }
}
