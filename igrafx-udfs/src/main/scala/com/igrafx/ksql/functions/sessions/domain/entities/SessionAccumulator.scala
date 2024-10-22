package com.igrafx.ksql.functions.sessions.domain.entities

import org.apache.kafka.connect.data.Struct

final case class SessionAccumulator(
    startingSessionLine: String,
    isSessionStarted: Boolean,
    currentSessionId: String,
    pendingSessionList: Seq[Struct],
    groupList: Seq[Struct]
)
