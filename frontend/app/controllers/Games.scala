package controllers

import java.io.File

import com.fasterxml.jackson.databind.JsonNode
import play.api._
import play.api.mvc._
import play.api.libs.json
import play.libs.Json
import scala.io.Source
import scala.util.Random
import scala.util.parsing.json.JSONObject
import play.api.libs.json.{JsObject, JsArray, JsValue}

/**
 * Created with IntelliJ IDEA.
 * User: xlab
 * Date: 15/01/14
 * Time: 16:43
 * To change this template use File | Settings | File Templates.
 */
object Games extends Controller {

  val games = {
    import play.api.Play.current
    val gamesFolder = Play.getFile("data/games")

    val files = gamesFolder.listFiles()
    files.map (f=>parsePgn(f))
  }

  val gamesList = "["+games.mkString(",")+"]"

  def parsePgn (file : File): JsonNode = {

    // Split on tags and moves
    val (tags, moves) = Source.fromFile(file).getLines().map(_.trim).filter(_.nonEmpty) span {
      line => line.startsWith("[")
    } match {
      case (tags, moves) => (tags.mkString("\n"), moves.mkString(" "))
    }

    // Parse tags
    val tagRegex = """\[(\w+) \"(.+)\"\]""".r
    val tagMap = tags.lines.map(line => line match {
      case tagRegex(attr, value) => (attr, value)
    } ).toList

    // Create JSON object
    val json = Json.newObject()

    // with Custom 6digits id - 100000 - 999999
    json.put("id", 100000 + Random.nextInt(900000))

    // with tags
    tagMap.foreach(t=> {json.put(t._1, t._2)})

    // with moves
    json.put("moves", moves)

    // with complete pgn
    json.put("pgn", tags +"\n"+ moves)

    json
  }

  def get (id: Int) = Action{
    games.find(_.get("id").asInt() == id) match {
      case Some(res) => Ok(res.toString)
      case None => Results.NotFound
    }
  }

  def list = Action{
    Ok (gamesList)
  }
}
