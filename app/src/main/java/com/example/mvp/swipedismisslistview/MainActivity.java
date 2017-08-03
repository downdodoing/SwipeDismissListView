package com.example.mvp.swipedismisslistview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwipeDismissListView listView = (SwipeDismissListView) findViewById(R.id.list);
        mList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mList.add("滑动删除" + i);
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, mList);

        listView.setAdapter(adapter);
        listView.setOnDismissCallBack(new OnDismissCallBack() {
            @Override
            public void onDismiss(int dismissPosition) {
                adapter.remove(mList.get(dismissPosition));
            }
        });
    }
}
