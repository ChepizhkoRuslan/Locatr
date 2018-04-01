package com.chepizhko.locatr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class LocatrFragment extends SupportMapFragment {

    private static final String TAG = "LocatrFragment";
    // массив констант со списком всех необходимых разрешений
    private static final String[] LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private static final int REQUEST_LOCATION_PERMISSIONS = 0;
    private Bitmap mMapImage;
    private GalleryItem mMapItem;
    private Location mCurrentLocation;
    // Чтобы использовать Play Services, необходимо создать клиента — экземпляр класса GoogleApiClient
    private GoogleApiClient mClient;
    private GoogleMap mMap;

    public static LocatrFragment newInstance() {
        return new LocatrFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Чтобы создать клиента, создайте экземпляр GoogleApiClient.Builder и настройте его.
        // Как минимум необходимо включить в экземпляр информацию о конкретных API, которые вы собираетесь использовать.
        // Затем вызовите build() для создания экземпляра.
        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                // вызов invalidateOptionsMenu() для обновления состояния элемента меню при получении информации о подключении
                // Информация о состоянии подключения передается через два интерфейса обратного вызова: ConnectionCallbacks и OnConnectionFailedListener
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        // вызов getActivity().invalidateOptionsMenu() для обновления состояния элемента меню при получении информации о подключении
                        getActivity().invalidateOptionsMenu();
                    }
                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .build();
        // Назначение метода SupportMapFragment.getMapAsync(…) полностью соответствует его имени:
        // метод асинхронно получает объект карты. Если вызвать его из onCreate(Bundle),
        // то вы получите ссылку на уже созданный и инициализированный объект GoogleMap.
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                updateUI();
            }
        });
    }
    public void onStart() {
        super.onStart();
        // Вызов connect() для клиента также изменит возможности кнопки меню,
        // поэтому мы вызовем invalidateOptionsMenu() для обновления ее визуального состояния.
        getActivity().invalidateOptionsMenu();
        mClient.connect();
    }
    @Override
    public void onStop() {
        super.onStop();
        mClient.disconnect();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_locatr, menu);
        // Если клиент не подключен, приложение ничего не сможет сделать. Соответственно, мы устанавливаем
        // или снимаем блокировку кнопки в зависимости от состояния подключения клиента
        MenuItem searchItem = menu.findItem(R.id.action_locate);
        searchItem.setEnabled(mClient.isConnected());
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_locate:
                // отправка запроса
                if (hasLocationPermission()) {
                    findImage();
                }
                // следует обработать ситуацию с отсутствием разрешения
                else {
                    // Метод requestPermissions(…) выполняется как собой асинхронный запрос.
                    // При его вызове Android отображает системное диалоговое окно разрешений с сообщением,
                    // соответствующим запрашиваемому разрешению
                    requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    // Для обработки реакции на системное диалоговое окно Android вызывает этот метод обратного вызова
    // при нажатии пользователем кнопки ALLOW или DENY
    // снова проверяет разрешение и вызывает findImage(), если разрешение было предоставлено
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSIONS:
                if (hasLocationPermission()) {
                    findImage();
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }
    // Чтобы получить позиционные данные от API, необходимо построить запрос
    @SuppressLint("MissingPermission")
    private void findImage() {
        LocationRequest request = LocationRequest.create();
        // как следует поступать в ситуации выбора между расходом заряда аккумулятора и точностью выполнения запроса
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // сколько раз должны обновляться позиционные данные
        request.setNumUpdates(1);
        // как часто должны обновляться позиционные данные
        // значение 0, показывающее, что обновление позиционных данных должно происходить как можно быстрее
        request.setInterval(0);
        // прослушивание возвращаемых объектов Location
        // Чтобы вызов requestLocationUpdates(…) успешно работал, сначала необходимо запросить разрешение !!!!!!!!!
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mClient, request, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        Log.i(TAG, "Got a fix: " + location);
                        new SearchTask().execute(location);
                    }
                });
    }
    // метод для проверки доступности первого разрешения в массиве LOCATION_PERMISSIONS
    private boolean hasLocationPermission() {
        int result = ContextCompat
                // используйте версию checkSelfPermission(…) из ContextCompat, чтобы избежать
                // громоздкого кода с проверкой условий. Она обеспечит совместимость автоматически
                .checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }
    // updateUI() для выполнения масштабирования
    private void updateUI() {
        if (mMap == null || mMapImage == null) {
            return;
        }
        LatLng itemPoint = new LatLng(mMapItem.getLat(), mMapItem.getLon());
        LatLng myPoint = new LatLng(
                mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        BitmapDescriptor itemBitmap = BitmapDescriptorFactory.fromBitmap(mMapImage);
        MarkerOptions itemMarker = new MarkerOptions()
                .position(itemPoint)
                .icon(itemBitmap);
        MarkerOptions myMarker = new MarkerOptions()
                .position(myPoint);
        mMap.clear();
        mMap.addMarker(itemMarker);
        mMap.addMarker(myMarker);
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(itemPoint)
                .include(myPoint)
                .build();
        int margin = getResources().getDimensionPixelSize(R.dimen.map_inset_margin);
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, margin);
        mMap.animateCamera(update);
    }

    // AsyncTask для получения объекта GalleryItem поблизости от текущей позиции,
    // загрузки связанного с ним изображения и вывода его в приложении
    private class SearchTask extends AsyncTask<Location,Void,Void> {
        private GalleryItem mGalleryItem;
        private Bitmap mBitmap;
        private Location mLocation;


        @Override
        protected Void doInBackground(Location... params) {
            mLocation = params[0];
            FlickrFetchr fetchr = new FlickrFetchr();
            List<GalleryItem> items = fetchr.searchPhotos(params[0]);
            if (items.size() == 0) {
                return null;
            }
            mGalleryItem = items.get(0);
            try {
                byte[] bytes = fetchr.getUrlBytes(mGalleryItem.getUrl());
                mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } catch (IOException ioe) {
                Log.i(TAG, "Unable to download bitmap", ioe);
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            mMapImage = mBitmap;
            mMapItem = mGalleryItem;
            mCurrentLocation = mLocation;
            updateUI();
        }
    }
}