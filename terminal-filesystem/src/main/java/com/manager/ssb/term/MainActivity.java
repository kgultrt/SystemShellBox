package com.manager.ssb.term;

import android.app.Activity;
import android.os.Bundle;
import com.manager.ssb.term.databinding.ActivityMainBinding;

public class MainActivity extends Activity {
	
	  private ActivityMainBinding binding;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		    binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}