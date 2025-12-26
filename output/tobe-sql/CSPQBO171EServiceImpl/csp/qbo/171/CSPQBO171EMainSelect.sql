SELECT A.RCMD_BOO_SLN /* 추천도서연번 */,
    A.BOO_TIT_NM /* 도서제목명 */,
    A.RSCH_RPTP_AUT_NM /* 연구보고서저자명 */,
    F_USR_INFO(A.RCMR_CNB,'2') AS F_USR_INFO /* 추천자명 */,
    A.BOO_RCMD_TXT /* 도서추천내용 */,
    A.BOO_RCMD_RMK /* 도서추천비고 */,
    A.RCMR_CNB /* 추천자전산번호 */,
    A.CVLCPT_DOC_ID /* 민원문서아이디 */
FROM TBCSPQBO005M A /*추천도서기본*/ WHERE 1=1 AND (A.BOO_TIT_NM LIKE '%'||#strSEARCH_NM#||'%' OR A.RSCH_RPTP_AUT_NM LIKE '%'||#strSEARCH_NM#||'%' OR A.RCMR_CNB IN (SELECT TRGT_NOP_ENO /* 대상인원직원전산번호 */
FROM TBDCMACM001M WHERE USER_NM LIKE '%'||#strSEARCH_NM#||'%')) ORDER BY TO_NUMBER(A.RCMD_BOO_SLN) DESC