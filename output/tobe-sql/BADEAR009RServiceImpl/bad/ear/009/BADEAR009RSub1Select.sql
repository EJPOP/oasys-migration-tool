SELECT CVLCPT_ADT_YR /* 민원감사년도 */,
    ADT_NO /* 감사번호 */,
    DSPS_RQT_SQNO /* 처분요구순번 */,
    EXTT_NM AS EXTT_NM /* 적출자명 */,
    CTB_RT||'%' AS CTB_RT /* 기여비율 */
FROM TB_BADEAR004M WHERE 1=1 AND CVLCPT_ADT_YR = #CVLCPT_ADT_YR# /*인자값 1*/ AND ADT_NO = #ADT_NO# /*인자값 2*/ AND DSPS_RQT_SQNO = #DSPS_RQT_SQNO# /*인자값 3*/ ORDER BY EXTT_JBGD_SECD , EXTT_ID