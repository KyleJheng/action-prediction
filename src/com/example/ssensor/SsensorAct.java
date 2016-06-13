package com.example.ssensor;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;




import android.app.Activity;
import android.app.Service;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SsensorAct extends Activity implements SensorEventListener{
	
	
	private PromtVoice PV;
	private SensorManager ASM;
	private Sensor ASensor;
	private float G[]=new float[3];
	boolean flag = false;
	int DelayTime = 50;
	int i=0,r=0,count=0;
	private float DataSet[][] = new float[1200][3];
	private float PredictSet[][] = new float[100][3];
	private static float NTSet[][]= new float[70][43];
	private static float NPSet[][]= new float[1][43];
	private static float Column_MAX[] = new float[43];
	private static float Column_MIN[]= new float[43];
	private static  String Motion_String[] ={"Walking","Jogging","AscendingStairs","DescendingStairs","Sitting","Standing","Riding"};
	String SaveType,OnRun,tempwindow="";
	
	private static float Motion_Feature[][] = new float[71][43];
	private static float Predict_Target_Feature[][] = new float[1][43];
	private float[][] temp = new float[1000][3];
	String result[]=new String[10];
	private Button button_sense;
	private Button button_recog;
	private TextView tv_timer;
	private TextView tv_progress;
	private TextView tv_x;
	private TextView tv_y;
	private TextView tv_z;
	private TextView tv_window;

	
	private Handler G_handler = new Handler();
	private Handler GThreadHandler;
	private HandlerThread GThread;
	
	public void setVibrate(int time){
        Vibrator myVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        myVibrator.vibrate(time);
    }
	
	 private Runnable Acc_Predict= new Runnable(){
	    	@Override
	    	public void run(){
	    		
	    		PredictSet[count%100][0]=G[0];
	    		PredictSet[count%100][1]=G[1];
	    		PredictSet[count%100][2]=G[2];
//	    		Log.d("load",count+String.valueOf(PredictSet[count%100][0]));
	    		count++;
	    		flag = true;
	    		
	    		if(count<500){
	    		G_handler.postDelayed(this, DelayTime);
	    		//Log.d("clock",String.valueOf(count));
	    		if(count%10==0){
	    			tv_timer.setText("Start: "+(String.format("%.02f",(count%100)*0.05)+" 秒"));}
	    		if(count%50==0 && count>100 ){
	    	        //G_handler.removeCallbacks(Acc_Predict);
	    			//if(count<700){
	    	        //tv_timer.setText("Start: "+(String.format("%.02f",(count%100)*0.05)+" 秒") +"\nPredict...");
	    	        setVibrate(500);
	    	        
					PV.playRecognition();
	    	        //每次感測完就去重算一次特徵表
	    	        //Motion_Reading();
	    	        //直接讀已經算好的特徵表再去做正規化
	    	        Train_Read();
	    	        
	    	        
	    	        //預測函數比對
	    	        Predict_getFeature();
//	    	        for(int x=0;x<200;x++){
//	    	        	for(int y=0;y<3;y++)
//	    	        		Log.d("Predict Set: "+x+":"+y,String.valueOf(PredictSet[x][y]));
//	    	        }
//	    	        for(int t=0;t<2;t++){
//	    				for(int s=0;s<43;s++){
//	    					Log.d("Predict data: "+t+":"+s,String.valueOf(Predict_Target_Feature[t][s]));
//	    				}
//	    			}
	    	        
	    	        //正規化
	    	        Normalization_TrainSet( );
	    	        Normalization_PESet( );
	    	        //KNN()
	    	        KNN();
	    	        
	    	        //////////////////////////////////////
				    try{
				    	FileWriter FW = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+"Featuretest.csv",false);
				    	//Log.d("tag Feature", Environment.getExternalStorageDirectory().getPath()+"/FeatureNormal.csv");
				
				    	for(i=0;i<70;i++){
				    		for(int j=0;j<43;j++){
				    			FW.append(String.valueOf(Motion_Feature[i][j]));
				    			FW.append(',');
				    		}FW.append('\n');
				    	}
				    	FW.flush();
				    	FW.close();
				
				
				    }catch (IOException e){
				    	e.printStackTrace();
				    }
				    ////////////////////////////////////////////
					//////////////////////////////////////
					    try{
					    	FileWriter FW = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+"FeatureNormal.csv",false);
					    	//Log.d("tag Feature", Environment.getExternalStorageDirectory().getPath()+"/FeatureNormal.csv");
					
					    	for(i=0;i<70;i++){
					    		for(int j=0;j<43;j++){
					    			FW.append(String.valueOf(NTSet[i][j]));
					    			FW.append(',');
					    		}FW.append('\n');
					    	}
					    	FW.flush();
					    	FW.close();
					
					
					    }catch (IOException e){
					    	e.printStackTrace();
					    }
					    ////////////////////////////////////////////
	    	        }
	    		}
	    		else{
	    			PV.playComplete();
	    			GThreadHandler.removeCallbacks(Acc_Predict);
	    			G_handler.removeCallbacks(Acc_Predict);
	    			tv_timer.setText("Predict...");
	    		}
	    		
	    		}
	    		
	    		
	    	};
	
	 private Runnable Acc_Sensing= new Runnable(){
	    	@Override
	    	public void run(){
	    		
	    		DataSet[i][0]=G[0];
				DataSet[i][1]=G[1];
				DataSet[i][2]=G[2];
	    		i++;
	    		//Log.d("time", String.valueOf(i++));
	    		flag = true;
	    		G_handler.postDelayed(this, DelayTime);
	    		
	    		if(i%10==0)
	    			tv_timer.setText("Start: "+(String.format("%.02f",i*0.05)+" 秒"));
	    		
	    		
	    		if(i==1200){
	    	        G_handler.removeCallbacks(Acc_Sensing);
	    	        tv_timer.setText("Finish");
	    	        i=0; setVibrate(2000);
					PV.playComplete();
	    	        
	    	        try{
	    	        	FileWriter FW = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+SaveType+"test.csv",false);
	    	        	//Log.d("tag", Environment.getExternalStorageDirectory().getPath()+"/test.csv");
	    	        	
	    	        	for(i=200;i<1200;i++){
	    	        		
	    	        		FW.append(String.valueOf(DataSet[i][0]));
	    	        		FW.append(',');
	    	        		FW.append(String.valueOf(DataSet[i][1]));
	    	        		FW.append(',');
	    	        		FW.append(String.valueOf(DataSet[i][2]));
	    	        		FW.append('\n');
	    	        	}
	    	        	
	    	        	FW.flush();
	    	        	FW.close();
	    	        	
	    	        	
	    	        }catch (IOException e){
	    	        	e.printStackTrace();
	    	        }
	    		}
	    		
	    	}
	    };
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ssensor);
        
         PV = new PromtVoice(this);
        
        ASM=(SensorManager) getSystemService(SENSOR_SERVICE);
        ASensor = ASM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        ASM.registerListener(this, ASensor, SensorManager.SENSOR_DELAY_UI);
        
        button_sense= (Button)findViewById(R.id.button_sense);
        button_sense.setEnabled(false);
        button_recog= (Button)findViewById(R.id.button_Recognition);
        tv_timer =(TextView)findViewById(R.id.tv_timer);
        tv_progress =(TextView)findViewById(R.id.tv_progress);
        tv_x = (TextView)findViewById(R.id.tv_x);
        tv_y = (TextView)findViewById(R.id.tv_y);
        tv_z = (TextView)findViewById(R.id.tv_z);
        tv_window = (TextView)findViewById(R.id.tv_window);
        
        Spinner Spinner_Action = (Spinner)findViewById(R.id.spinner_action);
        ArrayAdapter Action_List = 
        		new ArrayAdapter(this,android.R.layout.simple_spinner_item,Motion_String);
        Action_List.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner_Action.setAdapter(Action_List);
        
        Spinner_Action.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            public void onItemSelected(AdapterView adapterView, View view, int position, long id){
                Toast.makeText(SsensorAct.this, "You choose "+adapterView.getSelectedItem().toString(), Toast.LENGTH_LONG).show();
                SaveType = adapterView.getSelectedItem().toString();
            }
            public void onNothingSelected(AdapterView item) {
                Toast.makeText(SsensorAct.this, "You didn't choose any item", Toast.LENGTH_LONG).show();
            }
        });
        
        
        
       
        GThread = new HandlerThread("Accelerometer");
        GThread.start();
        GThreadHandler=new Handler(GThread.getLooper());
       
        //GThreadHandler.post(Acc_Sensing);
        
        
        button_sense.setOnClickListener(new Button.OnClickListener(){
        	@Override
        	public void onClick(View v){
        		i=0;PV.playSense();        		
        		tv_timer.setText("Start: ");
        	    GThreadHandler.post(Acc_Sensing);
        	    
        	}
        });
			
        
        button_recog.setOnClickListener(new Button.OnClickListener(){
        	@Override
        	public void onClick(View v){
        		i=0;PV.playRecognition();       		
        		tv_timer.setText("Start: ");
        	    GThreadHandler.post(Acc_Predict);
        	}
        });
        
        
    } 
   
    public void Train_Read(){
    	
    	FileReader FR=null;
    	
    	try {
			FR = new FileReader(Environment.getExternalStorageDirectory().getPath()+"/Featuretest.csv");
			BufferedReader BR =  new BufferedReader(FR);
		
			//Log.d("file: "+Motion_String[index]+index, Motion_String[index]);
		
		
		
			try{
				String line;int RowIndex=0;
			
				while((line = BR.readLine())!= null){
					String[] RowData = line.split(",");
					for(int i=0;i<43;i++){
					Motion_Feature[RowIndex][i]=Float.parseFloat(RowData[i]);
					}
					RowIndex++;
					//Log.d("read"+ index,RowData[0]);
				}
			}catch(IOException ex){
					ex.getStackTrace();
					}
			finally{
					try{
						FR.close();
					}catch(IOException e){
						e.getStackTrace();
					}}
			
		}catch(IOException e){
			e.printStackTrace();
		}
    }
    
    public void Predict_getFeature(){
    	//為處理預測資料集的特徵值
    	double sum = 0;
		float tempnumberA=0,tempnumberB=0,tempnumberC=0;
		int k=100,j=0,Act=0;boolean increase =true;
    	
    	for( j=0;j<1;j++){
			for( k=0;k<43;k++){
				Predict_Target_Feature[j][k]=0;}}
    	
    	for(int t=0;t<1;t++){
			for(int s=0;s<43;s++){
				//Log.d("Predict data: "+t+":"+s,String.valueOf(Predict_Target_Feature[t][s]));
			}
		}
    	k=0;
    	//x,y,z的平均
    	while(k<100){
			tempnumberA+=PredictSet[k][0];
			tempnumberB+=PredictSet[k][1];
			tempnumberC+=PredictSet[k][2];
			
			if(k%100==0){
				
				Predict_Target_Feature[Act][0]=tempnumberA/100;
				Predict_Target_Feature[Act][1]=tempnumberB/100;
				Predict_Target_Feature[Act][2]=tempnumberC/100;
				
				tempnumberA=0;tempnumberB=0;tempnumberC=0;Act++;
			}k++;
		}k=0;Act=0;
		
		//x,y,z的標準差
				while(k<100 &&Act<1 ){
					tempnumberA+=Math.pow(PredictSet[k][0]-Predict_Target_Feature[Act][0], 2);
					tempnumberB+=Math.pow(PredictSet[k][1]-Predict_Target_Feature[Act][1], 2);
					tempnumberC+=Math.pow(PredictSet[k][2]-Predict_Target_Feature[Act][2], 2);
					
					if(k%100==0){
						Predict_Target_Feature[Act][3]=tempnumberA/100;
						Predict_Target_Feature[Act][4]=tempnumberB/100;
						Predict_Target_Feature[Act][5]=tempnumberC/100;
						
						tempnumberA=0;tempnumberB=0;tempnumberC=0;Act++;
					}k++;
				}k=0;Act=0;
				
		//x,y,z的平均絕對差
				while(k<100 &&Act<1){
					tempnumberA+=PredictSet[k][0]-Predict_Target_Feature[Act][0];
					tempnumberB+=PredictSet[k][1]-Predict_Target_Feature[Act][1];
					tempnumberC+=PredictSet[k][2]-Predict_Target_Feature[Act][2];
					
					if(k%100==0){
						Predict_Target_Feature[Act][6]=tempnumberA/100;
						Predict_Target_Feature[Act][7]=tempnumberB/100;
						Predict_Target_Feature[Act][8]=tempnumberC/100;
						
						tempnumberA=0;tempnumberB=0;tempnumberC=0;Act++;
					}k++;
				}k=0;Act=0;
				
				//平均生成加速度
				while(k<100){
					sum+=Math.sqrt(Math.pow(PredictSet[k][0], 2)+Math.pow(PredictSet[k][1],2)+Math.pow(PredictSet[k][2],2));
					
					if(k%100==0){
						Predict_Target_Feature[Act][9]=(float) (sum/100);
						sum=0;
					}k++;
				}k=0;Act=0;sum=0;
				
				//波峰值時間標記()
				float max_peaks[][] = new float [3][10];k=0;
				float min_peaks[][] = new float [3][10];
				int peaks[][] = new int[30][10];
				for(int s=0;s<3;s++){
					for( j=0;j<1;j++){
						for(int t=0;t<100;t++){
//								if(PredictSet[t+j*100][s]>PredictSet[t+j*100][s]){
//									increase=true;
//								}else if(PredictSet[t+j*100][s]<PredictSet[t+j*100][s] && PredictSet[t+j*100][s]>Predict_Target_Feature[j][0] && increase){
//									increase=false;
//									if(k>=10)
//										break;
//									peaks[j][k]=t;k++;
//								}
							if(PredictSet[t+j*100][s]>max_peaks[s][j])
								max_peaks[s][j]=PredictSet[t+j*100][s];
							if(PredictSet[t+j*100][s]<min_peaks[s][j])
								min_peaks[s][j]=PredictSet[t+j*100][s];
								}k=0;
							}
						}
//				for(k=0;k<18;k++)
//					for(j=0;j<10;j++)
//						Log.d("pm", String.valueOf(peaks[k][j]));
					
					//波峰時間篩選，改為-1的是小於門檻值的波峰
					int amount_peaks[][] = new int[3][10];
					for(int s=0;s<3;s++){
						for(k=0;k<10;k++){
							for(j=3;j<7;j++){
								if(PredictSet[peaks[s*10+k][j]][s]<max_peaks[s][k]*0.9 && max_peaks[s][k]>0 && PredictSet[k*10+j][s]>=PredictSet[k*10+j+3][s]&& PredictSet[k*10+j][s]>=PredictSet[k*10+j-3][s] ){
									amount_peaks[s][k]++;}
								else if(PredictSet[peaks[s*10+k][j]][s]<max_peaks[s][k]-Math.abs(max_peaks[s][k])*0.1 &&max_peaks[s][k]<0 && PredictSet[k*10+j][s]>=PredictSet[k*10+j+3][s]&& PredictSet[k*10+j][s]>=PredictSet[k*10+j-3][s]){
									amount_peaks[s][k]++;
									}
								}
							}
						}
					
//				for(k=0;k<18;k++)
//					for(j=0;j<50;j++)
//						Log.d("cp", String.valueOf(peaks[k][j]));
			    
			    	//每個段落的波峰平均時間(暫不加入)
			    	float time_peaks = 0;
			    	for(k=0;k<3;k++){int t=0;
			    		for(j=0;j<10;j++){
			    			if(amount_peaks[k][j]==0){
			    				Predict_Target_Feature[0][10+k]= (float) (5.0/3.0);//Log.d("nop",String.valueOf(amount_peaks[k][j]));
			    				}
			    			else{
			    				Predict_Target_Feature[0][10+k]=(float) (5.0/amount_peaks[k][j]);
			    			//Log.d("Peak time:",String.valueOf(Motion_Feature[index*6+j/3][10+k]));
			    			}//Motion_Feature[index*10+j][10+k]=(float) (5.0/10.0);
			    			
			    		}
			    	}
				
				//落點區間計算
		    	float max_unit[][]=new float [3][1];
		    	float min_unit[][]=new float [3][1];
		    	
		    	for(int s=0;s<3;s++){
		    		for(j=0;j<1;j++){
		    			max_unit[s][j]=PredictSet[j*100][s];
		    		}
		    	}for(int s=0;s<3;s++){
		    		for(j=0;j<1;j++){
		    			min_unit[s][j]=PredictSet[j*100][s];
		    		}
		    	}
		    	
		    	for(int s=0;s<3;s++){
		    		for(j=0;j<1;j++){
		    			for(k=0;k<100;k++){
		    				if(PredictSet[j*100+k][s]>max_unit[s][j])
		    					max_unit[s][j]=PredictSet[j*100+k][s];
		    			}
		    		}
		    	}
		    	
		    	for(int s=0;s<3;s++){
		    		for(j=0;j<1;j++){
		    			for(k=0;k<100;k++){
		    				if(PredictSet[j*100+k][s]<min_unit[s][j])
		    					min_unit[s][j]=PredictSet[j*100+k][s];
		    			}
		    		}
		    	}
		    	for(int s=0;s<3;s++){
		    		for(j=0;j<1;j++){
		    			//Log.d("inter_predict","max-min interval: "+max_unit[s][j]+":"+min_unit[s][j]);
		    		}
		    	}
		    	
		    	//落點統計
		    	float point_interval;
		    	for(int s=0;s<3;s++){
		    		for(j=0;j<1;j++){
		    			for(k=0;k<100;k++){
		    				point_interval= (float) ((PredictSet[j*100+k][s]-min_unit[s][j])/((max_unit[s][j]-min_unit[s][j])*10));
		    				Predict_Target_Feature[j][(int) (13+s*10+Math.floor(point_interval*10))%10]++;
		    			}	
		    		}
		    	}
		    	
		    	
				
    }
    public void Motion_Reading() {
    	//為處理訓練資料集的特徵值
		//初始化特徵為0，有運算過才為0~1之間的值
		for(int j=0;j<70;j++){
			for(int k=0;k<43;k++){
				Motion_Feature[j][k]=0;}}
		
		for(int index=0;index<7;index++){
			FileReader FR = null;

			
		try {
			FR = new FileReader(Environment.getExternalStorageDirectory().getPath()+"/"+Motion_String[index]+"test.csv");
			BufferedReader BR =  new BufferedReader(FR);
		
			//Log.d("file: "+Motion_String[index]+index, Motion_String[index]);
		
		
		
			try{
				String line;int RowIndex=0;
			
				while((line = BR.readLine())!= null){
					String[] RowData = line.split(",");
					temp[RowIndex][0]=Float.parseFloat(RowData[0]);
					temp[RowIndex][1]=Float.parseFloat(RowData[1]);
					temp[RowIndex][2]=Float.parseFloat(RowData[2]);
					RowIndex++;
					//Log.d("read"+ index,RowData[0]);
				}
			}catch(IOException ex){
					ex.getStackTrace();
					}
			finally{
					try{
						FR.close();
					}catch(IOException e){
						e.getStackTrace();
					}}
			
		}catch(IOException e){
			e.printStackTrace();
		}
			getFeature(temp,index);
			
			
			}
		
		for(int t=0;t<70;t++){
				for(int s=0;s<43;s++){
					//Log.d("Write data: "+t+":"+s,String.valueOf(Motion_Feature[t][s]));
				}
			}
		}	
    
    
    protected void getFeature(float[][] temp_array,int index) {
		// TODO Auto-generated method stub
    	double sum = 0;
		float tempnumberA=0,tempnumberB=0,tempnumberC=0;
		int k=0,j=0;
		int peaks[][] = new int[30][100];
		boolean increase =true;
		
	//	Log.d("f","got feature.");
		for(int x=0;x<1000;x++){
			for(int y=0;y<3;y++){
			//	Log.d("correct","array data: "+temp_array[x][y]);
			}
		}
		
		while(k<1000){
			tempnumberA+=temp_array[k][0];
			tempnumberB+=temp_array[k][1];
			tempnumberC+=temp_array[k][2];
			
			if(k%100==0){
				//x,y,z的平均
				Motion_Feature[index*10+j][0]=tempnumberA/100;
				Motion_Feature[index*10+j][1]=tempnumberB/100;
				Motion_Feature[index*10+j][2]=tempnumberC/100;
				
				tempnumberA=0;tempnumberB=0;tempnumberC=0;j++;
			}k++;
		}k=0;j=0;
			
			//x,y,z的標準差
		while(k<1000){
			tempnumberA+=Math.pow(temp_array[k][0]-Motion_Feature[index*10+j][0], 2);
			tempnumberB+=Math.pow(temp_array[k][1]-Motion_Feature[index*10+j][1], 2);
			tempnumberC+=Math.pow(temp_array[k][2]-Motion_Feature[index*10+j][2], 2);
			
			if(k%100==0){
				Motion_Feature[index*10+j][3]=tempnumberA/100;
				Motion_Feature[index*10+j][4]=tempnumberB/100;
				Motion_Feature[index*10+j][5]=tempnumberC/100;
				
				tempnumberA=0;tempnumberB=0;tempnumberC=0;j++;
			}k++;
		}k=0;j=0;
				
			//x,y,z的平均絕對差
		while(k<1000){
			tempnumberA+=temp_array[k][0]-Motion_Feature[index*10+j][0];
			tempnumberB+=temp_array[k][1]-Motion_Feature[index*10+j][1];
			tempnumberC+=temp_array[k][2]-Motion_Feature[index*10+j][2];
			
			if(k%100==0){
				Motion_Feature[index*10+j][6]=tempnumberA/100;
				Motion_Feature[index*10+j][7]=tempnumberB/100;
				Motion_Feature[index*10+j][8]=tempnumberC/100;
				
				tempnumberA=0;tempnumberB=0;tempnumberC=0;j++;
			}k++;
		}k=0;j=0;
		
		//平均生成加速度
		while(k<1000){
			sum+=Math.sqrt(Math.pow(temp_array[k][0], 2)+Math.pow(temp_array[k][1],2)+Math.pow(temp_array[k][2],2));
			
			if(k%100==0){
				Motion_Feature[index*10+k/100][9]=(float) (sum/100);
				sum=0;
			}k++;
		}k=0;j=0;sum=0;
		
		
		
		//波峰值時間標記()
	float max_peaks[][] = new float [3][10];k=0;
	float min_peaks[][] = new float [3][10];
	for(int s=0;s<3;s++){
		for( j=0;j<10;j++){
			for(int t=1;t<100;t++){
//					if(temp_array[t+j*100][s]>temp_array[t+j*100-1][s]){
//						increase=true;
//					}else if(temp_array[t+j*100][s]<temp_array[t+j*100-1][s] && temp_array[t+j*100][s]>Motion_Feature[index*10+j][0] && increase){
//						increase=false;
//						if(j>=30&&k>=10){
//							break;}
//						peaks[j+s*3][k]=t;k++;
//					}
				if(temp_array[t+j*100][s]>max_peaks[s][j])
					max_peaks[s][j]=temp_array[t+j*100][s];
				if(temp_array[t+j*100][s]<min_peaks[s][j])
					min_peaks[s][j]=temp_array[t+j*100][s];
					}k=0;
				}
			}
//	for(k=0;k<3;k++)
//		for(j=0;j<10;j++)
//			Log.d("mp", String.valueOf(max_peaks[k][j])+";"+String.valueOf(min_peaks[k][j]));
		
		//波峰時間篩選，改為-1的是小於門檻值的波峰
		int amount_peaks[][] = new int[3][10];
		for(int s=0;s<3;s++){
			for(k=0;k<10;k++){
				for(j=5;j<95;j++){
					if(temp_array[k*100+j][s]>(max_peaks[s][k]-min_peaks[s][k])*0.9 && temp_array[k*100+j][s]<0 && temp_array[k*100+j][s]>=temp_array[k*100+j+5][s] && temp_array[k*100+j][s]>=temp_array[k*100+j-5][s]){
						amount_peaks[s][k]++;
						}else if(temp_array[k*100+j][s]>max_peaks[s][k]*0.9 && temp_array[k*100+j][s]>0&& temp_array[k*100+j][s]>=temp_array[k*100+j+5][s] &&temp_array[k*100+j][s]>=temp_array[k*100+j-5][s] ){
						amount_peaks[s][k]++;
						}
						//Log.d("pp",k+";"+j+":"+temp_array[k*100+j][s]+":"+(max_peaks[s][k]-min_peaks[s][k])*0.96);
					}
				}
			}
	
		
//	for(k=0;k<3;k++){
//		for(j=0;j<10;j++){
//			Log.d("cp", k+":"+j+":"+String.valueOf(amount_peaks[k][j]));
//			}}
    
    	//每個段落的波峰平均時間(暫不加入)
    	float time_peaks = 0;
    	for(k=0;k<3;k++){int t=0;
    		for(j=0;j<10;j++){
    			if(amount_peaks[k][j]==0){
    				Motion_Feature[index*10+j][10+k]= (float) (5.0/3.0);//Log.d("nop",String.valueOf(amount_peaks[k][j]));
    				}
//    			else if(amount_peaks[k][j]>30){
//    				Motion_Feature[index*10+j][10+k]=(float) (5.0/20);}
    			else{
    					Motion_Feature[index*10+j][10+k]=(float) (5.0/amount_peaks[k][j]);
    				}
//    			Log.d("Peak time:",String.valueOf(Motion_Feature[index*10+j][10+k]));
    			//Motion_Feature[index*10+j][10+k]=(float) (5.0/10.0);
    			
    		}
    	}
		
		
		//落點區間計算
    	float max_unit[][]=new float [3][10];
    	float min_unit[][]=new float [3][10];
    	
    	for(int s=0;s<3;s++){
    		for(j=0;j<10;j++){
    			max_unit[s][j]=temp_array[j*100][s];
    		}
    	}for(int s=0;s<3;s++){
    		for(j=0;j<10;j++){
    			min_unit[s][j]=temp_array[j*100][s];
    		}
    	}
    	
    	for(int s=0;s<3;s++){
    		for(j=0;j<10;j++){
    			for(k=0;k<100;k++){
    				if(temp_array[j*100+k][s]>max_unit[s][j])
    					max_unit[s][j]=temp_array[j*100+k][s];
    			}
    		}
    	}
    	
    	for(int s=0;s<3;s++){
    		for(j=0;j<10;j++){
    			for(k=0;k<100;k++){
    				if(temp_array[j*100+k][s]<min_unit[s][j])
    					min_unit[s][j]=temp_array[j*100+k][s];
    			}
    		}
    	}
    	for(int s=0;s<3;s++){
    		for(j=0;j<10;j++){
    			//Log.d("interval","max-min interval: "+max_unit[s][j]+":"+min_unit[s][j]);
    		}
    	}
    	
    	//落點統計
    	float point_interval;
    	for(int s=0;s<3;s++){
    		for(j=0;j<10;j++){
    			for(k=0;k<100;k++){
    				point_interval=  10*((temp_array[j*100+k][s]-min_unit[s][j])/((max_unit[s][j]-min_unit[s][j])));
    				//Log.d("interval", String.valueOf(point_interval));
    				Motion_Feature[index*10+j][ (13+s*10+(int)(Math.floor(point_interval*10))%10)]++;
    				//Log.d("range",String.valueOf((int)(Math.floor(point_interval*10))%10));
    			}	
    		}
    	}
	}
    
    public void Normalization_TrainSet(){
    	
    	
    	for(int i=0;i<43;i++){
    		Column_MAX[i]=Motion_Feature[1][i];
    		Column_MIN[i]=Motion_Feature[1][i];
    		//Log.d("iMaMi",String.valueOf(Column_MAX[i])+":"+String.valueOf(Column_MIN[i]));
    	}
    	
    	for(int i=0;i<43;i++){
    		for(int j=0;j<70;j++){
    			if(Column_MAX[i]<Motion_Feature[j][i])
    				Column_MAX[i]=Motion_Feature[j][i];
    			if(Column_MIN[i]>Motion_Feature[j][i])
    				Column_MIN[i]= Motion_Feature[j][i];
    		}
    	}
    	for(int i=0;i<43;i++){
    		//Log.d("iMaMi",String.valueOf(Column_MAX[i])+":"+String.valueOf(Column_MIN[i]));
    	}
    	
    	for(i=0;i<70;i++){
    		for(int j=0;j<43;j++){
    			//有些負數和超過1的數 需不需要取絕對值?
    			if(Column_MAX[j]-Column_MIN[j]!=0){
    			NTSet[i][j] = (Motion_Feature[i][j]-Column_MIN[j])/(Column_MAX[j]-Column_MIN[j]);} 
    			else{
    			NTSet[i][j] = 0;}
    			//Log.d("NTSet",String.valueOf(NTSet[i][j]));Log.d("check",j+" 's "+String.valueOf(Column_MAX[j])+":"+String.valueOf(Column_MIN[j]));
    		}
    	}
    	//fliter outlier
//    	for(i=0;i<70;i++){
//    		for(int j=0;j<43;j++){
//    			if(NTSet[i][j]>1)
//    				NTSet[i][j]=1;
//    			if(NTSet[i][j]<0)
//    				NTSet[i][j]=0;
//    		}
//    	}
    }
    
    public void Normalization_PESet( ){
    	float Column_PMAX[] = new float[43];
    	float Column_PMIN[]= new float[43];
    	
    	for(int i=0;i<43;i++){
    		Column_PMAX[i]=Predict_Target_Feature[0][i];
    		Column_PMIN[i]=Predict_Target_Feature[0][i];
    	}
    	
    	
    	for(int i=0;i<43;i++){
    		for(int j=0;j<1;j++){
    			if(Column_MAX[i]<Predict_Target_Feature[j][i])
    				Column_MAX[i]=Predict_Target_Feature[j][i];
    			if(Column_MIN[i]>Predict_Target_Feature[j][i])
    				Column_MIN[i]= Predict_Target_Feature[j][i];
    		}
    	}
    	
    	for(int i=0;i<43;i++){
    		if(Column_PMAX[i]<Column_MAX[i])
    			Column_PMAX[i]=Column_MAX[i];
    		if(Column_PMIN[i]>Column_MIN[i])
    			Column_PMIN[i]=Column_MIN[i];
    	}
    	
    	for(int i=0;i<1;i++){
    		for(int j=0;j<43;j++){
    			//有些負數和超過1的數 需不需要取絕對值?
    			NPSet[i][j] = (Predict_Target_Feature[i][j]-Column_PMIN[j])/(Column_PMAX[j]-Column_PMIN[j]); 
    			//Log.d("PESet",String.valueOf(NPSet[i][j]));//Log.d("check",j+" 's "+String.valueOf(Column_PMAX[j])+":"+String.valueOf(Column_PMIN[j]));
    		}
    	}
    	
//    	for(int i=0;i<43;i++){
//    		NPSet[0][i]=NTSet[43][i];
//    	Log.d("test",NPSet[0][i]+";"+NTSet[0][i]);}
    	
    	//fliter outlier
//    	for(i=0;i<3;i++){
//    		for(int j=0;j<43;j++){
//    			if(NPSet[i][j]>1)
//    				NPSet[i][j]=1;
//    			if(NPSet[i][j]<0)
//    				NPSet[i][j]=0;
//    		}
//    	}
    }
	
    public void KNN(){
    	float dist[][]=new float[70][43];
    	float D [] =new float[70];
    	int closestclass[]=new int [3];
    	float closedistance[]=new float [3]; 
    	float temp_close=5;
    	int vote[]= new int [7];
    	int index_close=0,j=0;
    	
    	
    	/*11.17修改KNN
    	 * 將其內容的距離忽略掉後面30巷的特徵值，
    	 * 只使用前面13項的結果，但是特徵值依然還是會保留*/
    	
    	for(int i=0;i<70;i++){
    		for( j=0;j<10;j++){
    				dist[i][j]= (float) Math.pow(NPSet[0][j]-NTSet[i][j], 2);
    				//Log.d("sim", String.valueOf(dist[i][j]));
    		}
    	}
    	for(int k=0;k<70;k++){
					D[k]=0;
					
			}
    	
    		for(int k=0;k<70;k++){
    			for( j=0;j<10;j++){
    					D[k]+=dist[k][j];
    				//Log.d("add", String.valueOf(dist[i][k][j]));
    			}//Log.d("dist", String.valueOf(D[i][k]));
    		}
    	
    	
    	
    		for( j=0;j<70;j++){
    			D[j]=(float) Math.sqrt(D[j]);Log.d("D",j+":"+D[j]);
    		}
    	
    	
    		for(int t=0;t<3;t++)
    			closestclass[t]=0;
    				
    		for(int t=0;t<3;t++){temp_close=5;index_close=0;
    			for( j=0;j<70;j++){
    				if(D[j]<temp_close){
    					temp_close=D[j];
    					index_close=j;
    				}
    			}closestclass[t]=index_close/10;closedistance[t]=D[index_close];D[index_close]=3;Log.d("closeclass",String.valueOf(closestclass[t]));Log.d("closeindex",String.valueOf(index_close));
    		}
    	
    	
    		for(j=0;j<3;j++){
    			switch(closestclass[j]){
    				case 0:
    					vote[0]++;
    					break;
    				case 1:
    					vote[1]++;
    					break;
    				case 2:
    					vote[2]++;
    					break;
    				case 3:
    					vote[3]++;
    					break;
    				case 4:
    					vote[4]++;
    					break;
    				case 5:
    					vote[5]++;
    					break;
    				case 6:
    					vote[6]++;
    					break;
    				default:
    					break;
    			}
    		}
    		for(int t=0;t<7;t++)
    	Log.d("vote", t+";"+vote[t]);
    	
    	result[0]=result[1];result[1]=result[2];result[2]=result[3];result[3]=result[4];result[4]=result[5];
    	result[5]=result[6];
    		boolean flag_find=false; 
    		for(j=0;j<7;j++){
    		if(vote[j]>=2 &&flag_find == false){
    				//Toast.makeText(SsensorAct.this, "You are doing "+Motion_String[j], Toast.LENGTH_LONG).show();
    				flag_find=true;
    				result[6]=Motion_String[j];
    				//Log.d("result",Motion_String[j]);
    				break;
    			}
    		else if(flag_find == false) {
    				float temp=3;int order = 0,t=0;
    				//result[2]="unknown";
    				for( t=0;t<3;t++){
    					if(closedistance[t]<temp){
    						temp=closedistance[t];
    						order = t;//Log.d("cec",t+":"+String.valueOf(closedistance[t]));
    						//Log.d("ww","true");
    					}
    				}
    					result[6]=Motion_String[closestclass[order]];
    			}
    		else{
    			result[6] = Motion_String[closestclass[0]];
    		}
    		}
    	if( result[6]==result[5] )
    			OnRun=result[5];
    	else 
    			OnRun="unknown";
    	
    		//Toast.makeText(SsensorAct.this, "You are doing "+OnRun, Toast.LENGTH_LONG).show();
    	tv_progress.setText("Record..\n"+result[0]+";"+result[1]+";"+result[2]+";\n"+result[3]+";"+result[4]+";"+result[5]+";\n"+result[6]);i=0;
    	tv_window.setText("Sliding window:\n"+tempwindow+OnRun);
    	tempwindow=tempwindow+OnRun;
    	

    }
    
    @Override
    protected void onDestroy(){
    	super.onDestroy();
    	
    	if(GThreadHandler !=null){
    		GThreadHandler.removeCallbacks(Acc_Sensing);
    		GThreadHandler.removeCallbacks(Acc_Predict);    	}
    	
    	if(GThread != null){
    		GThread.quit();
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ssensor, menu);
        return true;
    }
 
    @Override
    public void onResume() {
        super.onResume();
        ASM.registerListener(this, ASensor,
                SensorManager.SENSOR_DELAY_NORMAL);
       // GThreadHandler.post(Acc_Sensing);
    }

    @Override
    public void onPause() {
        GThreadHandler.removeCallbacks(Acc_Sensing);
        GThreadHandler.removeCallbacks(Acc_Predict);
        super.onPause();
    }
    
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if(flag){
			G[0]=event.values[0];
			G[1]=event.values[1];
			G[2]=event.values[2];
		
			tv_x.setText("X ="+G[0]);
			tv_y.setText("Y ="+G[1]);
			tv_z.setText("Z ="+G[2]);
			
			flag = false;
		}
	}
}
