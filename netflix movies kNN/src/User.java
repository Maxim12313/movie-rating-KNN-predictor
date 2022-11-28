//Maxim Kim


public class User implements Comparable<User>{
    private double dist;
    private int id;
    private int commonMovies;

    public User(int id, double dist,int commonMovies){
        this.id = id;
        this.dist = dist;
        this.commonMovies = commonMovies;
    }

    public double getDist(){
        return dist;
    }

    public int getId(){
        return id;
    }

    public int getCommonMoviesNum(){
        return commonMovies;
    }

    public int compareTo(User other){
        return Double.compare(dist,other.getDist());
    }
}
