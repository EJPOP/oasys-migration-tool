SELECT A.ADT_STP_SECD /* 감사단계구분코드 */,
    B.ADT_STP_NM /* 감사단계명 */,
    A.RPTP_NM /* 보고서명 */,
    A.RPTP_KDCD /* 보고서종류코드 */,
    A.ATRZ_DMND_ID /* 결재요청아이디 */,
    A.ADT_YR /* 감사년도 */,
    A.ADT_NO /* 감사번호 */,
    A.DOC_ID /* 문서아이디 */,
    A.ATRZ_STTS_SECD /* 결재상태구분코드 */,
    F_OPEN_EDIT(A.LNKG_URL_ADDR) AS F_OPEN_EDIT
FROM TB_BADDED103M A INNER JOIN TB_BADDED101M B ON A.ADT_YR = B.ADT_YR AND A.ADT_NO = B.ADT_NO AND A.ADT_STP_SECD = B.ADT_STP_SECD WHERE 1=1 AND A.ATRZ_DMND_ID IS NULL (#docInfoList[].audYr#,#docInfoList[].audNo#,#docInfoList[].audStepCd#,#docInfoList[].docId#)