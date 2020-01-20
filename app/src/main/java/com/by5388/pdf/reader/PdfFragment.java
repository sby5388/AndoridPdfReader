package com.by5388.pdf.reader;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * @author Administrator  on 2020/1/19.
 */
public class PdfFragment extends Fragment {
    public static final String PDF_FILE_PATH = "pdfFilePath";
    private boolean mLoadedFile = false;
    private String mFilePath;

    private Toast mToast;
    /**
     * File descriptor of the PDF.
     */
    private ParcelFileDescriptor mFileDescriptor;
    /**
     * pdf 渲染器/加载器
     */
    private PdfRenderer mPdfRenderer;


    private PdfRenderer.Page mCurrentPage;

    private Button mButtonNext;
    private Button mButtonPre;
    private ImageView mImageView;
    private Handler mHandler = new Handler();

    /**
     * Key string for saving the state of current page index.
     */
    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";


    public static PdfFragment newInstance(final String pdfFilePath) {
        final PdfFragment fragment = new PdfFragment();
        final Bundle args = new Bundle();
        if (!TextUtils.isEmpty(pdfFilePath)) {
            args.putString(PDF_FILE_PATH, pdfFilePath);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if (arguments != null) {
            mFilePath = arguments.getString(PDF_FILE_PATH, null);

        }
        //openRenderer();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pdf_reader, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mButtonNext = view.findViewById(R.id.button_next_page);
        mButtonPre = view.findViewById(R.id.button_pre_page);
        mImageView = view.findViewById(R.id.image_view);
        mButtonPre.setOnClickListener(v -> toPrePage());
        mButtonNext.setOnClickListener(v -> toNextPage());

        // Show the first page by default.
        int index = 0;
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            index = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0);
        }
        final int pageIndex = index;

        ReaderApp.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File originFile = new File(mFilePath);
                    final File cacheFile = new File(ReaderApp.getInstance().getCacheDir(), originFile.getName());
                    if (!cacheFile.exists()) {
                        final FileInputStream fileInputStream = new FileInputStream(originFile);
                        final FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
                        final byte[] buffer = new byte[1024];
                        int size;
                        while ((size = fileInputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, size);
                        }
                        fileInputStream.close();
                        fileOutputStream.close();
                        mHandler.post(() -> openRenderer(cacheFile, pageIndex));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }


            }
        });


        //showPage(index);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentPage != null) {
            //保存当前的页数
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage.getIndex());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeRenderer();
    }

    /**
     * renderer:渲染器
     * openRenderer:打开渲染器
     */
    private void openRenderer(File file, int page) {

        if (file == null || !file.exists()) {
            toast("文件不存在");
            return;
        }
        try {
            mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            // This is the PdfRenderer we use to render the PDF.
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
            mLoadedFile = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        showPage(page);
    }


    private void toast(String s) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
        mToast.show();

    }

    /**
     * 下一页
     */
    private void toNextPage() {
        if (!mLoadedFile) {
            return;
        }
        showPage(mCurrentPage.getIndex() + 1);
    }

    /**
     * 上一页
     */
    private void toPrePage() {
        if (!mLoadedFile) {
            return;
        }
        showPage(mCurrentPage.getIndex() - 1);
    }

    private void showPage(int page) {
        if (!mLoadedFile) {
            return;
        }
        // TODO: 2020/1/19
        if (mPdfRenderer.getPageCount() <= page) {
            return;
        }

        if (mCurrentPage != null) {
            mCurrentPage.close();
        }

        try {
            mCurrentPage = mPdfRenderer.openPage(page);
            // TODO: 2020/1/19 最后一个参数的含义
            // Important: the destination bitmap must be ARGB (not RGB).
            final Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(), Bitmap.Config.ARGB_8888);
            // Here, we render the page onto the Bitmap.
            // To render a portion of the page, use the second and third parameter. Pass nulls to get
            // the default result.
            // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
            mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            mImageView.setImageBitmap(bitmap);
            updateUI();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeRenderer() {
        if (!mLoadedFile) {
            return;
        }
        if (mCurrentPage != null) {
            mCurrentPage.close();
        }
        mPdfRenderer.close();
        try {
            mFileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        final int index = mCurrentPage.getIndex();
        final int pageCount = mPdfRenderer.getPageCount();
        mButtonNext.setEnabled(index + 1 < pageCount);
        mButtonPre.setEnabled(0 != index);
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.setTitle(getString(R.string.app_name_with_index, index + 1, pageCount));
    }


}
