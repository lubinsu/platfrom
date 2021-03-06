package com.changtu.api

import akka.actor.{ActorSystem, Props}
import com.changtu.core.Host
import com.changtu.service.CollectMonitor

/**
  * Created by lubinsu on 8/23/2016.
  * 主Actor
  */
object CollectLogs extends App {

  if (args.length < 2) {
    System.err.println("Usage: CollectLogs <hosts> <command>")
    System.exit(1)
  }

  val Array(hosts, cmd) = args

  val system = ActorSystem("CollectSystem")

  val monitor = system.actorOf(Props[CollectMonitor], name = "CollectMonitor-".concat(hosts))

  hosts.split(",").foreach( p => {
    // default Actor constructor
    monitor ! Host(p.split(":")(0), p.split(":")(1), cmd)
  })

}
