//Maxim Kim

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.PriorityQueue;
import static java.lang.System.currentTimeMillis;

public class MoviePredictor {
    private SimpleFile ratingsFile;
    private SimpleFile testFile;
    private SimpleFile validationFile;
    private HashMap<Integer,HashMap<Integer,Integer>> trainingData;
    private HashMap<Integer,HashMap<Integer,Integer>> validationData;




    public MoviePredictor(SimpleFile ratingsFile, SimpleFile testFile, SimpleFile validationFile){
        this.ratingsFile = ratingsFile;
        this.testFile = testFile;
        this.validationFile = validationFile;
        long start = currentTimeMillis();
        this.trainingData = processData2(ratingsFile);
        this.validationData = processData2(validationFile);
        System.out.println("Time to process data: "+(currentTimeMillis()-start)/1000.0+" seconds");

    }

    //doesn't work unless you use the data structure from process data 1. Andrew said it was fine if it didn't work anymore
    public double bogusPredict(int movieId, int userId){
        double value = 0;
        Collection<Integer> allMovieRatings = trainingData.get(movieId).values();
        for (int rating:allMovieRatings){
            value+=rating;
        }
        return value/allMovieRatings.size(); //average
    }

    public double predictKNN(int movieId, int userId, int neighborsKNN,boolean weighted){
        PriorityQueue<User> sortedUsers = getClosestUsers(movieId,userId);

        //if there weren't neighborsKNN people that had watched the movie, use the number of available neighbors
        int foundNeighbors = neighborsKNN;
        if (neighborsKNN>sortedUsers.size()) foundNeighbors = sortedUsers.size();

        if (foundNeighbors==0) {
            System.out.println("Nobody in the data set has ever seen this movie");
            return 3;
        }
        if (weighted) return averageOfClosestNeighborsWeighted(foundNeighbors,sortedUsers,movieId);
        else return averageOfClosestNeighbors(foundNeighbors, sortedUsers,movieId);
    }


    private PriorityQueue<User> getClosestUsers(int movieId, int userId){
        PriorityQueue<User> sortedUsers = new PriorityQueue<>(); //least to greatest

        HashMap<Integer,Integer> movieDataTarget = trainingData.get(userId);

        for (int userIdNum:trainingData.keySet()){
            if (userIdNum == userId) continue; //skip if checking target user id

            HashMap<Integer,Integer> movieDataOther = trainingData.get(userIdNum);
            ArrayList<Integer> movieIdSimilar = new ArrayList<>();

            if (!movieDataOther.containsKey(movieId)) continue; //skip if the user has not seen the movie


            //get similar movie id
            for (int availableMovieId:movieDataTarget.keySet()){
                if (movieDataOther.containsKey(availableMovieId)){
                    movieIdSimilar.add(availableMovieId);
                }
            }

            if (movieIdSimilar.size()==0) continue; //skip if user has no movies in common with target


            //loop through similar movies and compute distance
            double distance = 0;
            for (int movieIdNum:movieIdSimilar){
                int otherRating = movieDataOther.get(movieIdNum);
                int targetRating = movieDataTarget.get(movieIdNum);
                double ratingDifference = otherRating-targetRating;
                distance+=Math.pow(ratingDifference,2);
            }
            distance = Math.sqrt(distance/movieIdSimilar.size());
            User userData = new User(userIdNum,distance,movieIdSimilar.size());
            sortedUsers.add(userData);
        }

        return sortedUsers;
    }

    private double averageOfClosestNeighbors(int foundNeighbors, PriorityQueue<User> sortedUsers, int movieId){
        double average = 0;
        for (int i = 0;i<foundNeighbors;i++){
            User profile = sortedUsers.poll();
            int rating = trainingData.get(profile.getId()).get(movieId);
            average+=rating;
        }
        average = average/foundNeighbors;
        return average;
    }


    //Add more of a users ratings into average based on closeness ranking. Add by (listLength-ranking).
    //Bias to closer users

    //Also, add more user ratings into average by number of common similar movies seen
    //Bias to users that have more movies in common
    private double averageOfClosestNeighborsWeighted(int foundNeighbors, PriorityQueue<User> sortedUsers, int movieId){
        //0.9106600946100696 RMSE with 30 neighbors
        int totalTerms = 0;
        double average = 0;

        PriorityQueue<User> localSortedUsers = new PriorityQueue<User>(sortedUsers); //copy to not interfere with main list

        for (int i = 0;i<foundNeighbors;i++){
            User profile = localSortedUsers.poll();
            int ranking = foundNeighbors-i; //from 1 to foundNeighbors
            int similarMovies = profile.getCommonMoviesNum();
            int importance = ranking*similarMovies;
            int rating = trainingData.get(profile.getId()).get(movieId);
            average+=rating*importance;
            totalTerms+=importance;
        }
        average = average/totalTerms;
        return average;
    }

    //I used this for the bogus predictor, but decided to switch the way my data structure was formed for the real knn
    //This is just here as left over
    private HashMap<Integer,HashMap<Integer,Integer>> processData(SimpleFile file){


        HashMap<Integer,HashMap<Integer,Integer>> dataSet = new HashMap<>();
        for (String line : file) {

            String[] information = line.split(";");
            //best 17
            //index and value: 0 is movieId, 1 is userId, 2 is rating

            int movieId = Integer.parseInt(information[0]);
            int userId = Integer.parseInt(information[1]);
            int rating = Integer.parseInt(information[2]);

            if (dataSet.containsKey(movieId)){
                dataSet.get(movieId).put(userId,rating);
            }
            else{
                HashMap<Integer,Integer> userData = new HashMap<>();
                userData.put(userId,rating);
                dataSet.put(movieId,userData);
            }
        }
        return dataSet;
    }


    private HashMap<Integer,HashMap<Integer,Integer>> processData2(SimpleFile file){

        HashMap<Integer,HashMap<Integer,Integer>> dataSet = new HashMap<>();
        for (String line : file) {

            String[] information = line.split(";");
            //best 17
            //index and value: 0 is movieId, 1 is userId, 2 is rating

            int movieId = Integer.parseInt(information[0]);
            int userId = Integer.parseInt(information[1]);
            int rating = Integer.parseInt(information[2]);

            if (dataSet.containsKey(userId)){
                dataSet.get(userId).put(movieId,rating);
            }
            else{
                HashMap<Integer,Integer> movieData = new HashMap<>();
                movieData.put(movieId,rating);
                dataSet.put(userId,movieData);
            }
        }

        return dataSet;
    }

    public void writePredictions(int neighborsKNN, String fileName, boolean weighted, SimpleFile data){
        long start = currentTimeMillis();
        SimpleFile predictions = new SimpleFile(fileName);
        predictions.startWriting();
        PrintStream stream = predictions.getPrintStream();


        for (String line : data) {
            String[] information = line.split(";");
            int movieId = Integer.parseInt(information[0]);
            int userId = Integer.parseInt(information[1]);
            double prediction = predictKNN(movieId,userId,neighborsKNN,weighted);
            stream.println(prediction);
        }
        predictions.stopWriting();
        System.out.println("Time to write "+fileName+": "+(currentTimeMillis()-start)/1000.0+" seconds");
    }


    public double getValidationRMSE(int neighborsKNN,boolean weighted){
        long start = currentTimeMillis();

        double calculatedError = 0;
        int length = 0;
        for (int userId:validationData.keySet()){
            HashMap<Integer,Integer> movieData = validationData.get(userId);
            for (int movieId:movieData.keySet()){
                double actualRating = movieData.get(movieId);
                double predictedRating = predictKNN(movieId,userId, neighborsKNN, weighted);
                double error = predictedRating-actualRating; //find error from difference
                error = Math.pow(error,2); //square error
                calculatedError+=error; //sum all squared error
                length++;
            }
        }

        calculatedError = calculatedError/length; //mean all squared error
        calculatedError = Math.sqrt(calculatedError); //root the mean of all squared error

        System.out.println("Time to getRMSE: "+(currentTimeMillis()-start)/1000.0+" seconds");
        return calculatedError;
    }

    public int getBestK(int startK, int endK){
        System.out.println("");
        System.out.println("Unweighted");
        long start = currentTimeMillis();
        HashMap<Integer,Double> errorByK = new HashMap<>();

        int length = 0;
        for (int userId:validationData.keySet()){
            HashMap<Integer,Integer> movieData = validationData.get(userId);
            for (int movieId:movieData.keySet()){
                double actualRating = movieData.get(movieId);
                PriorityQueue<User> sortedUsers = getClosestUsers(movieId,userId);

                int foundNeighbors = endK;
                if (endK>sortedUsers.size()) foundNeighbors = sortedUsers.size(); //if there weren't neighborsKNN people that had watched the movie

                if (foundNeighbors==0) {
                    System.out.println("Nobody in the data set has ever seen this movie");
                    continue;
                }


                int totalTerms = 0;
                double sumRating = 0;
                double[] averageList = new double[foundNeighbors];


                //make predictions for each k level
                for (int i = 0;i<foundNeighbors;i++){
                    User profile = sortedUsers.poll();
                    int rating = trainingData.get(profile.getId()).get(movieId);
                    sumRating+=rating;
                    totalTerms++;
                    averageList[i] = sumRating/totalTerms;
                }

                //find square error of each k level prediction vs actual and store in hashmap
                for (int k=startK;k<=endK;k++){
                    double prediction;
                    if (k>foundNeighbors){
                        prediction = averageList[foundNeighbors-1];
                    }
                    else{
                        prediction = averageList[k-1];
                    }
                    double error=actualRating-prediction;
                    error = Math.pow(error,2);


                    if (!errorByK.containsKey(k)){
                        errorByK.put(k,error);
                    }
                    else{
                        errorByK.put(k,errorByK.get(k)+error);
                    }
                }

                length++;
            }
        }
        int bestK = compareRMSE(startK,endK,length,errorByK);
        System.out.println("Time to test unweighted: "+(currentTimeMillis()-start)/1000.0+" seconds");
        System.out.println("");

        return bestK;
    }


    private int compareRMSE(int startK,int endK,int length, HashMap<Integer,Double> errorByK){
        //compute meansquareroot error of each k level from their sums of square error
        int bestK = 1;
        double bestRMSE = Double.POSITIVE_INFINITY;
        for (int u=startK;u<=endK;u++){
            double calculatedError = errorByK.get(u);
//            System.out.println(calculatedError+" calculated error"+ length);
            calculatedError = calculatedError/length; //mean all squared error
            calculatedError = Math.sqrt(calculatedError); //root the mean of all squared error
            System.out.println("k: "+u+"     RMSE: "+calculatedError);
//            System.out.println("("+u+","+calculatedError+")");
            if (calculatedError<bestRMSE){
                bestRMSE = calculatedError;
                bestK = u;
            }
        }
        System.out.println("best k: "+bestK+"     best RMSE: "+bestRMSE);
        return bestK;
    }


    public int getBestKWeighted(int startK, int endK){
        System.out.println("");
        System.out.println("Weighted");
        long start = currentTimeMillis();
        HashMap<Integer,Double> errorByK = new HashMap<>();

        int length = 0;
        double bestRMSE = Double.POSITIVE_INFINITY;
        for (int userId:validationData.keySet()) {
            HashMap<Integer, Integer> movieData = validationData.get(userId);
            for (int movieId : movieData.keySet()) {
                double actualRating = movieData.get(movieId);
                PriorityQueue<User> sortedUsers = getClosestUsers(movieId,userId);



                for (int k=startK;k<=endK;k++){
                    int foundNeighbors = k;
                    if (k>+sortedUsers.size()) foundNeighbors = sortedUsers.size()-1; //if there weren't k people that had watched the movie

                    double prediction = averageOfClosestNeighborsWeighted(foundNeighbors,sortedUsers,movieId);
                    double error=actualRating-prediction;
                    error = Math.pow(error,2);

                    if (!errorByK.containsKey(k)){
                        errorByK.put(k,error);
                    }
                    else{
                        errorByK.put(k,errorByK.get(k)+error);
                    }
                }
                length++;
            }
        }
        int bestK = compareRMSE(startK,endK,length,errorByK);
        System.out.println("Time to test weighted: "+(currentTimeMillis()-start)/1000.0+" seconds");
        System.out.println("");
        return bestK;
    }

}

