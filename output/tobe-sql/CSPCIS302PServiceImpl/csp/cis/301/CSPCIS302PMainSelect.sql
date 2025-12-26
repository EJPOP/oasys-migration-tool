SELECT A.REG_YMD /* 등록일자 */,
    A.REG_NO /* 등록번호 */,
    A.ENAIS_PST_ANS_NMTM /* ENAIS게시물답변횟수 */,
    A.USER_HSTRY_SECD /* 사용자이력구분코드 */,
    A.TIT_ENN,
    A.ENTX,
    A.ECN,
    A.USR_EID,
    A.USER_NM /* 사용자명 */,
    A.ATT_FL_EID,
    A.ADT_PLAN_DOC_TPCD /* 감사계획문서유형코드 */,
    A.SLCT_STTS_YN /* 청탁상태여부 */
FROM TBCSPCIS003M A WHERE 1=1 AND (A.ECN = #cnb# OR (A.REG_YMD , A.REG_NO) IN (SELECT A.REG_YMD /* 등록일자 */,
    A.REG_NO /* 등록번호 */
FROM TBCSPCIS003M A WHERE A.ECN = #cnb#)) ORDER BY REG_YMD DESC, REG_NO DESC, ENAIS_PST_ANS_NMTM