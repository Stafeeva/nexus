package ch.epfl.bluebrain.nexus.kg.directives

import java.time.Instant

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.syntax.show._
import ch.epfl.bluebrain.nexus.admin.projects.Project
import ch.epfl.bluebrain.nexus.iam.types.Identity.Anonymous
import ch.epfl.bluebrain.nexus.kg.TestHelper
import ch.epfl.bluebrain.nexus.kg.directives.PathDirectives._
import ch.epfl.bluebrain.nexus.rdf.Iri
import ch.epfl.bluebrain.nexus.rdf.Iri.AbsoluteIri
import ch.epfl.bluebrain.nexus.delta.config.Vocabulary.nxv
import ch.epfl.bluebrain.nexus.util.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ch.epfl.bluebrain.nexus.admin.types.ResourceF

class PathDirectivesSpec
    extends AnyWordSpecLike
    with Matchers
    with ScalatestRouteTest
    with EitherValues
    with TestHelper {

  "A PathDirectives" should {
    val mappings         = Map(
      "nxv"   -> Iri.absolute("https://bluebrain.github.io/nexus/vocabulary/").rightValue,
      "a"     -> Iri.absolute("https://www.w3.org/1999/02/22-rdf-syntax-ns#type").rightValue,
      "alias" -> Iri.absolute("https://www.w3.org/1999/02").rightValue
    )
    // format: off
    implicit val project = ResourceF(genIri, genUUID, 1L, deprecated = false, Set.empty, Instant.EPOCH, Anonymous, Instant.EPOCH, Anonymous, Project("project", genUUID, "organization", None, mappings, Iri.absolute("http://example.com/base/").rightValue, genIri))
    // format: on

    def route(): Route =
      (get & pathPrefix(IdSegment)) { iri =>
        complete(StatusCodes.OK -> iri.show)
      }

    def routeIsSegment(iri: AbsoluteIri): Route =
      (get & isIdSegment(iri) & pathPrefix(Segment) & pathEndOrSingleSlash) { s =>
        complete(StatusCodes.OK -> s)
      }

    "pass a route with a specific segment id" in {
      Get("/nxv:rev/other") ~> routeIsSegment(nxv.rev.value) ~> check {
        responseAs[String] shouldEqual "other"
      }
    }

    "do not pass a route when a specific segment id does not match" in {
      Get("/nxv:rev/other") ~> routeIsSegment(nxv.results.value) ~> check {
        handled shouldEqual false
      }
    }

    "expand a curie" in {
      Get("/alias:%2Frev/other") ~> route() ~> check {
        responseAs[String] shouldEqual "https://www.w3.org/1999/02/rev"
      }
    }

    "replace an alias" in {
      Get("/a") ~> route() ~> check {
        responseAs[String] shouldEqual "https://www.w3.org/1999/02/22-rdf-syntax-ns#type"
      }
    }

    "matches an absoluteIri" in {
      Get("/c:d") ~> route() ~> check {
        responseAs[String] shouldEqual "c:d"
      }
    }

    "append the uuid to the base" in {
      Get("/uuid") ~> route() ~> check {
        responseAs[String] shouldEqual "http://example.com/base/uuid"
      }
    }

    "do not match a wrong iri" in {
      Get("/?nds?i=ad?dsd") ~> route() ~> check {
        handled shouldEqual false
      }
    }

  }

}
