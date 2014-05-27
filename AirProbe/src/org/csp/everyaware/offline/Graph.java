/**
 * AirProbe
 * Air quality application for Android, developed as part of 
 * EveryAware project (<http://www.everyaware.eu>).
 *
 * Copyright (C) 2014 CSP Innovazione nelle ICT. All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * For any inquiry please write to <devel@csp.it>
 * 
 * CONTRIBUTORS
 * 
 * This program was made with the contribution of:
 *   Fabio Saracino <fabio.saracino@csp.it>
 *   Patrick Facco <patrick.facco@csp.it>
 * 
 * 
 * SOURCE CODE
 * 
 *  The source code of this program is available at
 *  <https://github.com/CSPICT/airprobe>
 */

package org.csp.everyaware.offline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.Pan;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;
import org.csp.everyaware.Constants;
import org.csp.everyaware.R;
import org.csp.everyaware.Start;
import org.csp.everyaware.Utils;
import org.csp.everyaware.bluetooth.BluetoothBroadcastReceiver;
import org.csp.everyaware.bluetooth.BluetoothManager;
import org.csp.everyaware.db.DbManager;
import org.csp.everyaware.db.Record;
import org.csp.everyaware.db.Track;
import org.csp.everyaware.tabactivities.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Graph extends Activity
{
	private DbManager mDbManager;
	private GraphManager mGraphManager;
	private List<Record>mRecords = new ArrayList<Record>();
	private Handler mHandler;
	//opened track
	private Track mTrack;
	
	//timestamps showed under graph
	private TextView mTsStartTv, mTsMiddleTv, mTsEndTv;
	private TextView mAvgCoTv, mAvgNo2Tv, mVocTv, mO3Tv, mBcTv;
	
	private final long HALF_HOUR = 30*60*1000;
	private final long ONE_MINUTE = 60*1000;
	private final long FIVE_MINUTES = 5*60*1000;
	private final long FIFTEEN_MINUTES = 15*60*1000;
	
	private long mMillisAgo;
	
	private final int lineWidth = 3;
	private Toast mExitToast; //toast showed when user press back button
	
	//private double mPrecRecordId = -1;
	private long mPrecRecordSysTs = 0;
	private long mEndWindowTs = 0;
	private long mStartWindowTs = 0;
	private final int divider = 5;
	
	//change length in minutes of sliding window
	private Button mChangeBtn;
	
    private CharSequence[] sliding_window_lengths; 
    private final int[] minutes = {30, 1, 5, 15}; //lengths of slinding window in minutes
    private int lengthsIndex; //index on two above arrays
    
	private ProgressDialog mProgressDialog;
	
	private PowerManager mPowerMan; 
	private PowerManager.WakeLock mWakeLock;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.graph);
		
		Log.d("Graph", "******************************onCreate()******************************");

        mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUalwaysOn");
        
		Utils.setStep(Constants.GRAPH, getApplicationContext());
		
		mDbManager = DbManager.getInstance(getApplicationContext());
		mDbManager.openDb();
		
		mHandler = new Handler();

		mTsStartTv = (TextView)findViewById(R.id.tsStartTv);
		mTsMiddleTv = (TextView)findViewById(R.id.tsMiddleTv);
		mTsEndTv = (TextView)findViewById(R.id.tsEndTv);
		
		lengthsIndex = 0;
		sliding_window_lengths = getResources().getStringArray(R.array.sliding_window_lengths);		
		mChangeBtn = (Button)findViewById(R.id.changeBtn);
		mChangeBtn.setText(sliding_window_lengths[lengthsIndex]);
		mChangeBtn.setOnClickListener(mChangeBtnOnClickListener);
		mMillisAgo = HALF_HOUR;
		
		//get reference to text views containing last values for sensors and bc
		mAvgCoTv = (TextView)findViewById(R.id.avgCoTv);
		mAvgNo2Tv = (TextView)findViewById(R.id.avgNo2Tv);
		mVocTv = (TextView)findViewById(R.id.vocTv);
		mO3Tv = (TextView)findViewById(R.id.o3Tv);
		mBcTv = (TextView)findViewById(R.id.bcTv);
		
		mGraphManager = new GraphManager();	
	}
	
	@Override
	protected void onStop() 
	{
		super.onStop();	
		Log.d("Graph", "******************************onStop()******************************");
		//mHandler.removeCallbacks(mGetLastRecIdRunnable);
	}	    
    
    @Override
    public void onDestroy()
    {
    	Utils.paused = false;
    	super.onDestroy(); 	
    	Log.d("Graph", "onDestroy()");
    	
    	//release partial wake lock
    	if(mWakeLock != null)
    		mWakeLock.release();
    }      
    
    @Override
    public void onPause()
    {
    	Utils.paused = true;
    	super.onPause(); 
    	Log.d("Graph", "onPause()");
		
    	mGraphManager.removeAllData();
    }
    
	@Override
	public void onResume()
	{
		Utils.paused = false;
		super.onResume();
		Log.d("Graph", "onResume()");

		//acquire partial wake lock
		if(!mWakeLock.isHeld())
			mWakeLock.acquire();  
		
		mTrack = Utils.track;
		
		if(mTrack != null)
		{
			//open progress dialog data loading
			showProgressDialog("Loading data...", false);
			mHandler.postDelayed(mLoadGraphData, 1000);		
			
			mChangeBtn.setText(sliding_window_lengths[lengthsIndex]);
			if(lengthsIndex == 0)
			{
				mGraphManager.setXAxisMax(900);
				mMillisAgo = HALF_HOUR;
			}
			else if(lengthsIndex == 1)
			{
				mGraphManager.setXAxisMax(30);
				mMillisAgo = ONE_MINUTE;
			}
			else if(lengthsIndex == 2)
			{
				mGraphManager.setXAxisMax(150);	
				mMillisAgo = FIVE_MINUTES;
			}
			else if(lengthsIndex == 3)
			{
				mGraphManager.setXAxisMax(450);
				mMillisAgo = FIFTEEN_MINUTES;
			}
		}
	}    

	@Override
	public void onBackPressed()
	{
		closeAppDialog();
		/*
		if(mExitToast != null)
		{		
			if(mExitToast.getView().isShown())
			{
				mExitToast.cancel();
				mExitToast = null;
				closeAppDialog();
			}
			else
			{
				//mExitToast = Toast.makeText(getApplicationContext(), R.string.press_again_to_exit_msg, Toast.LENGTH_SHORT);
				mExitToast.show();	
			}
		}
		else
		{
			mExitToast = Toast.makeText(getApplicationContext(), R.string.press_again_to_exit_msg, Toast.LENGTH_SHORT);
			mExitToast.show();		
		}*/
	}
	
	private Runnable mLoadGraphData = new Runnable()
	{
		@Override
		public void run() 
		{
			//inizialmente inizializzo mPrecRecordSysTs al valore di mezz'ora fa. Poi conterrà, invece, il timestamp del record
			//immediatamente precedente a quello attualmente disegnato
			mPrecRecordSysTs = mStartWindowTs;

			if(mDbManager == null)
				mDbManager = DbManager.getInstance(getApplicationContext());
			
			if(mTrack == null)
			{
				if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
					mProgressDialog.dismiss();
				Toast.makeText(Graph.this, "Track is null - please reselect the track", Toast.LENGTH_LONG).show();
				return;
			}
			
			mRecords = mDbManager.loadAllRecordsBySessionId(mTrack.mSessionId, 2);

			if((mRecords != null)&&(mRecords.size() > 0))
			{
				mGraphManager.addAllData(mRecords);
				mGraphManager.repaint();
			}
			else
			{
				Log.d("LoadGraphData", "run()--> no records for this track!");
				Toast.makeText(getApplicationContext(), "No records to show for this track!", Toast.LENGTH_LONG).show();
			}
				
			if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
				mProgressDialog.dismiss();
		}		
	};
	
    public void closeAppDialog() 
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_close_app)
        	.setIcon(android.R.drawable.ic_dialog_info)
        	.setTitle(R.string.app_name)
        	.setCancelable( false )
        	.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() 
        	{
        		public void onClick(DialogInterface dialog, int id) 
        		{
        			closeApp();
        		}
        	})
    		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() 
    		{
    			public void onClick(DialogInterface dialog, int id) 
    			{
    				dialog.dismiss();
    			}
    		});
        
        AlertDialog alert = builder.create();
        alert.show(); 
    }
    
    public void closeApp()
    {   		
    	Utils.backToHome = true;
    	
    	//stop store'n'forward service
     	if(Utils.storeForwServIntent != null)
    		stopService(Utils.storeForwServIntent);
    	   	
    	//clear shared prefs
    	Utils.deleteSharedPrefs(getApplicationContext());
    	  	
	    Utils.track = null;
	    Utils.selectedTrackIndex = -1;
	    
    	Intent intent = new Intent(Graph.this, Start.class);
    	startActivity(intent);
    	finish(); 
    }
	
	/*************** CLASSE INTERNA DI GESTIONE GRAFICO ****************************************************/
	
	private class GraphManager
	{
		//contiene elemento di tipo layout a cui sarà agganciato il grafico
		private LinearLayout mChartLayout;
		
	    //elemento di tipo View di Android
	    private GraphicalView mChartView; 
	    //si occupa del rendering degli elementi grafici del grafico
	    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	    
	    //insieme delle sequenze di coppie (per rappresentare più sequenze sullo stesso grafico)
	    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	    
	    //co_1, co_2, co_3, co_4, no2_1, no2_2, voc, o3, bc
	    private XYSeries[] mSeries = new XYValueSeries[9];
	    
	    //oggetti che definiscono come vanno renderizzate le varie sequenze di coppie mSeries
	    private XYSeriesRenderer[] mSeriesRenderer = new XYSeriesRenderer[10];
	    
	    //nomi sensori
	    private String[] mSensorNames = {"co_1", "co_2", "co_3", "co_4", "no2_1", "no2_2", "voc", "o3", "bc"};
	    
	    private int yMax = 5; //max value on y axis (it can change)
	    
	    private int xAxisNumOfElems = 300/divider; //number of elements along x axis
	    private int yAxisNumOfElems = 15;
	    
	    //array che contiene gli 8 valori ricevuti dalla sensor box
	    private double[]mValues = new double[8];
	    
	    //contiene i massimi aggiornati di ognuna delle 9 serie (incluso bc)
	    private double[]mMaxValues = new double[9];
	    
	    public GraphManager()
	    {
	    	mChartLayout = (LinearLayout)findViewById(R.id.chart); 

			//numero di quadranti lungo gli assi
		    mRenderer.setXLabels(300/20);
		    mRenderer.setYLabels(yAxisNumOfElems);

		    mRenderer.setPanEnabled(true, false); //enable horizontal pan to scroll graph by touch
		    mRenderer.setZoomEnabled(true);

		    //graph margins from viewport
		    //top, left, bottom, right
			mRenderer.setMargins(new int[] {0, 10, 0, 0});
			mRenderer.setMarginsColor(Color.BLACK);
			
			mRenderer.setApplyBackgroundColor(true);
			//mRenderer.setBackgroundColor(Color.parseColor("#ffffcb"));
			//mRenderer.setBackgroundColor(Color.parseColor("#feffef"));
			mRenderer.setBackgroundColor(Color.TRANSPARENT);
		
			//show/hide legend
			mRenderer.setShowLegend(false); 
			mRenderer.setFitLegend(true);
			//mRenderer.setLegendHeight(30);
			mRenderer.setLegendTextSize(18);
			
			//per evitare shrinking del grafico quando è dentro una scrollview
			mRenderer.setInScroll(true); // for inscroll
			mRenderer.setClickEnabled(true);
			mRenderer.setSelectableBuffer(100);  // for fixed char
			
			//show grid and color
			mRenderer.setShowGrid(true);
			mRenderer.setGridColor(Color.LTGRAY);
	    	
			//show labels on axis
	    	mRenderer.setShowLabels(false);
	    	mRenderer.setLabelsColor(Color.LTGRAY); //colore etichette
	    	mRenderer.setLabelsTextSize(14);
	    	mRenderer.setShowAxes(false);
	    	mRenderer.setYLabelsAlign(Align.LEFT);
	    	mRenderer.setXLabelsColor(Color.BLACK);
	    	mRenderer.setYLabelsAngle(270);
	    	mRenderer.setXLabelsColor(Color.BLACK);	    	
	    	
			//radius of the circle around user touch point (useful to catch on touch event)
		    mRenderer.setSelectableBuffer(10);

			//mRenderer.setAntialiasing(true);

			//istanzio le serie numeriche e i loro renderizzatori
			for (int i = 0; i < 9; i++)
			{
				mSeries[i] = new XYValueSeries(mSensorNames[i]);
				mSeriesRenderer[i] = new XYSeriesRenderer();				
			}
			
			//graphical aspects of series
		
			//setDottedStroke();
			setLineStroke();
			
			/********************* MONOSSIDO CARBONIO (4 sensori --> 4 serie) ****************/
			mSeriesRenderer[0].setColor(Color.parseColor("#ff0000f6")); //colore blue
			mSeriesRenderer[0].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[0].setLineWidth(lineWidth);
			mSeriesRenderer[0].setDisplayChartValues(false);
			mSeriesRenderer[0].setChartValuesTextSize(12);
			mSeriesRenderer[0].setFillBelowLine(true);
			mSeriesRenderer[0].setFillBelowLineColor(Color.parseColor("#220000f6"));
			mRenderer.addSeriesRenderer(mSeriesRenderer[0]);
		
			mSeriesRenderer[1].setColor(Color.parseColor("#fff5f5f5")); //colore white
			mSeriesRenderer[1].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[1].setLineWidth(lineWidth);
			mSeriesRenderer[1].setDisplayChartValues(false);
			mSeriesRenderer[1].setChartValuesTextSize(12);
			mSeriesRenderer[1].setFillBelowLine(true);
			mSeriesRenderer[1].setFillBelowLineColor(Color.parseColor("#22f5f5f5"));
			mRenderer.addSeriesRenderer(mSeriesRenderer[1]);
			
			mSeriesRenderer[2].setColor(Color.parseColor("#fffa0000")); //colore red
			mSeriesRenderer[2].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[2].setLineWidth(lineWidth);
			mSeriesRenderer[2].setDisplayChartValues(false);
			mSeriesRenderer[2].setChartValuesTextSize(12);
			mSeriesRenderer[2].setFillBelowLine(true);
			mSeriesRenderer[2].setFillBelowLineColor(Color.parseColor("#22fa0000"));
			mRenderer.addSeriesRenderer(mSeriesRenderer[2]);
			
			mSeriesRenderer[3].setColor(Color.parseColor("#ffc9c9c9")); //colore ltgray
			mSeriesRenderer[3].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[3].setLineWidth(lineWidth);
			mSeriesRenderer[3].setDisplayChartValues(false);
			mSeriesRenderer[3].setChartValuesTextSize(12);
			mSeriesRenderer[3].setFillBelowLine(true);
			mSeriesRenderer[3].setFillBelowLineColor(Color.parseColor("#22c9c9c9"));
			mRenderer.addSeriesRenderer(mSeriesRenderer[3]);
			
			/*********************** BIOSSIDO DI AZOTO (2 sensori --> 2 serie) ***************/ 
			
			mSeriesRenderer[4].setColor(Color.parseColor("#fffc00fc")); //colore magenta
			mSeriesRenderer[4].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[4].setLineWidth(lineWidth);
			mSeriesRenderer[4].setDisplayChartValues(false);
			mSeriesRenderer[4].setChartValuesTextSize(12);
			mSeriesRenderer[4].setFillBelowLine(true);
			mSeriesRenderer[4].setFillBelowLineColor(Color.parseColor("#22fc00fc"));
			mRenderer.addSeriesRenderer(mSeriesRenderer[4]);
			
			mSeriesRenderer[5].setColor(Color.parseColor("#ff00f6f6")); //colore cyan
			mSeriesRenderer[5].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[5].setLineWidth(lineWidth);
			mSeriesRenderer[5].setDisplayChartValues(false);
			mSeriesRenderer[5].setChartValuesTextSize(12);
			mSeriesRenderer[5].setFillBelowLine(true);
			mSeriesRenderer[5].setFillBelowLineColor(Color.parseColor("#2200f6f6"));
			mRenderer.addSeriesRenderer(mSeriesRenderer[5]);
			
			/******************** COMPOSTI VOLATILI ORGANICI (1 sensore --> 1 serie **********/
			
			mSeriesRenderer[6].setColor(Color.parseColor("#fff9f900")); //colore yellow
			mSeriesRenderer[6].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[6].setLineWidth(lineWidth);
			mSeriesRenderer[6].setDisplayChartValues(false);
			mSeriesRenderer[6].setChartValuesTextSize(12);
			mSeriesRenderer[6].setFillBelowLine(true);
			mSeriesRenderer[6].setFillBelowLineColor(Color.parseColor("#22f9f900"));
			mRenderer.addSeriesRenderer(mSeriesRenderer[6]);
			
			/****************************** OZONO ********************************************/
			
			mSeriesRenderer[7].setColor(Color.parseColor("#ff00fb00")); //colore green
			mSeriesRenderer[7].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[7].setLineWidth(lineWidth);
			mSeriesRenderer[7].setDisplayChartValues(false);
			mSeriesRenderer[7].setChartValuesTextSize(12);
			mSeriesRenderer[7].setFillBelowLine(true);
			mSeriesRenderer[7].setFillBelowLineColor(Color.parseColor("#2200fb00"));
			mRenderer.addSeriesRenderer(mSeriesRenderer[7]);  
			
			/*************************** BLACK CARBON SERIE *******************************************/
			
			mSeriesRenderer[8].setColor(Color.DKGRAY);
			mSeriesRenderer[8].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[8].setLineWidth(lineWidth);			
			mSeriesRenderer[8].setStroke(new BasicStroke(Cap.ROUND, Join.ROUND, 0, null, 0));
			/*
			mSeriesRenderer[8].setGradientEnabled(true);
			mSeriesRenderer[8].setGradientStart(1, Color.RED);
			mSeriesRenderer[8].setGradientStop(5, Color.BLUE);
			*/
			mRenderer.addSeriesRenderer(mSeriesRenderer[8]);

			//add all series to dataset
			for (int i = 0; i < 9; i++)
				mDataset.addSeries(mSeries[i]);
			
			//check if serie length is the same both renderer and dataset
			if((mDataset == null)|| (mRenderer == null) || (mDataset.getSeriesCount() != mRenderer.getSeriesRendererCount()))
				throw new IllegalArgumentException("Dataset and renderer should not be null and " +
						"should have the same number of series");
			
			//encapsulate the graph into viewl
			if (mChartView == null) 
			{		    
				Log.d("GraphManager", "encapsulate the graph into view");
				
				mChartView = ChartFactory.getLineChartView(getApplicationContext(), mDataset, mRenderer);
				
				if(mChartView == null)
				{ 			
					Log.d("GraphManager","mChartView == null");
				}
				else
				{
					
					mChartView.setOnClickListener(new View.OnClickListener() 
					{
				        public void onClick(View v) 
				        {
				        	// handle the click event on the chart
				        	SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
				        	if (seriesSelection == null) 
				        	{
				        		Toast.makeText(Graph.this, "No chart element", Toast.LENGTH_SHORT).show();
				        	} 
				        	else 
				        	{
				        		// display information of the clicked point
	
				        		int serieIndex = seriesSelection.getSeriesIndex();
				        		int pointIndex = seriesSelection.getPointIndex();
				        		double serieValue = mSeries[serieIndex].getY(pointIndex);
				        		
				        		if(serieIndex == 8) //8 for aqi serie
				        			Toast.makeText(Graph.this, mSeries[serieIndex].getTitle()+" clicked value: "+serieValue*2, Toast.LENGTH_LONG).show();
				        		else
				        			Toast.makeText(Graph.this, mSeries[serieIndex].getTitle()+" clicked value: "+serieValue, Toast.LENGTH_LONG).show();
				        	}
				         }
				    });
					
					mChartView.addZoomListener(new ZoomListener()
					{
						@Override
						public void zoomApplied(ZoomEvent ze) 
						{
							String type = "out";
					          if (ze.isZoomIn()) {
					            type = "in";
					          }
					          System.out.println("Zoom " + type + " rate " + ze.getZoomRate());
						}

						@Override
						public void zoomReset() 
						{
							System.out.println("Reset");
						}
						
					}, true, true);
					
					mChartView.addPanListener(new PanListener()
					{
						@Override
						public void panApplied() 
						{	
							int minIndex = (int)Math.round(mRenderer.getXAxisMin());
							int maxIndex = (int)Math.round(mRenderer.getXAxisMax());
							
							Log.d("PanListener", "panApplied()--> pan change triggered - xAxisMin: " +minIndex+" xAxisMax: "+maxIndex);
							
							if((minIndex > 0)&&(maxIndex < mRecords.size()))
								updateTsTextViews(mRecords.get(minIndex), mRecords.get(maxIndex));
					
						/*		
							String initialRange = "";
							double[] iR = mRenderer.getInitialRange();
							if(iR != null)
								for(int i = 0; i < iR.length; i++)
									initialRange += iR[i]+" ";
							Log.d("PanListener", "panApplied()--> pan change triggered - getInitialRange: "+initialRange);	
							
							String panLimits = "";
							double[] pL = mRenderer.getPanLimits();
							if(pL != null)
								for(int i = 0; i < pL.length; i++)
									panLimits += pL[i]+" ";
							Log.d("PanListener", "panApplied()--> pan change triggered - pan limits: "+panLimits);
						*/	
						}						
					});

					//add graph view to layout
					mChartLayout.addView(mChartView, new LinearLayout.LayoutParams
					    		(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
				}    
			} 
			else 
			{
				Log.d("GraphManager", "graph redraw");
				mChartView.repaint();
			}
	    }
	    
	    //given an array of records, draw each records on the graph, at the right position
	    public void addAllData(List<Record>records)
	    {
	    	int size = records.size();
	    	
	    	Log.d("GraphManager", "addAllData()--> loaded "+size+" records");
	    	
	    	//Sets the axes range values showed in a view
	    	//equivalent to setXAxisMin, setXAxisMax, setYAxisMin, setYAxisMax
	    	mRenderer.setRange(new double[]{1, xAxisNumOfElems, 0, yMax});
	    	
	    	//set limit to orizontal scrolling (pan is possible over the entire serie)
	    	mRenderer.setPanLimits(new double[]{1, size, 0, 0});
	    		    	
			//draw all loaded records on graph
			for(int i = 0; i < size; i++)
			{
				Record rec = mRecords.get(i);		

				addDataToGraph(rec.mValues, rec.mBcMobile);				
			}	
			
	    	int minIndex = (int)Math.round(mRenderer.getXAxisMin());
			int maxIndex = (int)Math.round(mRenderer.getXAxisMax());
			
			Log.d("GraphManager", "addAllData()--> - xAxisMin: " +minIndex+" xAxisMax: "+maxIndex);
			
			if((minIndex > 0)&&(maxIndex < size))
				updateTsTextViews(mRecords.get(minIndex), mRecords.get(maxIndex));
			else if((minIndex > 0)&&(minIndex < size))
				updateTsTextViews(mRecords.get(minIndex), null);
	    }
	    
	    public void repaint()
	    {
	    	if(mChartView != null)
	    		//ridisegna la view con i dati aggiunti
				mChartView.repaint();
	    }
	    
	    public double calcAvgCo()
	    {
	    	double avgCo = (mValues[0] + mValues[1] + mValues[2] + mValues[3]) /4;
	    	int avgCo_int = (int)Math.round(avgCo * 100);
	    	return (double)avgCo_int / 100;
	    }
	    
	    public double calcAvgNo2()
	    {
	    	double avgNo2 = (mValues[4] + mValues[5]) /2;
	    	int avgNo2_int = (int)Math.round(avgNo2 * 100);
	    	return (double)avgNo2_int / 100;
	    }
	    
	    public double calcVoc()
	    {
	    	int voc_int = (int)Math.round(mValues[6] * 100);
	    	return (double)voc_int / 100;
	    }
	    
	    public double calcO3()
	    {
	    	int o3_int = (int)Math.round(mValues[7] * 100);
	    	return (double)o3_int / 100;
	    }
	    
	    public double calcBc()
	    {
	    	if(Utils.bc == 0)
	    		return 0;
	    	
	    	int bc_int = (int)Math.round(Utils.bc * 100);
	    	return (double)bc_int / 100;
	    }
	    //aggiunge un nuovo vettore di dati senza gestire lo scrolling e senza fare repaint
	    public void addDataToGraph(double[] pollutants, double blackCarbon)
	    {	    	
	    	double x;
	    	
	    	//se non ho coppie nella serie, sono al tempo 0
	    	if(mSeries[0].getItemCount() == 0)
	    		x = 0;
	    	else
	    		//ottengo i valori massimo x dalla xyseries, che corrisponde al tempo
	    		x = mSeries[0].getMaxX();
	    	
	    	//Log.d("GraphManager", "refreshData()--> x= " +x);
	    
			//copio nell'array i valori
			mValues[0] = pollutants[2];
			mValues[1] = pollutants[3];
			mValues[2] = pollutants[4];
			mValues[3] = pollutants[5];
			mValues[4] = pollutants[6];
			mValues[5] = pollutants[7];
			mValues[6] = pollutants[8];
			mValues[7] = pollutants[9];				
			/*			  
			try
			{
				mAvgCoTv.setText(String.valueOf(calcAvgCo()));
				mAvgNo2Tv.setText(String.valueOf(calcAvgNo2()));				
				mVocTv.setText(String.valueOf(calcVoc()));
				mO3Tv.setText(String.valueOf(calcO3()));
				
				int bc_int = 0;				
				if(blackCarbon > 0)
					bc_int = (int)Math.round(blackCarbon * 100);

				mBcTv.setText(String.valueOf((double)bc_int / 100));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			*/
			//calcolo il valore più alto fra i 9 valori (sull'intera serie)
			double max = calcMax();
			
			mRenderer.setYAxisMax(Math.ceil(max));
			
			//Log.d("GraphManager", "addDataToGraph()--> getYAxisMax: "+mRenderer.getYAxisMax()+ " black carbon: " +blackCarbon);
			
			//aggiungo alla mSeries i-esima il valore x+1 sulle ascisse e il valore i-esimo di mValues alle ordinate
			addData(mSeries[0], x+1, mValues[0]);   
			addData(mSeries[1], x+1, mValues[1]);
			addData(mSeries[2], x+1, mValues[2]);
			addData(mSeries[3], x+1, mValues[3]);
					
			addData(mSeries[4], x+1, mValues[4]);
			addData(mSeries[5], x+1, mValues[5]);
					
			addData(mSeries[6], x+1, mValues[6]);
			addData(mSeries[7], x+1, mValues[7]);
								
		
		    addData(mSeries[8], x+1, blackCarbon); //raw air quality index serie
		    
	    	//carico in x il nuovo massimo x dopo aver aggiunto le n coppie (x, y)
	    	x = mSeries[0].getMaxX();    			    		    	
	    }
	    
	    public void removeAllData()
	    {
			mSeries[0].clear();
			mSeries[1].clear();
			mSeries[2].clear();
			mSeries[3].clear();
					
			mSeries[4].clear();
			mSeries[5].clear();
					
			mSeries[6].clear();
			mSeries[7].clear();
			
			mSeries[8].clear(); //black carbon serie 
	    }
	    
	    //aggiunge alla serie (mSeries) xyseries di coppie (x, y) una nuova coppia
	    public void addData(XYSeries xyseries, double d1, double d2)
	    {
	    	xyseries.add(d1, d2);
	    }
	    
	    //calcola il max fra le 8 serie, considerando ogni serie nella sua interezza
	    public double calcMax()
	    {
	    	for(int i = 0; i < 9; i++)
	    	{
	    		mMaxValues[i] = mSeries[i].getMaxY(); 
	    	}
	    	
	    	double max = 0;
	    	
	    	for(int i = 0; i < mMaxValues.length; i++)
	    	{
	    		if(mMaxValues[i] >= max)
	    			max = mMaxValues[i];
	    	}
	    	return max;
	    }
	    
	    //calcola il max dell'arrya di valori passati in ingresso
	    public double calcActualMax(double[] values)
	    {
	    	double max = 0;
	    	
	    	for(int i = 0; i < values.length; i++)
	    	{
	    		if(i == 0)
	    			max = values[i];
	    		else
	    		{
	    			if(values[i] >= max)
	    				max = values[i];
	    		}
	    	}
	    	return max;
	    }
	    
	    public void setXAxisMax(int xNum)
	    {
	    	xAxisNumOfElems = xNum;
	    	mRenderer.setXAxisMax(xNum-1);
	    }
	    
	    public void setDottedStroke()
	    {
	    	//BasicStroke.DASHED: trattini
	    	//BasicStroke.DOTTED: puntini
	    	
	    	for(int i = 0; i < 8; i++)
	    		mSeriesRenderer[i].setStroke(BasicStroke.DOTTED);
	    }
	    
	    public void setLineStroke()
	    {
	    	for(int i = 0; i < 8; i++)
	    		mSeriesRenderer[i].setStroke(new BasicStroke(Cap.ROUND, Join.ROUND, 0, null, 0));
	    }
	}
	
	private void updateTsTextViews(Record older, Record newer)
	{
		if(older.mSysTimestamp == 0)
			mStartWindowTs = older.mBoxTimestamp;
		else
			mStartWindowTs = older.mSysTimestamp;
		
		String ts = new SimpleDateFormat("HH:mm:ss\nMM/dd/yyyy").format(mStartWindowTs);		
		mTsStartTv.setText(ts);
		
		if(newer != null)
		{
			if(newer.mSysTimestamp == 0)
				mEndWindowTs = newer.mBoxTimestamp;
			else
				mEndWindowTs = newer.mSysTimestamp; 
			
			//middle between actual time and half hour ago
			ts = new SimpleDateFormat("HH:mm:ss\nMM/dd/yyyy").format( mStartWindowTs + (mEndWindowTs - mStartWindowTs)/2 );
			mTsMiddleTv.setText(ts);
			
			ts = new SimpleDateFormat("HH:mm:ss\nMM/dd/yyyy").format(mEndWindowTs);
			mTsEndTv.setText(ts);
		}
		else
		{
			mTsMiddleTv.setText("");
			mTsEndTv.setText("");
		}
	}
	
    public void showProgressDialog(String msg, boolean cancelable)
    {
    	mProgressDialog = ProgressDialog.show(Graph.this, getResources().getString(R.string.app_name), msg, true, true);
    }
    
    /*************** CHANGE LENGTH OF SLIDING WINDOW BTN ON CLICK LISTENER ********************************/
    
    private OnClickListener mChangeBtnOnClickListener = new OnClickListener()
    {
		@Override
		public void onClick(View v) 
		{
			lengthsIndex++;
			if(lengthsIndex > 3)
				lengthsIndex = 0;
			mChangeBtn.setText(sliding_window_lengths[lengthsIndex]);
			
			//open progress dialog data loading
			showProgressDialog("Loading data...", false);
			
			mHandler.postDelayed(new Runnable()
			{
				@Override
				public void run() 
				{
					mGraphManager.removeAllData();
					
					if(lengthsIndex == 0)
					{
						mGraphManager.setXAxisMax(900);
						mMillisAgo = HALF_HOUR;
					}
					else if(lengthsIndex == 1)
					{
						mGraphManager.setXAxisMax(30);
						mMillisAgo = ONE_MINUTE;
					}
					else if(lengthsIndex == 2)
					{
						mGraphManager.setXAxisMax(150);	
						mMillisAgo = FIVE_MINUTES;
					}
					else if(lengthsIndex == 3)
					{
						mGraphManager.setXAxisMax(450);
						mMillisAgo = FIFTEEN_MINUTES;
					}
					
					mHandler.post(mLoadGraphData);
				}			
			}, 500);
		} 	
    };
}
