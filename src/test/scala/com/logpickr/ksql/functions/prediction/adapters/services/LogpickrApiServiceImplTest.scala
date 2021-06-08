package com.logpickr.ksql.functions.prediction.adapters.services

import core.UnitTestSpec

class LogpickrApiServiceImplTest extends UnitTestSpec {
  val logpickrApiServiceImpl = new LogpickrApiServiceImpl

  describe("setCaseIdsForQuery") {
    it("should create the query correctly") {
      assert("" == logpickrApiServiceImpl.setCaseIdsForQuery(Seq.empty))
      assert("caseIds[]=3" == logpickrApiServiceImpl.setCaseIdsForQuery(Seq("3")))
      assert("caseIds[]=3,5,7" == logpickrApiServiceImpl.setCaseIdsForQuery(Seq("3", "5", "7")))
    }
  }
}
