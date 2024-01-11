package com.finance.Query

import cats.effect.{IO, Resource}
import com.google.cloud.bigtable.data.v2.BigtableDataClient
import com.google.cloud.bigtable.data.v2.models.{Query, RowMutation}
import com.google.protobuf.ByteString
object bigTable {

  def readToTable(data: (String, String))(using db: Resource[IO, BigtableDataClient]): IO[Unit] = {
    db.use{ xa =>
      val readRowsRequest = Query.create("transactions").limit(10)
      val query = Query.create("transactions").prefix("phone")
      IO(xa.readRows(query).forEach(b => println(b.getCells("cf1").get(0))))
    }
  }

  def writeToTable(data: (String, String))(using db: Resource[IO, BigtableDataClient]): IO[Unit] = {
    db.use { xa =>
      val rowMutation = RowMutation.create("transactions", s"phone-${data._1}").setCell("user", ByteString.copyFromUtf8("cf1"), ByteString.copyFromUtf8(data._2))
      IO(xa.mutateRow(rowMutation))

    }
  }

}
