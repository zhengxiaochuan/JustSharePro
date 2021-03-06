package com.logan.weibo.ui;

import java.util.List;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.logan.R;
import com.logan.app.AppContext;
import com.logan.util.StringUtils;
import com.logan.util.UIHelper;
import com.logan.weibo.adapter.QListViewAdapter;
import com.logan.weibo.bean.BaseTimeLine;
import com.logan.weibo.bean.QStatus;
import com.logan.weibo.bean.Status;
import com.logan.weibo.widget.NewDataToast;
import com.logan.weibo.widget.PullToRefreshListView;

public class QFriendTimeLine extends BaseTimeLine {

	private final String TAG = "QFriendTimeLine";
//	private AppContext mApplication;//全局Context
	private List<QStatus> statusList = null;
	private String mData = null;
	private int pageSize = 10;
	private int pageFlag = 0;//分页标识（0：第一页，1：向下翻页，2向上翻页）
	private String pageTime = "0";//本页起始时间（第一页：填0，向上翻页：填上一次请求返回的第一条记录时间，向下翻页：填上一次请求返回的最后一条记录时间）
	private int pageSum = 0;
	private Handler lvNewsHandler;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		mApplication = (AppContext) getApplication();
//		//网络连接判断
//        if(!mApplication.isNetworkConnected())UIHelper.ToastMessage(this, R.string.network_not_connected);
		setSelectedFooterTab(0);
		titleTV.setText("微博主页");
		mData = getQFriendTimeline(String.valueOf(pageFlag), pageTime);
		statusList = getQStatusList(mData);
		QListViewAdapter mAdapter = new QListViewAdapter(QFriendTimeLine.this, statusList);
		pullToRefreshListView.setAdapter(mAdapter);

		pullToRefreshListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	    	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	    		//点击头部、底部栏无效
	    		if(position == 0 || view == listView_footer) return;
				Status status = null;
				// 判断是否是TextView
				if (view instanceof TextView) {
					status = (Status) view.getTag();
				} else {
					TextView tv = (TextView) view.findViewById(R.id.item_text);
					status = (Status) tv.getTag();
				}
				if (status == null)
					return;
	    		//跳转到微博详情
	    		//UIHelper.showNewsRedirect(view.getContext(), news);
	    		Intent intent = new Intent();
	    		Bundle mBundle = new Bundle();  
	            mBundle.putSerializable("detail",statusList.get(position));  
	    		intent.putExtras(mBundle);
				intent.setClass(QFriendTimeLine.this, QStatusDetail.class);
				startActivity(intent);
	    	}        	
		});
	    pullToRefreshListView.setOnScrollListener(new AbsListView.OnScrollListener() {
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				pullToRefreshListView.onScrollStateChanged(view, scrollState);
				
				//数据为空--不用继续下面代码了
				if(statusList.isEmpty()) return;
				
				//判断是否滚动到底部
				boolean scrollEnd = false;
				try {
					if(view.getPositionForView(listView_footer) == view.getLastVisiblePosition())
						scrollEnd = true;
				} catch (Exception e) {
					scrollEnd = false;
				}
				
				int lvDataState = StringUtils.toInt(pullToRefreshListView.getTag());
				if(scrollEnd && lvDataState==UIHelper.LISTVIEW_DATA_MORE)
				{
					pullToRefreshListView.setTag(UIHelper.LISTVIEW_DATA_LOADING);
					listView_foot_more.setText(R.string.load_ing);
					listView_foot_progress.setVisibility(View.VISIBLE);
					//页码
					int pageIndex = pageSum/pageSize;
					pageTime = statusList.get(pageSum).getCreated_at();
					Log.v(TAG, "pageTime is: "+pageTime);
					loadLvNewsData(pageIndex, pageTime, lvNewsHandler, UIHelper.LISTVIEW_ACTION_SCROLL);
				}
			}
			public void onScroll(AbsListView view, int firstVisibleItem,int visibleItemCount, int totalItemCount) {
				pullToRefreshListView.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
			}
		});
	    pullToRefreshListView.setOnRefreshListener(new PullToRefreshListView.OnRefreshListener() {
	        public void onRefresh() {
	        	loadLvNewsData(0, "0", lvNewsHandler, UIHelper.LISTVIEW_ACTION_REFRESH);
	        }
	    });	
	    lvNewsHandler = this.getLvHandler(pullToRefreshListView, mAdapter, listView_foot_more, listView_foot_progress, pageSize);
	}
	/**
     * 获取listview的初始化Handler
     * @param lv
     * @param adapter
     * @return
     */
    private Handler getLvHandler(final PullToRefreshListView lv,final BaseAdapter adapter,final TextView more,final ProgressBar progress,final int pageSize){
    	return new Handler(){
			public void handleMessage(Message msg) {
				if(msg.what >= 0){
					//listview数据处理
//					Notice notice = 
					handleLvData(msg.what, msg.obj, msg.arg2, msg.arg1);
					
					if(msg.what < pageSize){
						lv.setTag(UIHelper.LISTVIEW_DATA_FULL);
						adapter.notifyDataSetChanged();
						more.setText(R.string.load_full);
					}else if(msg.what == pageSize){
						lv.setTag(UIHelper.LISTVIEW_DATA_MORE);
						adapter.notifyDataSetChanged();
						more.setText(R.string.load_more);
						
					}
				}
				else if(msg.what == -1){
					//有异常--显示加载出错 & 弹出错误消息
					lv.setTag(UIHelper.LISTVIEW_DATA_MORE);
					more.setText(R.string.load_error);
//					((AppException)msg.obj).makeToast(Main.this);
				}
				if(adapter.getCount()==0){
					lv.setTag(UIHelper.LISTVIEW_DATA_EMPTY);
					more.setText(R.string.load_empty);
				}
				progress.setVisibility(ProgressBar.GONE);
				mHeadProgress.setVisibility(ProgressBar.GONE);
				if(msg.arg1 == UIHelper.LISTVIEW_ACTION_REFRESH){
					lv.onRefreshComplete(getString(R.string.pull_to_refresh_update) + new Date().toLocaleString());
					lv.setSelection(0);
				}
//				else if(msg.arg1 == UIHelper.LISTVIEW_ACTION_CHANGE_CATALOG){
//					lv.onRefreshComplete();
//					lv.setSelection(0);
//				}
			}
		};
    }
    
	/**
     * 线程加载新闻数据
     * @param catalog 分类
     * @param pageIndex 当前页数
     * @param handler 处理器
     * @param action 动作标识
     */
	private void loadLvNewsData(final int pageIndex,final String pageTime,final Handler handler,final int action){ 
		mHeadProgress.setVisibility(ProgressBar.VISIBLE);		
		new Thread(){
			public void run() {				
				Message msg = new Message();
				boolean isRefresh = false;
				if(action == UIHelper.LISTVIEW_ACTION_REFRESH || action == UIHelper.LISTVIEW_ACTION_SCROLL)
					isRefresh = true;
				try {					
//					NewsList list = appContext.getNewsList(catalog, pageIndex, isRefresh);
					mData = getQFriendTimeline(String.valueOf(pageIndex), String.valueOf(pageTime));
					statusList = getQStatusList(mData);
					msg.what = statusList.size();
					msg.obj = statusList;
	            } catch (Exception e) {
	            	e.printStackTrace();
	            	msg.what = -1;
	            	msg.obj = e;	
	            }
				msg.arg1 = action;
				msg.arg2 = UIHelper.LISTVIEW_DATATYPE_NEWS;
//                if(curNewsCatalog == catalog)
//                	handler.sendMessage(msg);
			}
		}.start();
	} 
	
	 /**
     * listview数据处理
     * @param what 数量
     * @param obj 数据
     * @param objtype 数据类型
     * @param actiontype 操作类型
     * @return notice 通知信息
     */
    private void handleLvData(int what,Object obj,int objtype,int actiontype){
//    	Notice notice = null;
		switch (actiontype) {
			case UIHelper.LISTVIEW_ACTION_INIT:
			case UIHelper.LISTVIEW_ACTION_REFRESH:
			case UIHelper.LISTVIEW_ACTION_CHANGE_CATALOG:
				int newdata = 0;//新加载数据-只有刷新动作才会使用到
				switch (objtype) {
					case UIHelper.LISTVIEW_DATATYPE_NEWS:
						@SuppressWarnings({ "unchecked" })
						List<QStatus> newData = (List<QStatus>) obj;
//						notice = nlist.getNotice();
						pageSum = what;
						if(actiontype == UIHelper.LISTVIEW_ACTION_REFRESH){
							if(statusList.size() > 0){
								for(QStatus status : newData){
									boolean b = false;
									for(QStatus status2 : statusList){
										if(status.getId() == status2.getId()){
											b = true;
											break;
										}
									}
									if(!b) newdata++;
								}
							}else{
								newdata = what;
							}
						}
//						statusList.clear();//先清除原有数据
						statusList = newData;
						break;
					
				}
				if(actiontype == UIHelper.LISTVIEW_ACTION_REFRESH){
					//提示新加载数据
					if(newdata >0){
//						NewDataToast.makeText(this, getString(R.string.new_data_toast_message, newdata), mApplication.isAppSound()).show();
						NewDataToast.makeText(this, getString(R.string.new_data_toast_message, newdata), true).show();
					}else{
						NewDataToast.makeText(this, getString(R.string.new_data_toast_none), false).show();
					}
				}
				break;
			case UIHelper.LISTVIEW_ACTION_SCROLL:
				switch (objtype) {
					case UIHelper.LISTVIEW_DATATYPE_NEWS:
						List<QStatus> newData = (List<QStatus>) obj;
						pageSum += what;
						if(statusList.size() > 0){
							for(QStatus news1 : newData){
								boolean b = false;
								for(QStatus news2 : statusList){
									if(news1.getId() == news2.getId()){
										b = true;
										break;
									}
								}
								if(!b) statusList.add(news1);
							}
						}else{
							statusList = newData;
						}
						break;
					
				}
				break;
		}
//		return notice;
    }
    
}
