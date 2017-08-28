package de.mfe.experimental.sparkml;

import static org.apache.spark.sql.functions.*;

import java.util.Arrays;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.DecisionTreeClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.IDF;
import org.apache.spark.ml.feature.IndexToString;
import org.apache.spark.ml.feature.StopWordsRemover;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.mfe.experimental.sparkml.model.Estimation;

@Component
public class DecisionTreeStoryEstimat0r implements Estimat0r {

   private static final Logger LOG = LoggerFactory.getLogger( DecisionTreeStoryEstimat0r.class );
   private final PipelineModel model;
   private final SparkSession sparkSession;

   DecisionTreeStoryEstimat0r() {
      this.sparkSession = SparkSession.builder()
            .appName( "SCP-StoryEstimat0r" )
            .master( "local[2]" )
            .getOrCreate();

      // Prepare training and test data of a stored csv-file, exported from SCP-tracker01 jira
      final DataFrameReader dataFrameReader = sparkSession.read()
            .option( "header", "true" )
            .option( "delimiter", ";" )
            // sets the correct datatypes for the columns - or at least tries to ;)
            .option( "inferSchema", "true" )
            .format( "com.databricks.spark.csv" );

      LOG.info( "Reading CSV training data.." );
      Dataset<Row> trainingDataSet = dataFrameReader
            .load( "src/main/resources/trainingData.csv" );

      // replace all " and \ within each column and lowercase everything
      LOG.info( "Cleaning up imported data..." );

      // was inside the loop, but failed... probably of datatype mismatch
      trainingDataSet
            .withColumn( "Description", regexp_replace( trainingDataSet.col( "Description" ), "[\"\\\\\\\\]", "" ) )
            .withColumn( "Summary", regexp_replace( trainingDataSet.col( "Summary" ), "[\"\\\\\\\\]", "" ) );

      for ( String col : trainingDataSet.columns() ) {
         // this is ugly in java, because of re-assignment..
         trainingDataSet = trainingDataSet
               .withColumn( col, lower( trainingDataSet.col( col ) ) );
      }

      LOG.info( "Using {} - training entries ... SUCH BIG DATA - SUCH MACHINE LEARNING - WOW..",
            trainingDataSet.count() );

      // get the words out of the description + summary
      final Tokenizer tokenizer = new Tokenizer()
            .setInputCol( "Description" )
            .setInputCol( "Summary" )
            .setOutputCol( "words" );

      final StopWordsRemover stopWordsRemover = new StopWordsRemover()
            .setInputCol( "words" )
            .setOutputCol( "filteredWords" );

      // setting the labels
      final StringIndexerModel stringIndexerModel = new StringIndexer()
            .setInputCol( "Story Points" )
            .setHandleInvalid( "skip" )
            .setOutputCol( "indexedLabel" )
            .fit( trainingDataSet );

      LOG.info( "used labels: {}", Arrays.toString( stringIndexerModel.labels() ) );

      // should only be applied to the human language stuff aka description / summary
      final HashingTF hashingTF = new HashingTF()
            .setInputCol( "filteredWords" )
            .setOutputCol( "frequentedWords" )
            .setNumFeatures( 300 );

      // should only be applied to the human language stuff aka description / summary
      final IDF idf = new IDF()
            .setInputCol( "frequentedWords" )
            .setOutputCol( "features" );

      DecisionTreeClassifier dt = new DecisionTreeClassifier()
            .setLabelCol( "indexedLabel" )
            .setFeaturesCol( "features" );

      IndexToString labelConverter = new IndexToString()
            .setInputCol( "prediction" )
            .setOutputCol( "predictedLabel" )
            .setLabels( stringIndexerModel.labels() );

      // Chain indexers and tree in a Pipeline.
      Pipeline pipeline = new Pipeline()
            .setStages( new PipelineStage[] {
                  tokenizer, stringIndexerModel, stopWordsRemover, hashingTF, idf, dt,
                  labelConverter } );

      // Train model. This also runs the indexers.
      this.model = pipeline.fit( trainingDataSet );
   }

   public Estimation estimate( final String storyData ) {

      //      String filePath = "src/main/resources/" + storyData.toLowerCase() + ".csv";

      // TODO convert storyData to dataframe .. https://stackoverflow.com/questions/39963495/creating-a-spark-dataframe-from-a-single-string
      // reading test data

      // Prepare training and test data of a stored csv-file, exported from SCP-tracker01 jira
      final Dataset<Row> testData = sparkSession.read()
            .option( "header", "true" )
            .option( "delimiter", ";" )
            // sets the correct datatypes for the columns - or at least tries to ;)
            .option( "inferSchema", "true" )
            .format( "com.databricks.spark.csv" )
            .csv();

      LOG.warn( "!!! ROCK'N'ROLL !!! Let's predict.." );
      Dataset<Row> predictions = model.transform( testData );
      // Select example rows to display.
      predictions.select( "predictedLabel", "Story Points", "features", "Key" ).show();

      // Select (prediction, true label) and compute test error.
      final MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
            .setLabelCol( "indexedLabel" )
            .setPredictionCol( "prediction" )
            .setMetricName( "accuracy" );

      double accuracy = evaluator.evaluate( predictions );
      LOG.info( "Test Error (1.0-accuracy) = {}", (1.0 - accuracy) );

      final String key = predictions.first().<String> getAs( "Key" );
      final String estimatedStoryPoints = predictions.first().getAs( "predictedLabel" );

      return new Estimation( key, estimatedStoryPoints, "DecisionTree" );
   }
}
