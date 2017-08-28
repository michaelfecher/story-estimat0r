package de.mfe.experimental.sparkml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan( basePackages = { "de.mfe.experimental.sparkml" } )
public class DemoApplication {

   public static void main( String[] args ) {
      SpringApplication.run( DemoApplication.class, args );
   }
}
