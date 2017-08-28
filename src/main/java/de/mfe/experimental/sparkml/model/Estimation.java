package de.mfe.experimental.sparkml.model;

public class Estimation {

   private final String storyId;
   private final String predictedStoryPoints;
   private final String usedClassification;

   public Estimation( final String storyId, final String predictedStoryPoints,
         final String usedClassification ) {
      this.storyId = storyId;
      this.predictedStoryPoints = predictedStoryPoints;
      this.usedClassification = usedClassification;
   }

   public String getStoryId() {
      return storyId;
   }

   public String getPredictedStoryPoints() {
      return predictedStoryPoints;
   }

   public String getUsedClassification() {
      return usedClassification;
   }
}
