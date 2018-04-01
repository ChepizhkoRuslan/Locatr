package com.chepizhko.locatr;

import android.location.Location;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "d52b7e32b139061b2443eb37531062fb";
    // вы сможете использовать уже написанный код разбора JSON независимо от того,
    // проводите вы поиск или получаете последние фотографии
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            // Метод Uri.Builder.appendQueryParameter(String,String) автоматически кодирует строки запросов
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            // Значение url_s приказывает Flickr включить URL-адрес для уменьшенной версии изображения, если оно доступно
            .appendQueryParameter("extras", "url_s,geo")
            .build();

    // Метод getUrlBytes(String) получает низкоуровневые данные по URL и возвращает их в виде массива байтов
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        // Этот код создает объект URL на базе строки адреса например www. google.com
        URL url = new URL(urlSpec);
        // вызов метода openConnection() создает объект подключения к заданному URL-адресу
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            // После создания объекта URL и открытия подключения программа многократно вызывает read(),
            // пока в подключении не кончатся данные.
            // Объект InputStream(in) предоставляет байты по мере их доступности
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            // Когда чтение будет завершено, программа закрывает его и выдает массив байтов из ByteArrayOutputStream
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }
    // Метод getUrlString(String) преобразует результат из getUrlBytes(String) в String
    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }
    // методы, инициирующие загрузку: они строят URL и вызывают downloadGalleryItems(String).
    public List<GalleryItem> fetchRecentPhotos() {
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }
    // методы, инициирующие загрузку: они строят URL и вызывают downloadGalleryItems(String).
    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }
    // метод downloadGalleryItems(String) получает URL-адрес и загружает его содержимое
    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();
        try {
            // получаем JSON из запроса по URL-адресу
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            // Тексты JSON легко разбираются в соответствующие объекты Java при помощи конструктора JSONObject(String)
            // и строит иерархию объектов, соответствующую исходному тексту JSON
            JSONObject jsonBody = new JSONObject(jsonString);
            // метод вызывает parseItems(…) и возвращает List с объектами GalleryItem
            parseItems(items, jsonBody);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        }
        return items;
    }
    // метод для построения URL-адреса по значениям метода и запроса
    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);
        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    ///////////////////////////////////// Location //////////////////////
    // поиск, но с указанием широты и долготы
    private String buildUrl(Location location) {
        return ENDPOINT.buildUpon()
                .appendQueryParameter("method", SEARCH_METHOD)
                .appendQueryParameter("lat", "" + location.getLatitude())
                .appendQueryParameter("lon", "" + location.getLongitude())
                .build().toString();
    }
    public List<GalleryItem> searchPhotos(Location location) {
        String url = buildUrl(location);
        return downloadGalleryItems(url);
    }
    ///////////////////////////////////////////////////////////////////////

    // метод для извлечения информации каждой фотографии и добавления её в список.
    // Создайте для каждой фотографии объект GalleryItem и добавьте его в список
    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));
            // Flickr не всегда возвращает компонент url_s для каждого изображения.
            // Добавьте проверку для игнорирования изображений, не имеющих URL-адреса изображения
            if (!photoJsonObject.has("url_s")) {
                continue;
            }
            item.setUrl(photoJsonObject.getString("url_s"));
            item.setOwner(photoJsonObject.getString("owner"));
            item.setLat(photoJsonObject.getDouble("latitude"));
            item.setLon(photoJsonObject.getDouble("longitude"));
            items.add(item);
        }
    }
}
