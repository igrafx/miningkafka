package com.igrafx.core

trait Mock[T] {
  def build(): T
}
