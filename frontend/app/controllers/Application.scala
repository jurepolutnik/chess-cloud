package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def landing = Action {
    Ok(views.html.app.landing())
  }
  def index = Action {
    Ok(views.html.app.index())
  }
}