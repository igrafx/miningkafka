package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock

final class ValidActivityColumnMock extends Mock[ValidActivityColumn] {
  private var columnIndex: Index = new IndexMock().setIndex(1).build()

  def setColumnIndex(columnIndex: Index): ValidActivityColumnMock = {
    this.columnIndex = columnIndex
    this
  }

  override def build(): ValidActivityColumn = {
    ValidActivityColumn(columnIndex = columnIndex)
  }
}
