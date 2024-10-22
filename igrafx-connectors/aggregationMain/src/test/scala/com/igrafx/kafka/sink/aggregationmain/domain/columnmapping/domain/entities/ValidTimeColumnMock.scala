package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock

final class ValidTimeColumnMock extends Mock[ValidTimeColumn] {
  private var columnIndex: Index = new IndexMock().setIndex(2).build()
  private var format: String = "dd/MM/yy HH:mm"

  def setColumnIndex(columnIndex: Index): ValidTimeColumnMock = {
    this.columnIndex = columnIndex
    this
  }

  def setFormat(format: String): ValidTimeColumnMock = {
    this.format = format
    this
  }

  override def build(): ValidTimeColumn = {
    ValidTimeColumn(columnIndex = columnIndex, format = format)
  }
}
