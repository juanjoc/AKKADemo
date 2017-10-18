# AKKA Demo

Código que completa el artículo publicado en el blog de Cygnus Source sobre AKKA.

## Getting Started

La finalidad de este pequeño proyecto no es otra que la de mostrar de una forma más práctica lo expuesto en el artículo
publicado en el blog de Cygnus Source sobre la tecnología AKKA.

Puedes seguir el artículo mediante este [enlace](http://www.cygnussource.com/creacion-microservicio-apache-tomee/). 

### Funcionalidad
La funcionalidad propuesta se ha intentado mantener lo más simple posible. 
Se ha pensado en crear una aplicación que fuese capaz de recoger las cotizaciones bursátiles de unas aciones (por ejemplo, del mercado contínuo) y poder consultar
su valor en cualquier momento.

Evidentemente, crear un sistema por el cual la aplicación estuviera en todo momento actualizada con los valores adecuados 
quedaba muy lejos del alcance de este proyecto. Se obtó en tonces por una pequeña carga inicial con una serie de valores, sin
más pretensiones.

Toda la aplicación es accesible a través de lo que podríamos llamar un 'microservicio', el cuál es expuesto a través de un Tomcat embebido y que puede ser ejecutado de forma totalmente 
autónoma desde la línea de comandos. Esta funcionalidad nos la proporciona Springboot.


### Prerequisitos

Para poder ejecutar el proyecto se deberá tener instalado en el sistema:
* JDK 1.8
* Maven 3.x

```
Give examples
```

### Ejecución
Una vez se tenga descargado y descomprimido (en su caso) el código de la aplicación, con Maven se podrá ejecutar desde la línea
de comandos de la siguiente forma:

```
cd [dir_install]
mvn clean spring-boot:run
```


## Estructura

Como se ha mencionado, es un proyecto simple, por lo que su estructura también lo es.

Si revisamos el directorio con los fuentes, veremos que sólo existen 4 archivos. Esto es lo más destacado de cada uno de ellos.

**DemoApplication.java**
 
 Archivo con los fuentes que se encargan de arrancar la aplicación. Como pdemos ver, es un fichero de arranque controlado
 por Springboot.
 
 Nos encontramos con dos sentencias importantes aquí:
 
 ```
 static ActorSystem system = ActorSystem.create("testSystem");
 static ActorRef manager = system.actorOf(Props.create(StockManager.class), "manager");
 ```
 En estas dos línea se declara el sistema de actores, el contexto de todos los actores que se creen en la aplicación.
 También se declara el primer actor (manager) que será el responsable de exponer toda la funcionalidad necesaria para 
 la inclusión de nuevos valores y sus consultas.
 
 También está el método init perteneciente a Springboot en el que se ha colocado una carga inicial de valores simulando 
 el on-line de un sistema que alimentase contínuamente a nuestro proyecto con los valores y cotizaciones actualizados.
 
 Por cada pareja de XXX,99.99 se realiza una llamada a nuestro mánager que incluirá el nuevo valor en el sistema.
 
 
 
**StocksQuoteController.java**

Este fichero, anotado como @RestController, es el responsable de hacer accesible, a través de un servicio web tipo REST,
la funcionalidad de la apliación.

Además de establecer la ruta de la aplicación "/stocks", expone dos servicios:

* **/stocks/{tickerSymbol}** (GET )Nos proporciiona la última cotización registrada en el sistema para el valor (tickerSymbol) indicado.
* **/stocks/{tickerSymbol}/{quote}** (POST) Permite la inclusión de un nuevo valor junto con su cotización en el sistema.

Dado que cuando trabajemos con AKKA deberemos usar futuros para extraer del sistema de actores valores derivados de sus
computaciones, en este método podemos ver cómo hacerlo.

Se usa la clase Patterns de la librería de AKKA llamando a su método ask:

```java
Future<Object> future = Patterns.ask(DemoApplication.manager, new Stock.StockQuoteReading(tickerSymbol.toUpperCase()), timeout);
```

Además de pasarle un timeout (se crea unas líneas más arriba) donde se indica al sistema de actores cuánto esperar por una
respuesta (no bloqueo ante errores o caídas de sistemas externos como bb.dd.) se le pasa como primer parámetro el propio
actor mánager y el mensaje que queremos que atienda (protocolo de mensajes).

Una vez obtenido el futuro del resultado de la consulta, mediante el método result de la clase Await, podremos "materializar"
ese resultado para poder usarlo de una forma convencional.

```java
result = (StockManager.StockValueReading) Await.result(future, timeout.duration());
```

Existen otros modos de obtener ese resultado, por ejemplo, usando los métodos onSuccess o onComplete, como se puede 
ver en este código de ejemplo que nos devolvería el código del valor cotizado:

```java
String s = "";
future.onSuccess(new OnSuccess<Object>() {
    @Override
    public void onSuccess(Object result) throws Throwable {
        s = ((Stock.ResponseStockQoute) result).tickerSymbol;
    }
}, system.dispatcher());
```

En sistemas multihilo no es muy recomendable usar Await, por lo que se recomienda mejor los onSucces, onComplete y onFailure
para conseguir el resultado del proceso de un actor.

Por último, en esta clase, podemos ver una serie de clases declaradas para devolver errores del sistema a través de códigos
HTML.

**Stock.java**

Esta clase representa a un actor con una serie de tareas muy concretas. Entre ellas, está la de registrar un valor para una 
determinada acción bursátil. Los actores Stock son creados por StockManager y es el único punto de acceso a ellos.

Como se dice en el post, en un patrón actor no es posible definir una interface de comunicación con el sistema, sino que 
hay que definir un protocolo. Normalmente, este protocolo, se define a través de clases anidadas dentro de la propia clase
que maneja al actor.

En concreto, el protocolo definido para Stock es el siguiente:

```java
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
```

Como se puede observar, el actor Stock maneja 4 tipos de mensaje. Dos de ellos, StockQuoteRegister y StockQuoteReading, 
son mensajes ante los que ejecutará ciertas acciones, y los otros dos, son los mensajes con los que responderá una vez
ejecutadas estas acciones.

Otro método importante de todo actor (ya que se debe sobreescribir) es createReceive. Es el método que es invocado por
el sistema cuando se consumen los mensajes de la cola del actor.

También encontramos en esta clase (y normalmente en todas las clases que maneja un actor, aunque no es obligado) dos 
método que se ejecutan justo antes de la creación del actor y antes de su parada. Son los métodos preStart y postStop.



**StockManager.java**

Es el punto de acceso desde la aplicación al sistema de actores. Como se puede ver, extiende de AbstractActor, lo que
indica ya su naturaleza.

Los actores nunca son creados por el usuario a través de un constructor, sino que se usa el contexto (system) antes creado
para crear nuevos actores o recuperar existentes a través de su nombre.

Este mánager es el encargado de gestionar los actores hijos que, a su vez, se encargan de almacenar el valor de una acción.

En su protocolo están definidos dos tipos de mensajes, StockQuoteRegister para el registro de un nuevo valor y su cotización
y StockQuoteReading, para leer la cotización de un valor guardado.

Los métodos de su protocolo son:

```java
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
```

En este caso, el método preStop se ha usado para recorrer la colección de actores creados (se guarda su referencia en un 
mapa) y ordenar su parada.


## Autores

* **Juanjo Cuadrado** - *Trabajo inicial* - [juanjoc](https://github.com/juanjoc)

Ver la lista de [contribuidores](https://github.com/juanjoc/AKKADemo/contributors) que participaron en este proyecto.

## Licencia

Este proyecto está licenciado como GNU General Public License (GPL) 3.0 - ver el fichero [LICENCE.md](https://github.com/juanjoc/AKKADemo/blob/master/LICENCE.md) para más detalles.


## Agradecimientos

* Al nicho de conocimiento inagotable que es [Stack Overflow](https://es.stackoverflow.com/). 
* A nuestra no siempre bien reconocida [Wikipedia](https://es.wikipedia.org/wiki/Wikipedia:Portada).
* A la documentación en línea de [AKKA](https://doc.akka.io/docs/akka/current/java/guide/index.html), todo un lujo.
* A [Cygnus Source](http://www.cygnussource.com) por obligarme.
