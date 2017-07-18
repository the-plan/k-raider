package org.typeunsafe

import io.vertx.ext.web.client.WebClient
import io.vertx.servicediscovery.Record

class BaseStar constructor(val record: Record, val client: WebClient) {
  /*
    “val” means the corresponding property is generated for the constructor parameter.
  */
}
