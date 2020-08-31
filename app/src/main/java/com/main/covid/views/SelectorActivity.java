package com.main.covid.views;

import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.main.covid.R;
import com.main.covid.views.MainActivity;

import java.util.List;

public class SelectorActivity extends AppCompatActivity {

    private String[] urls, baseUrls, names;
    private Integer ARG_ALL_PERM = 1000;

    private List<String> listOfPermissions;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);

        urls = getString(R.string.url).split(",");
        baseUrls = getString(R.string.base_url).split(",");
        names = getString(R.string.names).split(",");

        execute();

        /*try
        {
            listOfPermissions = Arrays.asList(getResources().getStringArray(R.array.permissions));

            requestPhotoPermissions(listOfPermissions);


        }catch (Resources.NotFoundException e)
        {
            //no permissions
            execute();
        }*/



    }

    private void execute()
    {
        if (urls.length==1) {

            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            final LinearLayout linearLayout = findViewById(R.id.linearLayout);

            for (int i = 0; i < urls.length; i++) {
                View v = View.inflate(this, R.layout.item_selector, null);
                ((TextView)v.findViewById(R.id.text)).setText(names[i]);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(v.getContext(), MainActivity.class);
                        i.putExtra(MainActivity.EXTRA_URL, urls[linearLayout.indexOfChild(v)]);
                        i.putExtra(MainActivity.EXTRA_BASE_URL, baseUrls[linearLayout.indexOfChild(v)]);
                        startActivity(i);
                    }
                });

                linearLayout.addView(v);
            }


        }
    }

    private void requestPhotoPermissions(List<String> listPermissions)
    {
        if(!checkAllPermissions(listPermissions))
        {
            ActivityCompat.requestPermissions(this,listPermissions.toArray(new String[0]),ARG_ALL_PERM);
        }else
        {
            execute();
        }
    }

    private Boolean checkAllPermissions(List<String> listPermissions)
    {
        for(String element: listPermissions)
        {
            if(ContextCompat.checkSelfPermission(this,element) != PackageManager.PERMISSION_GRANTED)
                return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ARG_ALL_PERM && resultCode == RESULT_OK) {

            execute();
        }


    }




}
