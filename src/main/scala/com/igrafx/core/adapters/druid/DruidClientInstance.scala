package com.igrafx.core.adapters.druid

import com.igrafx.core.adapters.druid.interfaces.DruidClient

object DruidClientInstance {
  val druidClient: DruidClient = new DruidClientImpl
}
