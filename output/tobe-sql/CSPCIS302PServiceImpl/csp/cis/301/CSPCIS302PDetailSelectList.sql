SELECT REG_YMD /* 등록일자 */,
    REG_NO /* 등록번호 */,
    ENAIS_PST_ANS_NMTM /* ENAIS게시물답변횟수 */,
    USER_HSTRY_SECD /* 사용자이력구분코드 */,
    TIT_ENN,
    ENTX,
    ECN,
    USR_EID,
    USER_NM /* 사용자명 */,
    ATT_FL_EID,
    ADT_PLAN_DOC_TPCD /* 감사계획문서유형코드 */,
    SLCT_STTS_YN /* 청탁상태여부 */
FROM TBCSPCIS003M WHERE 1=1 AND REG_YMD = #REG_YMD# AND REG_NO = #REG_NO# AND ENAIS_PST_ANS_NMTM = #ENAIS_PST_ANS_NMTM#