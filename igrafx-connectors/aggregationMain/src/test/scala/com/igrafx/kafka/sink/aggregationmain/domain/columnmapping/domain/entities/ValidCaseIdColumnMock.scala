package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock

final class ValidCaseIdColumnMock extends Mock[ValidCaseIdColumn] {
  private var columnIndex: Index = new IndexMock().setIndex(0).build()

  def setColumnIndex(columnIndex: Index): ValidCaseIdColumnMock = {
    this.columnIndex = columnIndex
    this
  }

  override def build(): ValidCaseIdColumn = {
    ValidCaseIdColumn(columnIndex = columnIndex)
  }
}
