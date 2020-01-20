package com.by5388.pdf.reader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * @author Administrator  on 2020/1/19.
 */
public class PdfFragment2 extends Fragment {
    private static final String TAG = PdfFragment2.class.getSimpleName();
    private static final String PDF_FILE_URI = "pad_file_uri";
    private boolean mLoadedFile = false;
    private Uri mUri;

    private Toast mToast;
    /**
     * TODO 需要熟悉这个类
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


    public static PdfFragment2 newInstance(final Uri uri) {
        final PdfFragment2 fragment = new PdfFragment2();
        final Bundle args = new Bundle();
        args.putString(PDF_FILE_URI, uri.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = Uri.parse(arguments.getString(PDF_FILE_URI, null));

        }
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

        mHandler.post(() -> openRenderer(pageIndex));
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
    private void openRenderer(final int page) {
        try {
//            mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            final Context context = Objects.requireNonNull(getContext());
            mFileDescriptor = context.getContentResolver().openFileDescriptor(mUri, "r");
            if (mFileDescriptor == null) {
                Log.e(TAG, "openRenderer: ", new Exception("mFileDescriptor == null"));
                return;
            }
            // This is the PdfRenderer we use to render the PDF.
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
            mLoadedFile = true;
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
//            final Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(), Bitmap.Config.ARGB_8888);
            final Bitmap bitmap = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(), Bitmap.Config.ARGB_8888);
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
