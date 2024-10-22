package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.ColumnsNumber

class ColumnsNumberMock extends Mock[ColumnsNumber] {
  private var number: Int = 3

  def setNumber(number: Int): ColumnsNumberMock = {
    this.number = number
    this
  }

  override def build(): ColumnsNumber = {
    ColumnsNumber(number = number)
  }
}
