package com.cygnussource.demo;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Optional;

/**
 * Actor para controlar las cotizaciones de un determinado valor.
 * param codCotizacion {@link String} C&oacute;digo de cotizaci&oacute;n. C&oacute;digo asignado por entidad bursatil.
 *
 * Created by juanjo on 10/9/17.
 */
public class Stock extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    /**
     * C&oacute;digo burs&aacute;til de este valor.
     */
    private String tickerSymbol;

    /**
     * Identificador del grupo al que pertenece este valor.
     */
    private String stockGroupId;

    /**
     * Connstructor que proporciona un valor asignando un identificador de valor y un identificador del grupo al que
     * pertenece.
     *
     * @param tickerSymbol {@link String} C&oacute;digo burs&aacute;til de este valor.
     * @param stockGroupId {@link String} Identificador de grupo.
     */
    public Stock(String tickerSymbol, String stockGroupId) {
        this.tickerSymbol = tickerSymbol;
        this.stockGroupId = stockGroupId;
    }

    /**
     * Crea un objeto {@link Props} para este actor.
     *
     * @param tickerSymbol {@link String} C&oacute;digo burs&aacute;til de este valor.
     * @param stockGroupId {@link String} Identificador de grupo.
     *
     * @return {@link Props} Objeto con las propiedades para este actor.
     */
    public static Props props (String tickerSymbol, String stockGroupId) {
        return Props.create(Stock.class, tickerSymbol, stockGroupId);
    }

    /******************************************************/
    /* Definicion de clases para intercambio de mensajes. */
    /******************************************************/

    /**
     * Mensaje de registro de cotizaci&oacute;n de un valor.
     */
    public static final class StockQuoteRegister {
        final String tickerSymbol;
        final double stockQuote;

        public StockQuoteRegister(String tickerSymbol, double stockQuote) {
            this.tickerSymbol = tickerSymbol;
            this.stockQuote = stockQuote;
        }
    }
    /**
     * Mensaje indicando que un valor ha registrado su cotizaci&oacute;n.
     */
    public static final class StockQuoteRegistered {
        final String tickerSymbol;

        public StockQuoteRegistered(String tickerSymbol) {
            this.tickerSymbol = tickerSymbol;
        }
    }


    /**
     * Mensaje solicitando la cotizaci&oacute;n de un valor.
     */
    public static final class StockQuoteReading {
        public String tickerSymbol;

        public StockQuoteReading(String tickerSymbol) {
            this.tickerSymbol = tickerSymbol;
        }
    }
    /**
     * Mensaje que devuelve la cotizaci&oacute;n le&iacute;da de un valor.
     */
    public static final class ResponseStockQoute implements StockManager.StockValueReading {
        public String tickerSymbol;
        public Optional<Double> stockQuote;

        public ResponseStockQoute(String tickerSymbol, Optional<Double> stockQuote) {
            this.tickerSymbol = tickerSymbol;
            this.stockQuote = stockQuote;
        }
    }

    /*****************************************/
    /* Fin definicion de clases de mensajes. */
    /*****************************************/

    /**
     * &Uacute;ltima cotizaci&oacute;n le&iacute;da para el valor.
      */
    Optional<Double> stockQuote = Optional.empty();

    @Override
    public void preStart() throws Exception {
        log.info("Arrancado el actor del valor {}-{}", tickerSymbol, stockGroupId);
    }

    @Override
    public void postStop() throws Exception {
        log.info("Parado el actor del valor {}-{}", tickerSymbol, stockGroupId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()

                //Registro de cotizacion
                .match(StockQuoteRegister.class, reg -> {
                    log.info("Cotizacion registrada para {} con valor {}", reg.tickerSymbol, reg.stockQuote);

                    stockQuote = Optional.of(reg.stockQuote);
                    getSender().tell(new StockQuoteRegistered(reg.tickerSymbol), getSelf());
                })

                //Lectura de la &uacute;ltima cotizaci&oacute.
                .match(StockQuoteReading.class, lec -> {
                    log.info("Cotizacion leida {} {}", lec.tickerSymbol, this.stockQuote);
                    getSender().tell(new ResponseStockQoute(lec.tickerSymbol, stockQuote), getSelf());
                })

                .build();
    }

    /**
     * Recupera el identificador del valor.
     * @return {@link String} Identificador del valor.
     */
    public String getTickerSymbol() {
        return tickerSymbol;
    }

    /**
     * Establece el idenficador del valor.
     * @param tickerSymbol {@link String} Identificador.
     */
    public void setCodCotizacion(String tickerSymbol) {
        this.tickerSymbol = tickerSymbol;
    }

    /**
     * Recupera el identificador del grupo.
     * @return {@link String} Identificador de grupo.
     */
    public String getStockGroupId() {
        return stockGroupId;
    }

    /**
     * Establece el idenficador del grupo al que pertenece el valor.
     * @param stockGroupId {@link String} Identificador de grupo.
     */
    public void setStockGroupId(String stockGroupId) {
        this.stockGroupId = stockGroupId;
    }
}
