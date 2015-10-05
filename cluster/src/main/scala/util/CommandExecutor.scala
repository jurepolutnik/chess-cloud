package util

/**
 * Created by jure on 8/3/14.
 */

class CommandExecutor (command:String, out:String=>Unit, err:String=>Unit, onExit:Int=>Unit) {

  import scala.sys.process._
  import scala.io._
  import java.io._
  import scala.concurrent._
  //import scala.concurrent.ops.spawn

  val inputStream = new SyncVar[OutputStream];

  val process = Process(command).run(
    new ProcessIO(
      stdin => inputStream.put(stdin),
      stdout => Source.fromInputStream(stdout).getLines.foreach(out),
      stderr => Source.fromInputStream(stderr).getLines.foreach(err)));

  //spawn { onExit(process.exitValue()) }

  def write(s:String):Unit = synchronized {
    inputStream.get.write((s + "\n").getBytes)
    inputStream.get.flush
  }

  def close():Unit = {
    inputStream.get.close
  }
}
