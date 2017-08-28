package de.mfe.experimental.sparkml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.mfe.experimental.sparkml.model.Estimation;

@RestController
public class Estimat0rController {

   private Estimat0r decisionTreeStoryEstimat0R;

   @Autowired
   public Estimat0rController( final Estimat0r estimat0r ) {
      this.decisionTreeStoryEstimat0R = estimat0r;
   }

   @PostMapping( "/estimate" )
   public ResponseEntity<Estimation> getEstimation( @RequestBody final String storyToEstimate ) {

      final Estimation estimate = decisionTreeStoryEstimat0R.estimate( storyToEstimate );
      return ResponseEntity.ok( estimate );
   }

}
