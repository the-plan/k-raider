package org.typeunsafe

import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.*
import io.vertx.core.http.HttpClient
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

import io.vertx.kotlin.core.http.HttpServerOptions
import io.vertx.ext.web.handler.StaticHandler

import io.vertx.servicediscovery.types.HttpEndpoint
import io.vertx.servicediscovery.ServiceDiscovery
//import io.vertx.servicediscovery.Status

import io.vertx.servicediscovery.ServiceDiscoveryOptions
import io.vertx.servicediscovery.Record

import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.Status
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.circuitbreaker.CircuitBreakerOptions

import io.vertx.kotlin.core.json.*
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint

import me.atrox.haikunator.HaikunatorBuilder
import java.util.*
import kotlin.reflect.KClass


private fun RoutingContext.json(jsonObject: JsonObject) {
  this.response().putHeader("content-type", "application/json;charset=UTF-8").end(jsonObject.encodePrettily())
}



fun main(args: Array<String>) {
  val vertx = Vertx.vertx()
  vertx.deployVerticle(Raider())

}

class Raider : AbstractVerticle() {

  private var discovery: ServiceDiscovery? = null
  private var record: Record? = null
  //private val discoveredServices: List<Record>? = null
  private var baseStar: BaseStar? =null
  private var healthCheck: HealthCheckHandler? =null
  private var breaker: CircuitBreaker? =null

  private val x: Double = 0.0
  private val y: Double = 0.0

  private val xVelocity: Double? = 1.0
  private val yVelocity: Double? = -1.0

  override fun stop(stopFuture: Future<Void>) {
    super.stop()
    println("Unregistration process is started (${record?.registration})...")

    discovery?.unpublish(record?.registration, { ar ->
      when {
        ar.failed() -> {
          println("😡 Unable to unpublish the microservice: ${ar.cause().message}")
          stopFuture.fail(ar.cause())
        }
        ar.succeeded() -> {
          println("👋 bye bye ${record?.registration}")
          stopFuture.complete()
        }
      }
    })
  }

  fun watchingMyBaseStar(baseStar: BaseStar) {

    vertx.setPeriodic(1000, { timerId ->
      // and the breaker ??? TODO: use the breaker before ?
      baseStar.client.get("/health").send { asyncGetRes ->
        when {
          asyncGetRes.failed() -> {
            record?.metadata?.getJsonObject("coordinates")
              ?.put("xVelocity",0)
              ?.put("yVelocity",0)
            // btw, you never stop in space 👾

            discovery?.update(record, {asyncUpdateResult ->
              println("😡😡😡 I'm alone ???")
            })
            
            // time to search a new basestar
            vertx.setTimer(3000, { id ->
              vertx.cancelTimer(timerId)
              searchAndSelectOneBaseStar()
            })
            
            // if we have no more basestar ??? => had to be managed in searchAndSelectOneBaseStar()

          }
          asyncGetRes.succeeded() -> {
            // 😃 === all is fine ===
            //println(asyncGetRes.result().bodyAsJsonObject())
          }
        }
      }


    })

  }

  /**
   * See: http://vertx.io/docs/vertx-service-discovery/kotlin/
   * Once you have chosen the Record, you can retrieve a ServiceReference and then the service object
   */
  fun subscribeToBaseStar(selectedRecord: Record) {

    val serviceReference = discovery?.getReference(selectedRecord)
    val webClient = serviceReference?.getAs(WebClient::class.java)


    // 👋 === CIRCUIT BREAKER === try to register to the basestar

    breaker?.execute<String>({ future ->

      webClient?.post("/api/raiders")?.sendJson(json { obj("registration" to record?.registration )}, { baseStarResponse ->

        when {
          baseStarResponse.failed() -> {
            this.baseStar = null // 😧 remove the basestar
            future.fail("😡 ouch something bad happened")
          }

          baseStarResponse.succeeded() -> {
            println("👏 you found a basestar ${baseStarResponse?.result()?.bodyAsJsonObject()?.encodePrettily()}")
            val selectedBaseStar = BaseStar(selectedRecord, webClient)
            this.baseStar = selectedBaseStar
            // 👀--- time to check the health of my basestar
            watchingMyBaseStar(selectedBaseStar)

            future.complete("😃 yesss!")

          }
        }

      })

    })?.setHandler({ breakerResult ->
  
      // TODO: eg, kill the raider
      // Do something with the result when future completed or failed
    })


  }

  fun searchAndSelectOneBaseStar() {

    /* 🤖 === search for a baseStar in the discovery backend === */

    discovery?.getRecords(
      {r -> r.metadata.getString("kind") == "basestar" && r.status == io.vertx.servicediscovery.Status.UP },
      { asyncResult ->
        when {
        // --- 😡 ---
          asyncResult.failed() -> { }
        // --- 😃 ---
          asyncResult.succeeded() -> {
            val baseStarsRecords = asyncResult.result()
            
            // === choose randomly a basestar === ⚠️ search a better computation

            baseStarsRecords.size.let {
              when(it) {
              // --- 😡 --- oh oh no basestar online ?!!!
                0 -> {
                  println("--- 😡 --- oh oh no basestar online ?!!!")
                  searchAndSelectOneBaseStar()
                } // ⚠️ wait and retry
                else -> {
                  val selectedRecord = baseStarsRecords.get(Random().nextInt(it)) // ? -1
                  subscribeToBaseStar(selectedRecord)
                }
              }
            }
          }
        }
      }
    ) // ⬅️ end of the discovery
  }

  override fun start() {

    fun random(min: Double, max: Double): Double {
      return (Math.random() * (max+1.0-min))+min
    }
    
    /* 🔦 === Discovery part === */

    // Redis Backend settings

    val redisPort= System.getenv("REDIS_PORT")?.toInt() ?: 6379
    val redisHost = System.getenv("REDIS_HOST") ?: "127.0.0.1"
    val redisAuth = System.getenv("REDIS_PASSWORD") ?: null
    val redisRecordsKey = System.getenv("REDIS_RECORDS_KEY") ?: "vert.x.ms" // the redis hash

    val serviceDiscoveryOptions = ServiceDiscoveryOptions()

    discovery = ServiceDiscovery.create(vertx,
      serviceDiscoveryOptions.setBackendConfiguration(
        json {
          obj(
            "host" to redisHost,
            "port" to redisPort,
            "auth" to redisAuth,
            "key" to redisRecordsKey
          )
        }
      ))

    // microservice informations
    val haikunator = HaikunatorBuilder().setTokenLength(3).build()
    val niceName = haikunator.haikunate()

    val serviceName = "${System.getenv("SERVICE_NAME") ?: "the-plan"}-$niceName"
    val serviceHost = System.getenv("SERVICE_HOST") ?: "localhost" // domain name
    val servicePort = System.getenv("SERVICE_PORT")?.toInt() ?: 80 // servicePort: this is the visible port from outside
    val serviceRoot = System.getenv("SERVICE_ROOT") ?: "/api"


    // create the microservice record
    record = HttpEndpoint.createRecord(
      serviceName,
      serviceHost,
      servicePort,
      serviceRoot
    )
    // add metadata
    record?.metadata = json {
      obj(
        "kind" to "raider",
        "message" to "🚀 ready to fight",
        "basestar" to null,
        "coordinates" to obj(
          "x" to random(0.0, 400.0), "y" to random(0.0, 400.0)
        ),
        "app_id" to (System.getenv("APP_ID") ?: "🤖"),
        "instance_id" to (System.getenv("INSTANCE_ID") ?: "🤖"),
        "instance_type" to (System.getenv("INSTANCE_TYPE") ?: "production"),
        "instance_number" to (Integer.parseInt(System.getenv("INSTANCE_NUMBER") ?: "0"))
      )
    }

    /* 🤖 === health check === */
    healthCheck = HealthCheckHandler.create(vertx)
    healthCheck?.register("iamok",{ future ->
      discovery?.getRecord({ r -> r.registration == record?.registration}, {
        asyncRes ->
        when {
          asyncRes.failed() -> future.fail(asyncRes.cause())
          asyncRes.succeeded() -> future.complete(Status.OK())
        }
      })
    })

    println("🎃  " + record?.toJson()?.encodePrettily())

    /* 🚦 === Define a circuit breaker === */
    breaker = CircuitBreaker.create("bsg-circuit-breaker", vertx, CircuitBreakerOptions(
      maxFailures = 5,
      timeout = 20000,
      fallbackOnFailure = true,
      resetTimeout = 100000))
    
    /* === Define routes === */

    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    
    // call by a basestar
    router.post("/api/coordinates").handler { context ->
      
      // check data -> if null, don't move
      val computedX =  context.bodyAsJson.getDouble("x") ?: x
      val computedY =  context.bodyAsJson.getDouble("y") ?: y

      val computedXVelocity =  context.bodyAsJson.getDouble("xVelocity") ?: xVelocity
      val computedYVelocity =  context.bodyAsJson.getDouble("yVelocity") ?: yVelocity
      
      println("🚀 (${record?.name}) moves: $computedX - $computedY thx to ${baseStar?.record?.name}")

      /* 💾 === updating record of the service === */

      record?.metadata?.getJsonObject("coordinates")
        ?.put("x", computedX)
        ?.put("y",computedY)
        ?.put("xVelocity",computedXVelocity)
        ?.put("yVelocity",computedYVelocity)

      //record?.metadata?.put("basestar", baseStar?.record?.name)

      record?.metadata?.put("basestar", json {
        obj(
          "name:" to baseStar?.record?.name,
          "color" to baseStar?.record?.metadata?.get("color")
        )
      })

      discovery?.update(record, {asyncUpdateResult ->
        // foo
      })

      context.json(jsonObject=json {
        obj(
          "message" to "👍", "x" to computedX, "y" to computedY, "from" to record?.name
        )
      })

      /*
      context
        .response()
        .putHeader("content-type", "application/json;charset=UTF-8")
        .end(json {
          obj(
            "message" to "👍", "x" to computedX, "y" to computedY
          )
        }.toString())
      */

    }

    // use me with other microservices
    ServiceDiscoveryRestEndpoint.create(router, discovery) // ⚠️ ne pas oublier

    // link/bind healthCheck to a route
    router.get("/health").handler(healthCheck)

    router.route("/*").handler(StaticHandler.create())

    /* === Start the server === */
    val httpPort = System.getenv("PORT")?.toInt() ?: 8080

    
    vertx.createHttpServer(
      HttpServerOptions(
        port = httpPort
      ))
      .requestHandler {
        router.accept(it)
      }
      .listen { ar ->
        when {
          ar.failed() -> println("😡 Houston?")
          ar.succeeded() -> {
            println("😃 🌍 Raider started on $httpPort")

            /* 👋 === publish the microservice record to the discovery backend === */
            discovery?.publish(record, { asyncRes ->
              when {
                asyncRes.failed() ->
                  println("😡 Not able to publish the microservice: ${asyncRes.cause().message}")

                asyncRes.succeeded() -> {
                  println("😃 Microservice is published! ${asyncRes.result().registration}")
                  
                  /* 🤖 === search for a baseStar === */
                  searchAndSelectOneBaseStar()
                  
                } // ⬅️ succeed
              } // ⬅️ when
            }) // ⬅️ publish
          } // ⬅️ succeed
        } // ⬅️ when
      } // ⬅️ listen
  } // ⬅️ start()
} // ⬅️ class



/*
vertx.setPeriodic(1000, { id ->
  // This handler will get called every second
  println("timer fired!")
})

                  /*
                  var timerID = vertx.setTimer(2000, { id ->

                  })
                  */
 */