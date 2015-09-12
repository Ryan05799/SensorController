package com.example.sensorcontroller;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

public class RecordListFragment extends Fragment {
	
	//Constructor
	public RecordListFragment(){
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_records,
				container, false);		
		ListView recordList = (ListView) rootView.findViewById(R.id.record_list);
		final Button refreshBtn = (Button) rootView.findViewById(R.id.btn_refresh);
		
		//dataList.setAdapter(MainActivity.mMeasurementAdapter);
		
		refreshBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				

			}
		});
		return rootView;

	}
	
}