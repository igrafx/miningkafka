package com.igrafx.ksql.functions.prediction.adapters.services

import core.UnitTestSpec

class IGrafxApiServiceImplTest extends UnitTestSpec {
  val igrafxApiServiceImpl = new IGrafxApiServiceImpl

  describe("setCaseIdsForQuery") {
    it("should create the query correctly") {
      assert("" == igrafxApiServiceImpl.setCaseIdsForQuery(Seq.empty))
      assert("caseIds[]=3" == igrafxApiServiceImpl.setCaseIdsForQuery(Seq("3")))
      assert("caseIds[]=3,5,7" == igrafxApiServiceImpl.setCaseIdsForQuery(Seq("3", "5", "7")))
    }
  }
}
