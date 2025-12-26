SELECT T1.ADT_YR /* 감사년도 */,
    T1.ADT_NO /* 감사번호 */,
    T1.DOC_ID /* 문서아이디 */,
    T1.ADT_KDCD /* 감사종류코드 */,
    T1.ADT_GRNT_NO /* 감사부여번호 */,
    T1.RPTP_NM /* 보고서명 */,
    T1.RPTP_KDCD /* 보고서종류코드 */,
    T1.LNKG_URL_ADDR /* 연결URL주소 */,
    T1.RFRNC_ID /* 참조아이디 */,
    T1.RFRNC_YMD /* 참조일자 */
FROM TB_BADDED103M T1 INNER JOIN TB_BADDED109M T2 ON (T1.DOC_ID = T2.DOC_ID) WHERE T1.ADT_YR = #CVLCPT_ADT_YR# AND T1.ADT_NO = #ADT_NO# AND T2.ATRZ_DMND_ID = #ATRZ_DMND_ID# AND T1.RPTP_KDCD NOT IN ('A10600200')