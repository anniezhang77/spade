package com.salesforce.mce.spade.orchard

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, Json}

import com.salesforce.mce.spade.SpadeContext
import com.salesforce.mce.spade.workflow.{WorkflowExpression, WorkflowGraph}

case class WorkflowRequest(
  name: String,
  activities: Seq[WorkflowRequest.Activity],
  resources: Seq[WorkflowRequest.Resource],
  // key depends on values
  dependencies: Map[String, Seq[String]]
)

object WorkflowRequest {

  case class Resource(
    id: String,
    name: String,
    resourceType: String,
    resourceSpec: Json,
    maxAttempt: Int
  )

  implicit val resourceEncoder: Encoder[Resource] = deriveEncoder
  implicit val resourceDecoder: Decoder[Resource] = deriveDecoder

  case class Activity(
    id: String,
    name: String,
    activityType: String,
    activitySpec: Json,
    resourceId: String,
    maxAttempt: Int
  )

  implicit val activityEncoder: Encoder[Activity] = deriveEncoder
  implicit val activityDecoder: Decoder[Activity] = deriveDecoder

  implicit val encoder: Encoder[WorkflowRequest] = deriveEncoder[WorkflowRequest]
  implicit val decoder: Decoder[WorkflowRequest] = deriveDecoder[WorkflowRequest]

  def apply(name: String, workflowExp: WorkflowExpression)(implicit
    ctx: SpadeContext
  ): WorkflowRequest = {
    val graph = WorkflowGraph(workflowExp)

    val activities = graph.activities.values.map { act =>
      Activity(
        act.id,
        act.name,
        act.activityType,
        act.activitySpec,
        act.runsOn.id,
        act.maxAttempt.getOrElse(ctx.maxAttempt)
      )
    }.toSeq

    val resources = graph.activities.values.map(_.runsOn).toSeq.distinctBy(_.id).map { r =>
      Resource(
        r.id,
        r.name,
        r.resourceType,
        r.resourceSpec,
        r.maxAttempt.getOrElse(ctx.maxAttempt)
      )
    }

    val dependencies = graph.flows.map(_.swap).groupMap(_._1)(_._2).view.mapValues(_.toSeq).toMap

    WorkflowRequest(name, activities, resources, dependencies)
  }
}
