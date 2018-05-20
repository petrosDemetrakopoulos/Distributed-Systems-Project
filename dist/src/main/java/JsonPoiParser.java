import org.json.JSONException;
import org.json.JSONObject;
import shared.Poi;

import java.util.*;

public class JsonPoiParser {

    private HashMap<Integer, Poi> pois = new HashMap<>();

    public JsonPoiParser(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        Iterator<String> keys = obj.keys();
        Poi poiTemp;
        int counter = 0;
        while (keys.hasNext()) {
            String keyValue = (String) keys.next();
            JSONObject obj1 = obj.getJSONObject(keyValue);
            String poi = obj1.getString("POI");
            double latitude = obj1.getDouble("latidude");
            double longitude = obj1.getDouble("longitude");
            String photos = obj1.getString("photos");
            String poi_category = obj1.getString("POI_category_id");
            String poi_name = obj1.getString("POI_name");
            //        System.out.println(" poi: "+ poi +" latitude : "+latitude + " longitude: " + longitude + " photos: " + photos
            //                 + " poi category: " + poi_category + " poi_name: " + poi_name + " key: " + keyValue);

            poiTemp = new Poi(Integer.parseInt(keyValue), poi, poi_name, latitude, longitude, poi_category, photos);
            pois.put(Integer.parseInt(keyValue), poiTemp);
            counter++;

        }
        //   System.out.println(counter);
    }

    public HashMap<Integer, Poi> getPoisMap() {
        return pois;
    }

    public Poi getSpecificPoi(int key) {
        Poi poi = pois.get(key);
        return poi;
    }
}
