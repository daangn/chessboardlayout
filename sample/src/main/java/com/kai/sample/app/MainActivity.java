package com.kai.sample.app;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jungkai.chessboardlayout.ChessBoardLayout;

import java.util.ArrayList;
import java.util.List;

/*
 Test Project
*/

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ChessBoardLayout layout = (ChessBoardLayout) findViewById(R.id.layout);

        final ArrayList<Fruit> list = getList();

        final CustomAdapter adapter = new CustomAdapter(this, R.layout.test_layout, list);

        layout.setAdapter(adapter);

        layout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "test position:" + position, Toast.LENGTH_LONG).show();
            }
        });
        layout.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "onItemLongClick:" + position, Toast.LENGTH_LONG).show();
                return true;
            }
        });

//        final GridView gridView = (GridView) findViewById(R.id.gridview);
//        gridView.setAdapter(adapter);

    }

    public ArrayList<Fruit> getList() {
        ArrayList<Fruit> list = new ArrayList<Fruit>();
        list.add(new Fruit("apple", R.drawable.icon_apple));
        list.add(new Fruit("mango", R.drawable.icon_mango));
        list.add(new Fruit("fruit", R.drawable.icon_fruit));
        list.add(new Fruit("orange", R.drawable.icon_orange));
        list.add(new Fruit("mango", R.drawable.icon_mango));
        list.add(new Fruit("apple", R.drawable.icon_apple));
        list.add(new Fruit("orange", R.drawable.icon_orange));
        list.add(new Fruit("fruit", R.drawable.icon_fruit));
        list.add(new Fruit("orange", R.drawable.icon_orange));
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class CustomAdapter extends ArrayAdapter<Fruit> {

        private LayoutInflater inflater;

        private int resource;

        public CustomAdapter(Context context, int resource, List<Fruit> data) {
            super(context, resource, data);
            inflater = LayoutInflater.from(context);
            this.resource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = inflater.inflate(resource, parent, false);
            }

            Fruit fruit = getItem(position);

            TextView tv = (TextView) convertView.findViewById(R.id.tv_test);
            tv.setText(fruit.name);

            ImageView iv = (ImageView) convertView.findViewById(R.id.iv_image);
            iv.setImageResource(fruit.drawableRes);

            return convertView;
        }
    }
}
