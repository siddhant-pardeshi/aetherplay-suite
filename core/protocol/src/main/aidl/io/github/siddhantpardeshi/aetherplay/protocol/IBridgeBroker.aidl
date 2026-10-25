package io.github.siddhantpardeshi.aetherplay.protocol;

import io.github.siddhantpardeshi.aetherplay.protocol.HandshakeInfo;
import io.github.siddhantpardeshi.aetherplay.protocol.GuestInfo;
import io.github.siddhantpardeshi.aetherplay.protocol.OpResult;
import io.github.siddhantpardeshi.aetherplay.protocol.SessionStatus;
import io.github.siddhantpardeshi.aetherplay.protocol.RegionInfo;
import io.github.siddhantpardeshi.aetherplay.protocol.SearchQuery;
import io.github.siddhantpardeshi.aetherplay.protocol.SearchHandle;
import io.github.siddhantpardeshi.aetherplay.protocol.SearchStatus;
import io.github.siddhantpardeshi.aetherplay.protocol.ResultPage;
import io.github.siddhantpardeshi.aetherplay.protocol.ValueSnapshot;
import io.github.siddhantpardeshi.aetherplay.protocol.WritePreview;
import io.github.siddhantpardeshi.aetherplay.protocol.RefineQuery;
import io.github.siddhantpardeshi.aetherplay.protocol.JournalEntry;
import io.github.siddhantpardeshi.aetherplay.protocol.FreezeEntry;

interface IBridgeBroker {
    HandshakeInfo handshake(String labBuild, int labProtocolMajor, int labProtocolMinor);
    List<GuestInfo> listGuests();
    OpResult startSession(String gameKey, String spaceKey);
    OpResult stopSession();
    SessionStatus getSessionStatus();
    List<RegionInfo> listRegions(int generation);
    SearchHandle startSearch(in SearchQuery query, int generation);
    SearchHandle refineSearch(long ticketId, in RefineQuery query, int generation);
    OpResult cancelOperation(long ticketId, int generation);
    SearchStatus getSearchStatus(long ticketId, int generation);
    ResultPage getResultPage(long ticketId, int offset, int count, int generation);
    List<ValueSnapshot> readValues(in long[] addresses, int valueType, int generation);
    WritePreview previewWrite(in long[] addresses, int valueType, String newValueText, int generation);
    OpResult commitWrite(long previewId, int generation);
    OpResult undoLastWrite(int generation);
    List<JournalEntry> listJournal(int generation);
    List<FreezeEntry> listFreezes(int generation);
    OpResult setFreeze(in long[] addresses, int valueType, String valueText, boolean enable, int generation);
    OpResult clearFreezes(int generation);
}
