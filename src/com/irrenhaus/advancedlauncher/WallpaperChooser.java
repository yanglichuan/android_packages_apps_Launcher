/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irrenhaus.advancedlauncher;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

import org.xmlpull.v1.XmlPullParserException;

import com.android.internal.util.XmlUtils;
import com.android.launcher.R;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;

public class WallpaperChooser extends Activity implements AdapterView.OnItemSelectedListener,
        OnClickListener {

    private static final Integer[] THUMB_IDS = {
	    R.drawable.wallpaper_skate_small,
	    R.drawable.wallpaper_cyan_small,
	    R.drawable.wallpaper_glass_small,
	    R.drawable.wallpaper_hazey_small,
	    R.drawable.wallpaper_brick_small
    };

    private static final Integer[] IMAGE_IDS = {
	    R.drawable.wallpaper_skate,
	    R.drawable.wallpaper_cyan,
	    R.drawable.wallpaper_glass,
	    R.drawable.wallpaper_hazey,
	    R.drawable.wallpaper_brick
    };

    private Gallery mGallery;
    private ImageView mImageView;
    private boolean mIsWallpaperSet;

    private BitmapFactory.Options mOptions;
    private Bitmap mBitmap;

    private ArrayList<Integer> mThumbs;
    private ArrayList<Integer> mImages;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        findWallpapers();

        setContentView(R.layout.wallpaper_chooser);

        mOptions = new BitmapFactory.Options();
        mOptions.inDither = false;
        mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        mGallery = (Gallery) findViewById(R.id.gallery);
        mGallery.setAdapter(new ImageAdapter(this));
        mGallery.setOnItemSelectedListener(this);
        mGallery.setCallbackDuringFling(false);

        Button b = (Button) findViewById(R.id.set);
        b.setOnClickListener(this);

        mImageView = (ImageView) findViewById(R.id.wallpaper);
    }

    private void findWallpapers() {
        mThumbs = new ArrayList<Integer>(THUMB_IDS.length + 4);
        Collections.addAll(mThumbs, THUMB_IDS);

        mImages = new ArrayList<Integer>(IMAGE_IDS.length + 4);
        Collections.addAll(mImages, IMAGE_IDS);

        final Resources resources = getResources();
        final String[] extras = resources.getStringArray(R.array.extra_wallpapers);
        final String packageName = getApplication().getPackageName();
        
        for (String extra : extras) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (res != 0) {
                final int thumbRes = resources.getIdentifier(extra + "_small",
                        "drawable", packageName);

                if (thumbRes != 0) {
                    mThumbs.add(thumbRes);
                    mImages.add(res);
                }
            }
        }
        
        final int xmlRes = resources.getIdentifier("extra_wallpapers", "xml", packageName);
        
        if(xmlRes != 0)
        {
        	final XmlResourceParser xml = resources.getXml(xmlRes);
        	
        	try {
				XmlUtils.beginDocument(xml, "wallpaper");
			} catch (XmlPullParserException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	
        	boolean readNext = true;
        	
        	while(readNext)
        	{
        		try {
					int event = xml.nextTag();
					String extra = xml.nextText();
					
					int res = resources.getIdentifier(extra, "drawable", packageName);
		            if (res != 0) {
		                final int thumbRes = resources.getIdentifier(extra + "_small",
		                        "drawable", packageName);

		                if (thumbRes != 0) {
		                    mThumbs.add(thumbRes);
		                    mImages.add(res);
		                }
		            }
		            else
		            	Log.d("WallpaperChooser", "Couldn't find wallpaper named "+extra);
				} catch (XmlPullParserException e) {
					readNext = false;
					Log.d("WallpaperChooser", e.getMessage());
				} catch (IOException e) {
					readNext = false;
					Log.d("WallpaperChooser", e.getMessage());
				}
        	}
        	
        	xml.close();
        }
        else
        	Log.d("WallpaperChooser", "Could not find XML resource extra_wallpapers!");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsWallpaperSet = false;
    }

    public void onItemSelected(AdapterView parent, View v, int position, long id) {
        final ImageView view = mImageView;
        Bitmap b = BitmapFactory.decodeResource(getResources(), mImages.get(position), mOptions);
        view.setImageBitmap(b);

        // Help the GC
        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mBitmap = b;

        final Drawable drawable = view.getDrawable();
        drawable.setFilterBitmap(true);
        drawable.setDither(true);
    }

    /*
     * When using touch if you tap an image it triggers both the onItemClick and
     * the onTouchEvent causing the wallpaper to be set twice. Ensure we only
     * set the wallpaper once.
     */
    private void selectWallpaper(int position) {
        if (mIsWallpaperSet) {
            return;
        }

        mIsWallpaperSet = true;
        try {
            InputStream stream = getResources().openRawResource(mImages.get(position));
            setWallpaper(stream);
            setResult(RESULT_OK);
            finish();
        } catch (IOException e) {
            Log.e(AdvancedLauncher.LOG_TAG, "Failed to set wallpaper: " + e);
        }
    }

    public void onNothingSelected(AdapterView parent) {
    }

    private class ImageAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater;

        ImageAdapter(WallpaperChooser context) {
            mLayoutInflater = context.getLayoutInflater();
        }

        public int getCount() {
            return mThumbs.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView image;

            if (convertView == null) {
                image = (ImageView) mLayoutInflater.inflate(R.layout.wallpaper_item, parent, false);
            } else {
                image = (ImageView) convertView;
            }

            image.setImageResource(mThumbs.get(position));
            image.getDrawable().setDither(true);
            return image;
        }
    }

    public void onClick(View v) {
        selectWallpaper(mGallery.getSelectedItemPosition());
    }
}