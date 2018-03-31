public class Poi {
    private int id;
    private String name;
    private double latitude;
    private double longitude;
    private String category;

    Poi(int id,String name,double latitude,double longitude,String category){
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
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

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
