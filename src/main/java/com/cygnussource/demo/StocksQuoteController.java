package com.cygnussource.demo;

/**
 * Created by juanjo on 10/17/17.
 */

import akka.pattern.Patterns;
import akka.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/stocks")
public class StocksQuoteController {

    private Timeout timeout = new Timeout(Duration.create(1, "seconds"));

    @Autowired
    StocksQuoteController () {

    }

    @RequestMapping(method = RequestMethod.OPTIONS, value = "/{tickerSymbol}", produces = MediaType.APPLICATION_JSON_VALUE)
    String getQuoteOptions(HttpServletResponse rsp, @PathVariable String tickerSymbol) {
        rsp.setHeader("Allow", "GET,OPTIONS");
        String sb = "{" +
                "\"GET\":{" +
                "\"description\":\"Recupera la cotización del valor indicado\"," +
                "\"parameters\":{" +
                "\"tickerSymbol\":{\"" +
                "type\":\"String\"," +
                "\"description\":\"Identificador del valor\"," +
                "\"required\":\"true\"" +
                "}}}}";

        return sb;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{tickerSymbol}")
    String getQuote(@PathVariable String tickerSymbol) {

        if (tickerSymbol != null && tickerSymbol.trim().length() > 0) {
            StockManager.StockValueReading result = null;

            Future<Object> future = Patterns.ask(DemoApplication.manager, new Stock.StockQuoteReading(tickerSymbol.toUpperCase()), timeout);
            try {
                result = (StockManager.StockValueReading) Await.result(future, timeout.duration());
            } catch (Exception e) {
                throw new DemoApplicationException();
            }

            if (result instanceof Stock.ResponseStockQoute) {
                return "{" + tickerSymbol.toUpperCase() + ":" + ((Stock.ResponseStockQoute) result).stockQuote.get().doubleValue() + "}";

            } else {
                throw new StockNotFoundException();
            }

        } else {
            throw new TickerSymbolOrQuoteRequiredException();
        }
    }

    @RequestMapping(method = RequestMethod.OPTIONS, value = "/{tickerSymbol}/{quote}", produces = MediaType.APPLICATION_JSON_VALUE)
    String setQuoteOptions(HttpServletResponse rsp, HttpServletRequest rqst, @PathVariable String tickerSymbol, @PathVariable String quote) {
        rsp.setHeader("Allow", "POST,OPTIONS");
        String sb = "{" +
                    "\"POST\":{" +
                        "\"description\":\"Añade un valor junto con su cotización\"," +
                        "\"parameters\":{" +
                            "\"tickerSymbol\":{\"" +
                                "type\":\"String\"," +
                                "\"description\":\"Identificador del valor\"," +
                                "\"required\":\"true\"" +
                                "}," +
                            "\"quote\":{" +
                                "\"type\":\"double\"," +
                                "\"description\":\"Cotización del valor\"," +
                                "\"required\":\"true\"" +
                                "}}}}";

        return sb;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{tickerSymbol}/{quote}")
    String setQoute(@PathVariable String tickerSymbol, @PathVariable String quote) {

        if ((tickerSymbol != null && tickerSymbol.trim().length() > 0)
                && (quote != null && quote.trim().length() > 0)) {
            try {

                DemoApplication.manager.tell(new Stock.StockQuoteRegister(tickerSymbol.toUpperCase(), Double.parseDouble(quote)), DemoApplication.manager);

                return "{" + tickerSymbol.toUpperCase() + ":" + quote + "}";

            } catch (NumberFormatException e) {
                throw new QuoteFormatException();
            }
        } else {
            throw new TickerSymbolOrQuoteRequiredException();
        }
    }


    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Valor no registrado")  // 404
    class StockNotFoundException extends RuntimeException {}

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason="Error no determinado") // 500
    class DemoApplicationException extends  RuntimeException {}

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Debe proporcionar un codigo y/o un valor")
    class TickerSymbolOrQuoteRequiredException extends RuntimeException {}

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Formato de cotizacion no valido")
    class QuoteFormatException extends RuntimeException {}





}