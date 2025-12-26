SELECT CVLCPT_ADT_YR /* 민원감사년도 */,
    ADT_NO /* 감사번호 */,
    ADT_STP_SECD /* 감사단계구분코드 */,
    CVLCPT_DOC_ID /* 민원문서아이디 */,
    TPOF_RFRNC_ID /* 제보참조아이디 */,
    TPOF_RFRNC_YMD /* 제보참조일자 */,
    SLCT_RPTP_KDCD /* 청탁보고서종류코드 */,
    ATRZ_STTS_SECD /* 결재상태구분코드 */,
    ADT_RPTP_SECD /* 감사보고서구분코드 */,
    NVL(LAST_ADT_RPTP_YN,'N') AS LAST_ADT_RPTP_YN /* 최종감사보고서여부 */
FROM TB_BADDED103M WHERE ATRZ_DMND_ID = #ATRZ_DMND_ID#