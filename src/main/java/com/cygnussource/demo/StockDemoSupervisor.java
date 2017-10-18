package com.cygnussource.demo;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Created by juanjo on 10/17/17.
 */
public class StockDemoSupervisor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props () {
        return Props.create(StockDemoSupervisor.class);
    }

    @Override
    public void preStart() throws Exception {
        log.info("Arrancada aplicacion Demo");
    }

    @Override
    public void postStop() throws Exception {
        log.info("Parada aplicacion Demo");
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }
}
