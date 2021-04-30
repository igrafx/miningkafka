package com.logpickr.core.adapters.druid

import com.logpickr.core.adapters.druid.interfaces.DruidClient

object DruidClientInstance {
  val druidClient: DruidClient = new DruidClientImpl
}
