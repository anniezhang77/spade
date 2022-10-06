package com.salesforce.mce.spade

import com.typesafe.config.{Config, ConfigFactory}

class SpadeContext private (config: Config) {

  def maxAttempt = config.getInt("max-attempt")

}

object SpadeContext {

  val configPath = "com.salesforce.mce.spade"

  def withRootConfig(rootConfig: Config): SpadeContext =
    new SpadeContext(rootConfig.getConfig(configPath))

  def apply(): SpadeContext = withRootConfig(ConfigFactory.load())

}
