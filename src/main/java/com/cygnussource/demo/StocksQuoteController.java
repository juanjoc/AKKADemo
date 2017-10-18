package com.cygnussource.demo;

/**
 * Created by juanjo on 10/17/17.
 */

import akka.pattern.Patterns;
import akka.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

@RestController
@RequestMapping("/stocks")
public class StocksQuoteController {

    private Timeout timeout = new Timeout(Duration.create(1, "seconds"));

    @Autowired
    StocksQuoteController () {

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