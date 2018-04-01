package com.chepizhko.locatr;


import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public abstract class SingleFragmentActivity extends AppCompatActivity {

    protected abstract Fragment createFragment();

    // аннотация @LayoutRes, чтобы сообщить Android Studio,
    // что любая реализация этого метода должна возвращать действительный идентификатор ресурса макета
    @LayoutRes
    // теперь субклассы SingleFragmentActivity могут переопределить getLayoutResId()
    // для возврата макета, отличного от activity_fragment.xml
    protected int getLayoutResId() {
        return R.layout.activity_fragment;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // так фрагмент привязан к определённому макету
        //setContentView(R.layout.activity_fragment);
        setContentView(getLayoutResId());

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = createFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}
