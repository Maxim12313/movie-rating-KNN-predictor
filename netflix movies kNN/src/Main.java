//Maxim Kim

public class Main {

    public static void main(String[] args) {
        SimpleFile ratingsFile = new SimpleFile("ratings.txt");
        SimpleFile testFile = new SimpleFile("test.txt");
        SimpleFile validationFile = new SimpleFile("validation.txt");

        MoviePredictor predictor = new MoviePredictor(ratingsFile,testFile,validationFile);

//        System.out.println(predictor.getBestParamaters(10,40,5,1.5,5,0.5));
//        System.out.println(predictor.getRMSEWrite(30));

        int bestKUnweighted = predictor.getBestK(1,100);
//        predictor.writePredictions(bestKUnweighted,"validation-predictions-unweighted.txt",false,validationFile);
//        predictor.writePredictions(bestKUnweighted,"test-predictions-unweighted.txt",false,testFile);
//        System.out.println("k: "+bestKUnweighted+"          RMSE: "+predictor.getValidationRMSE(bestKUnweighted,false));

        int bestKWeighted = predictor.getBestKWeighted(1,100);
        predictor.writePredictions(bestKWeighted,"validation-predictions.txt",true,validationFile);
        predictor.writePredictions(bestKWeighted,"test-predictions.txt",true,testFile);
//        System.out.println("k: "+bestKWeighted+"          RMSE: "+predictor.getValidationRMSE(bestKWeighted,true));

    }
}
