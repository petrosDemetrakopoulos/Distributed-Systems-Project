package shared;
import java.io.Serializable;

public class Poi implements Serializable {
    private String poi;
    private int id;
    private String name;
    private double latitude;
    private double longitude;
    private String category;
    private String photos;

    public Poi(int id, String poi,String name,double latitude,double longitude,String category,String photos){
        this.poi = poi;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.photos = photos;
        this.id = id;
    }

    public void setId(int id){
        this.id = id;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    public void setLongitude(double longitude){
        this.longitude = longitude;
    }

    public void setCategory(String category){
        this.category = category;
    }

    public int getId(){
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCategory() {
        return category;
    }

    public String getPhotos() {
        return photos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Poi poi1 = (Poi) o;
        return id == poi1.id &&
                Double.compare(poi1.latitude, latitude) == 0 &&
                Double.compare(poi1.longitude, longitude) == 0 &&
                poi.equals(poi1.poi) &&
                name.equals(poi1.name) &&
                category.equals(poi1.category) &&
                photos.equals(poi1.photos);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
