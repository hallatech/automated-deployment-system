package com.spindrift.autodeploy.common
 
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

abstract class AutoDeployBase {
 
  Logger getLogger() 
  { 
  	Logging.getLogger(this.class) 
  }

}
