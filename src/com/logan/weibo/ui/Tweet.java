package com.logan.weibo.ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.logan.R;
import com.logan.util.FileUtils;
import com.logan.util.ImageUtils;
import com.logan.util.MediaUtils;
import com.logan.util.StringUtils;
import com.logan.weibo.adapter.GridViewAdapter;
import com.logan.weibo.bean.BaseActivity;
import com.weibo.net.WeiboException;

public class Tweet extends BaseActivity {
	public static final int TWEET_OK = -1;
	private final String TAG = "Tweet";
	private final int MAX_TEXT_LENGTH = 140;
	private final String PREFERENCE_TEMP_DATA = "PREFERENCE_TEMP_DATA";
	private final String TEMP_TEXT_KEY = "TEMP_TWEET_KEY";
	private final String TEMP_IMAGE_KEY = "TEMP_IMAGE_KEY";
	private SharedPreferences preferences = null;
	
	private EditText mEditText;
	private TextView mTextNum;
	private ImageView mBack;
	private Button mPublishBtn;
	private ImageView mFace;
	private ImageView mPick;
	private ImageView mImage;
	private FrameLayout mForm;
	private View progressBar;
	private View write_delword_ll;
	private GridView mGridView;
	private InputMethodManager imm;
	private GridViewAdapter mGVAdapter;
	
	private Bitmap mBitmap = null;
	private File imgFile;
	private String theLarge;
	private String theThumbnail;
	
	public int getLayout() {
		return R.layout.weibo_tweet;
	}

	
	public void onBackPressed() {
		String str = this.mEditText.getText().toString();
		preferences = getSharedPreferences(PREFERENCE_TEMP_DATA, 0);
		SharedPreferences.Editor localEditor = this.preferences.edit();
		localEditor.putString(TEMP_TEXT_KEY, str);
		localEditor.commit();
		finish();
		super.onBackPressed();
	}

	protected void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);
		setContentView(getLayout());
		//软键盘管理类
		imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
		initView();
		initGridView();
	}
	//初始化视图控件
    private void initView()
    {    	
    	preferences = getSharedPreferences(PREFERENCE_TEMP_DATA, 0);
    	mForm = (FrameLayout)findViewById(R.id.tweet_form);
		mEditText = ((EditText) findViewById(R.id.tweet_et));
		mTextNum = ((TextView) findViewById(R.id.tweet_no));
		mFace = ((ImageView) findViewById(R.id.tweet_face));
		mPick = ((ImageView) findViewById(R.id.tweet_pic));
		mImage = ((ImageView) findViewById(R.id.tweet_preview));
		mPublishBtn = ((Button) findViewById(R.id.tweet_publish));
		mBack = ((ImageButton) findViewById(R.id.tweet_back));
		progressBar = findViewById(R.id.tweet_progressbar);
		write_delword_ll = findViewById(R.id.tweet_del);
		write_delword_ll.setOnClickListener(new View.OnClickListener() {
			public void onClick(View paramView) {
				Tweet.this.mEditText.setText("");
				mTextNum.setText("140");
			}
		});
		
		// 编辑器添加文本监听
		mEditText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable paramEditable) {}
			public void beforeTextChanged(CharSequence paramCharSequence, int paramInt1, int paramInt2, int paramInt3) {}
			public void onTextChanged(CharSequence paramCharSequence, int paramInt1, int paramInt2, int paramInt3) {
				// 显示剩余可输入的字数
				mTextNum.setText((MAX_TEXT_LENGTH - Tweet.this.mEditText.getText().length()) + "");
			}
		});
		//编辑器点击事件
    	mEditText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//显示软键盘
				showIMM();
			}
		});
    	//设置最大输入字数
    	InputFilter[] filters = new InputFilter[1];  
    	filters[0] = new InputFilter.LengthFilter(MAX_TEXT_LENGTH);
    	mEditText.setFilters(filters);
    	if (preferences != null) {
			mEditText.setText(preferences.getString(TEMP_TEXT_KEY, ""));
			// 设置光标位置
			mEditText.setSelection(preferences.getString(TEMP_TEXT_KEY, "").length());
			//显示临时保存图片
			String tempImage = getSharedPreferences(PREFERENCE_TEMP_DATA,0).getString(TEMP_IMAGE_KEY, null);
			if(!StringUtils.isEmpty(tempImage)) {
	    		Bitmap bitmap = ImageUtils.loadImgThumbnail(tempImage, 100, 100);
	    		if(bitmap != null) {
	    			imgFile = new File(tempImage);
					mImage.setImageBitmap(bitmap);
					mImage.setVisibility(View.VISIBLE);
				}
			}
		}
		
    	mBack.setOnClickListener(new View.OnClickListener() {
			public void onClick(View paramView) {
				String str = mEditText.getText().toString();
				preferences = getSharedPreferences(PREFERENCE_TEMP_DATA, 0);
				SharedPreferences.Editor localEditor = preferences.edit();
				localEditor.putString(TEMP_TEXT_KEY, str);
				localEditor.commit();
				finish();
			}
		});
//    	mBack.setOnClickListener(UIHelper.finish(this));
    	mPublishBtn.setOnClickListener(publishClickListener);
    	mImage.setOnLongClickListener(imageLongClickListener);
    	mFace.setOnClickListener(faceClickListener);
    	mPick.setOnClickListener(pickClickListener);
    }
    
    //初始化表情控件
    private void initGridView() {
    	mGridView = ((GridView) findViewById(R.id.gridView_faces));
		mGVAdapter = new GridViewAdapter(this);
		mGridView.setAdapter(mGVAdapter);
    	mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//插入的表情
				SpannableString ss = new SpannableString(view.getTag().toString());
				Drawable d = getResources().getDrawable((int)mGVAdapter.getItemId(position));
				d.setBounds(0, 0, 35, 35);//设置表情图片的显示大小
				ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
				ss.setSpan(span, 0, view.getTag().toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);				 
				//在光标所在处插入表情
				mEditText.getText().insert(mEditText.getSelectionStart(), ss);				
			}    		
    	});
    }
    
	
	private void showIMM() {
    	mFace.setTag(1);
    	showOrHideIMM();
    }
	private void showOrHideIMM() {
    	if(mFace.getTag() == null){
			//隐藏软键盘
			imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
			//显示表情
			showFace();				
		}else{
			//显示软键盘
			imm.showSoftInput(mEditText, 0);
			//隐藏表情
			hideFace();
		}
    }
	private void showFace() {
		mFace.setImageResource(R.drawable.write_insert_face_sel);
		mFace.setTag(1);
		mGridView.setVisibility(View.VISIBLE);
    }
    private void hideFace() {
    	mFace.setImageResource(R.drawable.write_insert_face_sel);
		mFace.setTag(null);
		mGridView.setVisibility(View.GONE);
    }
    private View.OnClickListener faceClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			showOrHideIMM();
		}
	};
	
	private View.OnClickListener pickClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			//隐藏软键盘
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);  
			//隐藏表情
			hideFace();		
			
			CharSequence[] items = {
					"本地上传","拍照上传"
//					this.getString(R.string.img_from_album),
//					this.getString(R.string.img_from_camera)
					
			};
			imageChooseItem(items);
		}
	};
	
	private View.OnClickListener publishClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			//隐藏软键盘
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);  
			String text = Tweet.this.mEditText.getText().toString();
			mForm.setVisibility(View.GONE);
			Tweet.this.progressBar.setVisibility(View.VISIBLE);
			try {
				if (isSina) {
					Tweet.this.update(text, "", "");
					Log.v(TAG, "Sina:   " + text);
				}
				if (isTencent) {
					Tweet.this.update(BaseActivity.getInstance(), text,
							"127.0.0.1");
					Log.v(TAG, "Tencent:   " + text);
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WeiboException e) {
				// TODO Auto-gener ated catch block
				e.printStackTrace();
			}
			Editor ed = preferences.edit();
			ed.clear();
			ed.commit();
			Tweet.this.setResult(TWEET_OK);
			Tweet.this.finish();
			
	}};
	
	private View.OnLongClickListener imageLongClickListener = new View.OnLongClickListener() {
		public boolean onLongClick(View v) {
			//隐藏软键盘
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);  
			
			new AlertDialog.Builder(v.getContext())
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("确定删除该图片？")
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					//清除之前保存的编辑图片
//					((AppContext)getApplication()).removeProperty(tempTweetImageKey);
//					
//					imgFile = null;
					mImage.setVisibility(View.GONE);
					dialog.dismiss();
				}
			})
			.setNegativeButton("取消", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create().show();
			return true;
		}
		
	};
	 @Override
	    protected void onResume() {
	    	super.onResume();
	    	if(mGridView.getVisibility() == View.VISIBLE){
	    		//隐藏表情
	    		hideFace();
	    	}
	    }
	    
	    @Override 
	    public boolean onKeyDown(int keyCode, KeyEvent event) {
	    	if(keyCode == KeyEvent.KEYCODE_BACK) {
	    		if(mGridView.getVisibility() == View.VISIBLE) {
	    			//隐藏表情
	    			hideFace();
	    		}else{
	    			return super.onKeyDown(keyCode, event);
	    		}
	    	}
	    	return true;
	    }
	    
	    /**
		 * 操作选择
		 * @param items
		 */
		public void imageChooseItem(CharSequence[] items )
		{
			AlertDialog imageDialog = new AlertDialog.Builder(this).setTitle("插入图片").setIcon(android.R.drawable.btn_star).setItems(items,
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int item)
					{
						//手机选图
						if( item == 0 )
						{
							Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
							intent.addCategory(Intent.CATEGORY_OPENABLE); 
							intent.setType("image/*"); 
							startActivityForResult(Intent.createChooser(intent, "选择图片"),ImageUtils.REQUEST_CODE_GETIMAGE_BYSDCARD); 
						}
						//拍照
						else if( item == 1 )
						{	
							String savePath = "";
							//判断是否挂载了SD卡
							String storageState = Environment.getExternalStorageState();		
							if(storageState.equals(Environment.MEDIA_MOUNTED)){
								savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OSChina/Camera/";//存放照片的文件夹
								File savedir = new File(savePath);
								if (!savedir.exists()) {
									savedir.mkdirs();
								}
							}
							
							//没有挂载SD卡，无法保存文件
							if(StringUtils.isEmpty(savePath)){
								Toast.makeText(Tweet.this,  "无法保存照片，请检查SD卡是否挂载", Toast.LENGTH_SHORT).show();
								return;
							}

							String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
							String fileName = "osc_" + timeStamp + ".jpg";//照片命名
							File out = new File(savePath, fileName);
							Uri uri = Uri.fromFile(out);
							
							theLarge = savePath + fileName;//该照片的绝对路径
							
							Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
							intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
							startActivityForResult(intent, ImageUtils.REQUEST_CODE_GETIMAGE_BYCAMERA);
						}   
					}}).create();
			
			 imageDialog.show();
		}
		
//		protected void onActivityResult(int paramInt1, int paramInt2, Intent paramIntent) {
//			ContentResolver localContentResolver = null;
//			if (paramInt1 == GET_PHOTO_BY_CAMERA) {
//				this.uri = paramIntent.getData();
//				Log.v(TAG, "uri: " + this.uri.toString());
//				String[] arrayOfString = new String[1];
//				arrayOfString[0] = "_data";
//				Cursor localCursor = managedQuery(this.uri, arrayOfString, null, null, null);
//				int i = localCursor.getColumnIndexOrThrow("_data");
//				localCursor.moveToFirst();
//				this.path = localCursor.getString(i);
//				Log.v(TAG, "path: " + this.path);
//				localContentResolver = getContentResolver();
//			}
//			while (true) {
//				try {
//					this.mBitmap = BitmapFactory.decodeStream(localContentResolver.openInputStream(this.uri));
//					super.onActivityResult(paramInt1, paramInt2, paramIntent);
//					if (this.mBitmap == null)
//						continue;
//					this.mImage.setVisibility(View.VISIBLE);
//					this.mImage.setImageBitmap(this.mBitmap);
//					this.mImage.setOnClickListener(new View.OnClickListener() {
//						public void onClick(View paramView) {
//							AlertDialog.Builder localBuilder = new AlertDialog.Builder(Tweet.this);
//							localBuilder.setTitle("删除");
//							localBuilder.setIcon(R.drawable.icon);
//							localBuilder.setPositiveButton("删除",
//									new DialogInterface.OnClickListener() {
//										public void onClick(DialogInterface paramDialogInterface, int paramInt) {
//											Tweet.this.uri = null;
//											Tweet.this.mBitmap = null;
//											Tweet.this.mImage.setVisibility(View.INVISIBLE);
//										}
//									});
//							localBuilder.setNegativeButton("取消",
//									new DialogInterface.OnClickListener() {
//										public void onClick(DialogInterface paramDialogInterface, int paramInt) {
//											// TODO something
//										}
//									});
//							localBuilder.create().show();
//						}
//					});
//					return;
//				} catch (FileNotFoundException localFileNotFoundException) {
//					localFileNotFoundException.printStackTrace();
//					continue;
//				}
////				if (paramInt1 != 100) {
////					continue;
////				}
////			    this.mBitmap = ((Bitmap)paramIntent.getExtras().get("data"));
//			      
//			}
//		}
		@Override 
		protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
		{ 
	    	if(resultCode != RESULT_OK) return;
			
			final Handler handler = new Handler(){
				public void handleMessage(Message msg) {
					if(msg.what == 1 && msg.obj != null){
						//显示图片
						mImage.setImageBitmap((Bitmap)msg.obj);
						mImage.setVisibility(View.VISIBLE);
					}
				}
			};
			
			new Thread(){
				public void run() 
				{
					Bitmap bitmap = null;
					
			        if(requestCode == ImageUtils.REQUEST_CODE_GETIMAGE_BYSDCARD) 
			        {         	
			        	if(data == null)  return;
			        	
			        	Uri thisUri = data.getData();
			        	String thePath = ImageUtils.getAbsolutePathFromNoStandardUri(thisUri);
			        	
			        	//如果是标准Uri
			        	if(StringUtils.isEmpty(thePath))
			        	{
			        		theLarge = ImageUtils.getAbsoluteImagePath(Tweet.this,thisUri);
			        	}
			        	else
			        	{
			        		theLarge = thePath;
			        	}
			        	
			        	String attFormat = FileUtils.getFileFormat(theLarge);
			        	if(!"photo".equals(MediaUtils.getContentType(attFormat)))
			        	{
			        		Toast.makeText(Tweet.this, "选择相片", Toast.LENGTH_SHORT).show();
			        		return;
			        	}
			        	
			        	//获取图片缩略图 只有Android2.1以上版本支持
			    		if(isMethodsCompat(android.os.Build.VERSION_CODES.ECLAIR_MR1)){
			    			String imgName = FileUtils.getFileName(theLarge);
			    			bitmap = ImageUtils.loadImgThumbnail(Tweet.this, imgName, MediaStore.Images.Thumbnails.MICRO_KIND);
			    		}
			        	
			        	if(bitmap == null && !StringUtils.isEmpty(theLarge))
			        	{
			        		bitmap = ImageUtils.loadImgThumbnail(theLarge, 100, 100);
			        	}
			        }
			        //拍摄图片
			        else if(requestCode == ImageUtils.REQUEST_CODE_GETIMAGE_BYCAMERA)
			        {	
			        	if(bitmap == null && !StringUtils.isEmpty(theLarge))
			        	{
			        		bitmap = ImageUtils.loadImgThumbnail(theLarge, 100, 100);
			        	}
			        }
			        
					if(bitmap!=null)
					{
						//存放照片的文件夹
						String savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OSChina/Camera/";
						File savedir = new File(savePath);
						if (!savedir.exists()) {
							savedir.mkdirs();
						}
						
						String largeFileName = FileUtils.getFileName(theLarge);
						String largeFilePath = savePath + largeFileName;
						//判断是否已存在缩略图
						if(largeFileName.startsWith("thumb_") && new File(largeFilePath).exists()) 
						{
							theThumbnail = largeFilePath;
							imgFile = new File(theThumbnail);
						} 
						else 
						{
							//生成上传的800宽度图片
							String thumbFileName = "thumb_" + largeFileName;
							theThumbnail = savePath + thumbFileName;
							if(new File(theThumbnail).exists())
							{
								imgFile = new File(theThumbnail);
							}
							else
							{
								try {
									//压缩上传的图片
									ImageUtils.createImageThumbnail(Tweet.this, theLarge, theThumbnail, 800, 80);
									imgFile = new File(theThumbnail);
								} catch (IOException e) {
									e.printStackTrace();
								}	
							}						
						}					
						//保存动弹临时图片
						preferences = getSharedPreferences(PREFERENCE_TEMP_DATA, 0);
						SharedPreferences.Editor localEditor = preferences.edit();
						localEditor.putString(TEMP_IMAGE_KEY, theThumbnail);
						localEditor.commit();
						
						Message msg = new Message();
						msg.what = 1;
						msg.obj = bitmap;
						handler.sendMessage(msg);
					}				
				};
			}.start();
	    }
		
		/**
		 * 判断当前版本是否兼容目标版本的方法
		 * @param VersionCode
		 * @return
		 */
		public static boolean isMethodsCompat(int VersionCode) {
			int currentVersion = android.os.Build.VERSION.SDK_INT;
			return currentVersion >= VersionCode;
		}
		
		
}