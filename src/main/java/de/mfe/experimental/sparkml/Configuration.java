package de.mfe.experimental.sparkml;

import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class Configuration {

   @Bean
   public DecisionTreeStoryEstimat0r estimat0r() {
      return new DecisionTreeStoryEstimat0r();
   }
}
