package com.igrafx.kafka.sink.aggregationmain.domain.columnmapping.domain.entities

import com.igrafx.core.Mock

final class ValidGroupedTasksColumnsMock extends Mock[ValidGroupedTasksColumns] {
  private var groupedTasksColumns: Set[Index] =
    Set(new IndexMock().setIndex(1).build(), new IndexMock().setIndex(2).build())

  def setGroupedTasksColumns(groupedTasksColumns: Set[Index]): ValidGroupedTasksColumnsMock = {
    this.groupedTasksColumns = groupedTasksColumns
    this
  }

  override def build(): ValidGroupedTasksColumns = {
    ValidGroupedTasksColumns(groupedTasksColumns = groupedTasksColumns)
  }
}
