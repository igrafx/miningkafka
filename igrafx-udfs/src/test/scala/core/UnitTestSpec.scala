package core

import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.{BeforeAndAfterAll, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar

class UnitTestSpec extends AsyncFunSpec with MockitoSugar with PrivateMethodTester with BeforeAndAfterAll
