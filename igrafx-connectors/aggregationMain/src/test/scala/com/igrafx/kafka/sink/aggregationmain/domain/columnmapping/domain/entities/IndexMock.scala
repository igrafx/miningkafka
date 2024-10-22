package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock

final class IndexMock extends Mock[Index] {
  private var index: Int = 0

  def setIndex(index: Int): IndexMock = {
    this.index = index
    this
  }

  override def build(): Index = {
    new Index(index = index)
  }
}
