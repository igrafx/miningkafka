package com.igrafx.kafka.sink.aggregationmain.domain.entities.mocks

import com.igrafx.core.Mock
import com.igrafx.kafka.sink.aggregationmain.domain.entities.Character

class CharacterMock extends Mock[Character] {
  private var character: String = ","

  def setCharacter(character: String): CharacterMock = {
    this.character = character
    this
  }

  override def build(): Character = {
    new Character(character = character)
  }
}
