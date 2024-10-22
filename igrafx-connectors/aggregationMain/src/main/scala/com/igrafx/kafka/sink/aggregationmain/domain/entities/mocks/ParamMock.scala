package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.Param

class ParamMock extends Mock[Param] {
  private var QUOTE: Boolean = true
  private var TEXT: String = "test"
  private var COLUMNID: Int = 0

  def setQuote(quote: Boolean): ParamMock = {
    this.QUOTE = quote
    this
  }

  def setText(text: String): ParamMock = {
    this.TEXT = text
    this
  }

  def setColumnId(columnId: Int): ParamMock = {
    this.COLUMNID = columnId
    this
  }

  override def build(): Param = {
    Param(QUOTE = QUOTE, TEXT = TEXT, COLUMNID = COLUMNID)
  }
}
