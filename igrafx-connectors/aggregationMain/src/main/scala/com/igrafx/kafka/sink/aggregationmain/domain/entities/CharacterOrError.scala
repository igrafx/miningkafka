package com.igrafx.kafka.sink.aggregationmain.domain.entities

import com.igrafx.core.exceptions.SafeException
import com.igrafx.kafka.sink.aggregationmain.Constants

sealed trait CharacterOrError

sealed trait CharacterError extends Exception with CharacterOrError

final case class DefaultCharacterException() extends CharacterOrError
final case class IncoherentCharacterException(message: String) extends SafeException(message) with CharacterError

final case class Character(character: String) extends CharacterOrError

object CharacterOrError {
  def apply(character: String): CharacterOrError = {
    character match {
      case Constants.columnMappingStringDefaultValue => DefaultCharacterException()
      case character if character.length != 1 =>
        IncoherentCharacterException(
          s"The value '$character' is not correct for a column mapping character, String length needs to be 1"
        )
      case _ => new Character(character)
    }
  }
}

object Character {
  private def apply(character: String): Character = {
    new Character(character)
  }
}
