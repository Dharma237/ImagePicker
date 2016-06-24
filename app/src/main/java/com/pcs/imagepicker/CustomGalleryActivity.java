package com.pcs.imagepicker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CustomGalleryActivity extends Activity {

    @Bind(R.id.gridGallery)
    protected GridView gridGallery;

    protected Handler handler;

    protected GalleryAdapter adapter;

    @Bind(R.id.imgNoMedia)
    protected ImageView imgNoMedia;

    @Bind(R.id.btnGalleryOk)
    protected Button btnGalleryOk;

    String action;

    private ImageLoader imageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.gallery);
        ButterKnife.bind(this);
        action = getIntent().getAction();
        if (action == null) {
            finish();
        }
        initImageLoader();
        init();
    }

    private void initImageLoader() {
        try {
            String CACHE_DIR = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/.temp_tmp";

            new File(CACHE_DIR).mkdirs();

            File cacheDir = StorageUtils.getOwnCacheDirectory(getBaseContext(),
                    CACHE_DIR);

            DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                    .cacheOnDisc(true).imageScaleType(ImageScaleType.EXACTLY)
                    .bitmapConfig(Bitmap.Config.RGB_565).build();
            ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(
                    getBaseContext())
                    .defaultDisplayImageOptions(defaultOptions)
                    .discCache(new UnlimitedDiscCache(cacheDir))
                    .memoryCache(new WeakMemoryCache());

            ImageLoaderConfiguration config = builder.build();
            imageLoader = ImageLoader.getInstance();
            imageLoader.init(config);

        } catch (Exception e) {

        }
    }

    private void init() {

        handler = new Handler();
        gridGallery.setFastScrollEnabled(true);
        adapter = new GalleryAdapter(getApplicationContext(), imageLoader);
        PauseOnScrollListener listener = new PauseOnScrollListener(imageLoader,
                true, true);
        gridGallery.setOnScrollListener(listener);

        if (action.equalsIgnoreCase(Action.ACTION_MULTIPLE_PICK)) {

            findViewById(R.id.llBottomContainer).setVisibility(View.VISIBLE);
            gridGallery.setOnItemClickListener(mItemMulClickListener);
            adapter.setMultiplePick(true);

        } else if (action.equalsIgnoreCase(Action.ACTION_PICK)) {

            findViewById(R.id.llBottomContainer).setVisibility(View.GONE);
            gridGallery.setOnItemClickListener(mItemSingleClickListener);
            adapter.setMultiplePick(false);

        }

        gridGallery.setAdapter(adapter);

        new Thread() {

            @Override
            public void run() {
                Looper.prepare();
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        adapter.addAll(getGalleryPhotos());
                        checkImageStatus();
                    }
                });
                Looper.loop();
            }

            ;

        }.start();

    }

    private void checkImageStatus() {
        if (adapter.isEmpty()) {
            imgNoMedia.setVisibility(View.VISIBLE);
        } else {
            imgNoMedia.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.btnGalleryOk)
    public void galleryOKClick() {
        ArrayList<CustomGallery> selected = adapter.getSelected();

        String[] allPath = new String[selected.size()];
        for (int i = 0; i < allPath.length; i++) {
            allPath[i] = selected.get(i).sdcardPath;
        }

        Intent data = new Intent().putExtra("all_path", allPath);
        setResult(RESULT_OK, data);
        finish();
    }

    AdapterView.OnItemClickListener mItemMulClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> l, View v, int position, long id) {
            adapter.changeSelection(v, position);

        }
    };

    AdapterView.OnItemClickListener mItemSingleClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> l, View v, int position, long id) {
            CustomGallery item = adapter.getItem(position);
            Intent data = new Intent().putExtra("single_path", item.sdcardPath);
            setResult(RESULT_OK, data);
            finish();
        }
    };

    public File getRootFolder() {
        //return new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"gallery").getAbsolutePath();
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "APWRD");

    }

    private ArrayList<CustomGallery> getGalleryPhotos() {
        ArrayList<CustomGallery> galleryList = new ArrayList<>();

        //root folder file
        File file = getRootFolder();

        //check file is existed orr not
        //If file is  not existed create new One
        if (!file.exists()) {
            file.mkdirs();
        }

        //array of files in the specified folder
        File[] listOfFiles = file.listFiles();

        if (listOfFiles != null && listOfFiles.length >= 1) {

            //converting to list of files
            List<File> filesList = Arrays.asList(listOfFiles);

            for (File temp : filesList) {
                CustomGallery item = new CustomGallery();
                item.sdcardPath = temp.getAbsolutePath();
                item.name = temp.getName();
                galleryList.add(item);

            }
        }


//		try {
//			final String[] columns = { MediaStore.Images.Media.DATA,
//					MediaStore.Images.Media._ID };
//			final String orderBy = MediaStore.Images.Media._ID;
//
//			Cursor imagecursor = managedQuery(
//					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
//					null, null, orderBy);
//
//			if (imagecursor != null && imagecursor.getCount() > 0) {
//
//				while (imagecursor.moveToNext()) {
//					CustomGallery item = new CustomGallery();
//
//					int dataColumnIndex = imagecursor
//							.getColumnIndex(MediaStore.Images.Media.DATA);
//
//					item.sdcardPath = imagecursor.getString(dataColumnIndex);
//
//					galleryList.add(item);
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

        // show newest photo at beginning of the list
        Collections.reverse(galleryList);
        return galleryList;
    }

}
