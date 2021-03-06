package com.changtu.service

import java.io.{BufferedWriter, OutputStreamWriter}

import akka.actor.Actor
import com.changtu.core.Host
import com.changtu.util.Logging
import com.changtu.util.hdfs.HDFSUtils
import com.changtu.util.host.{AES, Configuration, SSH}
import org.apache.hadoop.fs.Path
import org.joda.time.DateTime

import scala.util.{Failure, Success}

/**
  * Created by lubinsu on 8/22/2016.
  * 日志收集程序，通过指定的命令收集各个客户端的日志，通过akka实现并发操作
  */

class CollectLogService extends Actor with Logging {

  override def receive: Receive = {
    case Host(host, port, cmd) =>
      getLogs(host, port, cmd) match {
        case 0 => logger.info("success.")
        case _ => logger.error("error.")
      }
    case _ => logger.warn("unknown operation.")
  }


  /**
    * 根据shell命令收集指定主机上的日志
    *
    * @param host 需要收集的主机
    * @param port ssh端口号
    * @param cmd  执行命令
    * @return 返回执行后的状态码
    */
  private def getLogs(host: String, port: String, cmd: String): Int = {
    // 密码解密
    val password = AES.decrypt(Configuration("passwd").getString(host.concat("-hadoop")), "secretKey.changtu.com") match {
      case Success(encrypted) =>
        encrypted.asInstanceOf[String]
      case Failure(e) =>
        logger.error(e.getMessage)
        ""
    }

    val ssh = (cmd: String) => SSH(host, "hadoop", port.toInt, cmd, "", password, loadToHdfs)
    ssh(cmd)
  }

  /**
    * 收集到的日志处理方式
    * @param msg 传入一行行记录
    */
  private def loadToHdfs(msg: String, host: String): Unit = {
    //logger.info(msg)
    val currentTime = DateTime.now.toString("yyyyMMdd")
    val path = "/user/hadoop/bigdata/logs/rest.".concat(host).concat("-").concat(currentTime).concat(".log")
    HDFSUtils.createDirectory(path, deleteF = false)
    val fsout = HDFSUtils.getHdfs.append(new Path(path))

    val br = new BufferedWriter(new OutputStreamWriter(fsout))
    br.write(msg)
    br.newLine()
    br.close()
    fsout.close()
  }

  /**
    * 收集到的日志处理方式
    * @param msg 传入一行行记录
    */
  private def loadToKafka(msg: String, host: String): Unit = {
    //logger.info(msg)
    val currentTime = DateTime.now.toString("yyyyMMdd")
    val path = "/user/hadoop/bigdata/logs/rest.".concat(host).concat("-").concat(currentTime).concat(".log")
    HDFSUtils.createDirectory(path, deleteF = false)
    val fsout = HDFSUtils.getHdfs.append(new Path(path))

    val br = new BufferedWriter(new OutputStreamWriter(fsout))
    br.write(msg)
    br.newLine()
    br.close()
    fsout.close()
  }

}
