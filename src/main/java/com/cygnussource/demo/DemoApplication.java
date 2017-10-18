package com.cygnussource.demo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Arrays;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class DemoApplication {

    static ActorSystem system = ActorSystem.create("testSystem");
    static ActorRef manager = system.actorOf(Props.create(StockManager.class), "manager");


    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner init() {
        return (evt) -> Arrays.asList(
                "GAM,16.98;POP,0.32;ELE,18.94;TRE,26.02;ABE,17.33;IBE,6.58;MAP,2.73;AENA,150.3;BKT,7.86;GAS,18.42;ENG,23.75;REE,17.82;TEF,9.03;AMS,56.33;ANA,67.46;FER,18.34;CLNX,19.3;ACS,30.52;BBVA,7.17;VIS,51.32;SAB,1.66;MRL,11.06;GRF,23.92;ITX,30.86;MTS,22.17;ACX,11.79;FCC,7.96;SAN,5.66;CABK,4.04;DIA,4.54".split(";"))
                .forEach(s -> {
                    String[] stockQuote = s.split(",");
                    manager.tell(new Stock.StockQuoteRegister(stockQuote[0], Double.valueOf(stockQuote[1])), manager);

                });
    }

}





