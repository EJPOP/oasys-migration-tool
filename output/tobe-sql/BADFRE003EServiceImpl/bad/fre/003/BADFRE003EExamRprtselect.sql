SELECT CVLCPT_ADT_YR /* 민원감사년도 */,
    ADT_NO /* 감사번호 */,
    DSPS_RQT_SQNO /* 처분요구순번 */,
    RVW_RPTP_DSPS_RQT_CN /* 검토보고서처분요구내용 */,
    RVW_RPTP_UNEX_RTWR_CN /* 검토보고서미집행경위내용 */,
    RVW_RPTP_UNEX_RSN_RVW_CN /* 검토보고서미집행사유검토내용 */,
    RVW_RPTP_ATRZ_STTS_YMD /* 검토보고서결재상태일자 */,
    RVW_RPTP_ATRZ_DOC_NO /* 검토보고서결재문서번호 */,
    RVW_RPTP_ATRZ_STTS_SECD /* 검토보고서결재상태구분코드 */,
    RVW_RPTP_WHOL_CN /* 검토보고서전체내용 */
FROM TB_BADEAR002D WHERE 1 = 1 AND CVLCPT_ADT_YR = #CVLCPT_ADT_YR# AND ADT_NO = #ADT_NO# AND DSPS_RQT_SQNO = #DSPS_RQT_SQNO#