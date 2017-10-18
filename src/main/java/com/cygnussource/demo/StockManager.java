package com.cygnussource.demo;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Actor que se encarga de mantener las referencias a los actores {@link Stock}, adem&aacute;s de exponer la funcionalidad
 * necesaria para la lectura de valores.
 *
 * Created by juanjo on 10/10/17.
 */
public class StockManager extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    /**
     * Mapa donde se guardan las referencias a los actores (stoks) del que este manager es padre.
     */
    private final Map<String, ActorRef> stocks = new HashMap<>();

    public static Props smProps() {
        return Props.create(StockManager.class);
    }

    /******************************************************/
    /* Definicion de clases para intercambio de mensajes. */
    /******************************************************/

    /**
     * Interface que implementar&aacute;n las respuestas de lectura de cuota, ya sea con la &uacute;ltima cotizaci&oacute;n
     * registrada o el error por no existir el valor solicitado.
     */
    public static interface StockValueReading {
    }

    /**
     * Mensaje indicando que el valor solicitado no est&aacute; entre los valores cotizados.
     */
    public static  final class TickerSymbolNotFound implements StockValueReading {}

    /*****************************************/
    /* Fin definicion de clases de mensajes. */
    /*****************************************/



    /**
     * Evento que encamina al actor correspondiente el registro de una cotizaci&oacute;n.
     * En caso de no existir un actor para el <i>ticker</i> se crear&aacute; uno nuevo.
     *
     * @param register {@link com.cygnussource.demo.Stock.StockQuoteRegister} Contiene el c&oacute;digo de cotizaci&oacute;n
     *                                                                               del que se quiere guardar su valor y su cotizaci&oacute;n.
     */
    private void onStockQuoteRegister(Stock.StockQuoteRegister register) {
        ActorRef stock = stocks.get(register.tickerSymbol);

        if (stock == null) {
            log.info("Creando actor para el valor {}", register.tickerSymbol);
            stock = getContext().actorOf(Stock.props(register.tickerSymbol, "MC"), register.tickerSymbol);
            stocks.put(register.tickerSymbol, stock);
        }

        stock.forward(register, getContext());
    }

    /**
     * Evento para solicitar la &uacute;ltima cotizaci&oacute;n le&iacute;da de un valor.
     * @param reading {@link com.cygnussource.demo.Stock.StockQuoteReading} Contiene el c&oacute;digo de cotizaci&oacute;n
     *                                                                               del que se quiere recuperar su &uacute;ltima cotizaci&oacute;n guardada.
     */
    private void onStockQuoteReading(Stock.StockQuoteReading reading) {
        ActorRef stock = stocks.get(reading.tickerSymbol);
        if (stock != null) {
            stock.forward(reading, getContext());

        } else {
            log.warning("No existe una cotizacion para el valor {}", reading.tickerSymbol);
            sender().tell(new TickerSymbolNotFound(), getSelf());
        }
    }

    @Override
    public void preStart() throws Exception {
        log.info("Arrancado el actor manager");
    }

    @Override
    public void postStop() throws Exception {
        stocks.entrySet().stream().forEach(s -> {
            getContext().stop(s.getValue());
        });

        stocks.clear();

        log.info("Parado el actor manager");
    }

    public Optional<Double> valor;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Stock.StockQuoteRegister.class, this::onStockQuoteRegister)
                .match(Stock.StockQuoteReading.class, this::onStockQuoteReading)
                .build();
    }


}
